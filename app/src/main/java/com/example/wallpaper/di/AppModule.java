package com.example.wallpaper.di;

import android.content.Context;

import com.example.wallpaper.data.local.AppDatabase;
import com.example.wallpaper.data.local.WallpaperDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 应用级依赖注入模块
 * 提供全局单例依赖
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    /**
     * IO 密集型任务线程池
     * 用于网络请求、文件读写等操作
     */
    public static final String IO_EXECUTOR = "io_executor";
    
    /**
     * CPU 密集型任务线程池
     * 用于计算密集型操作
     */
    public static final String CPU_EXECUTOR = "cpu_executor";
    
    /**
     * 单线程执行器
     * 用于需要顺序执行的任务
     */
    public static final String SINGLE_EXECUTOR = "single_executor";
    
    @Provides
    @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    @Singleton
    public WallpaperDao provideWallpaperDao(AppDatabase database) {
        return database.wallpaperDao();
    }
    
    @Provides
    @Singleton
    @javax.inject.Named(IO_EXECUTOR)
    public ExecutorService provideIoExecutor() {
        // IO 密集型任务使用固定线程池，线程数根据 CPU 核心数 * 2
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        return Executors.newFixedThreadPool(threadCount);
    }
    
    @Provides
    @Singleton
    @javax.inject.Named(CPU_EXECUTOR)
    public ExecutorService provideCpuExecutor() {
        // CPU 密集型任务使用缓存线程池
        return Executors.newCachedThreadPool();
    }
    
    @Provides
    @Singleton
    @javax.inject.Named(SINGLE_EXECUTOR)
    public ExecutorService provideSingleExecutor() {
        // 单线程执行器
        return Executors.newSingleThreadExecutor();
    }
}
