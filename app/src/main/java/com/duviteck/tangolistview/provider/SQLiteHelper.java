package com.duviteck.tangolistview.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by duviteck on 15/06/15.
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    public static final class VideoTable {
        public static final String TABLE_NAME = "videos";
        // because URL works like primary key, and CursorAdapter needs a column with name "_id"
        public static final String URL = "_id";
        public static final String TITLE = "title";
        public static final String THUMB = "thumb";
        public static final String TOTAL_SIZE = "total_size";
        public static final String LOADED_SIZE = "loaded_size";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String LOADING_STATUS = "loading_status";
        public static final String SHOULD_BE_SHOWN = "should_be_shown";
    }

    private static final String DATABASE_CREATE_VIDEO_TABLE = "create table " + VideoTable.TABLE_NAME + "(" +
            VideoTable.URL + " text primary key, " +
            VideoTable.TITLE + " text not null, " +
            VideoTable.THUMB + " text not null, " +
            VideoTable.TOTAL_SIZE + " integer not null, " +
            VideoTable.LOADED_SIZE + " integer not null, " +
            VideoTable.WIDTH + " integer not null, " +
            VideoTable.HEIGHT + " integer not null, " +
            VideoTable.LOADING_STATUS + " integer not null, " +
            VideoTable.SHOULD_BE_SHOWN + " integer not null, " +
            "UNIQUE (" + VideoTable.URL + ") ON CONFLICT IGNORE" +
            ");";

    private static final String DATABASE_NAME = "video.db";
    private static final int DATABASE_VERSION = 1;

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_VIDEO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
