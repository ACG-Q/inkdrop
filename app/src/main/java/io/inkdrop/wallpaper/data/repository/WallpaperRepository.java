package io.inkdrop.wallpaper.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.inkdrop.wallpaper.data.local.WallpaperDao;
import io.inkdrop.wallpaper.data.local.WallpaperEntity;
import io.inkdrop.wallpaper.data.remote.Wallpaper;
import io.inkdrop.wallpaper.data.remote.WallpaperResponse;
import io.inkdrop.wallpaper.data.source.WallpaperLoadManager;
import io.inkdrop.wallpaper.data.source.WallpaperSource;
import io.inkdrop.wallpaper.data.source.WallpaperSourceManager;
import io.inkdrop.wallpaper.di.AppModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 壁纸数据仓库
 * 统一管理壁纸数据的获取、缓存和存储
 */
@Singleton
public class WallpaperRepository {
    private final WallpaperSourceManager sourceManager;
    private final WallpaperDao wallpaperDao;
    private final WallpaperLoadManager loadManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    @Inject
    public WallpaperRepository(
            WallpaperSourceManager sourceManager,
            WallpaperDao wallpaperDao,
            @Named(AppModule.IO_EXECUTOR) ExecutorService executor) {
        this.sourceManager = sourceManager;
        this.wallpaperDao = wallpaperDao;
        this.loadManager = new WallpaperLoadManager();
        this.executor = executor;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void init(android.content.Context context) {
        sourceManager.init(context);
    }

    public LiveData<Resource<List<Wallpaper>>> getWallpapers(String sourceId, int page, int pageSize) {
        return getWallpapers(sourceId, page, pageSize, null);
    }

    public LiveData<Resource<List<Wallpaper>>> getWallpapers(String sourceId, int page, int pageSize, java.util.Map<String, String> categoryParams) {
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
        
        source.getWallpapers(page, pageSize, categoryParams).enqueue(new Callback<WallpaperResponse>() {
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

    public void loadBatch(String sourceId, Map<String, String> categoryParams,
                          int page, int pageSize, WallpaperLoadManager.LoadCallback callback) {
        WallpaperSource source = sourceId != null ?
            sourceManager.getSource(sourceId) :
            sourceManager.getDefaultSource();

        if (source == null || !source.isAvailable()) {
            callback.onError("壁纸源不可用");
            return;
        }

        WallpaperLoadManager.LoadCallback wrappedCallback = new WallpaperLoadManager.LoadCallback() {
            @Override
            public void onWallpaperLoaded(Wallpaper wallpaper) {
                // 保存到Room
                executor.execute(() -> {
                    WallpaperEntity entity = new WallpaperEntity(
                        wallpaper.getId(), wallpaper.getUrl(), "",
                        wallpaper.getWidth(), wallpaper.getHeight(),
                        "", false, sourceId, System.currentTimeMillis()
                    );
                    wallpaperDao.insertAll(java.util.Collections.singletonList(entity));
                });
                callback.onWallpaperLoaded(wallpaper);
            }

            @Override
            public void onAllLoaded(boolean hasMore) {
                callback.onAllLoaded(hasMore);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        };

        loadManager.loadBatch(source, sourceId, categoryParams, page, pageSize, wrappedCallback);
    }

    public void clearUrlCache(String sourceId) {
        loadManager.clearUrlCache(sourceId);
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
        Wallpaper wallpaper = new Wallpaper(entity.getId(), entity.getUrl(), entity.getHash(),
            entity.getWidth(), entity.getHeight(), entity.getCreatedAt());
        wallpaper.setFavorite(entity.isFavorite());
        return wallpaper;
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
