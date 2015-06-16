package com.duviteck.tangolistview.api.response;

import android.os.Parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by duviteck on 13/06/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoEntityResponse {
    private String url;
    private String title;

    public VideoEntityResponse() {
    }

    public VideoEntityResponse(Parcel in) {
        this.url = in.readString();
        this.title = in.readString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
