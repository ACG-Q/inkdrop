package io.inkdrop.wallpaper.ui.source;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import io.inkdrop.wallpaper.util.ToastUtils;

import androidx.appcompat.app.AppCompatActivity;

import io.inkdrop.wallpaper.R;

public class JsEditorActivity extends AppCompatActivity {
    private EditText etJsCode;
    private TextView tvFilename;
    private String sourceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_js_editor);
        initViews();
        loadJsCode();
    }

    private void initViews() {
        etJsCode = findViewById(R.id.et_js_code);
        tvFilename = findViewById(R.id.tv_js_filename);

        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnSave = findViewById(R.id.btn_save_js);
        btnSave.setOnClickListener(v -> saveJs());

        etJsCode.setTypeface(Typeface.MONOSPACE);
        etJsCode.setTextSize(13);
    }

    private void loadJsCode() {
        sourceId = getIntent().getStringExtra("sourceId");
        String jsCode = getIntent().getStringExtra("jsCode");

        if (sourceId == null) sourceId = "js_" + System.currentTimeMillis();
        tvFilename.setText(sourceId + ".js");

        if (jsCode != null) {
            etJsCode.setText(jsCode);
        } else {
            etJsCode.setText(getDefaultJsCode());
        }
    }

    private String getDefaultJsCode() {
        return "// 壁纸源 JS 脚本\n// 必须实现 fetch(page, pageSize) 函数\n// 返回格式: { data: [{ id, url, width?, height? }] }\n\nfunction fetch(page, pageSize) {\n  // 在这里实现你的壁纸获取逻辑\n  var wallpapers = [];\n\n  // 示例：构建图片URL\n  for (var i = 0; i < pageSize; i++) {\n    var index = (page - 1) * pageSize + i;\n    wallpapers.push({\n      id: index + 1,\n      url: 'https://example.com/wallpaper/' + (index + 1) + '.jpg',\n      width: 1920,\n      height: 1080\n    });\n  }\n\n  return {\n    data: wallpapers\n  };\n}";
    }

    private void saveJs() {
        String jsCode = etJsCode.getText().toString();
        if (jsCode.trim().isEmpty()) {
            ToastUtils.show("JS代码不能为空");
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("jsCode", jsCode);
        resultIntent.putExtra("sourceId", sourceId);
        setResult(RESULT_OK, resultIntent);
        ToastUtils.show("JS代码已保存");
        finish();
    }
}
