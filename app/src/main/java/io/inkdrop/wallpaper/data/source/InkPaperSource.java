package io.inkdrop.wallpaper.data.source;

import io.inkdrop.wallpaper.util.LogUtils;

import io.inkdrop.wallpaper.data.remote.RetrofitClient;
import io.inkdrop.wallpaper.data.remote.WallpaperApi;
import io.inkdrop.wallpaper.data.remote.WallpaperResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
    public Call<WallpaperResponse> getWallpapers(int page, int pageSize, java.util.Map<String, String> categoryParams) {
        return api.getWallpapers(page, pageSize);
    }

    @Override
    public List<String> getUrlList(int page, int pageSize, Map<String, String> categoryParams) {
        List<String> urls = new ArrayList<>();
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
            String url = BASE_URL + "/api/wallpapers?page=" + page + "&pageSize=" + pageSize;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray data = json.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            String imgUrl = item.optString("imgUrl", "");
                            if (!imgUrl.isEmpty()) {
                                urls.add(imgUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Failed to fetch URL list", e);
        }
        return urls;
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
