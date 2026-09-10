package com.example.wallpaper.data.source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WallpaperSourceManager {
    private final Map<String, WallpaperSource> sources = new LinkedHashMap<>();

    @Inject
    public WallpaperSourceManager() {
        // 注册默认壁纸源
        registerSource(new InkPaperSource());
    }

    public void registerSource(WallpaperSource source) {
        sources.put(source.getSourceId(), source);
    }

    public void unregisterSource(String sourceId) {
        sources.remove(sourceId);
    }

    public List<WallpaperSource> getAvailableSources() {
        List<WallpaperSource> available = new ArrayList<>();
        for (WallpaperSource source : sources.values()) {
            if (source.isAvailable()) {
                available.add(source);
            }
        }
        available.sort(Comparator.comparingInt(s -> s.getConfig().getPriority()));
        return available;
    }

    public WallpaperSource getSource(String sourceId) {
        return sources.get(sourceId);
    }

    public WallpaperSource getDefaultSource() {
        List<WallpaperSource> available = getAvailableSources();
        return available.isEmpty() ? null : available.get(0);
    }

    public List<WallpaperSource> getAllSources() {
        return new ArrayList<>(sources.values());
    }
}
