package com.example.wallpaper.ui.detail;

import android.app.Application;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.wallpaper.R;
import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.data.repository.Resource;
import com.example.wallpaper.data.repository.WallpaperRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DetailViewModel extends AndroidViewModel {
    private final WallpaperRepository repository;
    private final ExecutorService executor;
    
    private final MutableLiveData<Resource<Wallpaper>> wallpaper = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    private String currentImageUrl;

    @Inject
    public DetailViewModel(@NonNull Application application, WallpaperRepository repository) {
        super(application);
        this.repository = repository;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Resource<Wallpaper>> getWallpaper() {
        return wallpaper;
    }

    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    public LiveData<String> getMessage() {
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
                currentImageUrl = "https://inkpaper.foolstack.net" + resource.getData().getUrl();
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
        
        Glide.with(getApplication())
            .asBitmap()
            .load(currentImageUrl)
            .listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                    Target<Bitmap> target, boolean isFirstResource) {
                    isLoading.setValue(false);
                    message.setValue("下载失败");
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, Object model, 
                    Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                    executor.execute(() -> {
                        boolean saved = saveBitmap(resource);
                        isLoading.setValue(false);
                        message.setValue(saved ? 
                            getApplication().getString(R.string.download_success) :
                            getApplication().getString(R.string.download_failed));
                    });
                    return false;
                }
            })
            .submit();
    }

    private boolean saveBitmap(Bitmap bitmap) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "Wallpapers");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, "wallpaper_" + System.currentTimeMillis() + ".jpg");
        
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            
            // 通知媒体库扫描
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            getApplication().sendBroadcast(mediaScanIntent);
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shareWallpaper() {
        if (currentImageUrl == null) {
            message.setValue("图片未加载");
            return;
        }
        
        Glide.with(getApplication())
            .asBitmap()
            .load(currentImageUrl)
            .listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                    Target<Bitmap> target, boolean isFirstResource) {
                    message.setValue("加载失败");
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, Object model, 
                    Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                    File file = new File(getApplication().getCacheDir(), "share_image.jpg");
                    try (OutputStream out = new FileOutputStream(file)) {
                        resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getApplication().startActivity(Intent.createChooser(shareIntent, 
                            getApplication().getString(R.string.share)));
                    } catch (IOException ex) {
                        message.setValue("分享失败");
                    }
                    return false;
                }
            })
            .submit();
    }

    public void setWallpaper(int type) {
        if (currentImageUrl == null) {
            message.setValue("图片未加载");
            return;
        }
        
        isLoading.setValue(true);
        
        Glide.with(getApplication())
            .asBitmap()
            .load(currentImageUrl)
            .listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                    Target<Bitmap> target, boolean isFirstResource) {
                    isLoading.setValue(false);
                    message.setValue("设置失败");
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, Object model, 
                    Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                    executor.execute(() -> {
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplication());
                            wallpaperManager.setBitmap(resource, null, true, type);
                            isLoading.setValue(false);
                            message.setValue(getApplication().getString(R.string.set_wallpaper_success));
                        } catch (IOException e) {
                            isLoading.setValue(false);
                            message.setValue(getApplication().getString(R.string.set_wallpaper_failed));
                        }
                    });
                    return false;
                }
            })
            .submit();
    }

    public void setFavorite(boolean favorite) {
        if (wallpaper.getValue() != null && wallpaper.getValue().isSuccess()) {
            int wallpaperId = wallpaper.getValue().getData().getId();
            repository.setFavorite(wallpaperId, favorite);
            isFavorite.setValue(favorite);
        }
    }
}
