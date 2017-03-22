package jp.honkot.exercize.basic.wwword.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by hiroki on 2017-03-22.
 */

public class CheckPermissionUtil {
    public static void checkPermission(Activity activity) {
        int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else {
            //callMethod();
        }
    }
}
