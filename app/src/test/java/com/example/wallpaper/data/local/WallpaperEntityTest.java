package com.example.wallpaper.data.local;

import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperEntityTest {
    
    @Test
    public void testEquals_SameObject() {
        WallpaperEntity entity = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertEquals(entity, entity);
    }
    
    @Test
    public void testEquals_SameValues() {
        WallpaperEntity entity1 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertEquals(entity1, entity2);
    }
    
    @Test
    public void testEquals_DifferentId() {
        WallpaperEntity entity1 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(2, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertNotEquals(entity1, entity2);
    }
    
    @Test
    public void testEquals_DifferentUrl() {
        WallpaperEntity entity1 = createEntity(1, "url1", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(1, "url2", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertNotEquals(entity1, entity2);
    }
    
    @Test
    public void testEquals_DifferentFavorite() {
        WallpaperEntity entity1 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", false, "source1", 1000L);
        assertNotEquals(entity1, entity2);
    }
    
    @Test
    public void testEquals_Null() {
        WallpaperEntity entity = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertNotEquals(entity, null);
    }
    
    @Test
    public void testEquals_DifferentType() {
        WallpaperEntity entity = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertNotEquals(entity, "string");
    }
    
    @Test
    public void testHashCode_SameValues() {
        WallpaperEntity entity1 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }
    
    @Test
    public void testHashCode_DifferentValues() {
        WallpaperEntity entity1 = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        WallpaperEntity entity2 = createEntity(2, "url2", "hash2", 300, 400, "2024-01-02", false, "source2", 2000L);
        assertNotEquals(entity1.hashCode(), entity2.hashCode());
    }
    
    @Test
    public void testSetFavorite() {
        WallpaperEntity entity = createEntity(1, "url", "hash", 100, 200, "2024-01-01", false, "source1", 1000L);
        assertFalse(entity.isFavorite());
        
        entity.setFavorite(true);
        assertTrue(entity.isFavorite());
    }
    
    @Test
    public void testSetCachedAt() {
        WallpaperEntity entity = createEntity(1, "url", "hash", 100, 200, "2024-01-01", true, "source1", 1000L);
        assertEquals(1000L, entity.getCachedAt());
        
        entity.setCachedAt(2000L);
        assertEquals(2000L, entity.getCachedAt());
    }
    
    private WallpaperEntity createEntity(int id, String url, String hash, int width, int height, 
                                         String createdAt, boolean isFavorite, String sourceId, long cachedAt) {
        WallpaperEntity entity = new WallpaperEntity(id, url, hash, width, height, createdAt, isFavorite, sourceId, cachedAt);
        return entity;
    }
}