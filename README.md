# Hide My Applist（Android 16 专用分支）

这是一个仅面向 Android 16 的个人维护分支，基于
[oneshu/Hide-My-Applist](https://github.com/oneshu/Hide-My-Applist)。

## 目标平台

- Android 16 / API 36
- `minSdk = 36`
- `targetSdk = 36`
- `compileSdk = 36`
- 不提供 Android 15 及更低版本的兼容代码

## 本分支改动

- 模板改为手动保存，未保存返回时会提示。
- 新建、重命名、删除模板统一为一次配置写入。
- 配置文件和运行时配置使用原子写入。
- 适配 Android 16 的 PackageManager 应用可见性拦截。
- 应用界面资源仅保留英语和简体中文。
- 移除旧版本 API 分支、旧 PackageManager hook 和 Crowdin 配置。
- GitHub Actions 只用于构建 debug APK，第三方 Action 固定到 commit SHA。

## 构建

在 GitHub Actions 中手动运行 `Pull Request` 工作流，选择
`feature/template-manual-save` 分支。构建完成后下载 `HMA-*-debug.apk`。

本项目不需要原上游的签名、Google Services 或 Telegram Secrets。

## 权限说明

- `QUERY_ALL_PACKAGES`：用于枚举应用并建立隐藏规则。
- `INTERNET`：用于上游更新检查和广告组件。
- `ServiceProvider` 只接受 `android` 包且 UID 为 system 的调用。
