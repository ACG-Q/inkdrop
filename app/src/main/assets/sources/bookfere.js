// 书伴 Kindle 屏保壁纸源
// 解析 https://bookfere.com/gallery/pictures/kindle-screensaver/

var SOURCE = {
  config: {
    sourceId: "bookfere",
    sourceName: "书伴壁纸",
    type: "js",
    enabled: true,
    priority: 1,
    categories: [
      { id: "all", name: "全部" },
      { id: "people", name: "人物", params: { theme: "人物" } },
      { id: "animal", name: "动物", params: { theme: "动物" } },
      { id: "landscape", name: "风景", params: { theme: "风景" } },
      { id: "building", name: "建筑", params: { theme: "建筑" } },
      { id: "plant", name: "植物", params: { theme: "植物" } },
      { id: "cartoon", name: "卡通", params: { theme: "卡通" } },
      { id: "religion", name: "宗教", params: { theme: "宗教" } },
      { id: "concept", name: "概念", params: { theme: "概念" } },
      { id: "still", name: "静物", params: { theme: "静物" } },
      { id: "beauty", name: "美女", params: { theme: "美女" } }
    ]
  },

  fetchWallpapers: function(page, pageSize, category) {
    var baseUrl = "https://bookfere.com/gallery/pictures/kindle-screensaver/";
    var url = page > 1 ? baseUrl + "page/" + page : baseUrl;

    if (category && category.theme) {
      url = "https://bookfere.com/gallery/theme/" + encodeURIComponent(category.theme);
      if (page > 1) {
        url += "/page/" + page;
      }
    }

    try {
      var html = OkHttpBridge.get(url);
      if (!html || html.length === 0) {
        return { wallpapers: [], total: 0, page: page, pageSize: pageSize, error: "empty_response" };
      }

      var wallpapers = [];
      var marker = '<li class="picture">';
      var searchFrom = 0;

      while (true) {
        var start = html.indexOf(marker, searchFrom);
        if (start === -1) break;
        start += marker.length;
        var end = html.indexOf('</li>', start);
        if (end === -1) break;
        var item = html.substring(start, end);
        searchFrom = end + 5;

        // 提取详情页 URL: <a href="https://bookfere.com/gallery/ss_XXXXX">
        var hrefStart = item.indexOf('href="');
        if (hrefStart === -1) continue;
        hrefStart += 6;
        var hrefEnd = item.indexOf('"', hrefStart);
        if (hrefEnd === -1) continue;
        var detailUrl = item.substring(hrefStart, hrefEnd);

        // 提取 ID
        var ssIdx = detailUrl.lastIndexOf('ss_');
        if (ssIdx === -1) continue;
        var idStr = detailUrl.substring(ssIdx + 3);
        var id = parseInt(idStr);
        if (isNaN(id)) continue;

        // 提取缩略图 URL
        var imgStart = item.indexOf('src="');
        if (imgStart === -1) continue;
        imgStart += 5;
        var imgEnd = item.indexOf('"', imgStart);
        if (imgEnd === -1) continue;
        var thumbUrl = item.substring(imgStart, imgEnd);

        // 使用缩略图 URL
        var fullUrl = thumbUrl;

        // 从 <img> 标签提取缩略图尺寸
        var w = 0, h = 0;
        var wIdx = item.indexOf('width="');
        if (wIdx !== -1) {
          wIdx += 7;
          var wEnd = item.indexOf('"', wIdx);
          if (wEnd !== -1) w = parseInt(item.substring(wIdx, wEnd));
        }
        var hIdx = item.indexOf('height="');
        if (hIdx !== -1) {
          hIdx += 8;
          var hEnd = item.indexOf('"', hIdx);
          if (hEnd !== -1) h = parseInt(item.substring(hIdx, hEnd));
        }

        wallpapers.push({
          id: id,
          url: fullUrl,
          width: w,
          height: h,
          hash: "",
          createdAt: ""
        });
      }

      var hasNext = html.indexOf('class="next page-numbers"') !== -1;

      return {
        wallpapers: wallpapers,
        total: hasNext ? -1 : page * pageSize,
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
