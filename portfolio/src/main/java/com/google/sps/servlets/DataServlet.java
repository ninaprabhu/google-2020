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



/** Servlet that stores and shows images and comments.*/
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private String uploadUrl; 

  @Override
  /* Show comments and images. */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    List<String> comments = new ArrayList<>();
    Query queryComment = new Query("Comment").addSort("timestamp", SortDirection.ASCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery resultsComment = datastore.prepare(queryComment);
    
    for (Entity entity : resultsComment.asIterable()) {
      String comment = (String) entity.getProperty("text"); // Comment text.
      comments.add(comment);
    }

    List<String> images = new ArrayList<>();
    Query queryImage = new Query("Image").addSort("timestamp", SortDirection.ASCENDING);
    PreparedQuery resultsImage = datastore.prepare(queryImage);
    
    for (Entity entity : resultsImage.asIterable()) {
      String img = (String) entity.getProperty("url"); // Image URL.
      images.add(img);
    }    

    String json = new Gson().toJson(comments);
    String url = new Gson().toJson(images);
    JSONObject obj = new JSONObject();
    obj.put("comments", json);
    obj.put("url", url);
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
        String imageUrl = getUploadedFileUrl(request, "image");
        Entity img = new Entity("Image");
        img.setProperty("url", imageUrl);
        img.setProperty("timestamp", timestamp);
        datastore.put(img);
    }    
    response.sendRedirect("/comments.html");
  }

    /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

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
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    }
  }
}
