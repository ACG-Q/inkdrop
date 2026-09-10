package com.example.wallpaper.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.wallpaper.R;
import com.example.wallpaper.data.remote.Wallpaper;

import java.util.ArrayList;
import java.util.List;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {
    private List<Wallpaper> wallpapers = new ArrayList<>();
    private OnItemClickListener listener;
    private OnFavoriteClickListener favoriteListener;

    public interface OnItemClickListener {
        void onItemClick(Wallpaper wallpaper);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Wallpaper wallpaper, boolean isFavorite);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setWallpapers(List<Wallpaper> newWallpapers) {
        this.wallpapers = newWallpapers;
        notifyDataSetChanged();
    }

    public void addWallpapers(List<Wallpaper> moreWallpapers) {
        int startPos = wallpapers.size();
        wallpapers.addAll(moreWallpapers);
        notifyItemRangeInserted(startPos, moreWallpapers.size());
    }

    public void clear() {
        wallpapers.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_wallpaper, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        Wallpaper wallpaper = wallpapers.get(position);
        holder.bind(wallpaper);
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    class WallpaperViewHolder extends RecyclerView.ViewHolder {
        private final ImageView wallpaperImage;
        private final TextView wallpaperSize;
        private final ImageButton btnFavorite;

        WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            wallpaperImage = itemView.findViewById(R.id.wallpaper_image);
            wallpaperSize = itemView.findViewById(R.id.wallpaper_size);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }

        void bind(Wallpaper wallpaper) {
            // 加载图片
            Glide.with(itemView.getContext())
                .load("https://inkpaper.foolstack.net" + wallpaper.getUrl())
                .transform(new CenterCrop(), new RoundedCorners(16))
                .into(wallpaperImage);

            // 显示尺寸
            wallpaperSize.setText(wallpaper.getWidth() + " x " + wallpaper.getHeight());

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(wallpaper);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(wallpaper, true);
                }
            });
        }
    }
}
