package com.example.wallpaper.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WallpaperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WallpaperEntity wallpaper);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WallpaperEntity> wallpapers);

    @Update
    void update(WallpaperEntity wallpaper);

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :wallpaperId")
    void setFavorite(int wallpaperId, boolean isFavorite);

    @Query("SELECT * FROM wallpapers WHERE id = :wallpaperId")
    WallpaperEntity getWallpaperById(int wallpaperId);

    @Query("SELECT * FROM wallpapers WHERE sourceId = :sourceId ORDER BY cachedAt DESC")
    List<WallpaperEntity> getWallpapersBySource(String sourceId);

    @Query("SELECT * FROM wallpapers WHERE sourceId = :sourceId ORDER BY cachedAt DESC")
    LiveData<List<WallpaperEntity>> getWallpapersBySourceLive(String sourceId);

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    LiveData<List<WallpaperEntity>> getFavoriteWallpapers();

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1")
    List<WallpaperEntity> getFavoriteWallpapersList();

    @Query("SELECT COUNT(*) FROM wallpapers WHERE sourceId = :sourceId")
    int getWallpaperCountBySource(String sourceId);

    @Query("DELETE FROM wallpapers WHERE sourceId = :sourceId AND isFavorite = 0")
    void deleteNonFavoriteBySource(String sourceId);

    @Query("DELETE FROM wallpapers")
    void deleteAll();
}
