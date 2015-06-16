package com.duviteck.tangolistview.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH;
import static com.duviteck.tangolistview.provider.VideoContentProvider.VIDEO_LIST_URI;
import static com.duviteck.tangolistview.utils.Utils.calcProgress;
import static java.lang.Integer.parseInt;

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
            DatabaseUtils.updateLoadingStatus(this, videoUrl, LoadingStatus.LOADING);
            loadVideo(videoUrl);
            storeVideoSize(videoUrl);
            storeVideoFirstFrame(videoUrl);
            DatabaseUtils.updateLoadingStatus(this, videoUrl, LoadingStatus.LOADED);
            notifyUI();
        } catch (IOException e) {
            Log.w(TAG, "can't load video [url]:" + videoUrl);
            // unmark current video as loading
            DatabaseUtils.updateLoadingStatus(this, videoUrl, LoadingStatus.NOT_LOADING);
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
            output = openFileOutput(getVideoName(url), MODE_PRIVATE);     // TODO: add comments about CacheDir

            byte buffer[] = new byte[1024];

            long totalSize = response.body().contentLength();
            long loadedSize = 0;
            int oldProgress = 0;
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);

                loadedSize += count;
                int newProgress = calcProgress(loadedSize, totalSize);
                if (newProgress > oldProgress) {
                    oldProgress = newProgress;
                    DatabaseUtils.updateLoadProgress(this, url, totalSize, loadedSize);
                    notifyUI();
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

    private void storeVideoSize(String url) {
        Uri videoUri = DataLoaderService.getVideoUri(this, url);

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(this, videoUri);
        int width = parseInt(metaRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH));
        int height = parseInt(metaRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT));
        metaRetriever.release();

        DatabaseUtils.updateVideoSize(this, url, width, height);
    }

    private void storeVideoFirstFrame(String url) throws FileNotFoundException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Uri videoUri = getVideoUri(this, url);

        retriever.setDataSource(this, videoUri);
        Bitmap bitmap = retriever.getFrameAtTime(0);

        FileOutputStream outputStream = null;
        try {
            outputStream = openFileOutput(getVideoFirstFrameName(url), MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        bitmap.recycle();
        retriever.release();
    }

    private boolean isVideoAlreadyLoading(String videoUrl) {
        LoadingStatus status = DatabaseUtils.getVideoLoadingStatus(this, videoUrl);
        if (status == null) {
            throw new IllegalStateException("This situation should never happen");
        }
        return status != LoadingStatus.NOT_LOADING;
    }

    private void notifyUI() {
        getContentResolver().notifyChange(VIDEO_LIST_URI, null);
    }

    private void notifyVideoListLoad(boolean success) {
        if (success) {
            notifyUI();
        } else {
            // TODO: implement it correctly
            // notify about loading video list via LocalBroadcast
//            Intent intent = new Intent(LOAD_VIDEO_LIST_ACTION);
//            intent.putExtra(SUCCESS_EXTRA, false);
//            sendLocalBroadcast(intent);
        }
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
        return Uri.fromFile(new File(context.getFilesDir(), getVideoName(videoUrl)));
    }

    public static String getVideoFirstFramePath(Context context, String videoUrl) {
        return new File(context.getFilesDir(), getVideoFirstFrameName(videoUrl)).getAbsolutePath();
    }

    private static String getVideoName(String videoUrl) {
        return URLEncoder.encode(videoUrl);
    }

    private static String getVideoFirstFrameName(String videoUrl) {
        return getVideoName(videoUrl) + "-image";
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


    public enum LoadingStatus {
        NOT_LOADING(0),
        LOADING(1),
        LOADED(2);

        private int value;

        LoadingStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static LoadingStatus fromValue(int val) {
            for (LoadingStatus status : LoadingStatus.values()) {
                if (status.value == val) {
                    return status;
                }
            }
            return null;
        }
    }
}
