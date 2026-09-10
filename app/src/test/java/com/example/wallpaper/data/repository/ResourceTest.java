package com.example.wallpaper.data.repository;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResourceTest {
    @Test
    public void testSuccess() {
        Resource<String> resource = Resource.success("data");
        assertTrue(resource.isSuccess());
        assertEquals("data", resource.getData());
        assertNull(resource.getMessage());
    }

    @Test
    public void testError() {
        Resource<String> resource = Resource.error("error message", null);
        assertTrue(resource.isError());
        assertEquals("error message", resource.getMessage());
        assertNull(resource.getData());
    }

    @Test
    public void testLoading() {
        Resource<String> resource = Resource.loading("loading");
        assertTrue(resource.isLoading());
        assertEquals("loading", resource.getData());
    }
}
