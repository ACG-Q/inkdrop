package com.example.wallpaper.data.source;

import android.content.Context;
import com.example.wallpaper.util.LogUtils;

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
    private Context context;

    @Inject
    public WallpaperSourceManager() {
    }

    public synchronized void init(Context context) {
        if (this.context != null) return;
        this.context = context.getApplicationContext();
        loadCustomSources();
    }

    private void loadCustomSources() {
        if (context == null) return;

        java.io.File sourcesDir = new java.io.File(context.getFilesDir(), "sources");
        if (!sourcesDir.exists()) {
            sourcesDir.mkdirs();
        }

        java.io.File[] files = sourcesDir.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            try {
                String fileName = file.getName();
                GenericSource source = GenericSource.createFromInternal(context, fileName);
                if (source != null && source.isAvailable()) {
                    sources.put(source.getSourceId(), source);
                    LogUtils.d("Loaded source: " + source.getSourceId());
                }
            } catch (Exception e) {
                LogUtils.e("Failed to load source: " + file.getName(), e);
            }
        }
    }

    public void registerSource(WallpaperSource source) {
        sources.put(source.getSourceId(), source);
    }

    public void unregisterSource(String sourceId) {
        sources.remove(sourceId);
    }

    public WallpaperSource getSource(String sourceId) {
        return sources.get(sourceId);
    }

    public WallpaperSource getDefaultSource() {
        List<WallpaperSource> available = getAvailableSources();
        return available.isEmpty() ? null : available.get(0);
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

    public List<WallpaperSource> getAllSources() {
        return new ArrayList<>(sources.values());
    }

    public boolean isSourceEnabled(String sourceId) {
        if (context == null) return true;
        android.content.SharedPreferences prefs = context.getSharedPreferences("source_settings", 0);
        return prefs.getBoolean("source_enabled_" + sourceId, true);
    }

    public void setSourceEnabled(String sourceId, boolean enabled) {
        if (context == null) return;
        context.getSharedPreferences("source_settings", 0)
                .edit()
                .putBoolean("source_enabled_" + sourceId, enabled)
                .apply();
        WallpaperSource source = sources.get(sourceId);
        if (source instanceof GenericSource) {
            ((GenericSource) source).setEnabled(enabled);
        }
    }

    public String getSelectedSourceId() {
        if (context == null) return null;
        return context.getSharedPreferences("source_settings", 0)
                .getString("selected_source", null);
    }

    public void setSelectedSource(String sourceId) {
        if (context == null) return;
        context.getSharedPreferences("source_settings", 0)
                .edit()
                .putString("selected_source", sourceId)
                .apply();
    }

    public boolean addCustomSource(String sourceId, String sourceName, String jsonConfig) {
        if (context == null) return false;
        try {
            java.io.File sourcesDir = new java.io.File(context.getFilesDir(), "sources");
            if (!sourcesDir.exists()) sourcesDir.mkdirs();

            java.io.File file = new java.io.File(sourcesDir, sourceId + ".json");
            com.example.wallpaper.util.FileUtils.writeFile(file, jsonConfig);

            GenericSource source = GenericSource.createFromInternal(context, sourceId + ".json");
            if (source != null) {
                sources.put(source.getSourceId(), source);
                return true;
            }
        } catch (Exception e) {
            LogUtils.e("Failed to add custom source", e);
        }
        return false;
    }

    public boolean addCustomJsSource(String sourceId, String sourceName, String jsCode) {
        if (context == null) return false;
        try {
            java.io.File sourcesDir = new java.io.File(context.getFilesDir(), "sources");
            if (!sourcesDir.exists()) sourcesDir.mkdirs();

            String fullJs = "// " + sourceName + "\n\n" + jsCode;
            java.io.File file = new java.io.File(sourcesDir, sourceId + ".js");
            com.example.wallpaper.util.FileUtils.writeFile(file, fullJs);

            GenericSource source = GenericSource.createFromJs(context, sourceId + ".js");
            if (source != null) {
                sources.put(source.getSourceId(), source);
                return true;
            }
        } catch (Exception e) {
            LogUtils.e("Failed to add JS source", e);
        }
        return false;
    }

    public boolean removeSource(String sourceId) {
        if (context == null) return false;
        try {
            java.io.File sourcesDir = new java.io.File(context.getFilesDir(), "sources");

            java.io.File jsonFile = new java.io.File(sourcesDir, sourceId + ".json");
            if (jsonFile.exists()) jsonFile.delete();

            java.io.File jsFile = new java.io.File(sourcesDir, sourceId + ".js");
            if (jsFile.exists()) jsFile.delete();

            sources.remove(sourceId);
            return true;
        } catch (Exception e) {
            LogUtils.e("Failed to remove source", e);
        }
        return false;
    }

    public String readSourceConfig(String sourceId) {
        if (context == null) return null;
        try {
            java.io.File sourcesDir = new java.io.File(context.getFilesDir(), "sources");

            java.io.File jsonFile = new java.io.File(sourcesDir, sourceId + ".json");
            if (jsonFile.exists()) {
                return readFile(jsonFile);
            }

            java.io.File jsFile = new java.io.File(sourcesDir, sourceId + ".js");
            if (jsFile.exists()) {
                return readFile(jsFile);
            }
        } catch (Exception e) {
            LogUtils.e("Failed to read source config", e);
        }
        return null;
    }

    private String readFile(java.io.File file) throws java.io.IOException {
        return com.example.wallpaper.util.FileUtils.readFile(file);
    }
}
