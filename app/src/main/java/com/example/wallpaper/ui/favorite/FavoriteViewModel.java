package com.example.wallpaper.ui.favorite;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.wallpaper.data.local.WallpaperEntity;
import com.example.wallpaper.data.repository.WallpaperRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FavoriteViewModel extends ViewModel {
    private final WallpaperRepository repository;
    
    private final MutableLiveData<List<WallpaperEntity>> favorites = new MutableLiveData<>();

    @Inject
    public FavoriteViewModel(WallpaperRepository repository) {
        this.repository = repository;
        loadFavorites();
    }

    public LiveData<List<WallpaperEntity>> getFavorites() {
        return favorites;
    }

    public void loadFavorites() {
        repository.getFavoriteWallpapers().observeForever(favorites::setValue);
    }

    public void removeFavorite(WallpaperEntity wallpaper) {
        repository.removeFavorite(wallpaper);
        loadFavorites();
    }
}
