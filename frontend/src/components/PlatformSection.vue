<template>
  <section id="platforms" class="py-24 sm:py-32 relative overflow-hidden" aria-labelledby="platforms-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 relative z-10">
      <div class="text-center mb-16">
        <h2 id="platforms-heading" class="text-3xl sm:text-5xl font-black text-white mb-6 tracking-tight uppercase">
          全球 <span class="text-primary text-glow">平台</span> 覆盖
        </h2>
        <p class="text-text-secondary text-lg max-w-2xl mx-auto leading-relaxed">
          支持超过 1800 个全球主流视频与社交媒体平台。
        </p>
      </div>

      <div class="flex flex-wrap justify-center gap-4 sm:gap-6 max-w-5xl mx-auto">
        <div
          v-for="platform in platforms"
          :key="platform.name"
          class="inline-flex items-center gap-3 px-6 py-3 glass-effect rounded-2xl border border-white/5 hover:border-primary/40 hover:bg-white/5 transition-all duration-300 group cursor-default"
        >
          <span class="text-xl group-hover:scale-125 transition-transform">{{ platform.icon }}</span>
          <span class="text-xs font-black text-white uppercase tracking-widest">{{ platform.name }}</span>
        </div>
      </div>

      <p class="text-center mt-12 text-[10px] font-black text-text-muted uppercase tracking-[0.2em] animate-pulse">
        以及数千个其他平台，包括 Vimeo、SoundCloud、Reddit 等
      </p>
    </div>
  </section>
</template>

<script setup>
const platforms = [
  { icon: '▶️', name: 'YouTube' },
  { icon: '📺', name: 'Bilibili' },
  { icon: '🎵', name: 'TikTok' },
  { icon: '🐦', name: 'Twitter / X' },
  { icon: '📷', name: 'Instagram' },
  { icon: '📘', name: 'Facebook' },
  { icon: '🎬', name: 'Vimeo' },
  { icon: '🔴', name: 'Reddit' },
  { icon: '🎮', name: 'Twitch' },
  { icon: '🎧', name: 'SoundCloud' },
]
</script>
