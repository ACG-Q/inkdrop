# JSON 源开发文档

## 概述

JSON 源是最常用的壁纸源类型，通过 HTTP 请求获取 JSON 格式的壁纸列表。

## 配置格式

```json
{
  "sourceId": "unique_source_id",
  "sourceName": "源名称",
  "url": "https://api.example.com/wallpapers?page={page}&pageSize={pageSize}",
  "response": {
    "list": "data",
    "item": {
      "id": "id",
      "url": "url",
      "width": "width",
      "height": "height"
    }
  },
  "imageUrlBase": "https://example.com",
  "previewSuffix": "?w=400",
  "categories": [
    {
      "id": "nature",
      "name": "自然风景",
      "params": {
        "tag": "nature"
      }
    }
  ]
}
```

## 字段说明

### 必填字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `sourceId` | string | 源的唯一标识符 |
| `url` | string | API 请求地址 |

### 可选字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `sourceName` | string | sourceId | 源的显示名称 |
| `imageUrlBase` | string | "" | 图片 URL 基础路径 |
| `previewSuffix` | string | "" | 图片 URL 后缀（如裁剪参数） |
| `response.list` | string | "data" | 壁纸列表字段名 |
| `response.item.id` | string | "id" | 壁纸 ID 字段名 |
| `response.item.url` | string | "url" | 壁纸 URL 字段名 |
| `response.item.width` | string | "width" | 壁纸宽度字段名 |
| `response.item.height` | string | "height" | 壁纸高度字段名 |

### URL 模板变量

| 变量 | 说明 |
|------|------|
| `{page}` | 当前页码 |
| `{pageSize}` | 每页数量 |
| `{timestamp}` | 当前时间戳 |

## 响应格式

### 标准格式

```json
{
  "data": [
    {
      "id": 1,
      "url": "/images/wallpaper1.jpg",
      "width": 1920,
      "height": 1080
    },
    {
      "id": 2,
      "url": "/images/wallpaper2.jpg",
      "width": 2560,
      "height": 1440
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5
}
```

### 分页响应

```json
{
  "data": [...],
  "page": 2,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5
}
```

## 分类配置

```json
{
  "categories": [
    {
      "id": "nature",
      "name": "自然风景",
      "params": {
        "tag": "nature",
        "orientation": "landscape"
      }
    },
    {
      "id": "city",
      "name": "城市夜景",
      "params": {
        "tag": "city",
        "orientation": "portrait"
      }
    }
  ]
}
```

分类参数会自动拼接到 URL 中，例如：
- `https://api.example.com/wallpapers?tag=nature&orientation=landscape`

## 完整示例

```json
{
  "sourceId": "wallhaven",
  "sourceName": "Wallhaven",
  "url": "https://wallhaven.cc/api/v1/search?page={page}&categories=110&purity=100&sorting=toplist",
  "response": {
    "list": "data",
    "item": {
      "id": "id",
      "url": "path",
      "width": "resolution.x",
      "height": "resolution.y"
    }
  },
  "imageUrlBase": "https://w.wallhaven.cc/full",
  "categories": [
    {
      "id": "all",
      "name": "全部",
      "params": {}
    },
    {
      "id": "anime",
      "name": "动漫",
      "params": {
        "categories": "010"
      }
    }
  ]
}
```

## 注意事项

1. 确保 API 返回的 JSON 格式正确
2. URL 中的分页参数需要与 API 文档一致
3. `imageUrlBase` 用于拼接相对路径的图片 URL
4. 分类参数会覆盖 URL 中的同名参数
