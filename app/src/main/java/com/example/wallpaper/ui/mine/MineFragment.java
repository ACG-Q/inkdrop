package com.example.wallpaper.ui.mine;

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

import com.example.wallpaper.R;
import com.example.wallpaper.data.source.GenericSource;
import com.example.wallpaper.ui.favorite.FavoriteActivity;
import com.example.wallpaper.ui.log.LogViewerActivity;
import com.example.wallpaper.ui.source.SourceManageActivity;
import com.example.wallpaper.util.LogUtils;

import java.io.File;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MineFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout btnFavorites = view.findViewById(R.id.btn_favorites);
        btnFavorites.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), FavoriteActivity.class));
        });

        LinearLayout btnSourceManage = view.findViewById(R.id.btn_source_manage);
        btnSourceManage.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SourceManageActivity.class));
        });

        LinearLayout btnLogs = view.findViewById(R.id.btn_logs);
        btnLogs.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LogViewerActivity.class));
        });

        LinearLayout btnClearCache = view.findViewById(R.id.btn_clear_cache);
        TextView tvCacheSize = view.findViewById(R.id.tv_cache_size);

        long cacheSize = getFolderSize(requireContext().getCacheDir());
        tvCacheSize.setText(formatSize(cacheSize));

        btnClearCache.setOnClickListener(v -> {
            deleteFolder(requireContext().getCacheDir());
            GenericSource.clearDimensionsCache();
            tvCacheSize.setText("0 B");
        });

        TextView tvVersion = view.findViewById(R.id.tv_version);
        try {
            String version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tvVersion.setText("版本 " + version);
        } catch (Exception e) {
            tvVersion.setText("版本 1.0.0");
        }

        Spinner spinnerKeepDays = view.findViewById(R.id.spinner_keep_days);
        String[] dayOptions = {"7 天", "14 天", "30 天"};
        int[] dayValues = {7, 14, 30};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, dayOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKeepDays.setAdapter(spinnerAdapter);

        int currentDays = LogUtils.getKeepDays();
        for (int i = 0; i < dayValues.length; i++) {
            if (dayValues[i] == currentDays) {
                spinnerKeepDays.setSelection(i);
                break;
            }
        }

        spinnerKeepDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                LogUtils.setKeepDays(dayValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += file.isDirectory() ? getFolderSize(file) : file.length();
                }
            }
        }
        return size;
    }

    private void deleteFolder(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
