package com.duviteck.tangolistview.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

import java.util.ArrayList;

import static android.net.Uri.withAppendedPath;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.duviteck.tangolistview.provider.VideoContentProvider";

    private static final String VIDEO_LIST_PATH = "video_list";
    private static final String INSERT_VIDEO_PATH = "insert_video";
    private static final String UPDATE_VIDEO_PATH = "update_video";

    private static final int VIDEO_LIST_INDEX = 1;
    private static final int INSERT_VIDEO_INDEX = 2;
    private static final int UPDATE_VIDEO_INDEX = 3;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri VIDEO_LIST_URI = withAppendedPath(CONTENT_URI, VIDEO_LIST_PATH);
    public static final Uri INSERT_VIDEO_URI = withAppendedPath(CONTENT_URI, INSERT_VIDEO_PATH);
    public static final Uri UPDATE_VIDEO_URI = withAppendedPath(CONTENT_URI, UPDATE_VIDEO_PATH);

    private SQLiteHelper dbHelper;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, VIDEO_LIST_PATH, VIDEO_LIST_INDEX);
        uriMatcher.addURI(AUTHORITY, INSERT_VIDEO_PATH, INSERT_VIDEO_INDEX);
        uriMatcher.addURI(AUTHORITY, UPDATE_VIDEO_PATH, UPDATE_VIDEO_INDEX);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(VideoTable.TABLE_NAME);

        Cursor cursor = builder.query(database, projection, selection,
                selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);  // TODO: is it needed?
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = null;
        long result = -1;
        try {
            database = dbHelper.getWritableDatabase();
            database.beginTransactionNonExclusive();
            result = database.insert(VideoTable.TABLE_NAME, null, values);
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return Uri.withAppendedPath(uri, String.valueOf(result));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // not implemented for now because of no need in it
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = null;
        int updated = 0;
        try {
            database = dbHelper.getWritableDatabase();
            database.beginTransactionNonExclusive();
            updated = database.update(VideoTable.TABLE_NAME, values, selection, selectionArgs);
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return updated;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase database = null;
        ContentProviderResult[] results = new ContentProviderResult[operations.size()];
        int i = 0;
        try {
            database = dbHelper.getWritableDatabase();
            database.beginTransactionNonExclusive();
            for (ContentProviderOperation operation : operations) {
                results[i++] = operation.apply(this, results, i);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return results;
    }
}
