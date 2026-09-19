package com.example.wallpaper.ui.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.example.wallpaper.util.ToastUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wallpaper.R;
import com.example.wallpaper.util.image.ImageLoader;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DetailActivity extends AppCompatActivity {
    private DetailViewModel viewModel;
    private ImageView wallpaperFull;
    private ImageButton btnDownload, btnShare, btnSetWallpaper, btnFavorite;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        
        initViews();
        setupClickListeners();
        observeViewModel();
        
        int wallpaperId = getIntent().getIntExtra("wallpaper_id", -1);
        if (wallpaperId != -1) {
            viewModel.loadWallpaper(wallpaperId);
        }
    }

    private void initViews() {
        wallpaperFull = findViewById(R.id.wallpaper_full);
        btnDownload = findViewById(R.id.btn_download);
        btnShare = findViewById(R.id.btn_share);
        btnSetWallpaper = findViewById(R.id.btn_set_wallpaper);
        btnFavorite = findViewById(R.id.btn_favorite);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnDownload.setOnClickListener(v -> viewModel.downloadWallpaper());
        btnShare.setOnClickListener(v -> viewModel.shareWallpaper());
        btnSetWallpaper.setOnClickListener(v -> showSetWallpaperDialog());
        btnFavorite.setOnClickListener(v -> {
            Boolean isFavorite = viewModel.getIsFavorite().getValue();
            viewModel.setFavorite(isFavorite == null || !isFavorite);
        });
    }

    private void showSetWallpaperDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.set_wallpaper)
            .setItems(new CharSequence[]{
                getString(R.string.set_wallpaper),
                getString(R.string.set_lockscreen),
                getString(R.string.set_both)
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        viewModel.setWallpaper(android.app.WallpaperManager.FLAG_SYSTEM);
                        break;
                    case 1:
                        viewModel.setWallpaper(android.app.WallpaperManager.FLAG_LOCK);
                        break;
                    case 2:
                        viewModel.setWallpaper(android.app.WallpaperManager.FLAG_SYSTEM | 
                            android.app.WallpaperManager.FLAG_LOCK);
                        break;
                }
            })
            .show();
    }

    private void observeViewModel() {
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        
        viewModel.getWallpaper().observe(this, resource -> {
            if (resource.isSuccess()) {
                // URL 已经在数据层处理完整，直接使用
                String imageUrl = resource.getData().getUrl();
                ImageLoader.load(wallpaperFull, imageUrl);
            }
        });
        
        viewModel.getIsFavorite().observe(this, isFavorite -> {
            btnFavorite.setImageResource(isFavorite ? 
                R.drawable.ic_favorite : R.drawable.ic_favorite);
            btnFavorite.setAlpha(isFavorite ? 1.0f : 0.5f);
        });
        
        viewModel.getMessage().observe(this, message -> {
            ToastUtils.show(message);
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
}
