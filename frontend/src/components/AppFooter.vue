<template>
  <footer class="bg-slate-950/80 backdrop-blur-md border-t border-white/5 py-12 sm:py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6">
      <!-- 底部关键词友好描述 -->
      <div class="text-center mb-12 animate-fade-in-up">
        <p class="text-[10px] font-black tracking-[0.2em] text-text-muted uppercase mb-4">一站式多媒体解析中心</p>
        <p class="text-sm text-text-secondary leading-relaxed max-w-4xl mx-auto font-medium">
          <strong class="text-white">Video Transcriber</strong> — 终极安全的媒体解析工具。支持 
          <span class="text-primary/80">YouTube, Bilibili, 抖音及全球 1800+ 平台</span>。 
          集成 AI 摘要总结、语义分析及思维导图生成。
        </p>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-between gap-8 pt-12 border-t border-white/5">
        <a href="/" class="flex items-center gap-3 group" title="Video Transcriber 首页">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-blue-600 flex items-center justify-center shadow-lg shadow-primary/20">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <span class="text-sm font-black text-white uppercase tracking-widest group-hover:text-primary transition-colors">Video Transcriber</span>
        </a>

        <p class="text-[10px] font-bold text-text-muted text-center leading-relaxed tracking-wider uppercase max-w-md">
          仅供学术研究使用，请尊重版权。
        </p>

        <div class="text-[10px] font-black text-text-muted tracking-widest uppercase">
          &copy; {{ new Date().getFullYear() }} <span class="text-white">Video Transcriber</span>
        </div>
      </div>
    </div>
  </footer>
</template>
