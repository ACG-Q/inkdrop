package io.inkdrop.wallpaper.ui.source;

import android.content.Intent;
import android.os.Bundle;
import io.inkdrop.wallpaper.util.LogUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.inkdrop.wallpaper.util.ToastUtils;

import androidx.appcompat.app.AppCompatActivity;

import io.inkdrop.wallpaper.R;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JsonFieldPickerActivity extends AppCompatActivity {
    private LinearLayout layoutChips;
    private TextView tvLoading;
    private JsonTreeView jsonTreeView;
    private String jsonUrl;
    private String[] targetFields;
    private String detectedListField, detectedIdField, detectedUrlField, detectedWidthField, detectedHeightField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_json_field_picker);
        initViews();
        loadJson();
    }

    private void initViews() {
        layoutChips = findViewById(R.id.layout_chips);
        tvLoading = findViewById(R.id.tv_loading);
        jsonTreeView = findViewById(R.id.json_tree_view);

        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnReset = findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(v -> {
            detectedListField = null;
            detectedIdField = null;
            detectedUrlField = null;
            detectedWidthField = null;
            detectedHeightField = null;
            ToastUtils.show("已重置");
        });

        TextView btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> confirmSelection());

        jsonTreeView.setOnFieldSelectedListener((fieldName, fieldType, path) -> {
            LogUtils.d("Selected: " + fieldName + " (" + fieldType + ") at " + path);
        });
    }

    private void loadJson() {
        jsonUrl = getIntent().getStringExtra("jsonUrl");
        targetFields = getIntent().getStringArrayExtra("targetFields");

        if (jsonUrl == null || jsonUrl.isEmpty()) {
            ToastUtils.show("URL为空");
            finish();
            return;
        }

        tvLoading.setVisibility(View.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(jsonUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    tvLoading.setVisibility(View.GONE);
                    ToastUtils.show("请求失败: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    tvLoading.setVisibility(View.GONE);
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        jsonTreeView.setData(json);
                        autoDetectFields(json);
                        updateChips();
                    } catch (Exception e) {
                        ToastUtils.show("JSON解析失败");
                    }
                });
            }
        });
    }

    private void autoDetectFields(org.json.JSONObject json) {
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = json.get(key);
                if (value instanceof org.json.JSONArray) {
                    org.json.JSONArray arr = (org.json.JSONArray) value;
                    if (arr.length() > 0 && arr.get(0) instanceof org.json.JSONObject) {
                        detectedListField = key;
                        org.json.JSONObject first = arr.getJSONObject(0);
                        java.util.Iterator<String> fieldKeys = first.keys();
                        while (fieldKeys.hasNext()) {
                            String field = fieldKeys.next();
                            String lower = field.toLowerCase();
                            if (lower.equals("id")) detectedIdField = field;
                            else if (lower.equals("url") || lower.equals("src") || lower.equals("image") || lower.equals("imageurl")) detectedUrlField = field;
                            else if (lower.equals("width")) detectedWidthField = field;
                            else if (lower.equals("height")) detectedHeightField = field;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateChips() {
        layoutChips.removeAllViews();
        addChip("数据列表", detectedListField);
        addChip("ID字段", detectedIdField);
        addChip("URL字段", detectedUrlField);
        addChip("宽度字段", detectedWidthField);
        addChip("高度字段", detectedHeightField);
    }

    private void addChip(String label, String value) {
        TextView chip = new TextView(this);
        chip.setText(label + ": " + (value != null ? value : "未检测"));
        chip.setTextSize(12);
        chip.setTextColor(getColor(R.color.white));
        chip.setBackgroundColor(getColor(R.color.black));
        chip.setPadding(24, 8, 24, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 12, 0);
        chip.setLayoutParams(params);

        layoutChips.addView(chip);
    }

    private void confirmSelection() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("listField", detectedListField);
        resultIntent.putExtra("idField", detectedIdField);
        resultIntent.putExtra("urlField", detectedUrlField);
        resultIntent.putExtra("widthField", detectedWidthField);
        resultIntent.putExtra("heightField", detectedHeightField);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
