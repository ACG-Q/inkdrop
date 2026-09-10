package com.example.wallpaper.data.source;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class InkPaperSourceTest {
    private InkPaperSource source;

    @Before
    public void setUp() {
        source = new InkPaperSource();
    }

    @Test
    public void testGetSourceId() {
        assertEquals("inkpaper", source.getSourceId());
    }

    @Test
    public void testGetSourceName() {
        assertEquals("InkPaper壁纸", source.getSourceName());
    }

    @Test
    public void testIsAvailable() {
        assertTrue(source.isAvailable());
    }

    @Test
    public void testGetConfig() {
        SourceConfig config = source.getConfig();
        assertNotNull(config);
        assertEquals("inkpaper", config.getSourceId());
        assertEquals("InkPaper壁纸", config.getSourceName());
        assertEquals("https://inkpaper.foolstack.net", config.getBaseUrl());
    }
}
