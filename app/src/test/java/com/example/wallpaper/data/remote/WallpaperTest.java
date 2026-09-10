package com.example.wallpaper.data.remote;

import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperTest {
    @Test
    public void testGetFullUrl() {
        Wallpaper wallpaper = new Wallpaper(1, "/api/images/test.jpg", "hash", 1080, 1920, "2024-01-01");
        String fullUrl = wallpaper.getFullUrl("https://example.com");
        assertEquals("https://example.com/api/images/test.jpg", fullUrl);
    }
    
    @Test
    public void testGetters() {
        Wallpaper wallpaper = new Wallpaper(1, "/url", "hash", 1080, 1920, "2024-01-01");
        assertEquals(1, wallpaper.getId());
        assertEquals("/url", wallpaper.getUrl());
        assertEquals("hash", wallpaper.getHash());
        assertEquals(1080, wallpaper.getWidth());
        assertEquals(1920, wallpaper.getHeight());
        assertEquals("2024-01-01", wallpaper.getCreatedAt());
    }
}
