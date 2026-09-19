package com.example.wallpaper;

import android.app.Application;

import com.example.wallpaper.util.LogUtils;
import com.example.wallpaper.util.ToastUtils;
import com.example.wallpaper.util.image.ImageDownloader;
import com.example.wallpaper.util.image.ImageLoader;
import com.example.wallpaper.util.image.WallpaperSetter;

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
