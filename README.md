# 我的助手

纯聊天的原生 Android 应用（Kotlin + Jetpack Compose + Material 3，跟随系统动态取色）。

## 打包
1. 新建 GitHub 仓库，上传本目录全部文件（含隐藏的 `.github`）。
2. Actions → “Build APK” 跑完，Artifacts 里下载 `MyAssistant-apk`（release 版，已用调试签名，可直接安装）。

## 使用
- 没配置 API：自动使用匿名公共接口聊天（按顺序尝试，失败自动换下一个）。
- 左上角菜单 → 添加 API：填地址、Key，点“获取模型列表”自动填模型，可“测试连接”，保存后即用。
- 模型返回思考过程时，会显示可展开的“思考过程”卡片。
