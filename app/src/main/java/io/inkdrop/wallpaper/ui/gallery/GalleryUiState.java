package io.inkdrop.wallpaper.ui.gallery;

import io.inkdrop.wallpaper.data.remote.Wallpaper;
import io.inkdrop.wallpaper.data.source.WallpaperCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 画廊页面 UI 状态
 * 使用不可变对象封装页面状态
 */
public class GalleryUiState {
    
    /**
     * 加载状态枚举
     */
    public enum LoadingState {
        /** 空闲状态 */
        IDLE,
        /** 加载中 */
        LOADING,
        /** 加载完成 */
        LOADED,
        /** 加载失败 */
        ERROR
    }
    
    private final int currentPage;
    private final int pageSize;
    private final boolean isLastPage;
    private final boolean freshLoad;
    private final String currentSourceId;
    private final WallpaperCategory currentCategory;
    private final List<Wallpaper> wallpapers;
    private final LoadingState loadingState;
    private final String errorMessage;
    
    /**
     * 默认构造函数
     */
    public GalleryUiState() {
        this(1, 5, false, false, null, null, Collections.emptyList(), LoadingState.IDLE, null);
    }
    
    /**
     * 全参构造函数
     */
    public GalleryUiState(
            int currentPage,
            int pageSize,
            boolean isLastPage,
            boolean freshLoad,
            String currentSourceId,
            WallpaperCategory currentCategory,
            List<Wallpaper> wallpapers,
            LoadingState loadingState,
            String errorMessage) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.isLastPage = isLastPage;
        this.freshLoad = freshLoad;
        this.currentSourceId = currentSourceId;
        this.currentCategory = currentCategory;
        this.wallpapers = wallpapers != null ? 
            Collections.unmodifiableList(new ArrayList<>(wallpapers)) : 
            Collections.emptyList();
        this.loadingState = loadingState;
        this.errorMessage = errorMessage;
    }
    
    // ========== Getters ==========
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public boolean isLastPage() {
        return isLastPage;
    }
    
    public boolean isFreshLoad() {
        return freshLoad;
    }
    
    public String getCurrentSourceId() {
        return currentSourceId;
    }
    
    public WallpaperCategory getCurrentCategory() {
        return currentCategory;
    }
    
    public List<Wallpaper> getWallpapers() {
        return wallpapers;
    }
    
    public LoadingState getLoadingState() {
        return loadingState;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isLoading() {
        return loadingState == LoadingState.LOADING;
    }
    
    public boolean hasError() {
        return loadingState == LoadingState.ERROR;
    }
    
    public boolean isEmpty() {
        return wallpapers.isEmpty();
    }
    
    // ========== With Methods (用于创建修改后的副本) ==========
    
    /**
     * 创建下一页的副本
     *
     * @return 新的 UI 状态
     */
    public GalleryUiState withNextPage() {
        return new GalleryUiState(
            currentPage + 1,
            pageSize,
            false,
            false,
            currentSourceId,
            currentCategory,
            wallpapers,
            LoadingState.IDLE,
            null
        );
    }
    
    /**
     * 创建添加壁纸后的副本
     *
     * @param wallpaper 要添加的壁纸
     * @return 新的 UI 状态
     */
    public GalleryUiState withWallpaperAdded(Wallpaper wallpaper) {
        List<Wallpaper> newList = new ArrayList<>(this.wallpapers);
        newList.add(wallpaper);
        return new GalleryUiState(
            currentPage,
            pageSize,
            isLastPage,
            false,
            currentSourceId,
            currentCategory,
            newList,
            loadingState,
            null
        );
    }
    
    /**
     * 创建清空壁纸列表的副本
     *
     * @return 新的 UI 状态
     */
    public GalleryUiState withClearedWallpapers() {
        return new GalleryUiState(
            currentPage,
            pageSize,
            isLastPage,
            freshLoad,
            currentSourceId,
            currentCategory,
            Collections.emptyList(),
            loadingState,
            null
        );
    }
    
    /**
     * 创建加载中的副本
     *
     * @return 新的 UI 状态
     */
    public GalleryUiState withLoading() {
        return new GalleryUiState(
            currentPage,
            pageSize,
            false,
            freshLoad,
            currentSourceId,
            currentCategory,
            wallpapers,
            LoadingState.LOADING,
            null
        );
    }
    
    /**
     * 创建加载完成的副本
     *
     * @param hasMore 是否有更多数据
     * @return 新的 UI 状态
     */
    public GalleryUiState withLoaded(boolean hasMore) {
        return new GalleryUiState(
            currentPage,
            pageSize,
            !hasMore,
            false,
            currentSourceId,
            currentCategory,
            wallpapers,
            LoadingState.LOADED,
            null
        );
    }
    
    /**
     * 创建加载失败的副本
     *
     * @param error 错误信息
     * @return 新的 UI 状态
     */
    public GalleryUiState withError(String error) {
        return new GalleryUiState(
            currentPage,
            pageSize,
            isLastPage,
            false,
            currentSourceId,
            currentCategory,
            wallpapers,
            LoadingState.ERROR,
            error
        );
    }
    
    /**
     * 创建切换源后的副本
     *
     * @param sourceId 新源 ID
     * @return 新的 UI 状态
     */
    public GalleryUiState withSourceSwitch(String sourceId) {
        return new GalleryUiState(
            1,
            pageSize,
            false,
            true,
            sourceId,
            null,
            Collections.emptyList(),
            LoadingState.IDLE,
            null
        );
    }
    
    /**
     * 创建切换分类后的副本
     *
     * @param category 新分类
     * @return 新的 UI 状态
     */
    public GalleryUiState withCategorySwitch(WallpaperCategory category) {
        return new GalleryUiState(
            1,
            pageSize,
            false,
            true,
            currentSourceId,
            category,
            Collections.emptyList(),
            LoadingState.IDLE,
            null
        );
    }
    
    /**
     * 创建刷新后的副本
     *
     * @return 新的 UI 状态
     */
    public GalleryUiState withRefresh() {
        return new GalleryUiState(
            1,
            pageSize,
            false,
            true,
            currentSourceId,
            currentCategory,
            Collections.emptyList(),
            LoadingState.IDLE,
            null
        );
    }
    
    @Override
    public String toString() {
        return "GalleryUiState{" +
            "currentPage=" + currentPage +
            ", pageSize=" + pageSize +
            ", isLastPage=" + isLastPage +
            ", sourceId='" + currentSourceId + '\'' +
            ", category=" + (currentCategory != null ? currentCategory.getName() : "null") +
            ", wallpaperCount=" + wallpapers.size() +
            ", loadingState=" + loadingState +
            ", errorMessage='" + errorMessage + '\'' +
            '}';
    }
}
