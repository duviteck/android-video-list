package com.duviteck.tangolistview.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoListResponse {
    @JsonProperty("VideoList")
    VideoEntityResponse[] videoList;

    public VideoListResponse() {
    }

    public VideoEntityResponse[] getVideoList() {
        return videoList;
    }

    public void setVideoList(VideoEntityResponse[] videoList) {
        this.videoList = videoList;
    }
}
