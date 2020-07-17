package com.google.sps.servlets;

/** Class to simplify image url/label association. */
public class ImagePair {
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