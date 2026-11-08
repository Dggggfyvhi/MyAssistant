# 我的助手 (MyAssistant)

Android 原生 AI 聊天助手 + 伪 Agent。界面已全面重做为**深色液态玻璃 + ChatGPT 风格**。

## 本次改动
- **移除语音**：听写 / 语音模式全部去掉（原先识别不稳定、无真人声）。
- **界面强力优化**：深色主题、液态玻璃（Haze 模糊）、侧边栏对话列表、消息气泡、思考中小卡片、可折叠「思考过程」块（支持 `<think>` / `<reasoning>` 标记）。
- **保留 Agent**：无障碍服务 + 无线调试（ADB），可读屏、点击、滚动、打开应用/网页等。
- **关于无头浏览器**：手机端没有原生稳定的 Chromium 无头环境。网页任务继续用 `open_url` 工具打开系统浏览器。若需要真正的本地/云端无头浏览器 + 强 Agent，推荐在 Linux/桌面使用 **DeepSeek Harness (DSH)** 或 Playwright + 任意 OpenAI 兼容模型。

## 打包 APK
1. 在 GitHub 新建仓库，上传本目录所有文件（包含隐藏的 `.github`）。
2. Actions → “Build APK” 跑完后，在 Artifacts 下载 `MyAssistant-apk`。

## 使用
1. 侧边栏 / 设置：填 API 地址和 Key，点「获取模型列表」自动填入模型，再「测试连接」。
2. Agent 二选一（都开优先用无障碍）：
   - **无障碍**：设置里点「去开启」，在系统里打开「我的助手」。
   - **无线调试**：开发者选项 → 无线调试 → 使用配对码配对设备 → 分屏打开本应用，端口自动出现，输入配对码点「配对」，再点「连接」。
3. 聊天：直接输入。模型若返回带 `<think>...</think>` 或 `<reasoning>...</reasoning>` 的内容，会自动变成可折叠的「思考过程」小卡片。

## 推荐模型
任意 OpenAI 兼容接口（DeepSeek、通义、Kimi、本地 Ollama 等）。支持工具调用的模型效果最好。

## 桌面强 Agent 参考
- DeepSeek Harness (DSH)：https://github.com/deepseek-ai （搜索 dsh / DeepSeek Harness）
- Playwright + LLM：真正无头浏览器，适合「打开百度搜索并实时反馈」这类任务。
