package com.example.wallpaper.data.remote;

import com.google.gson.annotations.SerializedName;

public class Wallpaper {
    @SerializedName("id")
    private int id;
    
    @SerializedName("url")
    private String url;
    
    @SerializedName("hash")
    private String hash;
    
    @SerializedName("width")
    private int width;
    
    @SerializedName("height")
    private int height;
    
    @SerializedName("createdAt")
    private String createdAt;

    private boolean isFavorite;

    public Wallpaper(int id, String url, String hash, int width, int height, String createdAt) {
        this.id = id;
        this.url = url;
        this.hash = hash;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getUrl() { return url; }
    public String getHash() { return hash; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getCreatedAt() { return createdAt; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
}
