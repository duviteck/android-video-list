package com.duviteck.tangolistview.api.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by duviteck on 13/06/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoEntityResponse implements Parcelable {
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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(title);
    }

    public static final Creator<VideoEntityResponse> CREATOR = new Creator<VideoEntityResponse>() {
        public VideoEntityResponse createFromParcel(Parcel source) {
            return new VideoEntityResponse(source);
        }

        public VideoEntityResponse[] newArray(int size) {
            return new VideoEntityResponse[size];
        }
    };
}
