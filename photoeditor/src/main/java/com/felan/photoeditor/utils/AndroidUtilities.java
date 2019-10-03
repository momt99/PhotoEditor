package com.felan.photoeditor.utils;

public class AndroidUtilities {
    public static int statusBarHeight;
    public static float density = 3.0f;

    public static int dp(float i) {
        return (int) (density * i);
    }

    public static float getPhotoSize() {
        return 1280;
    }

    public static boolean isTablet() {
        return false;
    }
}
