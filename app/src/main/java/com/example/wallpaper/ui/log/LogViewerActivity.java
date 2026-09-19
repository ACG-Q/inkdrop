package com.example.wallpaper.ui.log;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;
import com.example.wallpaper.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogViewerActivity extends AppCompatActivity {

    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView tvLogCount;
    private LogAdapter adapter;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final ActivityResultLauncher<Intent> exportLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    exportLogs(uri);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvLogCount = findViewById(R.id.tv_log_count);
        RecyclerView recyclerLogs = findViewById(R.id.recycler_logs);

        adapter = new LogAdapter();
        recyclerLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerLogs.setAdapter(adapter);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        updateDateDisplay();

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_copy).setOnClickListener(v -> copyLogs());
        findViewById(R.id.btn_export).setOnClickListener(v -> openExportPicker());

        loadLogs();
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = isStart ? startCal : endCal;
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            if (isStart) {
                selected.set(Calendar.HOUR_OF_DAY, 0);
                selected.set(Calendar.MINUTE, 0);
                selected.set(Calendar.SECOND, 0);
                selected.set(Calendar.MILLISECOND, 0);
                startCal.setTime(selected.getTime());
            } else {
                selected.set(Calendar.HOUR_OF_DAY, 23);
                selected.set(Calendar.MINUTE, 59);
                selected.set(Calendar.SECOND, 59);
                selected.set(Calendar.MILLISECOND, 999);
                endCal.setTime(selected.getTime());
            }
            updateDateDisplay();
            loadLogs();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        tvStartDate.setText(displayFormat.format(startCal.getTime()));
        tvEndDate.setText(displayFormat.format(endCal.getTime()));
    }

    private void loadLogs() {
        List<File> files = LogUtils.getLogFiles(startCal.getTime(), endCal.getTime());
        List<String> allLines = new ArrayList<>();
        for (File file : files) {
            String content = LogUtils.getLogContent(file);
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    allLines.add(line);
                }
            }
        }
        adapter.setLines(allLines);
        tvLogCount.setText(allLines.size() + " 条");
    }

    private void copyLogs() {
        String content = LogUtils.getAllLogContent(startCal.getTime(), endCal.getTime());
        if (content.isEmpty()) {
            Toast.makeText(this, "没有日志内容", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("logs", content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void openExportPicker() {
        String fileName = displayFormat.format(startCal.getTime()) + "_" +
            displayFormat.format(endCal.getTime()) + ".log";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }

    private void exportLogs(Uri uri) {
        String content = LogUtils.getAllLogContent(startCal.getTime(), endCal.getTime());
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(content.getBytes());
                outputStream.close();
                Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
        }
    }
}
