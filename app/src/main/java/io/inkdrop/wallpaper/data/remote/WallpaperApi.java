package io.inkdrop.wallpaper.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WallpaperApi {
    @GET("/api/wallpapers")
    Call<WallpaperResponse> getWallpapers(
        @Query("page") int page,
        @Query("pageSize") int pageSize
    );
}
