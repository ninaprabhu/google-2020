// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import java.util.Arrays;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import java.io.PrintWriter;
import org.json.simple.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;

/** Class to simplify image url/label association. */
final class ImagePair {
    private final String url;
    private final String label;

    public ImagePair(String url, String label) {
        this.url = url;
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }
}

/** Servlet that stores and shows images and comments.*/
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private String uploadUrl; 

  @Override
  /* Show comments and images. */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    List<String> comments = new ArrayList<>();
    Query queryComment = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery resultsComment = datastore.prepare(queryComment);
    
    for (Entity entity : resultsComment.asIterable()) {
      String comment = (String) entity.getProperty("text"); // Comment text.
      comments.add(comment);
    }

    List<String> images = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    Query queryImage = new Query("Image").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery resultsImage = datastore.prepare(queryImage);
    


    for (Entity entity : resultsImage.asIterable()) {
      String img = (String) entity.getProperty("url"); // Image URL.
      images.add(img);
      String label = (String) entity.getProperty("label"); // Top image label.
      labels.add(label);
    }    

    String json = new Gson().toJson(comments);
    String url = new Gson().toJson(images);
    String strLabels = new Gson().toJson(labels);

    JSONObject obj = new JSONObject();
    obj.put("comments", json);
    obj.put("url", url);
    obj.put("labels", strLabels);
    response.setContentType("application/json;");
    response.getWriter().println(obj);
  }

  /* Store comments and images. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = request.getParameter("text-container");
    String[] array = text.split("\\s*,\\s*"); // Comments formatted as "comment1, comment2."

    boolean save = Boolean.parseBoolean(request.getParameter("save"));
    if (save) { // Only store if we indicated.
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        long timestamp = System.currentTimeMillis();
        for(String s:array) {
            Entity comment = new Entity("Comment");
            comment.setProperty("text", s);
            comment.setProperty("timestamp", timestamp);
            datastore.put(comment);
        }
        ImagePair pair = getUploadedFileUrl(request, "image");
        Entity img = new Entity("Image");
        img.setProperty("url", pair.getUrl());
        img.setProperty("label", pair.getLabel());
        img.setProperty("timestamp", timestamp);
        datastore.put(img);
    }    
    response.sendRedirect("/comments.html");
  }

    /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private ImagePair getUploadedFileUrl(HttpServletRequest request, String formInputElementName) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // Get the top labels of the image that the user uploaded.
    // TODO: Potentially get all labels
    byte[] blobBytes = getBlobBytes(blobKey);
    EntityAnnotation label;
    String imageLabel;
    try {
      label = getImageLabels(blobBytes).get(0); 
      imageLabel = label.getDescription();
    } catch (Exception e) { //Catch if we run into null.get issue
      return null;
    }

    
    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // We could check the validity of the file here, e.g. to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must use the relative
    // path to the image, rather than the path returned by imagesService which contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return new ImagePair(url.getPath(), imageLabel);
    } catch (MalformedURLException e) {
      return new ImagePair(imagesService.getServingUrl(options), imageLabel);
    }
  }

  /**
   * Blobstore stores files as binary data. This function retrieves the binary data stored at the
   * BlobKey parameter.
   */
  private byte[] getBlobBytes(BlobKey blobKey) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    int fetchSize = BlobstoreService.MAX_BLOB_FETCH_SIZE;
    long currentByteIndex = 0;
    boolean continueReading = true;
    while (continueReading) {
      // End index is inclusive, so we have to subtract 1 to get fetchSize bytes.
      byte[] b =
          blobstoreService.fetchData(blobKey, currentByteIndex, currentByteIndex + fetchSize - 1);
      outputBytes.write(b);

      // If we read fewer bytes than we requested, then we reached the end.
      if (b.length < fetchSize) {
        continueReading = false;
      }

      currentByteIndex += fetchSize;
    }

    return outputBytes.toByteArray();
  }

  /**
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the image
   * represented by the binary data stored in imgBytes.
   */
  private List<EntityAnnotation> getImageLabels(byte[] imgBytes) throws IOException {
    ByteString byteString = ByteString.copyFrom(imgBytes);
    Image image = Image.newBuilder().setContent(byteString).build();

    Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    requests.add(request);

    ImageAnnotatorClient client = ImageAnnotatorClient.create();
    BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
    client.close();
    List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
    AnnotateImageResponse imageResponse = imageResponses.get(0);

    if (imageResponse.hasError()) {
      System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
      return null;
    }

    return imageResponse.getLabelAnnotationsList();
  }
}