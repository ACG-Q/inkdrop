package com.example.wallpaper.data.source;

import com.example.wallpaper.data.remote.RetrofitClient;
import com.example.wallpaper.data.remote.WallpaperApi;
import com.example.wallpaper.data.remote.WallpaperResponse;

import retrofit2.Call;

public class InkPaperSource implements WallpaperSource {
    private static final String SOURCE_ID = "inkpaper";
    private static final String SOURCE_NAME = "InkPaper壁纸";
    private static final String BASE_URL = "https://inkpaper.foolstack.net";
    
    private final WallpaperApi api;
    private final SourceConfig config;

    public InkPaperSource() {
        this.api = RetrofitClient.createApi(WallpaperApi.class, BASE_URL);
        this.config = new SourceConfig.Builder()
            .sourceId(SOURCE_ID)
            .sourceName(SOURCE_NAME)
            .baseUrl(BASE_URL)
            .enabled(true)
            .priority(1)
            .build();
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public Call<WallpaperResponse> getWallpapers(int page, int pageSize) {
        return api.getWallpapers(page, pageSize);
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled();
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }
}
