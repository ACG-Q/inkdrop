package com.example.wallpaper.di;

import android.content.Context;

import com.example.wallpaper.data.local.AppDatabase;
import com.example.wallpaper.data.local.WallpaperDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    @Provides
    @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    @Singleton
    public WallpaperDao provideWallpaperDao(AppDatabase database) {
        return database.wallpaperDao();
    }
}
