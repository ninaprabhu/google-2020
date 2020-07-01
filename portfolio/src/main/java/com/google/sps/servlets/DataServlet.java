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
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import java.util.Arrays;


/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /* Print running list of comments, save history */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = request.getParameter("text-container");
    String[] array = text.split("\\s*,\\s*"); //comment1, comment2
    boolean save = Boolean.parseBoolean(request.getParameter("save"));
    if (!save){
        return; //no datastore necessary if we don't check the save box
    }
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    for(String s:array) {
        Entity comment = new Entity("Comment");
        comment.setProperty("text", s);
        comment.setProperty("timestamp", System.currentTimeMillis());
        datastore.put(comment);
    }
    long timestamp = System.currentTimeMillis();
  }
}
