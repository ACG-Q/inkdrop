package com.example.wallpaper.data.local;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WallpaperDaoTest {
    private AppDatabase db;
    private WallpaperDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        dao = db.wallpaperDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertAndGet() {
        WallpaperEntity wallpaper = createTestWallpaper(1);
        dao.insert(wallpaper);
        
        WallpaperEntity retrieved = dao.getWallpaperById(1);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getId());
    }

    @Test
    public void testInsertAll() {
        List<WallpaperEntity> wallpapers = new ArrayList<>();
        wallpapers.add(createTestWallpaper(1));
        wallpapers.add(createTestWallpaper(2));
        wallpapers.add(createTestWallpaper(3));
        
        dao.insertAll(wallpapers);
        
        assertEquals(3, dao.getWallpaperCountBySource("test_source"));
    }

    @Test
    public void testSetFavorite() {
        WallpaperEntity wallpaper = createTestWallpaper(1);
        dao.insert(wallpaper);
        
        dao.setFavorite(1, true);
        WallpaperEntity retrieved = dao.getWallpaperById(1);
        assertTrue(retrieved.isFavorite());
        
        dao.setFavorite(1, false);
        retrieved = dao.getWallpaperById(1);
        assertFalse(retrieved.isFavorite());
    }

    @Test
    public void testGetFavoriteWallpapers() {
        dao.insert(createTestWallpaper(1));
        dao.insert(createTestWallpaper(2));
        dao.setFavorite(1, true);
        
        List<WallpaperEntity> favorites = dao.getFavoriteWallpapersList();
        assertEquals(1, favorites.size());
        assertEquals(1, favorites.get(0).getId());
    }

    private WallpaperEntity createTestWallpaper(int id) {
        return new WallpaperEntity(id, "/test/" + id + ".jpg", "hash" + id, 
            1080, 1920, "2024-01-01", false, "test_source", System.currentTimeMillis());
    }
}
