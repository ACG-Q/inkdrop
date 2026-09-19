package com.example.wallpaper.ui.source;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.example.wallpaper.util.LogUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.example.wallpaper.util.ToastUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;
import com.example.wallpaper.data.source.WallpaperSource;
import com.example.wallpaper.data.source.WallpaperSourceManager;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SourceManageActivity extends AppCompatActivity {
    @Inject
    WallpaperSourceManager sourceManager;

    private SourceAdapter adapter;
    private TextView tvEmpty;
    private LinearLayout layoutBatchBar;
    private TextView tvBatchCount;
    private boolean isBatchMode = false;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onFileSelected);

    private final ActivityResultLauncher<Intent> batchExportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) saveBatchExport(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_manage);
        initViews();
        loadSources();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSources();
    }

    private void initViews() {
        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(SourceManageActivity.this, SourceDetailActivity.class);
            startActivity(intent);
        });

        TextView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> showMenu(v));

        tvEmpty = findViewById(R.id.tv_empty);
        layoutBatchBar = findViewById(R.id.layout_batch_bar);
        tvBatchCount = findViewById(R.id.tv_batch_count);

        TextView tvBatchCancel = findViewById(R.id.tv_batch_cancel);
        tvBatchCancel.setOnClickListener(v -> exitBatchMode());

        TextView tvBatchMenu = findViewById(R.id.tv_batch_menu);
        tvBatchMenu.setOnClickListener(v -> showBatchMenu(v));

        RecyclerView rvSources = findViewById(R.id.rv_sources);
        rvSources.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SourceAdapter();
        adapter.setListener(new SourceAdapter.OnSourceActionListener() {
            @Override
            public void onSourceClick(WallpaperSource source) {
                if (isBatchMode) return;
                sourceManager.setSelectedSource(source.getSourceId());
                loadSources();
            }

            @Override
            public void onSourceToggle(WallpaperSource source, boolean enabled) {
                sourceManager.setSourceEnabled(source.getSourceId(), enabled);
            }

            @Override
            public void onSourceEdit(WallpaperSource source) {
                Intent intent = new Intent(SourceManageActivity.this, SourceDetailActivity.class);
                intent.putExtra("source_id", source.getSourceId());
                startActivity(intent);
            }
        });
        rvSources.setAdapter(adapter);
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "内置导入");
        popup.getMenu().add(0, 2, 1, "网路导入");
        popup.getMenu().add(0, 3, 2, "本地导入");
        popup.getMenu().add(0, 4, 3, "批量");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    importBuiltin();
                    return true;
                case 2:
                    importFromUrl();
                    return true;
                case 3:
                    filePickerLauncher.launch("application/json");
                    return true;
                case 4:
                    enterBatchMode();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void importBuiltin() {
        try {
            String[] files = getAssets().list("sources");
            if (files == null || files.length == 0) {
                ToastUtils.show("没有内置源");
                return;
            }

            String[] displayNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                String name = files[i].replace(".json", "").replace(".js", "");
                displayNames[i] = name.substring(0, 1).toUpperCase() + name.substring(1);
            }

            new AlertDialog.Builder(this)
                    .setTitle("内置源")
                    .setItems(displayNames, (dialog, which) -> {
                        copyBuiltinSource(files[which], displayNames[which]);
                    })
                    .show();
        } catch (Exception e) {
            ToastUtils.show("读取内置源失败");
        }
    }

    private void copyBuiltinSource(String fileName, String displayName) {
        try (java.io.InputStream is = getAssets().open("sources/" + fileName)) {
            String content = com.example.wallpaper.util.FileUtils.readInputStream(is);

            String sourceId = fileName.replace(".json", "").replace(".js", "");

            if (fileName.endsWith(".js")) {
                boolean ok = sourceManager.addCustomJsSource(sourceId, displayName, content);
                if (ok) {
                    ToastUtils.show("导入成功: " + displayName);
                    loadSources();
                } else {
                    ToastUtils.show("导入失败");
                }
            } else {
                boolean ok = sourceManager.addCustomSource(sourceId, displayName, content);
                if (ok) {
                    ToastUtils.show("导入成功: " + displayName);
                    loadSources();
                } else {
                    ToastUtils.show("导入失败");
                }
            }
        } catch (Exception e) {
            ToastUtils.show("导入失败: " + e.getMessage());
        }
    }

    private void importFromUrl() {
        EditText etUrl = new EditText(this);
        etUrl.setHint("https://example.com/sources.json");
        etUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("网路导入")
                .setView(etUrl)
                .setPositiveButton("导入", (dialog, which) -> {
                    String url = etUrl.getText().toString().trim();
                    if (!url.isEmpty()) {
                        fetchAndImportSource(url);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void fetchAndImportSource(String url) {
        ToastUtils.show("正在获取...");
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                ToastUtils.show("获取失败: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    ToastUtils.show("请求失败: HTTP " + response.code());
                    return;
                }
                
                String body;
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    body = responseBody.string();
                }
                
                runOnUiThread(() -> {
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(body);
                        String sourceId = obj.optString("sourceId", "");
                        String sourceName = obj.optString("sourceName", sourceId);
                        if (sourceId.isEmpty()) {
                            ToastUtils.show("JSON 缺少 sourceId");
                            return;
                        }
                        boolean ok = sourceManager.addCustomSource(sourceId, sourceName, body);
                        if (ok) {
                            ToastUtils.show("导入成功: " + sourceName);
                            loadSources();
                        } else {
                            ToastUtils.show("导入失败");
                        }
                    } catch (Exception e) {
                        ToastUtils.show("解析失败: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) return;
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            String json = com.example.wallpaper.util.FileUtils.readInputStream(is);
            
            org.json.JSONObject obj = new org.json.JSONObject(json);
            String sourceId = obj.optString("sourceId", "");
            String sourceName = obj.optString("sourceName", sourceId);

            if (sourceId.isEmpty()) {
                ToastUtils.show("JSON 缺少 sourceId 字段");
                return;
            }

            boolean ok = sourceManager.addCustomSource(sourceId, sourceName, json);
            if (ok) {
                ToastUtils.show("导入成功: " + sourceName);
                loadSources();
            } else {
                ToastUtils.show("导入失败");
            }
        } catch (Exception e) {
            ToastUtils.show("导入失败: " + e.getMessage());
        }
    }

    private void enterBatchMode() {
        isBatchMode = true;
        adapter.setBatchMode(true);
        layoutBatchBar.setVisibility(View.VISIBLE);
    }

    private void exitBatchMode() {
        isBatchMode = false;
        adapter.setBatchMode(false);
        layoutBatchBar.setVisibility(View.GONE);
        tvBatchCount.setText("已选择 0 个");
    }

    private void showBatchMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "导出");
        popup.getMenu().add(0, 2, 1, "删除");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    batchExport();
                    return true;
                case 2:
                    batchDelete();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void batchExport() {
        Set<String> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            ToastUtils.show("请先选择要导出的源");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "wallpaper_sources_" + System.currentTimeMillis() + ".json");
        batchExportLauncher.launch(intent);
    }

    private void batchDelete() {
        Set<String> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            ToastUtils.show("请先选择要删除的源");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("批量删除")
                .setMessage("确定要删除选中的 " + selectedIds.size() + " 个源吗？")
                .setPositiveButton("删除", (d, w) -> {
                    int count = 0;
                    for (String id : selectedIds) {
                        if (sourceManager.removeSource(id)) count++;
                    }
                    ToastUtils.show("已删除 " + count + " 个源");
                    exitBatchMode();
                    loadSources();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveBatchExport(Uri uri) {
        try {
            List<WallpaperSource> selectedSources = adapter.getSelectedSources();
            org.json.JSONArray jsonArray = new org.json.JSONArray();

            for (WallpaperSource source : selectedSources) {
                String configJson = sourceManager.readSourceConfig(source.getSourceId());
                if (configJson != null) {
                    try {
                        jsonArray.put(new org.json.JSONObject(configJson));
                    } catch (Exception e) {
                        LogUtils.e("Parse config error", e);
                    }
                }
            }

            try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(jsonArray.toString(2).getBytes("UTF-8"));
                    ToastUtils.show("导出成功，共 " + selectedSources.size() + " 个源");
                    exitBatchMode();
                }
            }
        } catch (Exception e) {
            ToastUtils.show("导出失败: " + e.getMessage());
        }
    }

    private void loadSources() {
        List<WallpaperSource> sources = sourceManager.getAllSources();
        String selectedId = sourceManager.getSelectedSourceId();
        adapter.setSources(sources, selectedId);
        tvEmpty.setVisibility(sources.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
