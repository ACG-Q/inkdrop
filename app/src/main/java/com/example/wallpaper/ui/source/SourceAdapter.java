package com.example.wallpaper.ui.source;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;
import com.example.wallpaper.data.source.WallpaperSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder> {
    private List<WallpaperSource> sources = new ArrayList<>();
    private String selectedSourceId;
    private OnSourceActionListener listener;
    private boolean batchMode = false;
    private Set<String> selectedIds = new HashSet<>();

    public interface OnSourceActionListener {
        void onSourceClick(WallpaperSource source);
        void onSourceToggle(WallpaperSource source, boolean enabled);
        void onSourceEdit(WallpaperSource source);
    }

    public void setListener(OnSourceActionListener listener) {
        this.listener = listener;
    }

    public void setSources(List<WallpaperSource> sources, String selectedSourceId) {
        this.sources = sources;
        this.selectedSourceId = selectedSourceId;
        notifyDataSetChanged();
    }

    public void setBatchMode(boolean batchMode) {
        this.batchMode = batchMode;
        this.selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isBatchMode() {
        return batchMode;
    }

    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public List<WallpaperSource> getSelectedSources() {
        List<WallpaperSource> selected = new ArrayList<>();
        for (WallpaperSource source : sources) {
            if (selectedIds.contains(source.getSourceId())) {
                selected.add(source);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_source, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WallpaperSource source = sources.get(position);
        holder.bind(source);
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSourceName;
        private final TextView tvSourceType;
        private final TextView tvSourceId;
        private final TextView btnEdit;
        private final Switch switchEnabled;
        private final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSourceName = itemView.findViewById(R.id.tv_source_name);
            tvSourceType = itemView.findViewById(R.id.tv_source_type);
            tvSourceId = itemView.findViewById(R.id.tv_source_id);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            checkBox = itemView.findViewById(R.id.checkbox);
        }

        void bind(WallpaperSource source) {
            tvSourceName.setText(source.getSourceName());
            java.util.Map<String, String> extra = source.getConfig().getExtraConfig();
            String type = (extra != null && extra.containsKey("type")) ? extra.get("type") : "json";
            tvSourceType.setText(type.toUpperCase());
            tvSourceId.setText(source.getSourceId());

            if (batchMode) {
                checkBox.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.GONE);
                switchEnabled.setVisibility(View.GONE);
                checkBox.setChecked(selectedIds.contains(source.getSourceId()));
                itemView.setOnClickListener(v -> toggleSelection(source));
            } else {
                checkBox.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);
                switchEnabled.setVisibility(View.VISIBLE);
                switchEnabled.setEnabled(true);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onSourceClick(source);
                });
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onSourceEdit(source);
                });
                switchEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (listener != null) listener.onSourceToggle(source, isChecked);
                });
            }
        }

        private void toggleSelection(WallpaperSource source) {
            String id = source.getSourceId();
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
            } else {
                selectedIds.add(id);
            }
            notifyItemChanged(getAdapterPosition());
        }
    }
}
