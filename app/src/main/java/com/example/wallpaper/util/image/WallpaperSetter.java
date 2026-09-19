package com.example.wallpaper.util.image;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executors;

/**
 * 壁纸设置工具类
 * 负责将图片设置为主屏/锁屏壁纸
 */
public final class WallpaperSetter {

    private static Context appContext;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public enum WallpaperType {
        HOME_SCREEN(WallpaperManager.FLAG_SYSTEM),
        LOCK_SCREEN(WallpaperManager.FLAG_LOCK),
        BOTH(WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);

        final int flag;

        WallpaperType(int flag) {
            this.flag = flag;
        }
    }

    private WallpaperSetter() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // ========== 设置方法 ==========

    public static void set(Context context, Uri imageUri, WallpaperType type,
                           ImageCallback.SetWallpaperCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                wallpaperManager.setStream(inputStream, null, true, type.flag);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void set(Bitmap bitmap, WallpaperType type,
                           ImageCallback.SetWallpaperCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(appContext);
                wallpaperManager.setBitmap(bitmap, null, true, type.flag);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void set(String url, WallpaperType type,
                           ImageCallback.SetWallpaperCallback callback) {
        ImageDownloader.downloadToCache(url, new ImageCallback.DownloadCallback() {
            @Override
            public void onSuccess(File file) {
                Uri uri = Uri.fromFile(file);
                set(appContext, uri, type, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
