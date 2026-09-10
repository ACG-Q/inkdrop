package com.example.wallpaper.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wallpaper.data.local.WallpaperDao;
import com.example.wallpaper.data.local.WallpaperEntity;
import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.data.remote.WallpaperResponse;
import com.example.wallpaper.data.source.WallpaperSource;
import com.example.wallpaper.data.source.WallpaperSourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class WallpaperRepository {
    private final WallpaperSourceManager sourceManager;
    private final WallpaperDao wallpaperDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    @Inject
    public WallpaperRepository(WallpaperSourceManager sourceManager, WallpaperDao wallpaperDao) {
        this.sourceManager = sourceManager;
        this.wallpaperDao = wallpaperDao;
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<Resource<List<Wallpaper>>> getWallpapers(String sourceId, int page, int pageSize) {
        MutableLiveData<Resource<List<Wallpaper>>> result = new MutableLiveData<>();
        
        WallpaperSource source = sourceId != null ? 
            sourceManager.getSource(sourceId) : 
            sourceManager.getDefaultSource();
        
        if (source == null || !source.isAvailable()) {
            result.setValue(Resource.error("壁纸源不可用", null));
            return result;
        }
        
        // 先显示缓存数据
        executor.execute(() -> {
            List<WallpaperEntity> cached = wallpaperDao.getWallpapersBySource(source.getSourceId());
            if (!cached.isEmpty()) {
                List<Wallpaper> wallpaperList = convertToDomain(cached);
                mainHandler.post(() -> result.setValue(Resource.success(wallpaperList)));
            }
        });
        
        // 网络请求
        result.setValue(Resource.loading(null));
        
        source.getWallpapers(page, pageSize).enqueue(new Callback<WallpaperResponse>() {
            @Override
            public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Wallpaper> wallpapers = response.body().getData();
                    
                    // 缓存到本地
                    executor.execute(() -> {
                        List<WallpaperEntity> entities = convertToEntity(wallpapers, source.getSourceId());
                        wallpaperDao.insertAll(entities);
                    });
                    
                    mainHandler.post(() -> result.setValue(Resource.success(wallpapers)));
                } else {
                    mainHandler.post(() -> result.setValue(Resource.error("请求失败", null)));
                }
            }

            @Override
            public void onFailure(Call<WallpaperResponse> call, Throwable t) {
                mainHandler.post(() -> result.setValue(Resource.error("网络错误: " + t.getMessage(), null)));
            }
        });
        
        return result;
    }

    public LiveData<List<WallpaperEntity>> getFavoriteWallpapers() {
        return wallpaperDao.getFavoriteWallpapers();
    }

    public void setFavorite(int wallpaperId, boolean isFavorite) {
        executor.execute(() -> wallpaperDao.setFavorite(wallpaperId, isFavorite));
    }

    public LiveData<Resource<Wallpaper>> getWallpaperById(int wallpaperId) {
        MutableLiveData<Resource<Wallpaper>> result = new MutableLiveData<>();
        
        executor.execute(() -> {
            WallpaperEntity entity = wallpaperDao.getWallpaperById(wallpaperId);
            if (entity != null) {
                Wallpaper wallpaper = convertSingleToDomain(entity);
                mainHandler.post(() -> result.setValue(Resource.success(wallpaper)));
            } else {
                mainHandler.post(() -> result.setValue(Resource.error("壁纸不存在", null)));
            }
        });
        
        return result;
    }

    public List<WallpaperSource> getAvailableSources() {
        return sourceManager.getAvailableSources();
    }

    private List<Wallpaper> convertToDomain(List<WallpaperEntity> entities) {
        List<Wallpaper> wallpapers = new ArrayList<>();
        for (WallpaperEntity entity : entities) {
            wallpapers.add(convertSingleToDomain(entity));
        }
        return wallpapers;
    }

    private Wallpaper convertSingleToDomain(WallpaperEntity entity) {
        return new Wallpaper(entity.getId(), entity.getUrl(), entity.getHash(),
            entity.getWidth(), entity.getHeight(), entity.getCreatedAt());
    }

    private List<WallpaperEntity> convertToEntity(List<Wallpaper> wallpapers, String sourceId) {
        List<WallpaperEntity> entities = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Wallpaper wallpaper : wallpapers) {
            entities.add(new WallpaperEntity(wallpaper.getId(), wallpaper.getUrl(), 
                wallpaper.getHash(), wallpaper.getWidth(), wallpaper.getHeight(),
                wallpaper.getCreatedAt(), false, sourceId, now));
        }
        return entities;
    }
}
