package com.example.wallpaper.data.source;

import com.example.wallpaper.data.remote.WallpaperResponse;

import retrofit2.Call;

public interface WallpaperSource {
    String getSourceId();
    String getSourceName();
    Call<WallpaperResponse> getWallpapers(int page, int pageSize);
    boolean isAvailable();
    SourceConfig getConfig();
}
