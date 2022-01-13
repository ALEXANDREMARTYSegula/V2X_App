package com.segula.v2x.utils;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

public class Utils {
    public static double lastLocationLatitude = 48.79542;
    public static double lastLocationLongitude = 1.98587;

    public static void updateLastLocation(SharedPreferences sharedPreferences){
        lastLocationLatitude = sharedPreferences.getLong(GlobalConstants.lastLocationLatitude, Double.doubleToLongBits(lastLocationLatitude));
        lastLocationLongitude = sharedPreferences.getLong(GlobalConstants.lastLocationLongitude, Double.doubleToLongBits(lastLocationLongitude));
    }

    public static void runOnUi(Runnable runnable) {
        UIHandler.post(runnable);
    }

    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
}
