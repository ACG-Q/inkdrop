package com.example.wallpaper.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallpapers")
public class WallpaperEntity {
    @PrimaryKey
    private int id;
    private String url;
    private String hash;
    private int width;
    private int height;
    private String createdAt;
    private boolean isFavorite;
    private String sourceId;
    private long cachedAt;

    public WallpaperEntity(int id, String url, String hash, int width, int height, 
                          String createdAt, boolean isFavorite, String sourceId, long cachedAt) {
        this.id = id;
        this.url = url;
        this.hash = hash;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
        this.isFavorite = isFavorite;
        this.sourceId = sourceId;
        this.cachedAt = cachedAt;
    }

    // Getters
    public int getId() { return id; }
    public String getUrl() { return url; }
    public String getHash() { return hash; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getCreatedAt() { return createdAt; }
    public boolean isFavorite() { return isFavorite; }
    public String getSourceId() { return sourceId; }
    public long getCachedAt() { return cachedAt; }

    // Setters
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setCachedAt(long cachedAt) { this.cachedAt = cachedAt; }
}
