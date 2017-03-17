package jp.honkot.exercize.basic.wwword.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hiroki on 2017-03-15.
 */

public class SharedPreferenceUtil {

    public static final String PREF_FILE_NAME = "pref";
    public static final String PREF_FIRST_BOOT = "PREF_FIRST_BOOT";

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isFirstBoot(Context context) {
        return getPref(context).getBoolean(PREF_FIRST_BOOT, true);
    }

    public static void doneFirstBoot(Context context) {
        getPref(context).edit().putBoolean(PREF_FIRST_BOOT, false).apply();
    }
}
