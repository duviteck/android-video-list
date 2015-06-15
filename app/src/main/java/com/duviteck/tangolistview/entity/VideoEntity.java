package com.duviteck.tangolistview.entity;

public class VideoEntity {
    protected String url;
    protected String title;

    public VideoEntity(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }
}
