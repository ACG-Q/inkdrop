package com.example.wallpaper;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WallpaperApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
