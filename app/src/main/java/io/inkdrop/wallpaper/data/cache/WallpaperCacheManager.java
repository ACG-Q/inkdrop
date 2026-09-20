package io.inkdrop.wallpaper.data.cache;

import android.util.LruCache;

/**
 * 壁纸缓存管理器
 * 统一管理各种缓存，使用 LRU 策略自动清理
 */
public class WallpaperCacheManager {
    
    private static volatile WallpaperCacheManager instance;
    
    // 图片尺寸缓存 - 最多缓存 500 张图片的尺寸信息
    private static final int DIMENSIONS_CACHE_SIZE = 500;
    private final LruCache<String, int[]> dimensionsCache;
    
    // URL 缓存 - 最多缓存 1000 个 URL 列表
    private static final int URL_CACHE_SIZE = 1000;
    private final LruCache<String, java.util.List<String>> urlCache;
    
    private WallpaperCacheManager() {
        dimensionsCache = new LruCache<>(DIMENSIONS_CACHE_SIZE);
        urlCache = new LruCache<>(URL_CACHE_SIZE);
    }
    
    /**
     * 获取单例实例
     *
     * @return WallpaperCacheManager 实例
     */
    public static WallpaperCacheManager getInstance() {
        if (instance == null) {
            synchronized (WallpaperCacheManager.class) {
                if (instance == null) {
                    instance = new WallpaperCacheManager();
                }
            }
        }
        return instance;
    }
    
    // ========== 图片尺寸缓存 ==========
    
    /**
     * 获取图片尺寸
     *
     * @param url 图片 URL
     * @return 尺寸数组 [width, height]，如果不存在返回 null
     */
    public int[] getDimensions(String url) {
        return dimensionsCache.get(url);
    }
    
    /**
     * 缓存图片尺寸
     *
     * @param url     图片 URL
     * @param width   宽度
     * @param height  高度
     */
    public void putDimensions(String url, int width, int height) {
        dimensionsCache.put(url, new int[]{width, height});
    }
    
    /**
     * 缓存图片尺寸
     *
     * @param url   图片 URL
     * @param dims  尺寸数组 [width, height]
     */
    public void putDimensions(String url, int[] dims) {
        if (dims != null && dims.length >= 2) {
            dimensionsCache.put(url, dims);
        }
    }
    
    /**
     * 检查尺寸缓存是否存在
     *
     * @param url 图片 URL
     * @return 是否存在
     */
    public boolean hasDimensions(String url) {
        return dimensionsCache.get(url) != null;
    }
    
    /**
     * 清除尺寸缓存
     */
    public void clearDimensions() {
        dimensionsCache.evictAll();
    }
    
    /**
     * 获取尺寸缓存大小
     *
     * @return 缓存大小
     */
    public int getDimensionsCacheSize() {
        return dimensionsCache.size();
    }
    
    // ========== URL 缓存 ==========
    
    /**
     * 获取 URL 列表
     *
     * @param cacheKey 缓存键
     * @return URL 列表，如果不存在返回 null
     */
    public java.util.List<String> getUrls(String cacheKey) {
        return urlCache.get(cacheKey);
    }
    
    /**
     * 缓存 URL 列表
     *
     * @param cacheKey 缓存键
     * @param urls     URL 列表
     */
    public void putUrls(String cacheKey, java.util.List<String> urls) {
        if (urls != null && !urls.isEmpty()) {
            urlCache.put(cacheKey, urls);
        }
    }
    
    /**
     * 检查 URL 缓存是否存在
     *
     * @param cacheKey 缓存键
     * @return 是否存在
     */
    public boolean hasUrls(String cacheKey) {
        return urlCache.get(cacheKey) != null;
    }
    
    /**
     * 清除指定 sourceId 的 URL 缓存
     *
     * @param sourceId 源 ID
     */
    public void clearUrlsBySource(String sourceId) {
        // 遍历所有缓存键，删除包含 sourceId 的缓存
        java.util.Set<String> keys = urlCache.snapshot().keySet();
        for (String key : keys) {
            if (key.contains(sourceId)) {
                urlCache.remove(key);
            }
        }
    }
    
    /**
     * 清除所有 URL 缓存
     */
    public void clearAllUrls() {
        urlCache.evictAll();
    }
    
    /**
     * 获取 URL 缓存大小
     *
     * @return 缓存大小
     */
    public int getUrlCacheSize() {
        return urlCache.size();
    }
    
    // ========== 综合操作 ==========
    
    /**
     * 清除所有缓存
     */
    public void clearAll() {
        dimensionsCache.evictAll();
        urlCache.evictAll();
    }
    
    /**
     * 获取缓存统计信息
     *
     * @return 统计信息字符串
     */
    public String getCacheStats() {
        return String.format("尺寸缓存: %d/%d, URL 缓存: %d/%d",
                dimensionsCache.size(), DIMENSIONS_CACHE_SIZE,
                urlCache.size(), URL_CACHE_SIZE);
    }
}
