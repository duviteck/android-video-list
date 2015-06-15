//package com.duviteck.tangolistview.provider;
//
//import android.accounts.Account;
//import android.app.Application;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Parcelable;
//import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;
//
//import com.duviteck.tangolistview.api.response.VideoEntityResponse;
//import com.duviteck.tangolistview.entity.CachedVideoEntity;
//import com.duviteck.tangolistview.entity.VideoEntity;
//import com.duviteck.tangolistview.network.DataLoaderService;
//import com.duviteck.tangolistview.network.DataLoaderService.LoadingStatus;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class DataProvider {
//    private static final String TAG = "DataProvider";
//
//    private final Application appContext;
//    private final List<VideoEntity>
//    private List<VideoEntity> videoList;
//
//    public DataProvider(Application appContext) {
//        this.appContext = appContext;
//        registerDataReceivers();
//        // TODO: prepare here list of already loaded videos
//    }
//
//    private void registerDataReceivers() {
//        IntentFilter videoListFilter = new IntentFilter(DataLoaderService.LOAD_VIDEO_LIST_ACTION);
//        BroadcastReceiver videoListReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (videoList != null) {
//                    Log.w(TAG, "Something go wrong: videoList should be null at this moment");
//                    return;
//                }
//
//                // get videos from intent
//                Parcelable[] temp = intent.getParcelableArrayExtra(DataLoaderService.VIDEO_LIST_EXTRA);
//                videoList = new ArrayList<>(temp.length);
//
//                for (Parcelable parcelableVideo : temp) {
//                    VideoEntityResponse video = (VideoEntityResponse) parcelableVideo;
//                    LoadingStatus status = DataLoaderService.getLoadingStatus(video.getUrl());
//                    switch (status) {
//                        case LOADED:
//                            CachedVideoEntity cachedVideo = new CachedVideoEntity(video);
//                            videoList.add(cachedVideo);
//                            break;
//                        case LOADING:
//                    }
//                }
//
//                videoList = new ArrayList<>()
//
//            }
//        };
//        LocalBroadcastManager.getInstance(appContext).registerReceiver(videoListReceiver, videoListFilter);
//
//        IntentFilter videoLoadFilter = new IntentFilter(DataLoaderService.LOAD_VIDEO_ACTION);
//        BroadcastReceiver videoLoadReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                // TODO:
//            }
//        };
//        LocalBroadcastManager.getInstance(appContext).registerReceiver(videoLoadReceiver, videoLoadFilter);
//
//        IntentFilter videoProgressFilter = new IntentFilter(DataLoaderService.LOAD_VIDEO_PROGRESS_ACTION);
//        BroadcastReceiver videoProgressReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                // TODO:
//            }
//        };
//        LocalBroadcastManager.getInstance(appContext).registerReceiver(videoProgressReceiver, videoProgressFilter);
//    }
//
//    public void getVideoListAsync() {
//        if (videoList != null) {
//            // just get from cache
//            notifyVideoList(videoList);
//        } else {
//            // request DataLoaderService for newest videoList
//            requestLoadVideoList();
//        }
//    }
//
//    public void getNewestVideoListAsync() {
//        videoList = null;
//        getVideoListAsync();
//    }
//
//    private void requestLoadVideoList() {
//        Intent intent = new Intent(appContext, DataLoaderService.class);
//        intent.setAction(DataLoaderService.LOAD_VIDEO_LIST_ACTION);
//        appContext.startService(intent);
//    }
//
//    private void requestLoadVideo(String url) {
//        Intent intent = new Intent(appContext, DataLoaderService.class);
//        intent.setAction(DataLoaderService.LOAD_VIDEO_ACTION);
//        intent.putExtra(DataLoaderService.VIDEO_URL_EXTRA, url);
//        appContext.startService(intent);
//    }
//}
