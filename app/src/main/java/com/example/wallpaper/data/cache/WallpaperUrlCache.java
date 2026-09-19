package com.example.wallpaper.data.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 壁纸 URL 缓存
 * 已被 WallpaperCacheManager 替代，保留此类用于向后兼容
 *
 * @deprecated 使用 {@link WallpaperCacheManager} 代替
 */
@Deprecated
public class WallpaperUrlCache {
    private final WallpaperCacheManager cacheManager = WallpaperCacheManager.getInstance();
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    /**
     * 构建缓存键
     *
     * @param sourceId       源 ID
     * @param categoryParams 分类参数
     * @param page           页码
     * @return 缓存键
     */
    public String buildKey(String sourceId, Map<String, String> categoryParams, int page) {
        StringBuilder key = new StringBuilder(sourceId);
        if (categoryParams != null && !categoryParams.isEmpty()) {
            TreeMap<String, String> sorted = new TreeMap<>(categoryParams);
            key.append(":").append(sorted.toString());
        }
        key.append(":p").append(page);
        return key.toString();
    }

    /**
     * 获取 URL 列表
     *
     * @param key 缓存键
     * @return URL 列表，如果不存在返回 null
     */
    public List<String> getUrls(String key) {
        // 优先从 WallpaperCacheManager 获取
        List<String> urls = cacheManager.getUrls(key);
        if (urls != null) {
            return new ArrayList<>(urls);
        }
        // 回退到本地缓存
        urls = cache.get(key);
        return urls != null ? new ArrayList<>(urls) : null;
    }

    /**
     * 缓存 URL 列表
     *
     * @param key  缓存键
     * @param urls URL 列表
     */
    public void putUrls(String key, List<String> urls) {
        if (key != null && urls != null) {
            List<String> copy = new ArrayList<>(urls);
            // 同时存入两个缓存
            cacheManager.putUrls(key, copy);
            cache.put(key, copy);
        }
    }

    /**
     * 检查是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean hasUrls(String key) {
        return cacheManager.hasUrls(key) || cache.containsKey(key);
    }

    /**
     * 清除指定 sourceId 的缓存
     *
     * @param sourceId 源 ID
     */
    public void clear(String sourceId) {
        cacheManager.clearUrlsBySource(sourceId);
        
        List<String> keysToRemove = new ArrayList<>();
        for (String key : cache.keySet()) {
            if (key.startsWith(sourceId + ":") || key.equals(sourceId)) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            cache.remove(key);
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        cacheManager.clearAllUrls();
        cache.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int size() {
        return Math.max(cacheManager.getUrlCacheSize(), cache.size());
    }
}
