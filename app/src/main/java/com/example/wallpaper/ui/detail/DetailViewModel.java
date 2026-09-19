package com.example.wallpaper.ui.detail;

import android.app.Application;
import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wallpaper.R;
import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.data.repository.Resource;
import com.example.wallpaper.data.repository.WallpaperRepository;
import com.example.wallpaper.di.AppModule;
import com.example.wallpaper.ui.common.SingleLiveEvent;
import com.example.wallpaper.util.image.ImageDownloader;
import com.example.wallpaper.util.image.WallpaperSetter;
import com.example.wallpaper.util.image.ImageCallback;

import java.io.File;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 壁纸详情 ViewModel
 * 处理壁纸加载、下载、分享、设置等操作
 */
@HiltViewModel
public class DetailViewModel extends AndroidViewModel {
    private final WallpaperRepository repository;
    private final ExecutorService executor;
    
    private final MutableLiveData<Resource<Wallpaper>> wallpaper = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    private String currentImageUrl;

    @Inject
    public DetailViewModel(
            @NonNull Application application,
            WallpaperRepository repository,
            @Named(AppModule.IO_EXECUTOR) ExecutorService executor) {
        super(application);
        this.repository = repository;
        this.executor = executor;
    }

    public LiveData<Resource<Wallpaper>> getWallpaper() {
        return wallpaper;
    }

    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    public SingleLiveEvent<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadWallpaper(int wallpaperId) {
        isLoading.setValue(true);
        repository.getWallpaperById(wallpaperId).observeForever(resource -> {
            isLoading.setValue(false);
            if (resource.isSuccess()) {
                wallpaper.setValue(resource);
                // URL 已经在数据层处理完整，直接使用
                currentImageUrl = resource.getData().getUrl();
            } else {
                message.setValue(resource.getMessage());
            }
        });
    }

    public void downloadWallpaper() {
        if (currentImageUrl == null) {
            message.setValue("图片未加载");
            return;
        }
        
        isLoading.setValue(true);
        
        ImageDownloader.downloadToWallpapers(currentImageUrl, new ImageCallback.DownloadCallback() {
            @Override
            public void onSuccess(File file) {
                isLoading.setValue(false);
                message.setValue(getApplication().getString(R.string.download_success));
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                message.setValue(getApplication().getString(R.string.download_failed));
            }
        });
    }

    public void shareWallpaper() {
        if (currentImageUrl == null) {
            message.setValue("图片未加载");
            return;
        }
        
        ImageDownloader.downloadToCache(currentImageUrl, new ImageCallback.DownloadCallback() {
            @Override
            public void onSuccess(File file) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getApplication().startActivity(Intent.createChooser(shareIntent, 
                    getApplication().getString(R.string.share)));
            }

            @Override
            public void onError(String errorMsg) {
                message.setValue("分享失败");
            }
        });
    }

    public void setWallpaper(int type) {
        if (currentImageUrl == null) {
            message.setValue("图片未加载");
            return;
        }
        
        isLoading.setValue(true);
        
        WallpaperSetter.WallpaperType wallpaperType;
        if (type == (WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK)) {
            wallpaperType = WallpaperSetter.WallpaperType.BOTH;
        } else if (type == WallpaperManager.FLAG_LOCK) {
            wallpaperType = WallpaperSetter.WallpaperType.LOCK_SCREEN;
        } else {
            wallpaperType = WallpaperSetter.WallpaperType.HOME_SCREEN;
        }
        
        WallpaperSetter.set(currentImageUrl, wallpaperType, new ImageCallback.SetWallpaperCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                message.setValue(getApplication().getString(R.string.set_wallpaper_success));
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                message.setValue(getApplication().getString(R.string.set_wallpaper_failed));
            }
        });
    }

    public void setFavorite(boolean favorite) {
        if (wallpaper.getValue() != null && wallpaper.getValue().isSuccess()) {
            int wallpaperId = wallpaper.getValue().getData().getId();
            repository.setFavorite(wallpaperId, favorite);
            isFavorite.setValue(favorite);
        }
    }
}
