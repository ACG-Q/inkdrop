# JS 源开发文档

## 概述

JS 源允许通过执行 JavaScript 代码获取壁纸列表，提供最大的灵活性。使用 Rhino 引擎执行 JS 代码。

## 基本结构

```javascript
var SOURCE = {
  sourceName: "源名称",
  
  categories: [
    {
      id: "nature",
      name: "自然风景",
      params: { tag: "nature" }
    }
  ],
  
  fetchWallpapers: function(page, pageSize, category) {
    // 实现获取壁纸逻辑
    return {
      data: [
        { id: 1, url: "https://example.com/image.jpg", width: 1920, height: 1080 }
      ],
      total: 100
    };
  }
};
```

## 内置变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `page` | number | 当前页码（从 1 开始） |
| `pageSize` | number | 每页数量 |
| `category` | object | 当前选中的分类参数 |

## 网络请求

使用 `OkHttpBridge.get(url)` 进行 HTTP GET 请求：

```javascript
var response = OkHttpBridge.get("https://api.example.com/data");
var json = JSON.parse(response);
```

## 返回格式

```javascript
{
  data: [
    {
      id: 1,
      url: "https://example.com/image.jpg",
      path: "/images/image.jpg",  // 可选，用于拼接 baseUrl
      width: 1920,
      height: 1080,
      resolution: "1920x1080"     // 可选，备用尺寸格式
    }
  ],
  total: 100,
  error: null  // 可选，错误信息
}
```

## 完整示例

### 示例 1：简单 API 调用

```javascript
var SOURCE = {
  sourceName: "示例源",
  
  fetchWallpapers: function(page, pageSize, category) {
    var url = "https://api.example.com/wallpapers?page=" + page + "&size=" + pageSize;
    
    if (category && category.tag) {
      url += "&tag=" + category.tag;
    }
    
    var response = OkHttpBridge.get(url);
    var json = JSON.parse(response);
    
    return {
      data: json.items.map(function(item) {
        return {
          id: item.id,
          url: item.imageUrl,
          width: item.width,
          height: item.height
        };
      }),
      total: json.total
    };
  }
};
```

### 示例 2：带分类的源

```javascript
var SOURCE = {
  sourceName: "风景壁纸",
  
  categories: [
    { id: "all", name: "全部", params: {} },
    { id: "mountain", name: "山川", params: { tag: "mountain" } },
    { id: "ocean", name: "海洋", params: { tag: "ocean" } },
    { id: "forest", name: "森林", params: { tag: "forest" } }
  ],
  
  fetchWallpapers: function(page, pageSize, category) {
    var params = "page=" + page + "&limit=" + pageSize;
    
    if (category && category.tag) {
      params += "&tag=" + category.tag;
    }
    
    var response = OkHttpBridge.get("https://api.example.com/images?" + params);
    var data = JSON.parse(response);
    
    return {
      data: data.images.map(function(img) {
        return {
          id: img.id,
          url: img.url,
          width: img.width,
          height: img.height
        };
      }),
      total: data.pagination.total
    };
  }
};
```

### 示例 3：多页数据聚合

```javascript
var SOURCE = {
  sourceName: "聚合源",
  
  fetchWallpapers: function(page, pageSize, category) {
    var allItems = [];
    
    // 从多个 API 获取数据
    var apis = [
      "https://api1.example.com/wallpapers?page=" + page,
      "https://api2.example.com/wallpapers?page=" + page
    ];
    
    for (var i = 0; i < apis.length; i++) {
      try {
        var response = OkHttpBridge.get(apis[i]);
        var json = JSON.parse(response);
        if (json.data) {
          allItems = allItems.concat(json.data);
        }
      } catch (e) {
        // 忽略失败的请求
      }
    }
    
    return {
      data: allItems.slice(0, pageSize),
      total: allItems.length
    };
  }
};
```

### 示例 4：处理分页

```javascript
var SOURCE = {
  sourceName: "分页源",
  
  fetchWallpapers: function(page, pageSize, category) {
    var url = "https://api.example.com/wallpapers";
    
    var requestBody = JSON.stringify({
      page: page,
      size: pageSize,
      filters: category || {}
    });
    
    // 使用 POST 请求（通过 GET 参数模拟）
    var response = OkHttpBridge.get(url + "?data=" + encodeURIComponent(requestBody));
    var json = JSON.parse(response);
    
    return {
      data: json.wallpapers,
      total: json.pagination.totalCount
    };
  }
};
```

## 安全限制

JS 代码不能访问以下 Java 包：

- `java.lang.*`
- `java.io.*`
- `java.net.*`
- `java.util.*`
- `javax.*`
- `android.os.*`
- `android.content.*`
- `Packages.*`

## 执行限制

- **超时时间**：10 秒
- **内存限制**：遵循 Android 应用内存限制
- **线程**：在专用线程池中执行

## 调试技巧

1. 使用 `console.log()` 输出调试信息（会记录到应用日志）
2. 确保 JSON.parse 的输入是有效的 JSON 字符串
3. 处理网络请求异常，避免 JS 执行崩溃
4. 返回格式必须包含 `data` 数组

## 常见问题

### Q: JS 执行超时
A: 检查是否有死循环或网络请求过慢，优化代码逻辑。

### Q: 网络请求失败
A: 确保 URL 正确，检查网络权限和连接状态。

### Q: 返回数据格式错误
A: 确保返回的是 JSON 字符串，包含 `data` 数组。
