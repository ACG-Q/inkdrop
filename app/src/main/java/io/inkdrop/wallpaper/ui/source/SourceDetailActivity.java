package io.inkdrop.wallpaper.ui.source;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import io.inkdrop.wallpaper.R;
import io.inkdrop.wallpaper.data.source.SourceConfig;
import io.inkdrop.wallpaper.data.source.WallpaperSourceManager;
import io.inkdrop.wallpaper.util.LogUtils;
import io.inkdrop.wallpaper.util.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SourceDetailActivity extends AppCompatActivity {
    @Inject
    WallpaperSourceManager sourceManager;

    private EditText etName, etSourceId, etUrlTemplate, etDataField, etIdField, etUrlField;
    private EditText etWidthField, etHeightField, etImageUrlBase, etPreviewSuffix, et302Url;
    private LinearLayout layoutTypeSelector, layoutTypeDisplay, layoutJsonFields, layout302Fields;
    private TextView tvTitle, tvType, btnEditJs, btnDelete;
    private String editingSourceId;
    private String selectedType = "json";
    private KeyListener etSourceIdOriginalListener;

    private final ActivityResultLauncher<Intent> jsEditorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String jsCode = result.getData().getStringExtra("jsCode");
                    String sourceId = result.getData().getStringExtra("sourceId");
                    if (jsCode != null && sourceId != null) {
                        sourceManager.addCustomJsSource(sourceId, etName.getText().toString(), jsCode);
                        ToastUtils.show("JS 源已保存");
                        finish();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onFileSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_detail);
        initViews();
        handleIntent();
    }

    private void initViews() {
        etName = findViewById(R.id.et_detail_name);
        etSourceId = findViewById(R.id.et_detail_source_id);
        etUrlTemplate = findViewById(R.id.et_detail_url_template);
        etDataField = findViewById(R.id.et_detail_data_field);
        etIdField = findViewById(R.id.et_detail_id_field);
        etUrlField = findViewById(R.id.et_detail_url_field);
        etWidthField = findViewById(R.id.et_detail_width_field);
        etHeightField = findViewById(R.id.et_detail_height_field);
        etImageUrlBase = findViewById(R.id.et_detail_image_base_url);
        etPreviewSuffix = findViewById(R.id.et_detail_preview_suffix);
        et302Url = findViewById(R.id.et_302_url);

        layoutTypeSelector = findViewById(R.id.layout_type_selector);
        layoutTypeDisplay = findViewById(R.id.layout_type_display);
        layoutJsonFields = findViewById(R.id.layout_json_fields);
        layout302Fields = findViewById(R.id.layout_302_fields);
        tvTitle = findViewById(R.id.tv_title);
        tvType = findViewById(R.id.tv_detail_type);
        btnEditJs = findViewById(R.id.btn_edit_js);
        btnDelete = findViewById(R.id.btn_delete);

        etSourceIdOriginalListener = etSourceId.getKeyListener();

        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> showMenu(v));

        TextView btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveSource());

        btnDelete.setOnClickListener(v -> deleteSource());

        TextView btnTypeJson = findViewById(R.id.btn_type_json);
        TextView btnTypeJs = findViewById(R.id.btn_type_js);
        TextView btnType302 = findViewById(R.id.btn_type_302);

        btnTypeJson.setOnClickListener(v -> selectType("json"));
        btnTypeJs.setOnClickListener(v -> selectType("js"));
        btnType302.setOnClickListener(v -> selectType("302"));

        TextView btnSmartParse = findViewById(R.id.btn_smart_parse);
        btnSmartParse.setOnClickListener(v -> smartParse());

        btnEditJs.setOnClickListener(v -> {
            String jsCode = getOrCreateJsCode();
            Intent intent = new Intent(this, JsEditorActivity.class);
            intent.putExtra("jsCode", jsCode);
            intent.putExtra("sourceId", etSourceId.getText().toString());
            jsEditorLauncher.launch(intent);
        });
    }

    private void handleIntent() {
        editingSourceId = getIntent().getStringExtra("source_id");
        if (editingSourceId != null) {
            tvTitle.setText("编辑源");
            etSourceId.setText(editingSourceId);
            etSourceId.setKeyListener(null);
            btnDelete.setVisibility(View.VISIBLE);
            layoutTypeSelector.setVisibility(View.GONE);
            layoutTypeDisplay.setVisibility(View.VISIBLE);

            String configJson = sourceManager.readSourceConfig(editingSourceId);
            if (configJson != null) {
                if (configJson.trim().startsWith("{")) {
                    loadConfig(configJson);
                } else {
                    loadJsSource(editingSourceId, configJson);
                }
            }
        } else {
            tvTitle.setText("新建源");
            btnDelete.setVisibility(View.GONE);
            layoutTypeSelector.setVisibility(View.VISIBLE);
            layoutTypeDisplay.setVisibility(View.GONE);
        }
    }

    private void loadConfig(String json) {
        try {
            LogUtils.d("loadConfig json: " + json);
            org.json.JSONObject obj = new org.json.JSONObject(json);
            etName.setText(obj.optString("sourceName", ""));

            Map<String, String> extra = new HashMap<>();

            org.json.JSONObject extraObj = obj.optJSONObject("extraConfig");
            if (extraObj != null) {
                java.util.Iterator<String> keys = extraObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    extra.put(key, extraObj.getString(key));
                }
            }

            if (extra.isEmpty()) {
                String type = "json";
                if (obj.has("type")) {
                    type = obj.optString("type", "json");
                } else if (obj.has("jsCode") || obj.optString("url", "").isEmpty()) {
                    type = "js";
                }
                extra.put("type", type);

                String url = obj.optString("url", "");
                if (!url.isEmpty()) extra.put("url", url);

                org.json.JSONObject response = obj.optJSONObject("response");
                if (response != null) {
                    extra.put("listField", response.optString("list", "data"));
                    org.json.JSONObject item = response.optJSONObject("item");
                    if (item != null) {
                        extra.put("idField", item.optString("id", "id"));
                        extra.put("urlField", item.optString("url", "url"));
                        extra.put("widthField", item.optString("width", "width"));
                        extra.put("heightField", item.optString("height", "height"));
                    }
                }

                String imageUrlBase = obj.optString("imageUrlBase", "");
                if (!imageUrlBase.isEmpty()) extra.put("imageUrlBase", imageUrlBase);

                String previewSuffix = obj.optString("previewSuffix", "");
                if (!previewSuffix.isEmpty()) extra.put("previewSuffix", previewSuffix);
            }

            String type = extra.containsKey("type") ? extra.get("type") : "json";
            tvType.setText(type.toUpperCase());
            layoutTypeDisplay.setVisibility(View.VISIBLE);
            layoutTypeSelector.setVisibility(View.GONE);

            if ("json".equals(type)) {
                layoutJsonFields.setVisibility(View.VISIBLE);
                layout302Fields.setVisibility(View.GONE);
                btnEditJs.setVisibility(View.GONE);
                etUrlTemplate.setText(extra.containsKey("url") ? extra.get("url") : "");
                etDataField.setText(extra.containsKey("listField") ? extra.get("listField") : (extra.containsKey("dataField") ? extra.get("dataField") : "data"));
                etIdField.setText(extra.containsKey("idField") ? extra.get("idField") : "id");
                etUrlField.setText(extra.containsKey("urlField") ? extra.get("urlField") : "url");
                etWidthField.setText(extra.containsKey("widthField") ? extra.get("widthField") : "width");
                etHeightField.setText(extra.containsKey("heightField") ? extra.get("heightField") : "height");
                etImageUrlBase.setText(extra.containsKey("imageUrlBase") ? extra.get("imageUrlBase") : "");
                etPreviewSuffix.setText(extra.containsKey("previewSuffix") ? extra.get("previewSuffix") : "");
            } else if ("302".equals(type)) {
                layoutJsonFields.setVisibility(View.GONE);
                layout302Fields.setVisibility(View.VISIBLE);
                btnEditJs.setVisibility(View.GONE);
                et302Url.setText(extra.containsKey("url") ? extra.get("url") : "");
            } else if ("js".equals(type)) {
                layoutJsonFields.setVisibility(View.GONE);
                layout302Fields.setVisibility(View.GONE);
                btnEditJs.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            LogUtils.e("loadConfig error: " + e.getMessage(), e);
            ToastUtils.show("配置解析失败: " + e.getMessage());
        }
    }

    private void loadJsSource(String sourceId, String jsCode) {
        tvTitle.setText("编辑源");
        etSourceId.setText(sourceId);
        etSourceId.setKeyListener(null);
        btnDelete.setVisibility(View.VISIBLE);
        layoutTypeSelector.setVisibility(View.GONE);
        layoutTypeDisplay.setVisibility(View.VISIBLE);

        tvType.setText("JS");
        layoutJsonFields.setVisibility(View.GONE);
        layout302Fields.setVisibility(View.GONE);
        btnEditJs.setVisibility(View.VISIBLE);

        parseJsMetadata(sourceId, jsCode);
    }

    private void parseJsMetadata(String sourceId, String jsCode) {
        try {
            int configIdx = jsCode.indexOf("config:");
            if (configIdx == -1) configIdx = jsCode.indexOf("config :");
            if (configIdx == -1) return;

            int srcIdIdx = jsCode.indexOf("sourceId:", configIdx);
            if (srcIdIdx == -1) srcIdIdx = jsCode.indexOf("sourceId :", configIdx);
            if (srcIdIdx != -1) {
                String val = extractJsStringValue(jsCode, srcIdIdx);
                if (val != null && !val.isEmpty()) etSourceId.setText(val);
            }

            int srcNameIdx = jsCode.indexOf("sourceName:", configIdx);
            if (srcNameIdx == -1) srcNameIdx = jsCode.indexOf("sourceName :", configIdx);
            if (srcNameIdx != -1) {
                String val = extractJsStringValue(jsCode, srcNameIdx);
                if (val != null && !val.isEmpty()) etName.setText(val);
            }
        } catch (Exception ignored) {}
    }

    private String extractJsStringValue(String js, int keyIdx) {
        int colonIdx = js.indexOf(':', keyIdx);
        if (colonIdx == -1) return null;
        int start = colonIdx + 1;
        while (start < js.length() && js.charAt(start) == ' ') start++;
        if (start >= js.length()) return null;
        char quote = js.charAt(start);
        if (quote != '"' && quote != '\'') return null;
        int end = js.indexOf(quote, start + 1);
        if (end == -1) return null;
        return js.substring(start + 1, end).trim();
    }

    private void selectType(String type) {
        selectedType = type;
        TextView btnTypeJson = findViewById(R.id.btn_type_json);
        TextView btnTypeJs = findViewById(R.id.btn_type_js);
        TextView btnType302 = findViewById(R.id.btn_type_302);

        int whiteRes = R.color.white;
        int blackRes = R.color.black;
        int borderRes = R.drawable.border_black_button;

        btnTypeJson.setBackgroundResource(type.equals("json") ? blackRes : borderRes);
        btnTypeJson.setTextColor(getColor(type.equals("json") ? whiteRes : blackRes));
        btnTypeJs.setBackgroundResource(type.equals("js") ? blackRes : borderRes);
        btnTypeJs.setTextColor(getColor(type.equals("js") ? whiteRes : blackRes));
        btnType302.setBackgroundResource(type.equals("302") ? blackRes : borderRes);
        btnType302.setTextColor(getColor(type.equals("302") ? whiteRes : blackRes));

        layoutJsonFields.setVisibility(type.equals("json") ? View.VISIBLE : View.GONE);
        layout302Fields.setVisibility(type.equals("302") ? View.VISIBLE : View.GONE);
        btnEditJs.setVisibility(type.equals("js") ? View.VISIBLE : View.GONE);
    }

    private void showMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "复制配置");
        popup.getMenu().add(0, 2, 1, "粘贴配置");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    copyConfig();
                    return true;
                case 2:
                    filePickerLauncher.launch("application/json");
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void copyConfig() {
        try {
            String json = buildConfigJson();
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("source_config", json);
            cm.setPrimaryClip(clip);
            ToastUtils.show("配置已复制");
        } catch (Exception e) {
            ToastUtils.show("复制失败: " + e.getMessage());
        }
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) return;
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            String json = io.inkdrop.wallpaper.util.FileUtils.readInputStream(is);
            
            org.json.JSONObject obj = new org.json.JSONObject(json);
            String sourceId = obj.optString("sourceId", etSourceId.getText().toString());
            String sourceName = obj.optString("sourceName", etName.getText().toString());

            if (!sourceId.isEmpty()) etSourceId.setText(sourceId);
            if (!sourceName.isEmpty()) etName.setText(sourceName);

            loadConfig(json);
            ToastUtils.show("配置已粘贴");
        } catch (Exception e) {
            ToastUtils.show("粘贴失败: " + e.getMessage());
        }
    }

    private void smartParse() {
        String url = etUrlTemplate.getText().toString().trim();
        if (url.isEmpty()) {
            ToastUtils.show("请先输入 URL");
            return;
        }
        Intent intent = new Intent(this, JsonFieldPickerActivity.class);
        intent.putExtra("jsonUrl", url);
        intent.putExtra("targetFields", new String[]{"listField", "idField", "urlField", "widthField", "heightField"});
        startActivityForResult(intent, 100);
    }

    private String getOrCreateJsCode() {
        String sourceId = etSourceId.getText().toString().trim();
        if (sourceId.isEmpty()) {
            sourceId = "js_" + System.currentTimeMillis();
            etSourceId.setText(sourceId);
        }
        String existing = sourceManager.readSourceConfig(sourceId);
        if (existing != null && existing.contains("function")) {
            return existing;
        }
        return "// wallpaper fetch function\n// Must return: { data: [{ id, url, width?, height? }] }\n\nfunction fetch(page, pageSize) {\n  return {\n    data: []\n  };\n}";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String listField = data.getStringExtra("listField");
            String idField = data.getStringExtra("idField");
            String urlField = data.getStringExtra("urlField");
            String widthField = data.getStringExtra("widthField");
            String heightField = data.getStringExtra("heightField");

            if (listField != null) etDataField.setText(listField);
            if (idField != null) etIdField.setText(idField);
            if (urlField != null) etUrlField.setText(urlField);
            if (widthField != null) etWidthField.setText(widthField);
            if (heightField != null) etHeightField.setText(heightField);
        }
    }

    private void saveSource() {
        String name = etName.getText().toString().trim();
        String sourceId = etSourceId.getText().toString().trim();

        if (name.isEmpty()) {
            ToastUtils.show("请输入源名称");
            return;
        }
        if (sourceId.isEmpty()) {
            ToastUtils.show("请输入源ID");
            return;
        }

        if ("js".equals(selectedType)) {
            String jsCode = getOrCreateJsCode();
            boolean ok = sourceManager.addCustomJsSource(sourceId, name, jsCode);
            if (ok) {
                ToastUtils.show("JS源已保存");
                finish();
            } else {
                ToastUtils.show("保存失败");
            }
            return;
        }

        try {
            String json = buildConfigJson();
            boolean ok = sourceManager.addCustomSource(sourceId, name, json);
            if (ok) {
                ToastUtils.show("源已保存");
                finish();
            } else {
                ToastUtils.show("保存失败");
            }
        } catch (Exception e) {
            ToastUtils.show("保存失败: " + e.getMessage());
        }
    }

    private String buildConfigJson() throws org.json.JSONException {
        String sourceId = etSourceId.getText().toString().trim();
        String sourceName = etName.getText().toString().trim();

        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("sourceId", sourceId);
        obj.put("sourceName", sourceName);

        org.json.JSONObject extra = new org.json.JSONObject();
        extra.put("type", selectedType);

        if ("json".equals(selectedType)) {
            extra.put("url", etUrlTemplate.getText().toString().trim());
            extra.put("dataField", etDataField.getText().toString().trim());
            extra.put("idField", etIdField.getText().toString().trim());
            extra.put("urlField", etUrlField.getText().toString().trim());
            extra.put("widthField", etWidthField.getText().toString().trim());
            extra.put("heightField", etHeightField.getText().toString().trim());
            extra.put("imageUrlBase", etImageUrlBase.getText().toString().trim());
            extra.put("previewSuffix", etPreviewSuffix.getText().toString().trim());
        } else if ("302".equals(selectedType)) {
            extra.put("url", et302Url.getText().toString().trim());
        }

        obj.put("extraConfig", extra);
        return obj.toString(2);
    }

    private void deleteSource() {
        if (editingSourceId == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除源")
                .setMessage("确定要删除此源吗？")
                .setPositiveButton("删除", (d, w) -> {
                    sourceManager.removeSource(editingSourceId);
                    ToastUtils.show("源已删除");
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
