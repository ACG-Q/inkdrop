# 墨纸壁纸 InkDrop

一款专为墨水屏设计的 Android 壁纸应用。

## 特性

- 纯黑白界面，适配墨水屏显示
- 支持自定义壁纸源（JSON / JS 脚本）
- 壁纸预览、下载、分享、设为桌面
- 收藏管理
- 自动保存功能
- 运行日志查看

## 技术栈

- 原生 Java Android
- Hilt 依赖注入
- Retrofit + OkHttp 网络请求
- Glide 图片加载
- Room 本地数据库
- Rhino JS 引擎（动态壁纸源解析）
- WorkManager 后台任务

## 构建

```bash
# Debug 构建
.\gradlew.bat assembleDebug

# Release 构建（需要签名配置）
.\gradlew.bat assembleRelease
```

## 安装到设备

```bash
.\gradlew.bat installDebug
```

## CI/CD

推送到 `master` 分支或创建 `v*` 标签会自动触发 GitHub Actions 构建，APK 上传到 Artifacts。

### 环境变量（GitHub Secrets）

| 变量名 | 说明 |
|--------|------|
| `KEYSTORE_BASE64` | 签名文件 base64 编码 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

## 许可证

MIT License
