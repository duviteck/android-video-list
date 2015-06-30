package com.duviteck.tangolistview.api.response;

import android.os.Parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by duviteck on 13/06/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoEntityResponse {
    @JsonProperty("mp4Url")
    private String url;
    private String title;
    private String thumb;

    public VideoEntityResponse() {
    }

    public VideoEntityResponse(Parcel in) {
        this.url = in.readString();
        this.title = in.readString();
        this.thumb = in.readString();
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
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
