package com.example.wallpaper.util;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * 统一 Toast 工具类
 * 自动切换主线程、防重复显示
 */
public final class ToastUtils {

    private static Context appContext;
    private static Toast lastToast;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ToastUtils() {}

    public static void init(Application app) {
        appContext = app.getApplicationContext();
    }

    // ========== 基础方法 ==========

    public static void show(String message) {
        show(message, Toast.LENGTH_SHORT);
    }

    public static void show(int resId) {
        show(appContext.getString(resId), Toast.LENGTH_SHORT);
    }

    public static void show(String message, int duration) {
        if (appContext == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> show(message, duration));
            return;
        }
        if (lastToast != null) {
            lastToast.cancel();
        }
        lastToast = Toast.makeText(appContext, message, duration);
        lastToast.show();
    }

    // ========== 便捷方法 ==========

    public static void showSuccess(String message) {
        show(message);
    }

    public static void showSuccess(int resId) {
        show(appContext.getString(resId));
    }

    public static void showError(String message) {
        show(message);
    }

    public static void showError(int resId) {
        show(appContext.getString(resId));
    }

    public static void showWarning(String message) {
        show(message);
    }

    public static void showWarning(int resId) {
        show(appContext.getString(resId));
    }

    public static void showInfo(String message) {
        show(message);
    }

    public static void showInfo(int resId) {
        show(appContext.getString(resId));
    }

    // ========== 网络错误专用 ==========

    public static void showNetworkError() {
        show("网络连接失败，请检查网络设置");
    }

    public static void showNetworkError(String detail) {
        show("网络错误: " + detail);
    }

    // ========== 取消 ==========

    public static void cancel() {
        if (lastToast != null) {
            lastToast.cancel();
            lastToast = null;
        }
    }
}
