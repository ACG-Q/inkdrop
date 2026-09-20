package io.inkdrop.wallpaper.data.source;

import java.util.HashMap;
import java.util.Map;

public class WallpaperCategory {
    private final String id;
    private final String name;
    private final Map<String, String> params;

    public WallpaperCategory(String id, String name, Map<String, String> params) {
        this.id = id;
        this.name = name;
        this.params = params != null ? params : new HashMap<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Map<String, String> getParams() { return params; }

    @Override
    public String toString() {
        return name;
    }
}
