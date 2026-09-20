package io.inkdrop.wallpaper.util;

public class ImageUrlUtil {

    public static String buildImageUrl(String imageUrlBase, String rawUrl, String previewSuffix) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return "";
        }

        String fullUrl;
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            fullUrl = rawUrl;
        } else if (imageUrlBase != null && !imageUrlBase.isEmpty()) {
            String base = imageUrlBase.endsWith("/") ? imageUrlBase.substring(0, imageUrlBase.length() - 1) : imageUrlBase;
            String path = rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl;
            fullUrl = base + path;
        } else {
            fullUrl = rawUrl;
        }

        if (previewSuffix != null && !previewSuffix.isEmpty()) {
            String separator = fullUrl.contains("?") ? "&" : "?";
            fullUrl = fullUrl + separator + previewSuffix;
        }

        return fullUrl;
    }

    public static String buildFullImageUrl(String imageUrlBase, String rawUrl) {
        return buildImageUrl(imageUrlBase, rawUrl, null);
    }
}
