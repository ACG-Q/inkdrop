// Wallhaven 壁纸源示例
// 使用 JavaScript 实现复杂的 API 解析逻辑

var SOURCE = {
  config: {
    sourceId: "wallhaven",
    sourceName: "Wallhaven壁纸",
    type: "js",
    enabled: true,
    priority: 2,
    categories: [
      { id: "toplist", name: "排行榜", params: { sorting: "toplist" } },
      { id: "latest", name: "最新", params: { sorting: "date_added" } },
      { id: "general", name: "通用", params: { categories: "100" } }
    ]
  },

  fetchWallpapers: function(page, pageSize, category) {
    // 构建请求 URL
    var url = "https://wallhaven.cc/api/v1/search?page=" + page + "&purity=100";
    
    // 应用分类参数
    if (category) {
      for (var key in category) {
        if (category.hasOwnProperty(key)) {
          url += "&" + key + "=" + category[key];
        }
      }
    }
    
    try {
      // 使用 OkHttpBridge 执行网络请求
      var response = OkHttpBridge.get(url);
      
      if (!response || response.length === 0) {
        return {
          wallpapers: [],
          total: 0,
          page: page,
          pageSize: pageSize,
          error: "empty_response"
        };
      }
      
      var json = JSON.parse(response);
      
      // 解析壁纸数据
      var wallpapers = [];
      if (json.data) {
        for (var i = 0; i < json.data.length; i++) {
          var item = json.data[i];
          var resolution = item.resolution ? item.resolution.split("x") : [0, 0];
          
          wallpapers.push({
            id: parseInt(item.id) || 0,
            url: item.path || "",
            width: parseInt(resolution[0]) || 0,
            height: parseInt(resolution[1]) || 0,
            hash: item.hash || "",
            createdAt: item.created_at || ""
          });
        }
      }
      
      return {
        wallpapers: wallpapers,
        total: json.meta ? json.meta.total : 0,
        page: page,
        pageSize: pageSize
      };
    } catch (e) {
      return {
        wallpapers: [],
        total: 0,
        page: page,
        pageSize: pageSize,
        error: e.toString()
      };
    }
  }
};
