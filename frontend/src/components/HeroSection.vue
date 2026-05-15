<template>
  <section class="relative overflow-hidden transition-all duration-700"
    :class="compact ? 'pt-6 pb-4 sm:pt-10 sm:pb-8' : 'pt-20 pb-16 sm:pt-32 sm:pb-24'"
  >
    <!-- 装饰背景 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full bg-[radial-gradient(circle_at_center,rgba(0,209,255,0.08)_0%,transparent_70%)]"></div>
    </div>

    <div class="relative max-w-5xl mx-auto px-4 sm:px-6 text-center">
      <template v-if="showSlogan">
        <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-effect border border-white/10 text-xs font-bold text-primary tracking-widest uppercase mb-8 animate-fade-in-up shadow-[0_0_20px_rgba(0,209,255,0.1)]"
        >
          <span class="w-2 h-2 rounded-full bg-primary animate-pulse shadow-[0_0_10px_rgba(0,209,255,0.8)]"></span>
          支持 1800+ 视频平台
        </div>

        <h1 :class="compact ? 'text-3xl sm:text-4xl mb-4' : 'text-4xl sm:text-6xl lg:text-7xl mb-6'" class="font-black text-white leading-[1.1] tracking-tight animate-fade-in-up [animation-delay:100ms]">
          万能视频下载，<br />
          <span class="text-transparent bg-clip-text bg-gradient-to-r from-primary to-blue-500 text-glow">一键即可解析。</span>
        </h1>
        <p :class="compact ? 'mb-6 text-sm sm:text-lg' : 'mb-12 text-lg sm:text-xl'" class="text-text-secondary max-w-2xl mx-auto leading-relaxed animate-fade-in-up [animation-delay:200ms]">
          终极 AI 驱动的视频下载器与 AI 总结工具。<br class="hidden sm:block" />
          支持 YouTube、Bilibili、抖音等平台高速解析。
        </p>
      </template>

      <!-- 搜索输入框 -->
      <div class="max-w-3xl mx-auto animate-fade-in-up [animation-delay:300ms]">
        <form @submit.prevent="onSubmit" class="group relative flex items-center p-1.5 rounded-2xl sm:rounded-full glass-effect border border-white/10 focus-within:border-primary/50 focus-within:shadow-[0_0_30px_rgba(0,209,255,0.15)] transition-all duration-500" role="search" aria-label="视频链接解析">
          <div class="relative flex-1">
            <label for="video-url-input" class="sr-only">粘贴视频链接进行解析下载</label>
            <svg class="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-text-muted group-focus-within:text-primary transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
            </svg>
            <input
              id="video-url-input"
              v-model="url"
              type="url"
              :placeholder="placeholder"
              class="w-full h-14 sm:h-16 pl-14 pr-6 rounded-xl sm:rounded-full bg-transparent text-lg text-white placeholder:text-text-muted focus:outline-none transition-all"
              :disabled="loading"
              autocomplete="url"
            />
          </div>
          <button
            type="submit"
            :disabled="loading || !url.trim()"
            class="hidden sm:flex items-center gap-3 h-16 px-10 rounded-full bg-primary hover:bg-primary-dark text-slate-950 font-black text-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_20px_rgba(0,209,255,0.3)] hover:shadow-[0_0_35px_rgba(0,209,255,0.5)] cursor-pointer"
          >
            <svg v-if="loading" class="animate-spin w-6 h-6" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            {{ loading ? '正在解析...' : '解析视频' }}
          </button>
          <!-- 移动端按钮 -->
          <button
            type="submit"
            :disabled="loading || !url.trim()"
            class="sm:hidden w-12 h-12 flex items-center justify-center rounded-xl bg-primary text-slate-950 disabled:opacity-50 cursor-pointer"
          >
            <svg v-if="loading" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </form>

        <div v-if="showSlogan" class="flex flex-wrap items-center justify-center gap-4 mt-8 text-xs font-bold tracking-widest text-text-muted">
          <span class="uppercase">快速开始：</span>
          <button
            v-for="example in examples"
            :key="example.label"
            @click="url = example.url"
            class="px-4 py-1.5 rounded-lg border border-white/5 bg-white/5 hover:border-primary/40 hover:text-primary transition-all cursor-pointer"
          >
            {{ example.label }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  loading: Boolean,
  compact: Boolean,
  showSlogan: { type: Boolean, default: true },
})
const emit = defineEmits(['parse'])

const url = ref('')
const placeholder = 'https://www.youtube.com/watch?v=... 粘贴视频链接'

const examples = [
  { label: 'YouTube', url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' },
  { label: 'Bilibili', url: 'https://www.bilibili.com/video/BV18x5a61EtZ' },
  { label: '抖音', url: 'https://v.douyin.com/i89Y65U/' },
]

function normalizeUrl(raw) {
  let u = raw
  if (u.includes('bilibili.com') && !u.includes('www.bilibili.com')) {
    u = u.replace('bilibili.com', 'www.bilibili.com')
  }
  return u
}

function onSubmit() {
  const trimmed = url.value.trim()
  if (trimmed) {
    emit('parse', normalizeUrl(trimmed))
  }
}
</script>
