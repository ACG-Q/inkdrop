package com.example.wallpaper.util.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.Executors;

/**
 * 图片加载工具类
 * 负责将图片加载到 ImageView
 */
public final class ImageLoader {

    private static Context appContext;

    private ImageLoader() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // ========== 基础方法 ==========

    public static void load(ImageView view, String url) {
        Glide.with(view.getContext())
            .load(url)
            .into(view);
    }

    public static void load(ImageView view, String url, ImageCallback.LoadCallback callback) {
        Glide.with(view.getContext())
            .load(url)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model,
                    Target<Drawable> target, boolean isFirstResource) {
                    if (callback != null) callback.onError(e != null ? e.getMessage() : "加载失败");
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model,
                    Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    if (callback != null) callback.onSuccess();
                    return false;
                }
            })
            .into(view);
    }

    // ========== 预加载 ==========

    public static void preload(String url) {
        if (appContext == null) return;
        Glide.with(appContext)
            .load(url)
            .preload();
    }

    // ========== 缓存管理 ==========

    public static void clearMemoryCache() {
        if (appContext == null) return;
        Glide.get(appContext).clearMemory();
    }

    public static void clearDiskCache() {
        if (appContext == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            Glide.get(appContext).clearDiskCache();
        });
    }
}
