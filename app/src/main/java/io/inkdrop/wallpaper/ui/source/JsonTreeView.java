package io.inkdrop.wallpaper.ui.source;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import io.inkdrop.wallpaper.util.LogUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.inkdrop.wallpaper.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonTreeView extends LinearLayout {
    private OnFieldSelectedListener listener;

    public interface OnFieldSelectedListener {
        void onFieldSelected(String fieldName, String fieldType, String path);
    }

    public JsonTreeView(Context context) {
        super(context);
        init();
    }

    public JsonTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    public void setOnFieldSelectedListener(OnFieldSelectedListener listener) {
        this.listener = listener;
    }

    public void setData(JSONObject json) {
        removeAllViews();
        try {
            renderObject(json, "", 0);
        } catch (JSONException e) {
            LogUtils.e("Failed to render JSON", e);
        }
    }

    private void renderObject(JSONObject obj, String path, int depth) throws JSONException {
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.get(key);
            String currentPath = path.isEmpty() ? key : path + "." + key;

            if (value instanceof JSONObject) {
                addExpandableNode(key, "object", currentPath, depth, (JSONObject) value);
            } else if (value instanceof JSONArray) {
                addExpandableNode(key, "array", currentPath, depth, (JSONArray) value);
            } else {
                addLeafNode(key, value, currentPath, depth);
            }
        }
    }

    private void renderArray(JSONArray arr, String path, int depth) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            Object value = arr.get(i);
            String currentPath = path + "[" + i + "]";

            if (value instanceof JSONObject) {
                addExpandableNode("[" + i + "]", "object", currentPath, depth, (JSONObject) value);
            } else if (value instanceof JSONArray) {
                addExpandableNode("[" + i + "]", "array", currentPath, depth, (JSONArray) value);
            } else {
                addLeafNode("[" + i + "]", value, currentPath, depth);
            }
        }
    }

    private void addExpandableNode(String key, String type, String path, int depth, Object child) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(VERTICAL);
        container.setPadding(depth * 24, 4, 0, 4);

        TextView toggle = new TextView(getContext());
        toggle.setText("▼ " + key + " {" + type + "}");
        toggle.setTextSize(13);
        toggle.setTypeface(null, Typeface.BOLD);
        toggle.setTextColor(getContext().getColor(R.color.black));
        toggle.setPadding(0, 4, 0, 4);
        toggle.setBackgroundResource(R.drawable.border_black_button);
        toggle.setClickable(true);
        toggle.setFocusable(true);

        LinearLayout childContainer = new LinearLayout(getContext());
        childContainer.setOrientation(VERTICAL);
        childContainer.setVisibility(VISIBLE);

        toggle.setOnClickListener(v -> {
            if (childContainer.getVisibility() == VISIBLE) {
                childContainer.setVisibility(GONE);
                toggle.setText("▶ " + key + " {" + type + "}");
            } else {
                childContainer.setVisibility(VISIBLE);
                toggle.setText("▼ " + key + " {" + type + "}");
            }
        });

        try {
            if (child instanceof JSONObject) {
                renderObject((JSONObject) child, path, depth + 1);
            } else if (child instanceof JSONArray) {
                renderArray((JSONArray) child, path, depth + 1);
            }
        } catch (JSONException e) {
            LogUtils.e("Error rendering child", e);
        }

        container.addView(toggle);
        container.addView(childContainer);
        addView(container);
    }

    private void addLeafNode(String key, Object value, String path, int depth) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setPadding(depth * 24, 2, 0, 2);

        TextView tvKey = new TextView(getContext());
        tvKey.setText(key + ": ");
        tvKey.setTextSize(13);
        tvKey.setTypeface(null, Typeface.BOLD);
        tvKey.setTextColor(getContext().getColor(R.color.black));

        TextView tvValue = new TextView(getContext());
        String displayValue = value != null ? value.toString() : "null";
        if (displayValue.length() > 100) {
            displayValue = displayValue.substring(0, 100) + "...";
        }
        tvValue.setText(displayValue);
        tvValue.setTextSize(13);
        tvValue.setTextColor(getContext().getColor(R.color.text_secondary));

        row.addView(tvKey);
        row.addView(tvValue);

        row.setBackgroundResource(R.drawable.border_black_button);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            if (listener != null) {
                String fieldType = value != null ? value.getClass().getSimpleName() : "null";
                listener.onFieldSelected(key, fieldType, path);
            }
        });

        addView(row);
    }
}
