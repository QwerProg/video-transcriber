<template>
  <section id="features" class="py-24 sm:py-32 relative overflow-hidden" aria-labelledby="features-heading">
    <!-- Background Accents -->
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-primary/5 rounded-full blur-[120px] pointer-events-none"></div>

    <div class="relative max-w-7xl mx-auto px-4 sm:px-6">
      <div class="text-center mb-20">
        <h2 id="features-heading" class="text-3xl sm:text-5xl font-black text-white mb-6 tracking-tight">
          由 <span class="text-primary text-glow">AI 智能驱动</span>
        </h2>
        <p class="text-text-secondary text-lg max-w-2xl mx-auto leading-relaxed">
          简单、快速、强大。体验由 AI 驱动的新一代视频处理工具集。
        </p>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
        <article
          v-for="(feature, idx) in features"
          :key="feature.title"
          class="group glass-effect rounded-3xl p-8 border border-white/5 hover:border-primary/30 transition-all duration-500 hover:-translate-y-2"
        >
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-6 text-3xl shadow-inner group-hover:scale-110 transition-transform duration-500"
            :class="feature.bgClass" role="img" :aria-label="feature.title">
            {{ feature.icon }}
          </div>
          <h3 class="text-lg font-black text-white mb-4 tracking-tight uppercase group-hover:text-primary transition-colors">{{ feature.title }}</h3>
          <p class="text-sm text-text-secondary leading-relaxed group-hover:text-text-primary transition-colors">{{ feature.desc }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
const features = [
  {
    icon: '🌐',
    title: '支持 1800+ 平台',
    desc: '全球 YouTube、Bilibili、抖音、TikTok、Twitter、Instagram 等全平台支持。',
    bgClass: 'bg-blue-500/10 border border-blue-500/20',
  },
  {
    icon: '⚡',
    title: '即时解析',
    desc: '智能链接分析与高速多线程解析，带给您零等待体验。',
    bgClass: 'bg-amber-500/10 border border-amber-500/20',
  },
  {
    icon: '📱',
    title: '完美适配移动端',
    desc: '针对手机浏览器深度优化。无需安装 App，随时随地保存视频。',
    bgClass: 'bg-green-500/10 border border-green-500/20',
  },
  {
    icon: '🎬',
    title: '4K 超高清',
    desc: '支持从 360p 到 4K 的所有清晰度。满足您对画质的极致追求。',
    bgClass: 'bg-purple-500/10 border border-purple-500/20',
  },
  {
    icon: '🤖',
    title: 'AI 视频总结',
    desc: '一键生成 AI 摘要、思维导图，并支持针对视频内容进行 AI 对话。',
    bgClass: 'bg-rose-500/10 border border-rose-500/20',
  },
]
</script>
