package io.inkdrop.wallpaper.ui.favorite;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.inkdrop.wallpaper.data.local.WallpaperEntity;
import io.inkdrop.wallpaper.data.repository.WallpaperRepository;
import io.inkdrop.wallpaper.ui.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FavoriteViewModel extends ViewModel {
    private final WallpaperRepository repository;
    
    private final MutableLiveData<UiState<List<WallpaperEntity>>> uiState = new MutableLiveData<>(UiState.empty());

    @Inject
    public FavoriteViewModel(WallpaperRepository repository) {
        this.repository = repository;
        loadFavorites();
    }

    public LiveData<UiState<List<WallpaperEntity>>> getUiState() {
        return uiState;
    }

    public void loadFavorites() {
        uiState.setValue(UiState.loading());
        repository.getFavoriteWallpapers().observeForever(favorites -> 
            uiState.setValue(UiState.success(favorites)));
    }

    public void removeFavorite(int wallpaperId) {
        repository.setFavorite(wallpaperId, false);
        loadFavorites();
    }
}
