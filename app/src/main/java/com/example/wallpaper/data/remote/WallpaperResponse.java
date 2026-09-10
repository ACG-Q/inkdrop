package com.example.wallpaper.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WallpaperResponse {
    @SerializedName("data")
    private List<Wallpaper> data;
    
    @SerializedName("page")
    private int page;
    
    @SerializedName("pageSize")
    private int pageSize;
    
    @SerializedName("total")
    private int total;
    
    @SerializedName("totalPages")
    private int totalPages;

    public List<Wallpaper> getData() { return data; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}
