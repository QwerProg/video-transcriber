# 视频转写器 (Video Transcriber)

万能视频下载与 AI 智能总结平台，多平台视频下载，基于 DeepSeek-V4 大模型自动生成视频摘要、思维导图和 AI 问答。

## 功能特性

- **万能视频下载** — 基于 yt-dlp，支持 YouTube、Bilibili、抖音、TikTok、Twitter 等 1800+ 平台
- **AI 视频总结** — DeepSeek 大模型驱动，自动生成结构化摘要
- **思维导图** — 交互式可视化知识结构，支持全屏和 4K 导出
- **AI 问答** — 基于视频内容的上下文感知问答
- **字幕下载** — 提取平台字幕，支持 SRT/VTT/TXT 格式
- **语音转录** — Faster-Whisper 本地转录，覆盖无字幕视频
- **多清晰度** — 360p 至 4K，多种格式可选
- **会员系统** — Stripe 支付，免费用户每日 3 次 AI 总结

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Tailwind CSS |
| 后端 | FastAPI (Python 3.12) |
| AI | DeepSeek API + Faster-Whisper |
| 视频 | yt-dlp (1800+ 平台) |
| 支付 | Stripe Checkout |
| 数据库 | SQLite (WAL 模式) |

## 本地运行

```bash
# 克隆仓库
git clone https://github.com/QwerProg/video-transcriber.git
cd video-transcriber

# 后端
cd backend
cp .env.example .env  # 编辑 .env 填入 API Key
uv sync
uv run python main.py

# 前端（新终端）
cd frontend
npm install
npm run dev
```

## 环境变量（backend/.env）

| 变量 | 说明 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `JWT_SECRET` | JWT 签名密钥 |
| `STRIPE_SECRET_KEY` | Stripe 密钥 |
| `STRIPE_WEBHOOK_SECRET` | Stripe Webhook 签名密钥 |
| `STRIPE_PRICE_ID_MONTHLY` | Stripe 月度订阅价格 ID |
| `FRONTEND_URL` | 前端 URL（支付回跳用） |
| `WHISPER_MODEL_SIZE` | Whisper 模型大小，默认 base |
