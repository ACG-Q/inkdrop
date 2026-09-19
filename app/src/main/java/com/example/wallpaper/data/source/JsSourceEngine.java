package com.example.wallpaper.data.source;

import com.example.wallpaper.data.remote.Wallpaper;
import com.example.wallpaper.data.remote.WallpaperResponse;
import com.example.wallpaper.util.ImageUrlUtil;
import com.example.wallpaper.util.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JsSourceEngine {
    private static final long JS_EXECUTION_TIMEOUT_MS = 10000; // 10秒超时
    private static volatile OkHttpClient sharedHttpClient;
    // 共享线程池，避免内存泄漏
    private static final ExecutorService SHARED_JS_EXECUTOR = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient;
    
    // JS 代码安全检查 - 禁止访问的包
    private static final String[] RESTRICTED_PACKAGES = {
        "java.lang.", "java.io.", "java.net.", "java.util.",
        "javax.", "android.os.", "android.content.", "Packages."
    };
    private static final int HASH_MODULO = 1_000_000_000;

    public JsSourceEngine() {
        this.httpClient = getSharedHttpClient();
    }
    
    /**
     * 获取共享的 OkHttpClient 实例，避免内存泄漏
     */
    private static OkHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            synchronized (JsSourceEngine.class) {
                if (sharedHttpClient == null) {
                    sharedHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return sharedHttpClient;
    }

    public WallpaperResponse executeJsSource(String jsCode, int page, int pageSize, String imageUrlBase, java.util.Map<String, String> categoryParams) {
        try {
            LogUtils.d("Executing JS source, page: " + page + ", pageSize: " + pageSize);
            
            // 使用CountDownLatch实现超时控制
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] resultHolder = new String[1];
            final Exception[] exceptionHolder = new Exception[1];
            
            Thread jsThread = new Thread(() -> {
                try {
                    resultHolder[0] = executeSync(jsCode, page, pageSize, categoryParams);
                } catch (Exception e) {
                    exceptionHolder[0] = e;
                } finally {
                    latch.countDown();
                }
            });
            
            jsThread.setName("JS-Engine-Thread");
            jsThread.setDaemon(true);
            
            // 使用共享线程池执行
            SHARED_JS_EXECUTOR.execute(jsThread::run);
            
            // 等待执行完成或超时
            boolean completed = latch.await(JS_EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!completed) {
                LogUtils.e("JS execution timed out after " + JS_EXECUTION_TIMEOUT_MS + "ms");
                jsThread.interrupt();
                // 等待线程结束，避免泄漏
                try {
                    jsThread.join(1000);
                } catch (InterruptedException ignored) {}
                throw new TimeoutException("JS execution timed out");
            }
            
            if (exceptionHolder[0] != null) {
                throw exceptionHolder[0];
            }
            
            if (resultHolder[0] == null) {
                throw new JsException("JS execution returned null");
            }
            
            LogUtils.d("JS execution completed successfully");
            LogUtils.d("JS result: " + resultHolder[0]);
            return parseResponse(resultHolder[0], imageUrlBase, page);
            
        } catch (TimeoutException e) {
            LogUtils.e("JS execution timeout", e);
            return createErrorResponse("JS execution timeout: " + e.getMessage(), page, pageSize);
        } catch (OutOfMemoryError e) {
            LogUtils.e("JS execution caused OutOfMemoryError", e);
            return createErrorResponse("Out of memory: " + e.getMessage(), page, pageSize);
        } catch (StackOverflowError e) {
            LogUtils.e("JS execution caused StackOverflowError", e);
            return createErrorResponse("Stack overflow: " + e.getMessage(), page, pageSize);
        } catch (JsException e) {
            LogUtils.e("JS execution failed", e);
            return createErrorResponse(e.getMessage(), page, pageSize);
        } catch (Exception e) {
            LogUtils.e("JS execution failed", e);
            return createErrorResponse(e.getMessage(), page, pageSize);
        } catch (Error e) {
            LogUtils.e("JS execution caused fatal error", e);
            return createErrorResponse("Fatal error: " + e.getMessage(), page, pageSize);
        }
    }
    
    private WallpaperResponse createErrorResponse(String errorMessage, int page, int pageSize) {
        WallpaperResponse response = new WallpaperResponse();
        response.setData(new ArrayList<>());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(0);
        response.setTotalPages(0);
        return response;
    }

    public String executeSync(String jsCode, int page, int pageSize, java.util.Map<String, String> categoryParams) throws JsException {
        if (jsCode == null || jsCode.trim().isEmpty()) {
            throw new JsException("JS 代码为空");
        }

        // 安全检查：验证 JS 代码不包含受限包
        for (String restricted : RESTRICTED_PACKAGES) {
            if (jsCode.contains(restricted)) {
                throw new JsException("JS 代码包含受限包: " + restricted);
            }
        }

        LogUtils.d("Starting JS execution, code length: " + jsCode.length());
        
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_ES6);

        try {
            ScriptableObject scope = cx.initStandardObjects();

            // 将 OkHttpBridge 包装为 NativeJavaObject，避免类型转换问题
            OkHttpBridge bridge = new OkHttpBridge(httpClient);
            org.mozilla.javascript.NativeJavaObject bridgeWrapper = 
                new org.mozilla.javascript.NativeJavaObject(scope, bridge, OkHttpBridge.class);
            scope.put("OkHttpBridge", scope, bridgeWrapper);
            LogUtils.d("OkHttpBridge registered in scope");

            ScriptableObject.putProperty(scope, "__page", page);
            ScriptableObject.putProperty(scope, "__pageSize", pageSize);

            // 注入 category 参数到 JS 作用域
            if (categoryParams != null && !categoryParams.isEmpty()) {
                org.mozilla.javascript.NativeObject categoryObj = 
                    (org.mozilla.javascript.NativeObject) cx.newObject(scope);
                for (java.util.Map.Entry<String, String> entry : categoryParams.entrySet()) {
                    categoryObj.put(entry.getKey(), categoryObj, entry.getValue());
                }
                scope.put("__category", scope, categoryObj);
                LogUtils.d("Category params injected: " + categoryParams);
            } else {
                scope.put("__category", scope, null);
            }

            String wrappedCode = wrapJsCode(jsCode);
            LogUtils.d("Executing wrapped JS code, length: " + wrappedCode.length());
            
            Object result = cx.evaluateString(scope, wrappedCode, "js", 1, null);

            if (result == null) {
                throw new JsException("JS 执行返回 null");
            }

            String resultStr = Context.toString(result);
            LogUtils.d("JS raw result: " + resultStr);
            
            if (resultStr.equals("undefined") || resultStr.isEmpty()) {
                throw new JsException("JS 执行返回空值");
            }

            LogUtils.d("JS execution successful, result length: " + resultStr.length());
            return resultStr;
            
        } catch (JsException e) {
            throw e;
        } catch (Exception e) {
            LogUtils.e("JS execution exception", e);
            throw new JsException("JS 执行异常: " + e.getMessage());
        } catch (Error e) {
            LogUtils.e("JS execution error", e);
            throw new JsException("JS 执行错误: " + e.getMessage());
        } finally {
            try {
                Context.exit();
            } catch (Exception e) {
                LogUtils.w("Failed to exit Rhino context", e);
            }
        }
    }

    private String wrapJsCode(String jsCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("(function() {\n");
        sb.append("  try {\n");

        sb.append("    var page = __page;\n");
        sb.append("    var pageSize = __pageSize;\n");
        sb.append("    var category = (typeof __category !== 'undefined' && __category !== null && typeof __category === 'object' && Object.keys(__category).length > 0) ? __category : undefined;\n");

        sb.append(jsCode).append("\n");

        sb.append("    if (typeof SOURCE !== 'undefined' && SOURCE.fetchWallpapers) {\n");
        sb.append("      var result = SOURCE.fetchWallpapers(page, pageSize, category);\n");
        sb.append("      if (result === null || result === undefined) {\n");
        sb.append("        return JSON.stringify({ data: [], total: 0, error: 'SOURCE.fetchWallpapers returned null' });\n");
        sb.append("      }\n");
        sb.append("      if (result.wallpapers) {\n");
        sb.append("        var out = { data: result.wallpapers, total: result.total || 0 };\n");
        sb.append("        if (result.error) out.error = result.error;\n");
        sb.append("        return JSON.stringify(out);\n");
        sb.append("      }\n");
        sb.append("      return JSON.stringify(result);\n");
        sb.append("    }\n");

        sb.append("    if (typeof fetch === 'function') {\n");
        sb.append("      var result = fetch(page, pageSize);\n");
        sb.append("      if (result === null || result === undefined) {\n");
        sb.append("        return JSON.stringify({ data: [], total: 0, error: 'fetch returned null' });\n");
        sb.append("      }\n");
        sb.append("      return JSON.stringify(result);\n");
        sb.append("    }\n");

        sb.append("    return JSON.stringify({ data: [], total: 0, error: 'No SOURCE or fetch function found' });\n");
        
        sb.append("  } catch(e) {\n");
        sb.append("    var errorMsg = 'JS catch: ' + e.toString();\n");
        sb.append("    if (typeof console !== 'undefined' && console.log) console.log(errorMsg);\n");
        sb.append("    return JSON.stringify({ data: [], total: 0, error: errorMsg });\n");
        sb.append("  }\n");
        
        sb.append("})()");
        return sb.toString();
    }

    private WallpaperResponse parseResponse(String resultStr, String imageUrlBase, int page) throws JSONException {
        LogUtils.d("Parsing JS response: " + resultStr);
        
        JSONObject json = new JSONObject(resultStr);
        List<Wallpaper> wallpapers = new ArrayList<>();

        // 检查是否有错误信息
        String error = json.optString("error", null);
        if (error != null) {
            LogUtils.e("JS execution error in response: " + error);
        }

        JSONArray arr = json.optJSONArray("data");
        if (arr == null) arr = json.optJSONArray("wallpapers");

        if (arr != null) {
            LogUtils.d("Found " + arr.length() + " wallpapers in response");
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject item = arr.getJSONObject(i);
                    int id = item.optInt("id", Math.abs(item.toString().hashCode() % HASH_MODULO));
                    String rawUrl = item.optString("url", item.optString("path", ""));
                    int width = item.optInt("width", 0);
                    int height = item.optInt("height", 0);

                    if (width == 0 && height == 0) {
                        String resolution = item.optString("resolution", "");
                        if (resolution.contains("x")) {
                            String[] parts = resolution.split("x");
                            try {
                                width = Integer.parseInt(parts[0].trim());
                                height = Integer.parseInt(parts[1].trim());
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if (!rawUrl.isEmpty()) {
                        // 使用 ImageUrlUtil 构建完整 URL
                        String fullUrl = ImageUrlUtil.buildImageUrl(imageUrlBase, rawUrl, null);
                        wallpapers.add(new Wallpaper(id, fullUrl, "", width, height, ""));
                        LogUtils.d("Added wallpaper: " + fullUrl);
                    } else {
                        LogUtils.w("Skipping empty URL at index " + i);
                    }
                } catch (JSONException e) {
                    LogUtils.e("Failed to parse wallpaper at index " + i, e);
                }
            }
        } else {
            LogUtils.e("No 'data' or 'wallpapers' array found in response");
            LogUtils.d("Response keys: " + json.keys().toString());
        }

        LogUtils.d("Total wallpapers parsed: " + wallpapers.size());
        
        WallpaperResponse response = new WallpaperResponse();
        response.setData(wallpapers);
        response.setPage(page);
        response.setPageSize(wallpapers.size());
        response.setTotal(json.optInt("total", wallpapers.size()));
        response.setTotalPages(wallpapers.size() > 0 ? 1 : 0);
        return response;
    }

    public static class OkHttpBridge extends ScriptableObject {
        private OkHttpClient client;
        private static OkHttpClient sharedClient;

        public OkHttpBridge() {}

        public OkHttpBridge(OkHttpClient client) {
            this.client = client;
        }

        @Override
        public String getClassName() {
            return "OkHttpBridge";
        }
        
        /**
         * 获取共享的 OkHttpClient 实例，避免每次请求都创建新实例
         */
        private static OkHttpClient getSharedClient() {
            if (sharedClient == null) {
                sharedClient = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();
            }
            return sharedClient;
        }

        public String get(String url) {
            LogUtils.d("OkHttpBridge.get() called with URL: " + url);
            
            // 优先使用传入的 client，否则使用共享实例
            OkHttpClient clientToUse = (client != null) ? client : getSharedClient();
            
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .build();
                
                try (Response response = clientToUse.newCall(request).execute()) {
                    LogUtils.d("OkHttpBridge response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        LogUtils.d("OkHttpBridge response length: " + body.length());
                        LogUtils.d("OkHttpBridge response preview: " + body.substring(0, Math.min(200, body.length())));
                        return body;
                    } else {
                        LogUtils.e("OkHttpBridge request failed with code: " + response.code());
                    }
                }
            } catch (Exception e) {
                LogUtils.e("OkHttpBridge request failed: " + url, e);
            }
            return null;
        }
    }

    public static class JsException extends Exception {
        public JsException(String message) {
            super(message);
        }
    }
}
