package io.inkdrop.wallpaper.ui.gallery;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.inkdrop.wallpaper.data.remote.Wallpaper;
import io.inkdrop.wallpaper.data.repository.WallpaperRepository;
import io.inkdrop.wallpaper.data.source.WallpaperCategory;
import io.inkdrop.wallpaper.data.source.WallpaperLoadManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 画廊页面 ViewModel
 * 管理壁纸列表的加载、分页、源切换等逻辑
 */
@HiltViewModel
public class GalleryViewModel extends ViewModel {
    private final WallpaperRepository repository;

    // UI 状态
    private final MutableLiveData<GalleryUiState> uiState = new MutableLiveData<>(new GalleryUiState());
    
    // 单条壁纸加载通知（用于增量更新）
    private final MutableLiveData<Wallpaper> newWallpaper = new MutableLiveData<>();
    
    // 防止重复加载下一页
    private boolean isLoadingPage = false;

    @Inject
    public GalleryViewModel(WallpaperRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取当前 UI 状态
     *
     * @return UI 状态 LiveData
     */
    public LiveData<GalleryUiState> getUiState() {
        return uiState;
    }

    /**
     * 获取新加载的壁纸通知
     *
     * @return 新壁纸 LiveData
     */
    public LiveData<Wallpaper> getNewWallpaper() {
        return newWallpaper;
    }

    /**
     * 获取当前已加载的壁纸列表（用于配置变更恢复）
     *
     * @return 壁纸列表
     */
    public List<Wallpaper> getWallpapers() {
        GalleryUiState state = uiState.getValue();
        return state != null ? state.getWallpapers() : Collections.emptyList();
    }

    /**
     * 获取当前页码
     *
     * @return 当前页码
     */
    public int getCurrentPage() {
        return uiState.getValue().getCurrentPage();
    }

    /**
     * 获取当前分类
     *
     * @return 当前分类
     */
    public WallpaperCategory getCurrentCategory() {
        return uiState.getValue().getCurrentCategory();
    }

    /**
     * 设置每页数量
     *
     * @param size 每页数量
     */
    public void setPageSize(int size) {
        GalleryUiState current = uiState.getValue();
        uiState.setValue(new GalleryUiState(
            current.getCurrentPage(),
            size,
            current.isLastPage(),
            current.isFreshLoad(),
            current.getCurrentSourceId(),
            current.getCurrentCategory(),
            current.getWallpapers(),
            current.getLoadingState(),
            current.getErrorMessage()
        ));
    }

    /**
     * 设置分类
     *
     * @param category 分类
     */
    public void setCategory(WallpaperCategory category) {
        GalleryUiState current = uiState.getValue();
        
        uiState.setValue(current.withCategorySwitch(category).withLoading());
        
        loadWallpapers();
    }

    /**
     * 加载壁纸
     */
    public void loadWallpapers() {
        GalleryUiState current = uiState.getValue();
        Map<String, String> categoryParams = current.getCurrentCategory() != null ? 
            current.getCurrentCategory().getParams() : null;

        repository.loadBatch(current.getCurrentSourceId(), categoryParams, 
            current.getCurrentPage(), current.getPageSize(),
            new WallpaperLoadManager.LoadCallback() {
                @Override
                public void onWallpaperLoaded(Wallpaper wallpaper) {
                    // 同步更新 state 的 wallpaper 列表（支持配置变更恢复）
                    GalleryUiState current = uiState.getValue();
                    uiState.setValue(current.withWallpaperAdded(wallpaper));
                    newWallpaper.setValue(wallpaper);
                }

                @Override
                public void onAllLoaded(boolean hasMore) {
                    isLoadingPage = false;
                    GalleryUiState state = uiState.getValue();
                    uiState.setValue(state.withLoaded(hasMore));
                }

                @Override
                public void onError(String error) {
                    isLoadingPage = false;
                    GalleryUiState state = uiState.getValue();
                    uiState.setValue(state.withError(error));
                }
            });
    }

    /**
     * 加载下一页
     */
    public void loadNextPage() {
        if (isLoadingPage) return;
        GalleryUiState current = uiState.getValue();
        if (!current.isLastPage() && !current.isLoading()) {
            isLoadingPage = true;
            uiState.setValue(current.withNextPage().withLoading());
            loadWallpapers();
        }
    }

    /**
     * 刷新数据
     */
    public void refresh() {
        GalleryUiState current = uiState.getValue();
        
        if (current.getCurrentSourceId() != null) {
            repository.clearUrlCache(current.getCurrentSourceId());
        }
        
        uiState.setValue(current.withRefresh().withLoading());
        loadWallpapers();
    }

    /**
     * 切换壁纸源
     *
     * @param sourceId 新源 ID
     */
    public void switchSource(String sourceId) {
        GalleryUiState current = uiState.getValue();
        
        if (current.getCurrentSourceId() != null) {
            repository.clearUrlCache(current.getCurrentSourceId());
        }
        
        uiState.setValue(current.withSourceSwitch(sourceId).withLoading());
        loadWallpapers();
    }

    /**
     * 设置收藏状态
     *
     * @param wallpaperId 壁纸 ID
     * @param isFavorite  是否收藏
     */
    public void setFavorite(int wallpaperId, boolean isFavorite) {
        repository.setFavorite(wallpaperId, isFavorite);
    }
}
