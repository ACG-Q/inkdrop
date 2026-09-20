package io.inkdrop.wallpaper.data.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonSourceParser {

    public static class ParsedConfig {
        public String sourceId;
        public String sourceName;
        public String url;
        public String listField;
        public String idField;
        public String urlField;
        public String widthField;
        public String heightField;
        public String imageUrlBase;
        public String previewSuffix;
        public boolean valid;
        public String errorMessage;
        public List<WallpaperCategory> categories = new ArrayList<>();
    }

    public static ParsedConfig parse(String jsonString) {
        ParsedConfig config = new ParsedConfig();
        config.valid = false;

        if (jsonString == null || jsonString.trim().isEmpty()) {
            config.errorMessage = "JSON 内容为空";
            return config;
        }

        try {
            JSONObject json = new JSONObject(jsonString);

            config.sourceId = json.optString("sourceId", "");
            config.sourceName = json.optString("sourceName", "");
            config.url = json.optString("url", "");
            config.imageUrlBase = json.optString("imageUrlBase", "");
            config.previewSuffix = json.optString("previewSuffix", "");

            if (config.url.isEmpty()) {
                JSONObject extraObj = json.optJSONObject("extraConfig");
                if (extraObj != null) {
                    config.url = extraObj.optString("url", "");
                    if (config.listField == null || config.listField.isEmpty()) {
                        config.listField = extraObj.optString("listField", extraObj.optString("dataField", "data"));
                    }
                    if (config.idField == null || config.idField.isEmpty()) {
                        config.idField = extraObj.optString("idField", "id");
                    }
                    if (config.urlField == null || config.urlField.isEmpty()) {
                        config.urlField = extraObj.optString("urlField", "url");
                    }
                    if (config.widthField == null || config.widthField.isEmpty()) {
                        config.widthField = extraObj.optString("widthField", "width");
                    }
                    if (config.heightField == null || config.heightField.isEmpty()) {
                        config.heightField = extraObj.optString("heightField", "height");
                    }
                    if (config.imageUrlBase.isEmpty()) {
                        config.imageUrlBase = extraObj.optString("imageUrlBase", "");
                    }
                    if (config.previewSuffix.isEmpty()) {
                        config.previewSuffix = extraObj.optString("previewSuffix", "");
                    }
                }
            }

            JSONObject response = json.optJSONObject("response");
            if (response != null) {
                config.listField = response.optString("list", "data");
                JSONObject item = response.optJSONObject("item");
                if (item != null) {
                    config.idField = item.optString("id", "id");
                    config.urlField = item.optString("url", "url");
                    config.widthField = item.optString("width", "width");
                    config.heightField = item.optString("height", "height");
                }
            }

            if (config.sourceId.isEmpty()) {
                config.errorMessage = "缺少 sourceId 字段";
                return config;
            }
            if (config.url.isEmpty()) {
                config.errorMessage = "缺少 url 字段";
                return config;
            }

            // 解析 categories
            JSONArray categoriesArr = json.optJSONArray("categories");
            if (categoriesArr != null) {
                for (int i = 0; i < categoriesArr.length(); i++) {
                    JSONObject catObj = categoriesArr.optJSONObject(i);
                    if (catObj != null) {
                        String catId = catObj.optString("id", "");
                        String catName = catObj.optString("name", "");
                        Map<String, String> catParams = new HashMap<>();
                        JSONObject paramsObj = catObj.optJSONObject("params");
                        if (paramsObj != null) {
                            Iterator<String> keys = paramsObj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                catParams.put(key, paramsObj.optString(key, ""));
                            }
                        }
                        if (!catId.isEmpty() && !catName.isEmpty()) {
                            config.categories.add(new WallpaperCategory(catId, catName, catParams));
                        }
                    }
                }
            }

            config.valid = true;
        } catch (JSONException e) {
            config.errorMessage = "JSON 解析错误: " + e.getMessage();
        }

        return config;
    }

    public static String toJson(ParsedConfig config) {
        try {
            JSONObject json = new JSONObject();
            json.put("sourceId", config.sourceId != null ? config.sourceId : "");
            json.put("sourceName", config.sourceName != null ? config.sourceName : "");
            json.put("url", config.url != null ? config.url : "");

            JSONObject response = new JSONObject();
            response.put("list", config.listField != null ? config.listField : "data");
            JSONObject item = new JSONObject();
            item.put("id", config.idField != null ? config.idField : "id");
            item.put("url", config.urlField != null ? config.urlField : "url");
            item.put("width", config.widthField != null ? config.widthField : "width");
            item.put("height", config.heightField != null ? config.heightField : "height");
            response.put("item", item);
            json.put("response", response);

            json.put("imageUrlBase", config.imageUrlBase != null ? config.imageUrlBase : "");
            json.put("previewSuffix", config.previewSuffix != null ? config.previewSuffix : "");

            return json.toString(2);
        } catch (JSONException e) {
            return "{}";
        }
    }

    static String safeGet(java.util.Map<String, String> map, String key, String def) {
        String v = map.get(key);
        return v != null ? v : def;
    }

    public static ParsedConfig parseFromMap(java.util.Map<String, String> extraConfig) {
        ParsedConfig config = new ParsedConfig();
        config.valid = false;

        if (extraConfig == null || extraConfig.isEmpty()) {
            config.errorMessage = "配置为空";
            return config;
        }

        config.sourceId = safeGet(extraConfig, "sourceId", "");
        config.sourceName = safeGet(extraConfig, "sourceName", "");
        config.url = safeGet(extraConfig, "url", "");
        config.listField = safeGet(extraConfig, "listField", "data");
        config.idField = safeGet(extraConfig, "idField", "id");
        config.urlField = safeGet(extraConfig, "urlField", "url");
        config.widthField = safeGet(extraConfig, "widthField", "width");
        config.heightField = safeGet(extraConfig, "heightField", "height");
        config.imageUrlBase = safeGet(extraConfig, "imageUrlBase", "");
        config.previewSuffix = safeGet(extraConfig, "previewSuffix", "");

        // 解析 categories JSON 字符串
        String categoriesJson = safeGet(extraConfig, "categories", "");
        if (!categoriesJson.isEmpty()) {
            try {
                JSONArray categoriesArr = new JSONArray(categoriesJson);
                for (int i = 0; i < categoriesArr.length(); i++) {
                    JSONObject catObj = categoriesArr.optJSONObject(i);
                    if (catObj != null) {
                        String catId = catObj.optString("id", "");
                        String catName = catObj.optString("name", "");
                        Map<String, String> catParams = new HashMap<>();
                        JSONObject paramsObj = catObj.optJSONObject("params");
                        if (paramsObj != null) {
                            Iterator<String> keys = paramsObj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                catParams.put(key, paramsObj.optString(key, ""));
                            }
                        }
                        if (!catId.isEmpty() && !catName.isEmpty()) {
                            config.categories.add(new WallpaperCategory(catId, catName, catParams));
                        }
                    }
                }
            } catch (JSONException ignored) {}
        }

        if (config.sourceId.isEmpty() || config.url.isEmpty()) {
            config.errorMessage = "缺少必要字段";
            return config;
        }

        config.valid = true;
        return config;
    }

    public static java.util.Map<String, String> toMap(ParsedConfig config) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("sourceId", config.sourceId != null ? config.sourceId : "");
        map.put("sourceName", config.sourceName != null ? config.sourceName : "");
        map.put("url", config.url != null ? config.url : "");
        map.put("listField", config.listField != null ? config.listField : "data");
        map.put("idField", config.idField != null ? config.idField : "id");
        map.put("urlField", config.urlField != null ? config.urlField : "url");
        map.put("widthField", config.widthField != null ? config.widthField : "width");
        map.put("heightField", config.heightField != null ? config.heightField : "height");
        map.put("imageUrlBase", config.imageUrlBase != null ? config.imageUrlBase : "");
        map.put("previewSuffix", config.previewSuffix != null ? config.previewSuffix : "");
        
        // 序列化 categories 为 JSON 字符串
        if (config.categories != null && !config.categories.isEmpty()) {
            try {
                JSONArray categoriesArr = new JSONArray();
                for (WallpaperCategory cat : config.categories) {
                    JSONObject catObj = new JSONObject();
                    catObj.put("id", cat.getId());
                    catObj.put("name", cat.getName());
                    if (cat.getParams() != null && !cat.getParams().isEmpty()) {
                        JSONObject paramsObj = new JSONObject();
                        for (java.util.Map.Entry<String, String> entry : cat.getParams().entrySet()) {
                            paramsObj.put(entry.getKey(), entry.getValue());
                        }
                        catObj.put("params", paramsObj);
                    }
                    categoriesArr.put(catObj);
                }
                map.put("categories", categoriesArr.toString());
            } catch (JSONException ignored) {}
        }
        
        return map;
    }

    public static String getFieldPath(JSONObject json, String fieldPath) {
        if (json == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        String[] parts = fieldPath.split("\\.");
        Object current = json;

        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).opt(part);
            } else {
                return null;
            }
        }

        return current != null ? current.toString() : null;
    }
}
