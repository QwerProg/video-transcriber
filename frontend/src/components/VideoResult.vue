<template>
  <article class="glass-effect rounded-3xl overflow-hidden cyber-border shadow-2xl animate-fade-in-up">
    <!-- 视频封面图区域 -->
    <div class="relative aspect-video bg-slate-900 group">
      <img
        v-if="video.thumbnail"
        :src="thumbnailUrl"
        :alt="video.title"
        class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110 opacity-80 group-hover:opacity-100"
        loading="lazy"
        @error="(e) => e.target.style.display = 'none'"
      />
      <div v-else class="w-full h-full flex items-center justify-center text-text-muted">
        <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      </div>
      <div class="absolute inset-0 bg-gradient-to-t from-slate-950 via-transparent to-transparent"></div>
      <div class="absolute bottom-4 left-4 right-4 flex items-center justify-between">
        <span v-if="video.duration_string" class="px-3 py-1 rounded-lg bg-black/60 backdrop-blur-md border border-white/10 text-[10px] font-black text-white tracking-widest uppercase">
          {{ video.duration_string }}
        </span>
        <span class="px-3 py-1 rounded-lg bg-primary/20 backdrop-blur-md border border-primary/30 text-[10px] font-black text-primary tracking-widest uppercase shadow-[0_0_15px_rgba(0,209,255,0.3)]">
          {{ video.platform?.toUpperCase() || 'VIDEO' }}
        </span>
      </div>
    </div>

    <div class="p-6 sm:p-8">
      <h2 class="text-xl sm:text-2xl font-black text-white leading-tight mb-6 line-clamp-2 hover:text-primary transition-colors cursor-default">
        {{ video.title }}
      </h2>

      <!-- 视频数据 -->
      <div class="grid grid-cols-2 gap-4 mb-8">
        <div class="p-4 rounded-2xl bg-white/5 border border-white/5">
          <p class="text-[10px] font-bold text-text-muted uppercase tracking-widest mb-1">上传者</p>
          <p class="text-sm font-bold text-white truncate">{{ video.uploader || '未知' }}</p>
        </div>
        <div v-if="video.view_count" class="p-4 rounded-2xl bg-white/5 border border-white/5">
          <p class="text-[10px] font-bold text-text-muted uppercase tracking-widest mb-1">播放量</p>
          <p class="text-sm font-bold text-white truncate">{{ formatViewCount(video.view_count) }}</p>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="space-y-4">
        <div class="relative group">
          <div class="absolute -inset-1 bg-gradient-to-r from-primary to-blue-600 rounded-2xl blur opacity-20 group-hover:opacity-40 transition duration-500"></div>
          <button
            @click="showFormats = !showFormats"
            class="relative w-full flex items-center justify-center gap-3 py-4 bg-slate-900 rounded-2xl border border-white/10 text-white font-black tracking-widest uppercase transition-all hover:bg-slate-800 cursor-pointer"
          >
            <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            下载选项
          </button>
        </div>

        <button
          @click="$emit('summarize')"
          :disabled="summarizing"
          class="w-full flex items-center justify-center gap-3 py-4 rounded-2xl bg-white/5 border border-white/10 text-text-secondary font-black tracking-widest uppercase transition-all hover:bg-white/10 hover:text-white disabled:opacity-50 cursor-pointer"
        >
          <svg v-if="summarizing" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          {{ summarizing ? '正在总结...' : 'AI 智能总结' }}
        </button>
      </div>

      <!-- 格式选择面板 -->
      <transition
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 -translate-y-4"
        enter-to-class="opacity-100 translate-y-0"
      >
        <div v-if="showFormats" class="mt-6 pt-6 border-t border-white/5 space-y-3">
          <div
            v-for="f in video.formats"
            :key="f.format_id"
            class="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/5 hover:border-primary/30 transition-all group"
          >
            <div class="min-w-0 flex-1 mr-4">
              <p class="text-sm font-bold text-white flex items-center gap-2 truncate">
                {{ f.label }}
                <span v-if="f.has_audio" class="text-[10px] px-1.5 py-0.5 rounded bg-primary/10 text-primary border border-primary/20">HD</span>
              </p>
              <p class="text-[10px] text-text-muted font-bold tracking-wider mt-1 uppercase">{{ f.ext }}</p>
            </div>
            <button
              @click="$emit('download', f.format_id)"
              :disabled="downloading"
              class="p-2.5 rounded-lg bg-primary/10 text-primary hover:bg-primary hover:text-slate-950 transition-all cursor-pointer shadow-[0_0_10px_rgba(0,209,255,0.1)]"
            >
              <svg v-if="downloading" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0l-4 4m4-4v12" />
              </svg>
            </button>
          </div>
        </div>
      </transition>
    </div>
  </article>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  video: { type: Object, required: true },
  downloading: Boolean,
  summarizing: Boolean,
})
defineEmits(['download', 'summarize'])

const showFormats = ref(false)

const thumbnailUrl = computed(() => {
  if (!props.video.thumbnail) return ''
  return '/api/proxy/thumbnail?url=' + encodeURIComponent(props.video.thumbnail)
})

function formatViewCount(count) {
  if (!count) return ''
  if (count >= 100000000) return (count / 100000000).toFixed(1) + '亿'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toLocaleString()
}
</script>
