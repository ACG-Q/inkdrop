package com.example.wallpaper.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;

public class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder> {

    private int itemCount = 6;

    public void setItemCount(int count) {
        this.itemCount = count;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_skeleton_wallpaper, parent, false);
        return new SkeletonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
