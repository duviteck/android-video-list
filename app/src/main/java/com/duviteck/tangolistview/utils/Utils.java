package com.duviteck.tangolistview.utils;

/**
 * Created by duviteck on 16/06/15.
 */
public class Utils {
    public static int calcProgress(long done, long total) {
        return (total == 0) ? 0 : (int)(done * 100 / total);
    }
}
