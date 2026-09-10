package com.example.wallpaper.ui.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;
import com.example.wallpaper.ui.detail.DetailActivity;
import com.example.wallpaper.ui.gallery.WallpaperAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FavoriteActivity extends AppCompatActivity {
    private FavoriteViewModel viewModel;
    private WallpaperAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        
        initViews();
        setupRecyclerView();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadFavorites();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void setupRecyclerView() {
        adapter = new WallpaperAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener(wallpaper -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.getId());
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        
        viewModel.getFavorites().observe(this, favorites -> {
            if (favorites == null || favorites.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                // 将 WallpaperEntity 转换为 Wallpaper 显示
                // adapter.setWallpapers(favorites); // 需要转换
            }
        });
    }
}
