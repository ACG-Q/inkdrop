package io.inkdrop.wallpaper.ui.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.inkdrop.wallpaper.R;
import io.inkdrop.wallpaper.data.local.WallpaperEntity;
import io.inkdrop.wallpaper.data.remote.Wallpaper;
import io.inkdrop.wallpaper.ui.detail.DetailActivity;
import io.inkdrop.wallpaper.ui.gallery.WallpaperAdapter;

import java.util.ArrayList;
import java.util.List;

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

        TextView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
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

        adapter.setOnFavoriteClickListener((wallpaper, isFavorite) -> {
            viewModel.removeFavorite(wallpaper.getId());
        });
    }

    private void observeViewModel() {
        viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        
        viewModel.getUiState().observe(this, state -> {
            if (state.isLoading()) {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            } else if (state.isError() || state.getData() == null || state.getData().isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setWallpapers(convertToWallpapers(state.getData()));
            }
        });
    }

    private List<Wallpaper> convertToWallpapers(List<WallpaperEntity> entities) {
        List<Wallpaper> wallpapers = new ArrayList<>();
        for (WallpaperEntity entity : entities) {
            Wallpaper wp = new Wallpaper(entity.getId(), entity.getUrl(), entity.getHash(),
                entity.getWidth(), entity.getHeight(), entity.getCreatedAt());
            wp.setFavorite(true);
            wallpapers.add(wp);
        }
        return wallpapers;
    }
}
