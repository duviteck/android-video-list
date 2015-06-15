package com.duviteck.tangolistview.entity;

import java.net.URI;

public class CachedVideoEntity extends VideoEntity {
    private URI pathToVideo;

    public CachedVideoEntity(String url, String title, URI pathToVideo) {
        super(url, title);
        this.pathToVideo = pathToVideo;
    }

    public URI getPathToVideo() {
        return pathToVideo;
    }
}
