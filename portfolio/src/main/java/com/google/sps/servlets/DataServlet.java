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
import com.google.gson.Gson;
import java.util.Arrays;

/** Servlet that returns some comments.*/
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  /* Show comments. */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    //Get saved comments.
    List<String> comments = new ArrayList<>();
    Query query = new Query("Comment").addSort("timestamp", SortDirection.ASCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    
    for (Entity entity : results.asIterable()) {
      String comment = (String) entity.getProperty("text"); // Comment text.
      comments.add(comment);
    }

    String json = new Gson().toJson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /* Store comments. */
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
    }
    response.sendRedirect("/comments.html");
  }
}