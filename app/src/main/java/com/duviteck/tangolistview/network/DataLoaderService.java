package com.duviteck.tangolistview.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.duviteck.tangolistview.api.response.VideoEntityResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by duviteck on 13/06/15.
 */
public class DataLoaderService extends IntentService {
    private static final String SERVICE_NAME = "DataLoaderService";
    private static final String TAG = SERVICE_NAME;

    public static final String LOAD_VIDEO_LIST_ACTION = "loadVideoList";
    public static final String LOAD_VIDEO_LIST_FAILED_ACTION = "loadVideoListFailed";
    public static final String LOAD_VIDEO_ACTION = "loadVideo";
    public static final String LOAD_VIDEO_FAILED_ACTION = "loadVideoFailed";
    public static final String LOAD_VIDEO_PROGRESS_ACTION = "loadVideoProgress";
    public static final String CLEAR_UNFINISHED_DOWNLOADS_ACTION = "clearUnfinishedDownloads";

    public static final String VIDEO_LIST_EXTRA = "extraVideoList";
    public static final String VIDEO_URL_EXTRA = "extraVideoUrl";
    public static final String VIDEO_LOAD_PROGRESS_EXTRA = "extraVideoLoadProgress";

    private static final String LOADING_VIDEO_POSTFIX = ".part";

    public DataLoaderService() {
        super(SERVICE_NAME);
    }

    public DataLoaderService(String name) {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case LOAD_VIDEO_LIST_ACTION:
                handleVideoListAction();
                break;
            case LOAD_VIDEO_ACTION:
                handleVideoAction(intent);
                break;
            case CLEAR_UNFINISHED_DOWNLOADS_ACTION:
                handleClearUnfinishedDownloadsAction();
                break;
            default:
                Log.w(TAG, "Unknown action: " + intent.getAction());
        }
    }

    private void handleVideoListAction() {
        Log.i(TAG, "handle VideoList action");

        VideoEntityResponse[] videos = null;
        try {
            videos = loadVideoList();
        } catch (IOException e) {
            Log.w(TAG, "can't load video list");
        }

        notifyAboutVideoListLoad(videos);
    }

    private void handleVideoAction(Intent intent) {
        String videoUrl = intent.getStringExtra(VIDEO_URL_EXTRA);
        if (videoUrl == null) {
            Log.w(TAG, "handle Video action without specified url");
            return;
        } else {
            Log.i(TAG, "handle Video action, [url]:" + videoUrl);
        }

        boolean isSuccessfulLoad = true;
        try {
            loadVideo(videoUrl);
        } catch (IOException e) {
            Log.w(TAG, "can't load video [url]:" + videoUrl);
            isSuccessfulLoad = false;
        }

        notifyLoadFinished(videoUrl, isSuccessfulLoad);
    }

    private void handleClearUnfinishedDownloadsAction() {
        // TODO: implement it
    }

    private VideoEntityResponse[] loadVideoList() throws IOException {
        OkHttpClient client = new OkHttpClient();   // TODO: think about reusing
        String url = Endpoint.getVideoListUrl();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();      // TODO: check response.isSuccessful
        return new ObjectMapper().readValue(response.body().bytes(), VideoEntityResponse[].class);
    }

    private void loadVideo(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();   // TODO: think about reusing
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        InputStream input = null;
        OutputStream output = null;
        try {
            input = new BufferedInputStream(response.body().byteStream());
            output = new FileOutputStream(getPathToVideo(this, url) + LOADING_VIDEO_POSTFIX);

            byte data[] = new byte[1024];

            long totalLength = response.body().contentLength();
            long totalLoaded = 0;
            int oldProgress = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);

                totalLoaded += count;
                int newProgress = (int)(totalLoaded * 100 / totalLength);
                if (newProgress > oldProgress) {
                    oldProgress = newProgress;
                    notifyLoadingProgress(url, newProgress);
                }
            }

            output.flush();
            input.close();
            output.close();

            // rename file (remove temporal postfix)
            File from = new File(getPathToVideo(this, url) + LOADING_VIDEO_POSTFIX);
            File to = new File(getPathToVideo(this, url));
            boolean renameSuccess = from.renameTo(to);
            if (!renameSuccess) {
                throw new IOException("Can't rename temporal file to target file");
            }
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    private void notifyAboutVideoListLoad(VideoEntityResponse[] videos) {
        // notify about loading video list via LocalBroadcast
        Intent intent = new Intent();
        if (videos == null) {
            // fail
            intent.setAction(LOAD_VIDEO_LIST_FAILED_ACTION);
        } else {
            // success
            intent.setAction(LOAD_VIDEO_LIST_ACTION);
            intent.putExtra(VIDEO_LIST_EXTRA, videos);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyLoadingProgress(String url, int progress) {
        // notify about loading progress via LocalBroadcast
        Intent intent = new Intent(LOAD_VIDEO_PROGRESS_ACTION);
        intent.putExtra(VIDEO_URL_EXTRA, url);
        intent.putExtra(VIDEO_LOAD_PROGRESS_EXTRA, progress);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyLoadFinished(String url, boolean successful) {
        // notify about video loading finish
        Intent intent = new Intent(successful ? LOAD_VIDEO_ACTION : LOAD_VIDEO_FAILED_ACTION);
        intent.putExtra(VIDEO_URL_EXTRA, url);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static String getPathToVideo(Context context, String videoName) {
        return context.getFilesDir() + File.separator + videoName;
    }
}
