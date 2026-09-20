package io.inkdrop.wallpaper.data.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceConfig {
    private final String sourceId;
    private final String sourceName;
    private final String baseUrl;
    private final boolean enabled;
    private final int priority;
    private final Map<String, String> extraConfig;
    private final List<WallpaperCategory> categories;

    private SourceConfig(Builder builder) {
        this.sourceId = builder.sourceId;
        this.sourceName = builder.sourceName;
        this.baseUrl = builder.baseUrl;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.extraConfig = builder.extraConfig;
        this.categories = builder.categories;
    }

    public String getSourceId() { return sourceId; }
    public String getSourceName() { return sourceName; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isEnabled() { return enabled; }
    public int getPriority() { return priority; }
    public Map<String, String> getExtraConfig() { return extraConfig; }
    public List<WallpaperCategory> getCategories() { return categories; }

    public static class Builder {
        private String sourceId;
        private String sourceName;
        private String baseUrl;
        private boolean enabled = true;
        private int priority = 1;
        private Map<String, String> extraConfig = new HashMap<>();
        private List<WallpaperCategory> categories = new ArrayList<>();

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder sourceName(String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder extraConfig(Map<String, String> extraConfig) {
            this.extraConfig = extraConfig;
            return this;
        }

        public Builder categories(List<WallpaperCategory> categories) {
            this.categories = categories;
            return this;
        }

        public SourceConfig build() {
            return new SourceConfig(this);
        }
    }
}
