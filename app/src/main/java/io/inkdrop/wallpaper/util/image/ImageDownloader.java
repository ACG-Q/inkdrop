package io.inkdrop.wallpaper.util.image;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import io.inkdrop.wallpaper.util.FileUtils;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * 图片下载工具类
 * 负责将图片下载到本地
 */
public final class ImageDownloader {

    private static Context appContext;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final File WALLPAPER_DIR = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Wallpapers");

    private ImageDownloader() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // ========== 下载方法 ==========

    public static void download(String url, File destFile, ImageCallback.DownloadCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FutureTarget<File> target = Glide.with(appContext)
                    .downloadOnly()
                    .load(url)
                    .submit();

                File downloadedFile = target.get();
                FileUtils.copyFile(downloadedFile, destFile);

                mainHandler.post(() -> callback.onSuccess(destFile));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void downloadToWallpapers(String url, ImageCallback.DownloadCallback callback) {
        if (!WALLPAPER_DIR.exists()) {
            WALLPAPER_DIR.mkdirs();
        }
        String filename = "wallpaper_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(WALLPAPER_DIR, filename);
        download(url, destFile, callback);
    }

    public static void downloadToCache(String url, ImageCallback.DownloadCallback callback) {
        File cacheDir = appContext.getCacheDir();
        String filename = "image_" + Math.abs(url.hashCode()) + ".jpg";
        File destFile = new File(cacheDir, filename);
        download(url, destFile, callback);
    }

    public static File getCachedFile(String url) {
        if (appContext == null) return null;
        File cacheDir = appContext.getCacheDir();
        String filename = "image_" + Math.abs(url.hashCode()) + ".jpg";
        File file = new File(cacheDir, filename);
        return file.exists() ? file : null;
    }
}
