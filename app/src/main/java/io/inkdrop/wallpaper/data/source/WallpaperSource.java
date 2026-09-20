package io.inkdrop.wallpaper.data.source;

import io.inkdrop.wallpaper.data.remote.WallpaperResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

public interface WallpaperSource {
    String getSourceId();
    String getSourceName();
    Call<WallpaperResponse> getWallpapers(int page, int pageSize);
    Call<WallpaperResponse> getWallpapers(int page, int pageSize, Map<String, String> categoryParams);

    default List<String> getUrlList(int page, int pageSize, Map<String, String> categoryParams) {
        return Collections.emptyList();
    }

    boolean isAvailable();
    SourceConfig getConfig();
}
