package com.example.wallpaper.data.source;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperSourceManagerTest {
    private WallpaperSourceManager manager;

    @Before
    public void setUp() {
        manager = new WallpaperSourceManager();
    }

    @Test
    public void testDefaultSourceRegistered() {
        assertNotNull(manager.getSource("inkpaper"));
    }

    @Test
    public void testGetDefaultSource() {
        WallpaperSource defaultSource = manager.getDefaultSource();
        assertNotNull(defaultSource);
        assertEquals("inkpaper", defaultSource.getSourceId());
    }

    @Test
    public void testGetAvailableSources() {
        assertFalse(manager.getAvailableSources().isEmpty());
    }

    @Test
    public void testRegisterAndUnregister() {
        WallpaperSource testSource = new InkPaperSource() {
            @Override
            public String getSourceId() {
                return "test";
            }
        };
        manager.registerSource(testSource);
        assertNotNull(manager.getSource("test"));
        
        manager.unregisterSource("test");
        assertNull(manager.getSource("test"));
    }
}
