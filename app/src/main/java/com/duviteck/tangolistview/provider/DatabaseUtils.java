package com.duviteck.tangolistview.provider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import com.duviteck.tangolistview.api.response.VideoEntityResponse;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

import java.util.ArrayList;
import java.util.List;

import static com.duviteck.tangolistview.provider.VideoContentProvider.INSERT_VIDEO_URI;
import static com.duviteck.tangolistview.provider.VideoContentProvider.VIDEO_LIST_URI;

/**
 * Created by duviteck on 15/06/15.
 */
public class DatabaseUtils {

    public static void clearShowFlag(Context context) {
        ContentValues cv = new ContentValues(1);
        cv.put(VideoTable.SHOULD_BE_SHOWN, 0);

        context.getContentResolver().update(
                VideoContentProvider.UPDATE_VIDEO_URI,
                cv,
                null,
                null
        );
    }

    public static void setShowFlag(Context context, List<String> urls) {
        ContentValues cv = new ContentValues(1);
        cv.put(VideoTable.SHOULD_BE_SHOWN, 1);

        context.getContentResolver().update(
                VideoContentProvider.UPDATE_VIDEO_URI,
                cv,
                VideoTable.URL + " in (" + joinEscaped(",", urls) + ")",
                null
        );
    }

    public static void insertVideos(Context context, VideoEntityResponse[] videos) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(videos.length);
        for (VideoEntityResponse video : videos) {
            ops.add(ContentProviderOperation.newInsert(INSERT_VIDEO_URI)
                    .withValue(VideoTable.URL, video.getUrl())
                    .withValue(VideoTable.TITLE, video.getTitle())
                    .withValue(VideoTable.TOTAL_SIZE, 0)
                    .withValue(VideoTable.LOADED_SIZE, 0)
                    .withValue(VideoTable.SHOULD_BE_SHOWN, 0)
                    .build());
        }

        try {
            context.getContentResolver().applyBatch(VideoContentProvider.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public static void updateLoadProgress(Context context, String url, long totalSize, long loadedSize) {
        ContentValues cv = new ContentValues(2);
        cv.put(VideoTable.TOTAL_SIZE, totalSize);
        cv.put(VideoTable.LOADED_SIZE, loadedSize);

        context.getContentResolver().update(
                VideoContentProvider.UPDATE_VIDEO_URI,
                cv,
                VideoTable.URL + " = ?",
                new String[] {url}
        );
    }

    public static long getVideoTotalSize(Context context, String url) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    VIDEO_LIST_URI,
                    new String[] {VideoTable.TOTAL_SIZE},
                    VideoTable.URL + " = ?",
                    new String[] {url},
                    null);
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    private static String joinEscaped(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(android.database.DatabaseUtils.sqlEscapeString(token.toString()));
        }
        return sb.toString();
    }
}
