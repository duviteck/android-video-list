package com.duviteck.tangolistview.entity;

/**
 * Created by duviteck on 14/06/15.
 */
public class LoadingVideoEntity extends VideoEntity {
    private int loadingProgress;

    public LoadingVideoEntity(String url, String title) {
        super(url, title);
    }

    public int getLoadingProgress() {
        return loadingProgress;
    }

    public void setLoadingProgress(int loadingProgress) {
        this.loadingProgress = loadingProgress;
    }
}
