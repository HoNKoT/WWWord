package jp.honkot.exercize.basic.wwword.util;

import android.util.Log;

/**
 * Created by hhonda_admin on 2017-02-09.
 */

public class Debug {

    public static boolean isDBG = true;

    public static void Log(String log) {
        Log.e("DLog", "### " + log);
    }

    public static void Log(String log, Throwable throwable) {
        Log.e("DLog", "### " + log, throwable);
    }
}
