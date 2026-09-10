package com.example.wallpaper.ui.gallery;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.data.repository.Resource;
import com.example.wallpaper.data.repository.WallpaperRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class GalleryViewModel extends ViewModel {
    private final WallpaperRepository repository;
    
    private final MutableLiveData<Resource<List<Wallpaper>>> wallpapers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private boolean isLastPage = false;

    @Inject
    public GalleryViewModel(WallpaperRepository repository) {
        this.repository = repository;
        loadWallpapers();
    }

    public LiveData<Resource<List<Wallpaper>>> getWallpapers() {
        return wallpapers;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadWallpapers() {
        if (isLoading.getValue() != null && isLoading.getValue()) {
            return;
        }
        
        isLoading.setValue(true);
        
        repository.getWallpapers(null, currentPage, PAGE_SIZE).observeForever(resource -> {
            isLoading.setValue(false);
            
            if (resource.isSuccess()) {
                List<Wallpaper> data = resource.getData();
                if (data != null && !data.isEmpty()) {
                    wallpapers.setValue(resource);
                    if (data.size() < PAGE_SIZE) {
                        isLastPage = true;
                    }
                } else {
                    isLastPage = true;
                }
            } else if (resource.isError()) {
                errorMessage.setValue(resource.getMessage());
            }
        });
    }

    public void loadNextPage() {
        if (!isLastPage && (isLoading.getValue() == null || !isLoading.getValue())) {
            currentPage++;
            loadWallpapers();
        }
    }

    public void refresh() {
        currentPage = 1;
        isLastPage = false;
        loadWallpapers();
    }

    public void setFavorite(int wallpaperId, boolean isFavorite) {
        repository.setFavorite(wallpaperId, isFavorite);
    }
}
