package io.inkdrop.wallpaper.data.source;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import io.inkdrop.wallpaper.data.remote.Wallpaper;
import io.inkdrop.wallpaper.data.remote.WallpaperResponse;
import io.inkdrop.wallpaper.util.ImageUrlUtil;
import io.inkdrop.wallpaper.util.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GenericSource implements WallpaperSource {
    private static final ExecutorService sizeExecutor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int HASH_MODULO = 1_000_000_000;
    
    // 全局尺寸缓存 (URL -> [width, height])
    private static final ConcurrentHashMap<String, int[]> dimensionsCache = new ConcurrentHashMap<>();
    
    // 302源URL缓存 - 使用线程安全的 CopyOnWriteArrayList
    private final CopyOnWriteArrayList<String> urlCache = new CopyOnWriteArrayList<>();

    private final SourceConfig config;
    private final JsonSourceParser.ParsedConfig jsonConfig;
    private final OkHttpClient httpClient;
    private boolean enabled;

    public GenericSource(SourceConfig config) {
        this.config = config;
        this.jsonConfig = JsonSourceParser.parseFromMap(config.getExtraConfig());
        String type = safeGet(config.getExtraConfig(), "type", "json");
        boolean followRedirects = !"302".equals(type);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(followRedirects)
                .followSslRedirects(followRedirects)
                .build();
        this.enabled = config.isEnabled();
    }

    @Override
    public String getSourceId() {
        return config.getSourceId();
    }

    @Override
    public String getSourceName() {
        return config.getSourceName();
    }

    private static String safeGet(Map<String, String> map, String key, String def) {
        String v = map.get(key);
        return v != null ? v : def;
    }

    @Override
    public retrofit2.Call<WallpaperResponse> getWallpapers(int page, int pageSize) {
        return getWallpapers(page, pageSize, null);
    }

    @Override
    public retrofit2.Call<WallpaperResponse> getWallpapers(int page, int pageSize, java.util.Map<String, String> categoryParams) {
        String type = safeGet(config.getExtraConfig(), "type", "json");
        LogUtils.d("getWallpapers() sourceId=%s, type=%s, extraConfig=%s",
            getSourceId(), type, config.getExtraConfig());

        if ("js".equals(type)) {
            return getJsWallpapers(page, pageSize, categoryParams);
        } else if ("302".equals(type)) {
            return get302Wallpapers(page, pageSize);
        } else {
            return getJsonWallpapers(page, pageSize);
        }
    }

    private retrofit2.Call<WallpaperResponse> getJsonWallpapers(int page, int pageSize) {
        String url = buildUrl(jsonConfig.url, page, pageSize);
        return new SimpleOkHttpCall(url, jsonConfig, httpClient, mainHandler, "json", page, pageSize, null, null);
    }

    private retrofit2.Call<WallpaperResponse> getJsWallpapers(int page, int pageSize, java.util.Map<String, String> categoryParams) {
        String jsCode = safeGet(config.getExtraConfig(), "jsCode", "");
        return new SimpleOkHttpCall(jsCode, jsonConfig, httpClient, mainHandler, "js", page, pageSize, null, categoryParams);
    }

    private retrofit2.Call<WallpaperResponse> get302Wallpapers(int page, int pageSize) {
        String url = buildUrl(jsonConfig.url, page, pageSize);
        String imageUrlBase = safeGet(config.getExtraConfig(), "imageUrlBase", "");
        return new SimpleOkHttpCall(url, jsonConfig, httpClient, mainHandler, "302", page, pageSize, imageUrlBase, null);
    }

    @Override
    public List<String> getUrlList(int page, int pageSize, Map<String, String> categoryParams) {
        String type = safeGet(config.getExtraConfig(), "type", "json");
        switch (type) {
            case "302":
                return fetch302Urls(page, pageSize);
            case "js":
                return fetchJsUrls(page, pageSize, categoryParams);
            case "json":
            default:
                return fetchJsonUrls(page, pageSize);
        }
    }

    private List<String> fetch302Urls(int page, int pageSize) {
        List<String> urls = new ArrayList<>();
        String input = buildUrl(jsonConfig.url, page, pageSize);
        int count = pageSize > 0 ? pageSize : 5;
        for (int i = 0; i < count; i++) {
            try {
                String location = fetchSingle302Url(input);
                if (location != null && !location.isEmpty()) {
                    location = resolveUrl(location, input);
                    urls.add(location);
                }
            } catch (Exception e) {
                LogUtils.e("Failed to fetch 302 URL", e);
            }
        }
        return urls;
    }

    private List<String> fetchJsonUrls(int page, int pageSize) {
        List<String> urls = new ArrayList<>();
        try {
            String url = buildUrl(jsonConfig.url, page, pageSize);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build();
            try (Response httpResponse = httpClient.newCall(request).execute()) {
                if (httpResponse.isSuccessful() && httpResponse.body() != null) {
                    String body = httpResponse.body().string();
                    JSONObject json = new JSONObject(body);
                    String listField = jsonConfig.listField != null ? jsonConfig.listField : "data";
                    JSONArray dataArray = json.optJSONArray(listField);
                    if (dataArray != null) {
                        String urlField = jsonConfig.urlField != null ? jsonConfig.urlField : "url";
                        String widthField = jsonConfig.widthField != null ? jsonConfig.widthField : "width";
                        String heightField = jsonConfig.heightField != null ? jsonConfig.heightField : "height";
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            String rawUrl = item.optString(urlField, "");
                            if (!rawUrl.isEmpty()) {
                                String fullUrl = ImageUrlUtil.buildImageUrl(jsonConfig.imageUrlBase, rawUrl, jsonConfig.previewSuffix);
                                // 预填充尺寸缓存
                                int w = item.optInt(widthField, 0);
                                int h = item.optInt(heightField, 0);
                                if (w > 0 && h > 0) {
                                    io.inkdrop.wallpaper.data.cache.WallpaperCacheManager.getInstance()
                                        .putDimensions(fullUrl, w, h);
                                }
                                urls.add(fullUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Failed to fetch JSON URLs", e);
        }
        return urls;
    }

    private List<String> fetchJsUrls(int page, int pageSize, Map<String, String> categoryParams) {
        List<String> urls = new ArrayList<>();
        try {
            String jsCode = safeGet(config.getExtraConfig(), "jsCode", "");
            String imageUrlBase = safeGet(config.getExtraConfig(), "imageUrlBase", "");
            JsSourceEngine engine = new JsSourceEngine();
            WallpaperResponse response = engine.executeJsSource(jsCode, page, pageSize, imageUrlBase, categoryParams);
            if (response.getData() != null) {
                for (Wallpaper w : response.getData()) {
                    if (w.getUrl() != null && !w.getUrl().isEmpty()) {
                        // 预填充尺寸缓存，避免 WallpaperLoadManager 重复网络请求
                        if (w.getWidth() > 0 && w.getHeight() > 0) {
                            io.inkdrop.wallpaper.data.cache.WallpaperCacheManager.getInstance()
                                .putDimensions(w.getUrl(), w.getWidth(), w.getHeight());
                        }
                        urls.add(w.getUrl());
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Failed to fetch JS URLs", e);
        }
        return urls;
    }

    static String fetchSingle302Url(String input) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        Request request = new Request.Builder()
                .url(input)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build();
        try (Response httpResponse = client.newCall(request).execute()) {
            if (httpResponse.code() >= 300 && httpResponse.code() < 400) {
                return httpResponse.header("Location");
            }
            return null;
        }
    }

    static String resolveUrl(String location, String baseUrl) {
        if (location.startsWith("http")) {
            return location;
        }
        try {
            java.net.URI baseUri = new java.net.URI(baseUrl);
            java.net.URI resolvedUri = baseUri.resolve(location);
            return resolvedUri.toString();
        } catch (Exception e) {
            if (location.startsWith("/")) {
                try {
                    java.net.URL urlObj = new java.net.URL(baseUrl);
                    return urlObj.getProtocol() + "://" + urlObj.getHost() + location;
                } catch (Exception ex) {
                    return location;
                }
            }
            return baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + location;
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JsonSourceParser.ParsedConfig getJsonConfig() {
        return jsonConfig;
    }
    
    public void clearUrlCache() {
        urlCache.clear();
    }
    
    public int getUrlCacheSize() {
        return urlCache.size();
    }
    
    public static ConcurrentHashMap<String, int[]> getDimensionsCache() {
        return dimensionsCache;
    }

    public static void clearDimensionsCache() {
        dimensionsCache.clear();
    }
    
    public static int getDimensionsCacheSize() {
        return dimensionsCache.size();
    }

    private String buildUrl(String templateUrl, int page, int pageSize) {
        if (templateUrl == null) return "";
        return templateUrl
                .replace("{page}", String.valueOf(page))
                .replace("{pageSize}", String.valueOf(pageSize))
                .replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
    }

    static WallpaperResponse parseJsonResponse(String responseBody, JsonSourceParser.ParsedConfig jsonConfig) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        String listField = jsonConfig.listField != null ? jsonConfig.listField : "data";
        JSONArray dataArray = json.optJSONArray(listField);
        if (dataArray == null) dataArray = new JSONArray();

        List<Wallpaper> wallpapers = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            Wallpaper wallpaper = parseWallpaperItem(item, jsonConfig);
            if (wallpaper != null) wallpapers.add(wallpaper);
        }

        WallpaperResponse response = new WallpaperResponse();
        response.setData(wallpapers);
        response.setPage(json.optInt("page", 1));
        response.setPageSize(json.optInt("pageSize", wallpapers.size()));
        response.setTotal(json.optInt("total", wallpapers.size()));
        response.setTotalPages(json.optInt("totalPages", 1));
        return response;
    }

    static WallpaperResponse parse302Response(String responseBody, JsonSourceParser.ParsedConfig jsonConfig) throws JSONException {
        List<Wallpaper> wallpapers = new ArrayList<>();

        LogUtils.d("Parsing 302 response, length: " + responseBody.length());
        LogUtils.d("Config listField: " + jsonConfig.listField + ", urlField: " + jsonConfig.urlField);

        if (responseBody.trim().startsWith("[")) {
            LogUtils.d("Response is JSON array format");
            JSONArray arr = new JSONArray(responseBody);
            LogUtils.d("Array length: " + arr.length());
            for (int i = 0; i < arr.length(); i++) {
                String url = arr.getString(i);
                if (url != null && !url.isEmpty()) {
                    int id = Math.abs(url.hashCode() % HASH_MODULO);
                    wallpapers.add(new Wallpaper(id, url, "", 0, 0, ""));
                    LogUtils.d("Added wallpaper from array: " + url);
                } else {
                    LogUtils.w("Skipping empty URL at index " + i);
                }
            }
        } else if (responseBody.trim().startsWith("{")) {
            LogUtils.d("Response is JSON object format");
            JSONObject json = new JSONObject(responseBody);
            String listField = jsonConfig.listField != null ? jsonConfig.listField : "data";
            LogUtils.d("Using listField: " + listField);
            
            JSONArray arr = json.optJSONArray(listField);
            if (arr != null) {
                LogUtils.d("Found array with " + arr.length() + " items");
                String urlField = jsonConfig.urlField != null ? jsonConfig.urlField : "url";
                LogUtils.d("Using urlField: " + urlField);
                
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject item = arr.getJSONObject(i);
                        String url = item.optString(urlField, "");
                        if (!url.isEmpty()) {
                            int id = Math.abs(url.hashCode() % HASH_MODULO);
                            wallpapers.add(new Wallpaper(id, url, "", 0, 0, ""));
                            LogUtils.d("Added wallpaper from object: " + url);
                        } else {
                            LogUtils.w("Empty URL at index " + i + ", item keys: " + item.keys().toString());
                        }
                    } catch (JSONException e) {
                        LogUtils.e("Failed to parse item at index " + i, e);
                    }
                }
            } else {
                LogUtils.e("Array not found with field: " + listField);
                LogUtils.d("Available keys in response: " + json.keys().toString());
            }
        } else {
            LogUtils.e("Response format not recognized (not JSON array or object)");
            LogUtils.d("Response preview: " + responseBody.substring(0, Math.min(500, responseBody.length())));
        }

        LogUtils.d("Total wallpapers parsed: " + wallpapers.size());
        
        WallpaperResponse response = new WallpaperResponse();
        response.setData(wallpapers);
        response.setPage(1);
        response.setPageSize(wallpapers.size());
        response.setTotal(wallpapers.size());
        response.setTotalPages(wallpapers.size() > 0 ? 1 : 0);
        return response;
    }

    private static Wallpaper parseWallpaperItem(JSONObject item, JsonSourceParser.ParsedConfig jsonConfig) {
        try {
            String idField = jsonConfig.idField != null ? jsonConfig.idField : "id";
            String urlField = jsonConfig.urlField != null ? jsonConfig.urlField : "url";
            String widthField = jsonConfig.widthField != null ? jsonConfig.widthField : "width";
            String heightField = jsonConfig.heightField != null ? jsonConfig.heightField : "height";

            int id = item.optInt(idField, 0);
            String rawUrl = item.optString(urlField, "");
            int width = item.optInt(widthField, 0);
            int height = item.optInt(heightField, 0);

            String fullUrl = ImageUrlUtil.buildImageUrl(jsonConfig.imageUrlBase, rawUrl, jsonConfig.previewSuffix);

            return new Wallpaper(id, fullUrl, "", width, height, "");
        } catch (Exception e) {
            LogUtils.e("Failed to parse wallpaper item", e);
            return null;
        }
    }

    public static GenericSource createFromInternal(Context context, String fileName) {
        try {
            java.io.File file = new java.io.File(context.getFilesDir(), "sources/" + fileName);
            if (!file.exists()) return null;

            if (fileName.endsWith(".js")) {
                return createFromJs(context, fileName);
            } else if (fileName.endsWith(".json")) {
                return createFromJson(context, fileName);
            }
        } catch (Exception e) {
            LogUtils.e("Failed to create source from: " + fileName, e);
        }
        return null;
    }

    public static GenericSource createFromJson(Context context, String fileName) {
        try {
            java.io.File file = new java.io.File(context.getFilesDir(), "sources/" + fileName);
            String json = io.inkdrop.wallpaper.util.FileUtils.readFile(file);
            LogUtils.d("createFromJson() file=" + fileName);
            LogUtils.d("createFromJson() json=" + json);
            JsonSourceParser.ParsedConfig parsed = JsonSourceParser.parse(json);
            if (!parsed.valid) {
                LogUtils.e("Invalid JSON config: " + parsed.errorMessage);
                return null;
            }

            Map<String, String> extraConfig = JsonSourceParser.toMap(parsed);

            try {
                org.json.JSONObject obj = new org.json.JSONObject(json);
                org.json.JSONObject extraObj = obj.optJSONObject("extraConfig");
                if (extraObj != null && extraObj.has("type")) {
                    extraConfig.put("type", extraObj.getString("type"));
                } else {
                    extraConfig.put("type", "json");
                }
            } catch (Exception e) {
                extraConfig.put("type", "json");
            }

            LogUtils.d("createFromJson() extraConfig=" + extraConfig);

            SourceConfig config = new SourceConfig.Builder()
                    .sourceId(parsed.sourceId)
                    .sourceName(parsed.sourceName)
                    .extraConfig(extraConfig)
                    .build();

            return new GenericSource(config);
        } catch (Exception e) {
            LogUtils.e("Failed to create JSON source: " + fileName, e);
            return null;
        }
    }

    public static GenericSource createFromJs(Context context, String fileName) {
        try {
            java.io.File file = new java.io.File(context.getFilesDir(), "sources/" + fileName);
            String jsCode = io.inkdrop.wallpaper.util.FileUtils.readFile(file);

            String sourceId = fileName.replace(".js", "");
            String sourceName = parseJsField(jsCode, "sourceName", sourceId);
            List<WallpaperCategory> categories = parseJsCategories(jsCode);

            Map<String, String> extraConfig = new HashMap<>();
            extraConfig.put("type", "js");
            extraConfig.put("jsCode", jsCode);

            SourceConfig config = new SourceConfig.Builder()
                    .sourceId(sourceId)
                    .sourceName(sourceName)
                    .categories(categories)
                    .extraConfig(extraConfig)
                    .build();

            return new GenericSource(config);
        } catch (Exception e) {
            LogUtils.e("Failed to create JS source: " + fileName, e);
            return null;
        }
    }

    private static String parseJsField(String jsCode, String fieldName, String fallback) {
        try {
            String[] keys = {fieldName + ":", fieldName + " :"};
            for (String key : keys) {
                int keyIdx = jsCode.indexOf(key);
                if (keyIdx == -1) continue;
                int colonIdx = jsCode.indexOf(':', keyIdx);
                if (colonIdx == -1) continue;
                int start = colonIdx + 1;
                while (start < jsCode.length() && jsCode.charAt(start) == ' ') start++;
                if (start >= jsCode.length()) continue;
                char quote = jsCode.charAt(start);
                if (quote != '"' && quote != '\'') continue;
                int end = jsCode.indexOf(quote, start + 1);
                if (end == -1) continue;
                String value = jsCode.substring(start + 1, end).trim();
                if (!value.isEmpty()) return value;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private static List<WallpaperCategory> parseJsCategories(String jsCode) {
        List<WallpaperCategory> categories = new ArrayList<>();
        try {
            String key = "categories:";
            int keyIdx = jsCode.indexOf(key);
            if (keyIdx == -1) {
                key = "categories :";
                keyIdx = jsCode.indexOf(key);
            }
            if (keyIdx == -1) return categories;

            int bracketStart = jsCode.indexOf('[', keyIdx);
            if (bracketStart == -1) return categories;

            int bracketEnd = findMatchingBracket(jsCode, bracketStart);
            if (bracketEnd == -1) return categories;

            String arrayContent = jsCode.substring(bracketStart + 1, bracketEnd);
            parseCategoryArray(arrayContent, categories);
        } catch (Exception e) {
            LogUtils.w("Failed to parse JS categories", e);
        }
        return categories;
    }

    private static int findMatchingBracket(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static void parseCategoryArray(String content, List<WallpaperCategory> categories) {
        int i = 0;
        while (i < content.length()) {
            int objStart = content.indexOf('{', i);
            if (objStart == -1) break;

            int objEnd = findMatchingBracket(content, objStart);
            if (objEnd == -1) break;

            String objStr = content.substring(objStart + 1, objEnd);
            parseCategoryObject(objStr, categories);

            i = objEnd + 1;
        }
    }

    private static void parseCategoryObject(String objStr, List<WallpaperCategory> categories) {
        String id = extractJsonString(objStr, "id");
        String name = extractJsonString(objStr, "name");
        Map<String, String> params = extractJsonObject(objStr, "params");

        if (id != null && name != null) {
            categories.add(new WallpaperCategory(id, name, params));
        }
    }

    private static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        int colonIdx;
        if (keyIdx != -1) {
            colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        } else {
            searchKey = key + ":";
            keyIdx = json.indexOf(searchKey);
            if (keyIdx == -1) return null;
            colonIdx = keyIdx + searchKey.length() - 1;
        }
        if (colonIdx == -1) return null;

        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;

        char quote = json.charAt(start);
        if (quote != '"' && quote != '\'') return null;

        int end = json.indexOf(quote, start + 1);
        if (end == -1) return null;

        return json.substring(start + 1, end);
    }

    private static Map<String, String> extractJsonObject(String json, String key) {
        Map<String, String> map = new HashMap<>();
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) {
            searchKey = key + ":";
            keyIdx = json.indexOf(searchKey);
            if (keyIdx == -1) return map;
        }

        int braceStart = json.indexOf('{', keyIdx);
        if (braceStart == -1) return map;

        int braceEnd = findMatchingBracket(json, braceStart);
        if (braceEnd == -1) return map;

        String objContent = json.substring(braceStart + 1, braceEnd);
        String[] pairs = objContent.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim().replace("\"", "");
                if (!k.isEmpty() && !v.isEmpty()) {
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    public static int[] fetchImageDimensions(String imageUrl) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);

            int redirectCount = 0;
            while (conn.getResponseCode() / 100 == 3 && redirectCount < 5) {
                String location = conn.getHeaderField("Location");
                if (location == null) break;
                conn.disconnect();
                url = new URL(location);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setInstanceFollowRedirects(true);
                redirectCount++;
            }

            is = conn.getInputStream();
            byte[] data = new byte[32 * 1024];
            int bytesRead = 0;
            int totalRead = 0;

            while (bytesRead != -1 && totalRead < data.length) {
                bytesRead = is.read(data, totalRead, data.length - totalRead);
                if (bytesRead > 0) totalRead += bytesRead;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, totalRead, opts);

            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return new int[]{opts.outWidth, opts.outHeight};
            }
        } catch (Exception e) {
            LogUtils.w("Failed to fetch image dimensions: " + imageUrl, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    static class SimpleOkHttpCall implements retrofit2.Call<WallpaperResponse> {
        private final String input;
        private final JsonSourceParser.ParsedConfig jsonConfig;
        private final OkHttpClient client;
        private final Handler mainHandler;
        private final String type;
        private final int page;
        private final int pageSize;
        private final String imageUrlBase;
        private final java.util.Map<String, String> categoryParams;
        private volatile boolean cancelled = false;
        private volatile boolean executed = false;
        private Call rawCall;

        SimpleOkHttpCall(String input, JsonSourceParser.ParsedConfig jsonConfig, OkHttpClient client, Handler mainHandler, String type, int page, int pageSize, String imageUrlBase, java.util.Map<String, String> categoryParams) {
            this.input = input;
            this.jsonConfig = jsonConfig;
            this.client = client;
            this.mainHandler = mainHandler;
            this.type = type;
            this.page = page;
            this.pageSize = pageSize;
            this.imageUrlBase = imageUrlBase;
            this.categoryParams = categoryParams;
        }

        @Override
        public retrofit2.Response<WallpaperResponse> execute() {
            executed = true;
            try {
                if ("js".equals(type)) {
                    return executeJs();
                } else if ("302".equals(type)) {
                    return execute302();
                } else {
                    return executeJson();
                }
            } catch (Exception e) {
                return retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, e.getMessage()));
            }
        }

        private retrofit2.Response<WallpaperResponse> executeJson() throws IOException {
            LogUtils.d("Executing JSON request to: " + input);
            
            Request request = new Request.Builder()
                    .url(input)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build();
            rawCall = client.newCall(request);
            try (Response httpResponse = rawCall.execute()) {
                if (cancelled) return retrofit2.Response.success(null);
                if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
                    return retrofit2.Response.error(httpResponse.code(), okhttp3.ResponseBody.create(null, ""));
                }
                String body = httpResponse.body().string();
                WallpaperResponse response = parseJsonResponse(body, jsonConfig);
                
                // 依次获取每张图片的尺寸（如果缺少）
                if (response.getData() != null) {
                    LogUtils.d("JSON response contains " + response.getData().size() + " wallpapers");
                    fetchDimensionsForWallpapers(response.getData());
                }
                
                // 设置分页信息
                response.setPage(page);
                response.setPageSize(response.getData() != null ? response.getData().size() : 0);
                
                return retrofit2.Response.success(response);
            } catch (JSONException e) {
                LogUtils.e("JSON parsing error", e);
                return retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, e.getMessage()));
            }
        }

        private retrofit2.Response<WallpaperResponse> execute302() throws IOException {
            int count = pageSize > 0 ? pageSize : 5;
            LogUtils.d("Executing 302 requests to: " + input + ", count: " + count);
            
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (cancelled) break;
                try {
                    String location = fetchSingle302Url(input);
                    if (location != null && !location.isEmpty()) {
                        location = resolveUrl(location, input);
                        urls.add(location);
                        LogUtils.d("Cached URL [" + (i + 1) + "/" + count + "]: " + location);
                    }
                } catch (Exception e) {
                    LogUtils.e("Failed to fetch URL " + (i + 1), e);
                }
            }
            
            LogUtils.d("Total URLs cached: " + urls.size());
            
            List<Wallpaper> wallpapers = new ArrayList<>();
            for (String url : urls) {
                int id = Math.abs(url.hashCode() % HASH_MODULO);
                wallpapers.add(new Wallpaper(id, url, "", 0, 0, ""));
            }
            
            fetchDimensionsForWallpapers(wallpapers);
            
            WallpaperResponse response = new WallpaperResponse();
            response.setData(wallpapers);
            response.setPage(page);
            response.setPageSize(wallpapers.size());
            response.setTotal(-1);
            response.setTotalPages(-1);
            return retrofit2.Response.success(response);
        }

        private retrofit2.Response<WallpaperResponse> executeJs() {
            try {
                LogUtils.d("Executing JS source");
                JsSourceEngine engine = new JsSourceEngine();
                WallpaperResponse response = engine.executeJsSource(input, page, pageSize, imageUrlBase, categoryParams);
                
                // 统一获取尺寸
                if (response.getData() != null) {
                    LogUtils.d("JS response contains " + response.getData().size() + " wallpapers");
                    fetchDimensionsForWallpapers(response.getData());
                }
                
                return retrofit2.Response.success(response);
            } catch (Exception e) {
                LogUtils.e("JS execution error", e);
                return retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, e.getMessage()));
            }
        }
        
        private void fetchDimensionsForWallpapers(List<Wallpaper> wallpapers) {
            for (int i = 0; i < wallpapers.size(); i++) {
                if (cancelled) break;
                Wallpaper wallpaper = wallpapers.get(i);
                String url = wallpaper.getUrl();
                
                // 1. 已有尺寸，跳过
                if (wallpaper.getWidth() > 0 && wallpaper.getHeight() > 0) {
                    LogUtils.d("Wallpaper " + (i + 1) + " already has dimensions: "
                        + wallpaper.getWidth() + "x" + wallpaper.getHeight());
                    continue;
                }
                
                // 2. 内存缓存命中
                int[] cached = dimensionsCache.get(url);
                if (cached != null) {
                    wallpaper.setWidth(cached[0]);
                    wallpaper.setHeight(cached[1]);
                    LogUtils.d("Wallpaper " + (i + 1) + " cache hit: " + cached[0] + "x" + cached[1]);
                    continue;
                }
                
                // 3. 网络获取
                LogUtils.d("Fetching dimensions for wallpaper " + (i + 1) + ": " + url);
                try {
                    int[] dims = fetchImageDimensions(url);
                    if (dims != null) {
                        wallpaper.setWidth(dims[0]);
                        wallpaper.setHeight(dims[1]);
                        dimensionsCache.put(url, dims);
                        LogUtils.d("Dimensions fetched and cached: " + dims[0] + "x" + dims[1]);
                    }
                } catch (Exception e) {
                    LogUtils.w("Failed to fetch dimensions for: " + url);
                }
            }
        }

        @Override
        public void enqueue(final retrofit2.Callback<WallpaperResponse> callback) {
            sizeExecutor.execute(() -> {
                retrofit2.Response<WallpaperResponse> response = execute();
                mainHandler.post(() -> {
                    if (cancelled) return;
                    if (response.isSuccessful()) {
                        callback.onResponse(SimpleOkHttpCall.this, response);
                    } else {
                        String errMsg = "Request failed";
                        if (response.errorBody() != null) {
                            try {
                                errMsg = response.errorBody().string();
                            } catch (Exception ignored) {}
                        }
                        if (errMsg.isEmpty()) errMsg = "HTTP " + response.code();
                        callback.onFailure(SimpleOkHttpCall.this, new IOException(errMsg));
                    }
                });
            });
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (rawCall != null) rawCall.cancel();
        }

        @Override
        public boolean isCanceled() { return cancelled; }

        @Override
        public boolean isExecuted() { return executed; }

        @Override
        public retrofit2.Call<WallpaperResponse> clone() {
            return new SimpleOkHttpCall(input, jsonConfig, client, mainHandler, type, page, pageSize, imageUrlBase, categoryParams);
        }

        @Override
        public okhttp3.Request request() {
            return new Request.Builder().url("https://placeholder").build();
        }

        @Override
        public okio.Timeout timeout() {
            return okio.Timeout.NONE;
        }
    }
}
