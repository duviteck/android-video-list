package com.duviteck.tangolistview.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.duviteck.tangolistview.api.response.VideoEntityResponse;
import com.duviteck.tangolistview.provider.DatabaseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.duviteck.tangolistview.provider.VideoContentProvider.VIDEO_LIST_URI;

/**
 * Created by duviteck on 13/06/15.
 */
public class DataLoaderService extends IntentService {
    private static final String SERVICE_NAME = "DataLoaderService";
    private static final String TAG = SERVICE_NAME;

    public static final String LOAD_VIDEO_LIST_ACTION = "loadVideoList";    // loads video list + all videos from list
    public static final String LOAD_VIDEO_ACTION = "loadVideo";
    public static final String LOAD_VIDEO_PROGRESS_ACTION = "loadVideoProgress";
    public static final String CLEAR_UNFINISHED_DOWNLOADS_ACTION = "clearUnfinishedDownloads";

    public static final String SUCCESS_EXTRA = "success";
    public static final String VIDEO_LIST_EXTRA = "extraVideoList";
    public static final String VIDEO_URL_EXTRA = "extraVideoUrl";
    public static final String VIDEO_LOAD_PROGRESS_EXTRA = "extraVideoLoadProgress";

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

        // TODO: clear all pending intents from working queue (possible only in own Service)

        VideoEntityResponse[] videos;
        List<String> videoUrls = null;
        boolean success = false;
        try {
            videos = loadVideoList();

            // insert videos to db
            DatabaseUtils.insertVideos(this, videos);

            // set SHOW flag for loaded videos
            videoUrls = getVideoUrls(videos);
            DatabaseUtils.setShowFlag(this, videoUrls);

            success = true;
        } catch (IOException e) {
            Log.w(TAG, "can't load video list");
        }

        notifyVideoListLoad(success);

        // request video loading for videos from loaded list
        if (success && videoUrls != null) {
            for (String url : videoUrls) {
                requestLoadVideo(url);
            }
        }
    }

    private void handleVideoAction(Intent intent) {
        String videoUrl = intent.getStringExtra(VIDEO_URL_EXTRA);
        Log.i(TAG, "handle Video action, load [url]:" + videoUrl);

        if (isVideoAlreadyLoading(videoUrl)) {  // TODO: implement multithreading loading
            Log.i(TAG, "video is already loading, [url]:" + videoUrl);
            return;
        }

        try {
            // mark current video as loading
            DatabaseUtils.updateLoadProgress(this, videoUrl, 1, 0);
            loadVideo(videoUrl);
        } catch (IOException e) {
            Log.w(TAG, "can't load video [url]:" + videoUrl);
            // unmark current video as loading
            DatabaseUtils.updateLoadProgress(this, videoUrl, 0, 0);
            notifyVideoLoadFailed(videoUrl);
        }
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
            output = openFileOutput(URLEncoder.encode(url), MODE_PRIVATE);     // TODO: add comments about CacheDir

            byte buffer[] = new byte[1024];

            long totalSize = response.body().contentLength();
            long loadedSize = 0;
            int oldProgress = 0;
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);

                loadedSize += count;
                int newProgress = (int)(loadedSize * 100 / totalSize);
                if (newProgress > oldProgress) {
                    oldProgress = newProgress;
                    DatabaseUtils.updateLoadProgress(this, url, totalSize, loadedSize);
                    notifyLoadingProgress(url, newProgress);
                }
            }

            output.flush();
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    private boolean isVideoAlreadyLoading(String videoUrl) {
        long videoTotalSize = DatabaseUtils.getVideoTotalSize(this, videoUrl);
        if (videoTotalSize == -1) {
            throw new IllegalStateException("This situation should never happen");
        }
        return videoTotalSize > 0;
    }

    private void notifyVideoListLoad(boolean success) {
        if (success) {
            getContentResolver().notifyChange(VIDEO_LIST_URI, null);
        } else {
            // TODO: implement it correctly
            // notify about loading video list via LocalBroadcast
//            Intent intent = new Intent(LOAD_VIDEO_LIST_ACTION);
//            intent.putExtra(SUCCESS_EXTRA, false);
//            sendLocalBroadcast(intent);
        }
    }

    private void notifyLoadingProgress(String url, int progress) {
        getContentResolver().notifyChange(VIDEO_LIST_URI, null);
//        // notify about loading progress via LocalBroadcast
//        Intent intent = new Intent(LOAD_VIDEO_PROGRESS_ACTION);
//        intent.putExtra(VIDEO_URL_EXTRA, url);
//        intent.putExtra(VIDEO_LOAD_PROGRESS_EXTRA, progress);
//        sendLocalBroadcast(intent);
    }

    private void notifyVideoLoadFailed(String url) {
        // TODO: refactor it
        // notify about video loading finish
//        Intent intent = new Intent(LOAD_VIDEO_ACTION);
//        intent.putExtra(SUCCESS_EXTRA, false);    // helps to differ fail and success
//        intent.putExtra(VIDEO_URL_EXTRA, url);
//        sendLocalBroadcast(intent);
    }

    private void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);  // TODO: ordered broadcast?
    }

    public static Uri getVideoUri(Context context, String videoUrl) {
        return Uri.fromFile(new File(context.getFilesDir(), URLEncoder.encode(videoUrl)));
    }

    private List<String> getVideoUrls(VideoEntityResponse[] videos) {
        List<String> urls = new ArrayList<>(videos.length);
        for (VideoEntityResponse video : videos) {
            urls.add(video.getUrl());
        }
        return urls;
    }

    private void requestLoadVideo(String url) {
        Intent intent = new Intent(this, DataLoaderService.class);
        intent.setAction(DataLoaderService.LOAD_VIDEO_ACTION);
        intent.putExtra(DataLoaderService.VIDEO_URL_EXTRA, url);
        startService(intent);
    }
}
