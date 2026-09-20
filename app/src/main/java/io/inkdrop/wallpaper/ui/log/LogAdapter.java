package io.inkdrop.wallpaper.ui.log;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.inkdrop.wallpaper.R;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private final List<String> lines = new ArrayList<>();

    public void setLines(List<String> newLines) {
        lines.clear();
        lines.addAll(newLines);
        notifyDataSetChanged();
    }

    public List<String> getLines() {
        return lines;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_log_line, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String line = lines.get(position);
        holder.tvLogLine.setText(line);

        if (line.startsWith("[E]")) {
            holder.tvLogLine.setTextColor(Color.parseColor("#FF6B6B"));
        } else if (line.startsWith("[W]")) {
            holder.tvLogLine.setTextColor(Color.parseColor("#FFA726"));
        } else if (line.startsWith("[I]")) {
            holder.tvLogLine.setTextColor(Color.parseColor("#BDBDBD"));
        } else {
            holder.tvLogLine.setTextColor(Color.parseColor("#757575"));
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogLine;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogLine = itemView.findViewById(R.id.tv_log_line);
        }
    }
}
