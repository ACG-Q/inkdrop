package io.inkdrop.wallpaper;

import android.app.Application;

import io.inkdrop.wallpaper.util.LogUtils;
import io.inkdrop.wallpaper.util.ToastUtils;
import io.inkdrop.wallpaper.util.image.ImageDownloader;
import io.inkdrop.wallpaper.util.image.ImageLoader;
import io.inkdrop.wallpaper.util.image.WallpaperSetter;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WallpaperApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ToastUtils.init(this);
        LogUtils.init(this);
        ImageLoader.init(this);
        ImageDownloader.init(this);
        WallpaperSetter.init(this);
    }
}
