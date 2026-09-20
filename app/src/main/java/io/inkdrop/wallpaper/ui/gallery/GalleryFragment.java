package io.inkdrop.wallpaper.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import io.inkdrop.wallpaper.R;
import io.inkdrop.wallpaper.data.source.WallpaperCategory;
import io.inkdrop.wallpaper.data.source.WallpaperSource;
import io.inkdrop.wallpaper.data.source.WallpaperSourceManager;
import io.inkdrop.wallpaper.ui.detail.DetailActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GalleryFragment extends Fragment {
    private GalleryViewModel viewModel;
    private WallpaperAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private RecyclerView recyclerView;
    private RecyclerView rvSkeleton;
    private TextView tvError;
    private Spinner spinnerSource;
    private Spinner spinnerCategory;
    private LinearLayout layoutCategory;

    @Inject
    WallpaperSourceManager sourceManager;

    private List<WallpaperSource> sourceList = new ArrayList<>();
    private List<WallpaperCategory> categoryList = new ArrayList<>();
    private boolean isInitialSetup = true;
    private boolean isCategorySpinnerInitial = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sourceManager.init(requireContext().getApplicationContext());
        
        initViews(view);
        setupRecyclerView();
        setupSkeleton();
        observeViewModel();
        setupSourceSpinner();
    }
    
    private int calculateInitialPageSize() {
        int screenHeight = recyclerView.getHeight();
        if (screenHeight <= 0) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            screenHeight = dm.heightPixels;
        }
        
        float density = getResources().getDisplayMetrics().density;
        int itemHeight = (int) (300 * density);
        int rowsNeeded = (screenHeight / itemHeight) + 2;
        int pageSize = rowsNeeded * 2;
        
        return Math.max(4, Math.min(pageSize, 20));
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        rvSkeleton = view.findViewById(R.id.rv_skeleton);
        tvError = view.findViewById(R.id.tv_error);
        spinnerSource = view.findViewById(R.id.spinner_source);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        layoutCategory = view.findViewById(R.id.layout_category);

        TextView btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            showSkeleton();
            viewModel.refresh();
        });
    }

    private void setupRecyclerView() {
        adapter = new WallpaperAdapter();
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, 
            StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener(wallpaper -> {
            Intent intent = new Intent(requireContext(), DetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.getId());
            startActivity(intent);
        });

        adapter.setOnFavoriteClickListener((wallpaper, isFavorite) -> {
            viewModel.setFavorite(wallpaper.getId(), isFavorite);
        });
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    StaggeredGridLayoutManager lm = 
                        (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                    if (lm != null) {
                        int[] lastVisible = lm.findLastVisibleItemPositions(null);
                        int maxVisible = 0;
                        for (int pos : lastVisible) {
                            if (pos > maxVisible) maxVisible = pos;
                        }
                        int total = lm.getItemCount();
                        if (maxVisible >= total - 4) {
                            viewModel.loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void setupSkeleton() {
        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setItemCount(6);
        StaggeredGridLayoutManager skeletonLayout = new StaggeredGridLayoutManager(2, 
            StaggeredGridLayoutManager.VERTICAL);
        rvSkeleton.setLayoutManager(skeletonLayout);
        rvSkeleton.setAdapter(skeletonAdapter);
    }

    private void showSkeleton() {
        rvSkeleton.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showContent() {
        rvSkeleton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        rvSkeleton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    private void setupSourceSpinner() {
        sourceList = sourceManager.getAvailableSources();
        String selectedId = sourceManager.getSelectedSourceId();

        List<String> sourceNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < sourceList.size(); i++) {
            WallpaperSource s = sourceList.get(i);
            sourceNames.add(s.getSourceName());
            if (s.getSourceId().equals(selectedId)) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sourceNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(spinnerAdapter);

        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSetup) {
                    isInitialSetup = false;
                    // 首次加载：设置spinner后直接加载，不触发switchSource
                    WallpaperSource selected = sourceList.get(position);
                    sourceManager.setSelectedSource(selected.getSourceId());
                    showSkeleton();
                    updateCategorySpinner(selected);
                    recyclerView.post(() -> {
                        int pageSize = calculateInitialPageSize();
                        viewModel.setPageSize(pageSize);
                        viewModel.switchSource(selected.getSourceId());
                    });
                    return;
                }
                
                WallpaperSource selected = sourceList.get(position);
                String currentId = sourceManager.getSelectedSourceId();
                if (!selected.getSourceId().equals(currentId)) {
                    sourceManager.setSelectedSource(selected.getSourceId());
                    showSkeleton();
                    viewModel.switchSource(selected.getSourceId());
                    updateCategorySpinner(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spinnerSource.setSelection(selectedIndex);
    }
    
    private void updateCategorySpinner(WallpaperSource source) {
        categoryList.clear();
        isCategorySpinnerInitial = true;
        
        if (source.getConfig() != null && source.getConfig().getCategories() != null) {
            categoryList.addAll(source.getConfig().getCategories());
        }
        
        if (categoryList.isEmpty()) {
            layoutCategory.setVisibility(View.GONE);
            return;
        }
        
        layoutCategory.setVisibility(View.VISIBLE);
        
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("全部");
        for (WallpaperCategory cat : categoryList) {
            if (!"全部".equals(cat.getName())) {
                categoryNames.add(cat.getName());
            }
        }
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setSelection(0);
        
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCategorySpinnerInitial) {
                    isCategorySpinnerInitial = false;
                    return;
                }
                if (position == 0) {
                    viewModel.setCategory(null);
                } else if (position - 1 < categoryList.size()) {
                    viewModel.setCategory(categoryList.get(position - 1));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeViewModel() {
        viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        
        // 配置变更后恢复 adapter 数据
        if (!viewModel.getWallpapers().isEmpty()) {
            adapter.setWallpapers(viewModel.getWallpapers());
            showContent();
        }
        
        // 观察 UI 状态变化
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            
            if (state.hasError()) {
                showError(state.getErrorMessage());
            } else if (state.isLoading() && state.isFreshLoad()) {
                // 新加载（切换分类/源/刷新）：清空旧数据，显示 skeleton
                adapter.clear();
                showSkeleton();
            } else if (state.isLoading()) {
                // 加载更多：保持现有数据，显示内容
                showContent();
            } else {
                showContent();
            }
        });
        
        // 观察新加载的壁纸
        viewModel.getNewWallpaper().observe(getViewLifecycleOwner(), wallpaper -> {
            if (wallpaper != null) {
                showContent();
                adapter.addWallpaper(wallpaper);
            }
        });
    }
}
