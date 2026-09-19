package com.example.wallpaper.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallpaper.R;
import com.example.wallpaper.util.image.ImageLoader;
import com.example.wallpaper.data.remote.Wallpaper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {
    private List<Wallpaper> wallpapers = new ArrayList<>();
    private Set<String> urlSet = new HashSet<>();
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
        this.wallpapers.clear();
        this.urlSet.clear();
        for (Wallpaper w : newWallpapers) {
            if (urlSet.add(w.getUrl())) {
                this.wallpapers.add(w);
            }
        }
        notifyDataSetChanged();
    }

    public void addWallpapers(List<Wallpaper> moreWallpapers) {
        int startPos = wallpapers.size();
        for (Wallpaper w : moreWallpapers) {
            if (urlSet.add(w.getUrl())) {
                wallpapers.add(w);
            }
        }
        int added = wallpapers.size() - startPos;
        if (added > 0) {
            notifyItemRangeInserted(startPos, added);
        }
    }

    public void addWallpaper(Wallpaper wallpaper) {
        if (urlSet.add(wallpaper.getUrl())) {
            wallpapers.add(wallpaper);
            notifyItemInserted(wallpapers.size() - 1);
        }
    }

    public void clear() {
        int size = wallpapers.size();
        wallpapers.clear();
        urlSet.clear();
        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }

    public void clearAndSet(List<Wallpaper> newWallpapers) {
        wallpapers.clear();
        urlSet.clear();
        for (Wallpaper w : newWallpapers) {
            if (urlSet.add(w.getUrl())) {
                wallpapers.add(w);
            }
        }
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
            int width = wallpaper.getWidth();
            int height = wallpaper.getHeight();

            if (width > 0 && height > 0) {
                ViewGroup.LayoutParams lp = wallpaperImage.getLayoutParams();
                int containerWidth = itemView.getResources().getDisplayMetrics().widthPixels / 2 - 12;
                lp.height = (int) (containerWidth * ((float) height / width));
                wallpaperImage.setLayoutParams(lp);
            } else {
                ViewGroup.LayoutParams lp = wallpaperImage.getLayoutParams();
                lp.height = 300;
                wallpaperImage.setLayoutParams(lp);
            }

            ImageLoader.load(wallpaperImage, wallpaper.getUrl());

            if (width > 0 && height > 0) {
                wallpaperSize.setText(width + " x " + height);
            } else {
                wallpaperSize.setText("未知尺寸");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(wallpaper);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (favoriteListener != null) {
                    boolean newState = !wallpaper.isFavorite();
                    wallpaper.setFavorite(newState);
                    notifyItemChanged(getAdapterPosition());
                    favoriteListener.onFavoriteClick(wallpaper, newState);
                }
            });

            btnFavorite.setImageResource(wallpaper.isFavorite() 
                ? R.drawable.ic_favorite_filled 
                : R.drawable.ic_favorite_border);
        }
    }
}
