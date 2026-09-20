package io.inkdrop.wallpaper.util.image;

import java.io.File;

/**
 * 图片操作回调接口
 */
public final class ImageCallback {

    public interface LoadCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(String message);
    }

    public interface SetWallpaperCallback {
        void onSuccess();
        void onError(String message);
    }
}
