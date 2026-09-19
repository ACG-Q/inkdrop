package com.example.wallpaper.data.source;

import android.os.Handler;
import android.os.Looper;

import com.example.wallpaper.data.cache.WallpaperCacheManager;
import com.example.wallpaper.data.cache.WallpaperUrlCache;
import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.util.LogUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 壁纸批量加载管理器
 * 负责从壁纸源加载壁纸列表，并提供逐条回调
 */
public class WallpaperLoadManager {
    private static final int HASH_MODULO = 1_000_000_000;

    /**
     * 加载回调接口
     */
    public interface LoadCallback {
        /**
         * 单张壁纸加载完成
         *
         * @param wallpaper 加载的壁纸
         */
        void onWallpaperLoaded(Wallpaper wallpaper);

        /**
         * 所有壁纸加载完成
         *
         * @param hasMore 是否还有更多数据
         */
        void onAllLoaded(boolean hasMore);

        /**
         * 加载出错
         *
         * @param error 错误信息
         */
        void onError(String error);
    }

    private final WallpaperUrlCache urlCache;
    private final WallpaperCacheManager cacheManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private volatile String currentLoadId = null;
    private volatile boolean isCancelled = false;

    public WallpaperLoadManager() {
        this.urlCache = new WallpaperUrlCache();
        this.cacheManager = WallpaperCacheManager.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 批量加载壁纸
     *
     * @param source         壁纸源
     * @param sourceId       源 ID
     * @param categoryParams 分类参数
     * @param page           页码
     * @param pageSize       每页数量
     * @param callback       回调
     */
    public void loadBatch(
            WallpaperSource source,
            String sourceId,
            Map<String, String> categoryParams,
            int page,
            int pageSize,
            LoadCallback callback) {

        String loadId = sourceId + ":" + (categoryParams != null ? categoryParams : "") + ":p" + page;
        // 取消上一次加载
        isCancelled = true;
        currentLoadId = loadId;
        isCancelled = false;
        LogUtils.d("loadBatch() loadId=" + loadId);

        String cacheKey = urlCache.buildKey(sourceId, categoryParams, page);

        executor.execute(() -> {
            try {
                List<String> urls;
                if (urlCache.hasUrls(cacheKey)) {
                    urls = urlCache.getUrls(cacheKey);
                    LogUtils.d("URL cache hit: key=" + cacheKey + ", size=" + urls.size());
                } else {
                    urls = source.getUrlList(page, pageSize, categoryParams);
                    if (urls == null) {
                        urls = new java.util.ArrayList<>();
                    }
                    if (!urls.isEmpty()) {
                        urlCache.putUrls(cacheKey, urls);
                    }
                    LogUtils.d("URL cache miss: key=" + cacheKey + ", fetched=" + urls.size());
                }

                if (isCancelled) {
                    LogUtils.d("loadBatch() cancelled before processing URLs");
                    return;
                }

                if (urls.isEmpty()) {
                    mainHandler.post(() -> callback.onAllLoaded(false));
                    return;
                }

                for (int i = 0; i < urls.size(); i++) {
                    if (isCancelled) {
                        LogUtils.d("loadBatch() interrupted at item " + i);
                        return;
                    }

                    String url = urls.get(i);

                    int[] dims = cacheManager.getDimensions(url);
                    if (dims == null) {
                        dims = GenericSource.fetchImageDimensions(url);
                        if (dims != null) {
                            cacheManager.putDimensions(url, dims);
                        }
                    }

                    Wallpaper wallpaper = new Wallpaper(
                            Math.abs(url.hashCode() % HASH_MODULO),
                            url, "",
                            dims != null ? dims[0] : 0,
                            dims != null ? dims[1] : 0,
                            ""
                    );

                    mainHandler.post(() -> {
                        if (!isCancelled) {
                            callback.onWallpaperLoaded(wallpaper);
                        }
                    });
                }

                final boolean hasMore = urls.size() >= pageSize;
                mainHandler.post(() -> {
                    if (!isCancelled) {
                        callback.onAllLoaded(hasMore);
                    }
                });

            } catch (Exception e) {
                LogUtils.e("Load batch error", e);
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * 清除指定 sourceId 的 URL 缓存
     *
     * @param sourceId 源 ID
     */
    public void clearUrlCache(String sourceId) {
        urlCache.clear(sourceId);
        cacheManager.clearUrlsBySource(sourceId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCaches() {
        urlCache.clearAll();
        cacheManager.clearAll();
    }

    /**
     * 检查是否正在加载
     *
     * @return 是否正在加载
     */
    public boolean isLoading() {
        return currentLoadId != null;
    }

    private void postError(LoadCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
