<template>
  <div class="glass-effect rounded-3xl border border-white/5 shadow-2xl h-full flex flex-col overflow-hidden animate-fade-in-up [animation-delay:400ms]">
        <!-- 标签页导航 -->
        <div class="flex border-b border-white/5 bg-white/5">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            @click="activeTab = tab.key"
            :class="[
              'flex items-center gap-2 px-6 py-4 text-xs font-black tracking-widest uppercase transition-all relative cursor-pointer overflow-hidden group',
              activeTab === tab.key
                ? 'text-primary'
                : 'text-text-muted hover:text-white'
            ]"
          >
            <span class="relative z-10">{{ tab.icon }}</span>
            <span class="relative z-10">{{ tab.label }}</span>
            <div
              v-if="activeTab === tab.key"
              class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary shadow-[0_0_10px_rgba(0,209,255,0.8)]"
            ></div>
            <div class="absolute inset-0 bg-primary/5 translate-y-full group-hover:translate-y-0 transition-transform duration-300"></div>
          </button>
        </div>

        <!-- 内容区域 -->
        <div class="p-6 sm:p-8 min-h-[450px] flex-1 overflow-y-auto custom-scrollbar">
          <!-- 加载状态 -->
          <div v-if="loading && !summaryText && activeTab === 'summary'" class="flex flex-col items-center justify-center py-24">
            <div class="relative w-16 h-16 mb-6">
              <div class="absolute inset-0 border-4 border-primary/20 rounded-full"></div>
              <div class="absolute inset-0 border-4 border-t-primary rounded-full animate-spin"></div>
              <div class="absolute inset-0 border-4 border-primary/40 rounded-full animate-ping scale-75 opacity-20"></div>
            </div>
            <p class="text-primary font-bold tracking-widest uppercase animate-pulse">{{ loadingMessage }}</p>
          </div>

          <!-- 总结摘要 Tab -->
          <div v-show="activeTab === 'summary'">
            <div
              v-if="summaryText"
              class="prose prose-invert prose-sm max-w-none summary-prose animate-fade-in"
              v-html="renderedSummary"
            ></div>
            <div v-if="loading && summaryText" class="mt-6 flex items-center gap-3 text-xs font-bold text-primary tracking-widest uppercase">
              <span class="flex gap-1">
                <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.3s]"></span>
                <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.15s]"></span>
                <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce"></span>
              </span>
              AI 正在总结中...
            </div>
            <!-- 免费用户剩余次数提示 -->
            <div v-if="quotaInfo && quotaInfo.remaining >= 0 && !loading" class="mt-8 p-4 rounded-2xl bg-primary/5 border border-primary/20 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-2 h-2 rounded-full bg-primary animate-pulse"></div>
                <span class="text-xs font-bold text-text-secondary uppercase tracking-widest">
                  今日剩余额度：<strong class="text-primary">{{ quotaInfo.remaining }}</strong> / {{ quotaInfo.limit }} 次
                </span>
              </div>
              <button v-if="quotaInfo.remaining <= 1" @click="emit('need-vip')" class="text-[10px] font-black text-primary hover:text-white transition-colors cursor-pointer uppercase tracking-tighter bg-primary/10 px-3 py-1.5 rounded-lg border border-primary/20">
                升级至 PRO
              </button>
            </div>
          </div>

          <!-- 字幕文本 Tab -->
          <div v-show="activeTab === 'subtitle'">
            <div v-if="subtitleData.segments && subtitleData.segments.length > 0">
              <div class="flex items-center justify-between mb-6">
                <div class="text-[10px] font-black text-text-muted tracking-widest uppercase flex items-center gap-3">
                  <span>{{ subtitleData.segments.length }} 条字幕片段</span>
                  <span v-if="subtitleData.language" class="px-2 py-0.5 bg-primary/10 text-primary border border-primary/20 rounded-md">
                    {{ subtitleData.subtitle_type === 'manual' ? '官方字幕' : '自动生成' }} · {{ subtitleData.language?.toUpperCase() }}
                  </span>
                </div>
                <div class="flex items-center gap-4">
                  <!-- 下载字幕按钮 -->
                  <div class="relative" ref="subtitleDropdownRef">
                    <button
                      @click="showSubtitleDropdown = !showSubtitleDropdown"
                      class="flex items-center gap-2 text-[10px] font-black text-primary hover:text-white transition-colors cursor-pointer px-3 py-2 rounded-lg bg-primary/10 border border-primary/20 hover:bg-primary/20"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                      </svg>
                      下载字幕
                      <svg class="w-3 h-3 transition-transform duration-300" :class="showSubtitleDropdown ? 'rotate-180' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                    <div
                      v-if="showSubtitleDropdown"
                      class="absolute right-0 top-full mt-2 glass-effect rounded-xl border border-white/10 shadow-2xl py-2 z-10 min-w-[140px] animate-menu-in"
                    >
                      <button
                        v-for="fmt in subtitleFormats"
                        :key="fmt.key"
                        @click="downloadSubtitle(fmt.key)"
                        class="w-full text-left px-4 py-2.5 text-[10px] font-bold text-white hover:bg-primary/20 transition-colors cursor-pointer flex items-center justify-between uppercase tracking-widest"
                      >
                        <span>{{ fmt.label }}</span>
                        <span class="text-text-muted">.{{ fmt.ext }}</span>
                      </button>
                    </div>
                  </div>
                  <button
                    @click="subtitleExpanded = !subtitleExpanded"
                    class="text-[10px] font-black text-text-muted hover:text-white transition-colors cursor-pointer uppercase tracking-widest"
                  >
                    {{ subtitleExpanded ? '收起全部' : '展开全部' }}
                  </button>
                </div>
              </div>
              <div
                :class="['space-y-2 overflow-y-auto pr-2 custom-scrollbar', subtitleExpanded ? 'max-h-none' : 'max-h-[550px]']"
              >
                <div
                  v-for="(seg, idx) in subtitleData.segments"
                  :key="idx"
                  class="flex gap-4 py-3 px-4 rounded-xl border border-white/5 hover:border-primary/30 bg-white/[0.02] hover:bg-white/[0.05] transition-all group"
                >
                  <span class="flex-shrink-0 text-[10px] font-black text-primary font-mono mt-0.5 min-w-[60px] opacity-60 group-hover:opacity-100">
                    [{{ formatTime(seg.start) }}]
                  </span>
                  <span class="text-sm text-text-secondary group-hover:text-white leading-relaxed transition-colors">{{ seg.text }}</span>
                </div>
              </div>
            </div>
            <div v-else-if="!loading" class="flex flex-col items-center justify-center py-24 text-text-muted">
              <div class="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-6">
                <svg class="w-8 h-8 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="text-xs font-bold tracking-widest uppercase">暂无可用字幕</p>
            </div>
            <div v-else class="flex flex-col items-center justify-center py-24">
              <div class="w-12 h-12 border-2 border-primary/20 border-t-primary rounded-full animate-spin mb-6"></div>
              <p class="text-primary font-bold tracking-widest uppercase animate-pulse">正在提取片段...</p>
            </div>
          </div>

          <!-- 思维导图 Tab -->
          <div v-show="activeTab === 'mindmap'">
            <div v-if="mindmapMarkdown" class="relative">
              <!-- 工具栏 -->
              <div class="flex items-center justify-end gap-3 mb-4">
                <button
                  @click="downloadMindmapPng"
                  class="flex items-center gap-2 text-[10px] font-black px-4 py-2 rounded-lg bg-white/5 border border-white/10 text-text-secondary hover:text-white hover:border-white/20 transition-all cursor-pointer uppercase tracking-widest"
                >
                  导出 PNG
                </button>
                <button
                  @click="downloadMindmapSvg"
                  class="flex items-center gap-2 text-[10px] font-black px-4 py-2 rounded-lg bg-white/5 border border-white/10 text-text-secondary hover:text-white hover:border-white/20 transition-all cursor-pointer uppercase tracking-widest"
                >
                  导出 SVG
                </button>
                <button
                  @click="toggleFullscreen"
                  class="flex items-center gap-2 text-[10px] font-black px-4 py-2 rounded-lg bg-primary/10 border border-primary/20 text-primary hover:bg-primary hover:text-slate-950 transition-all cursor-pointer uppercase tracking-widest"
                >
                  {{ isFullscreen ? '退出全屏' : '全屏查看' }}
                </button>
              </div>
              <div
                ref="mindmapContainer"
                class="mindmap-wrapper w-full border border-white/10 rounded-2xl glass-effect overflow-hidden relative shadow-inner"
                :class="isFullscreen ? 'mindmap-fullscreen' : 'min-h-[500px]'"
              >
                <div v-if="!isFullscreen" class="absolute inset-0 pointer-events-none bg-[radial-gradient(circle_at_center,rgba(0,209,255,0.03)_0%,transparent_70%)]"></div>
                <svg ref="mindmapSvg" class="w-full h-full" :style="isFullscreen ? 'height: 100%' : 'min-height: 500px'"></svg>
                
                <!-- 全屏模式下的退出按钮 -->
                <button
                  v-if="isFullscreen"
                  @click="toggleFullscreen"
                  class="fixed top-6 right-6 z-50 flex items-center gap-2 px-6 py-3 rounded-xl glass-effect shadow-2xl text-[10px] font-black text-white hover:bg-white/10 transition-colors cursor-pointer border border-white/10 uppercase tracking-widest"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  退出全屏
                </button>
              </div>
            </div>
            <div v-else-if="loading" class="flex flex-col items-center justify-center py-24">
              <div class="w-12 h-12 border-2 border-primary/20 border-t-primary rounded-full animate-spin mb-6"></div>
              <p class="text-primary font-bold tracking-widest uppercase animate-pulse">正在生成导图...</p>
            </div>
            <div v-else class="flex flex-col items-center justify-center py-24 text-text-muted">
              <div class="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-6">
                <svg class="w-8 h-8 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6z" />
                </svg>
              </div>
              <p class="text-xs font-bold tracking-widest uppercase">请先生成 AI 摘要</p>
            </div>
          </div>

          <!-- AI 问答 Tab -->
          <div v-show="activeTab === 'qa'">
            <div class="space-y-6 h-full flex flex-col">
              <!-- 对话列表 -->
              <div
                ref="chatContainer"
                class="space-y-6 max-h-[450px] overflow-y-auto pr-2 custom-scrollbar flex-1"
              >
                <div v-if="chatMessages.length === 0" class="flex flex-col items-center justify-center py-20 text-text-muted">
                  <div class="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-6">
                    <svg class="w-8 h-8 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                  </div>
                  <p class="text-[10px] font-black tracking-widest uppercase mb-2">针对此视频进行提问</p>
                  <p class="text-[10px] opacity-40 italic">"这个视频的主要内容是什么？"</p>
                </div>
                <div
                  v-for="(msg, idx) in chatMessages"
                  :key="idx"
                  :class="[
                    'flex',
                    msg.role === 'user' ? 'justify-end' : 'justify-start'
                  ]"
                >
                  <div
                    :class="[
                      'max-w-[85%] px-5 py-3.5 rounded-2xl text-sm leading-relaxed relative',
                      msg.role === 'user'
                        ? 'bg-primary text-slate-950 font-bold rounded-br-none shadow-[0_0_20px_rgba(0,209,255,0.2)]'
                        : 'bg-white/5 text-text-secondary border border-white/5 rounded-bl-none'
                    ]"
                  >
                    <div v-if="msg.role === 'assistant'" class="chat-prose prose prose-invert prose-sm max-w-none" v-html="renderMarkdown(msg.content)"></div>
                    <span v-else>{{ msg.content }}</span>
                    <span
                      v-if="msg.role === 'assistant' && msg.loading"
                      class="inline-block w-1.5 h-4 bg-primary/60 rounded-sm animate-pulse ml-2 align-text-bottom"
                    ></span>
                  </div>
                </div>
              </div>

              <!-- 输入区域 -->
              <div class="flex gap-3 pt-6 border-t border-white/5">
                <div class="relative flex-1">
                  <input
                    v-model="chatInput"
                    @keydown.enter.prevent="sendQuestion"
                    type="text"
                    placeholder="输入您的问题..."
                    class="w-full h-14 px-6 rounded-2xl border border-white/10 bg-white/5 text-sm text-white placeholder:text-text-muted focus:outline-none focus:border-primary/50 focus:bg-white/[0.08] transition-all"
                    :disabled="chatLoading"
                  />
                </div>
                <button
                  @click="sendQuestion"
                  :disabled="!chatInput.trim() || chatLoading"
                  class="h-14 px-8 rounded-2xl bg-primary hover:bg-primary-dark text-slate-950 font-black text-xs transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center gap-2 uppercase tracking-widest shadow-[0_0_20px_rgba(0,209,255,0.2)]"
                >
                  <svg v-if="chatLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                  </svg>
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { marked } from 'marked'
import { Transformer } from 'markmap-lib'
import { Markmap } from 'markmap-view'
import { summarizeVideo, chatWithVideo } from '../api/summarize.js'

const props = defineProps({
  videoUrl: { type: String, required: true },
  videoTitle: { type: String, default: '' },
  user: { type: Object, default: null },
})
const emit = defineEmits(['loading-change', 'need-login', 'need-vip'])

const tabs = [
  { key: 'summary', label: '摘要', icon: '📝' },
  { key: 'subtitle', label: '字幕', icon: '📄' },
  { key: 'mindmap', label: '思维导图', icon: '🧠' },
  { key: 'qa', label: 'AI 问答', icon: '💬' },
]

const activeTab = ref('summary')
const loading = ref(false)
const loadingMessage = ref('正在提取字幕...')

const summaryText = ref('')
const subtitleData = ref({ segments: [], has_subtitle: false })
const subtitleExpanded = ref(false)
const mindmapMarkdown = ref('')
const mindmapSvg = ref(null)
const mindmapContainer = ref(null)
let markmapInstance = null

const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatContainer = ref(null)

const renderedSummary = ref('')

// 思维导图全屏状态
const isFullscreen = ref(false)

// 字幕下载下拉菜单
const showSubtitleDropdown = ref(false)
const subtitleDropdownRef = ref(null)
const subtitleFormats = [
  { key: 'srt', label: 'SRT 格式', ext: 'srt' },
  { key: 'vtt', label: 'VTT 格式', ext: 'vtt' },
  { key: 'txt', label: '纯文本格式', ext: 'txt' },
]

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

watch(loading, (val) => {
  emit('loading-change', val)
})

watch(summaryText, (val) => {
  renderedSummary.value = renderMarkdown(val)
})

watch(mindmapMarkdown, async (val) => {
  if (val) {
    await nextTick()
    renderMindmap(val)
  }
})

function renderMarkdown(text) {
  if (!text) return ''
  return marked.parse(text)
}

function renderMindmap(md) {
  if (!mindmapSvg.value) return
  try {
    mindmapSvg.value.innerHTML = ''
    const transformer = new Transformer()
    const { root } = transformer.transform(md)
    markmapInstance = Markmap.create(mindmapSvg.value, { autoFit: true }, root)
  } catch (e) {
    console.warn('思维导图渲染失败:', e)
  }
}

function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

// ===== 思维导图全屏 =====
function toggleFullscreen() {
  if (!mindmapContainer.value) return

  if (!isFullscreen.value) {
    if (mindmapContainer.value.requestFullscreen) {
      mindmapContainer.value.requestFullscreen()
    } else if (mindmapContainer.value.webkitRequestFullscreen) {
      mindmapContainer.value.webkitRequestFullscreen()
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen()
    }
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  nextTick(() => {
    if (markmapInstance) {
      markmapInstance.fit()
    }
  })
}

// ===== 构建可导出的纯 SVG（将 foreignObject 替换为 text） =====
function buildExportableSvg() {
  if (!mindmapSvg.value) return null

  const cloned = mindmapSvg.value.cloneNode(true)

  cloned.querySelectorAll('[transform]').forEach(el => {
    const t = el.getAttribute('transform')
    if (t && t.includes('NaN')) {
      el.setAttribute('transform', 'translate(0,0) scale(1)')
    }
  })

  cloned.querySelectorAll('foreignObject').forEach(fo => {
    const textContent = fo.textContent?.trim() || ''
    if (!textContent) { fo.remove(); return }

    const x = parseFloat(fo.getAttribute('x')) || 0
    const y = parseFloat(fo.getAttribute('y')) || 0
    const h = parseFloat(fo.getAttribute('height')) || 20

    const textEl = document.createElementNS('http://www.w3.org/2000/svg', 'text')
    textEl.setAttribute('x', String(x + 4))
    textEl.setAttribute('y', String(y + h / 2 + 5))
    textEl.setAttribute('font-size', '14')
    textEl.setAttribute('font-family', 'sans-serif')
    textEl.setAttribute('fill', '#333')
    textEl.setAttribute('dominant-baseline', 'middle')
    textEl.textContent = textContent

    fo.parentNode.replaceChild(textEl, fo)
  })

  return cloned
}

function serializeSvg(svgEl) {
  const serializer = new XMLSerializer()
  let svgString = serializer.serializeToString(svgEl)

  if (!svgString.includes('xmlns=')) {
    svgString = svgString.replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"')
  }

  const styles = document.querySelectorAll('style')
  let markmapCss = ''
  styles.forEach(s => {
    if (s.textContent.includes('.markmap')) {
      markmapCss += s.textContent
    }
  })
  if (markmapCss) {
    svgString = svgString.replace('>', `><defs><style>${markmapCss}</style></defs>`)
  }

  return svgString
}

// ===== 获取思维导图完整内容边界（不受用户缩放/平移影响） =====
function getContentBBox() {
  const svgEl = mindmapSvg.value
  const gRoot = svgEl.querySelector('g')
  if (gRoot) {
    try {
      const bbox = gRoot.getBBox()
      if (bbox.width > 0 && bbox.height > 0) {
        const transform = gRoot.getAttribute('transform') || ''
        const translateMatch = transform.match(/translate\(\s*([-\d.e]+)\s*[,\s]\s*([-\d.e]+)\s*\)/)
        const scaleMatch = transform.match(/scale\(\s*([-\d.e]+)/)
        const tx = translateMatch ? parseFloat(translateMatch[1]) : 0
        const ty = translateMatch ? parseFloat(translateMatch[2]) : 0
        const sc = scaleMatch ? parseFloat(scaleMatch[1]) : 1
        return {
          x: bbox.x * sc + tx,
          y: bbox.y * sc + ty,
          width: bbox.width * sc,
          height: bbox.height * sc,
        }
      }
    } catch {}
  }
  try {
    const bbox = svgEl.getBBox()
    if (bbox.width > 0 && bbox.height > 0) return bbox
  } catch {}
  return { x: 0, y: 0, width: 800, height: 600 }
}

// ===== 为导出 SVG 设置完整内容的 viewBox =====
function setFullViewBox(svgClone) {
  const dims = getContentBBox()
  const padding = 60
  const vx = dims.x - padding
  const vy = dims.y - padding
  const vw = dims.width + padding * 2
  const vh = dims.height + padding * 2
  svgClone.setAttribute('viewBox', `${vx} ${vy} ${vw} ${vh}`)
  svgClone.setAttribute('width', String(vw))
  svgClone.setAttribute('height', String(vh))
  return { vw, vh }
}

// ===== 思维导图下载 PNG（4K 超清，完整内容） =====
async function downloadMindmapPng() {
  if (!mindmapSvg.value) return

  const exportSvg = buildExportableSvg()
  if (!exportSvg) return

  const { vw, vh } = setFullViewBox(exportSvg)
  const scale = Math.max(4, Math.ceil(3840 / vw))

  let svgString = serializeSvg(exportSvg)

  const canvas = document.createElement('canvas')
  canvas.width = vw * scale
  canvas.height = vh * scale
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  const img = new Image()
  const blob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  return new Promise((resolve) => {
    img.onload = () => {
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      URL.revokeObjectURL(url)
      canvas.toBlob((pngBlob) => {
        if (pngBlob) {
          triggerDownload(pngBlob, getSafeFilename() + ' - 思维导图.png')
        }
        resolve()
      }, 'image/png')
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      alert('PNG 导出失败，请使用 SVG 下载')
      resolve()
    }
    img.src = url
  })
}

// ===== 思维导图下载 SVG（完整内容，不受视口影响） =====
function downloadMindmapSvg() {
  if (!mindmapSvg.value) return

  const cloned = mindmapSvg.value.cloneNode(true)
  cloned.querySelectorAll('[transform]').forEach(el => {
    const t = el.getAttribute('transform')
    if (t && t.includes('NaN')) {
      el.setAttribute('transform', 'translate(0,0) scale(1)')
    }
  })

  setFullViewBox(cloned)

  const svgString = serializeSvg(cloned)
  const blob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' })
  triggerDownload(blob, getSafeFilename() + ' - 思维导图.svg')
}

// ===== 字幕文件下载 =====
function downloadSubtitle(format) {
  showSubtitleDropdown.value = false
  const segments = subtitleData.value.segments
  if (!segments || segments.length === 0) return

  let content = ''
  let ext = format
  const filename = getSafeFilename()

  if (format === 'srt') {
    content = segmentsToSrt(segments)
  } else if (format === 'vtt') {
    content = segmentsToVtt(segments)
  } else {
    content = segmentsToTxt(segments)
  }

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  triggerDownload(blob, `${filename} - 字幕.${ext}`)
}

function segmentsToSrt(segments) {
  return segments.map((seg, i) => {
    const start = formatSrtTime(seg.start)
    const end = formatSrtTime(seg.end)
    return `${i + 1}\n${start} --> ${end}\n${seg.text}\n`
  }).join('\n')
}

function segmentsToVtt(segments) {
  const header = 'WEBVTT\n\n'
  const body = segments.map((seg) => {
    const start = formatVttTime(seg.start)
    const end = formatVttTime(seg.end)
    return `${start} --> ${end}\n${seg.text}\n`
  }).join('\n')
  return header + body
}

function segmentsToTxt(segments) {
  return segments.map((seg) => seg.text).join('\n')
}

function formatSrtTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  const ms = Math.round((seconds % 1) * 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')},${String(ms).padStart(3, '0')}`
}

function formatVttTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  const ms = Math.round((seconds % 1) * 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
}

// ===== 通用工具函数 =====
function getSafeFilename() {
  return (props.videoTitle || 'video').replace(/[\\/*?:"<>|]/g, '_').substring(0, 80)
}

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function handleClickOutside(e) {
  if (subtitleDropdownRef.value && !subtitleDropdownRef.value.contains(e.target)) {
    showSubtitleDropdown.value = false
  }
}

const quotaInfo = ref(null)

async function startSummarize() {
  loading.value = true
  summaryText.value = ''
  mindmapMarkdown.value = ''
  quotaInfo.value = null
  loadingMessage.value = '正在提取视频字幕...'

  try {
    await summarizeVideo(props.videoUrl, 'zh', {
      subtitle: (data) => {
        try {
          subtitleData.value = JSON.parse(data)
          if (subtitleData.value.has_subtitle) {
            loadingMessage.value = 'AI 正在分析视频内容...'
          }
        } catch (e) { /* ignore parse error */ }
      },
      summary: (data) => {
        try { summaryText.value += JSON.parse(data) } catch { summaryText.value += data }
      },
      mindmap: (data) => {
        try {
          const parsed = JSON.parse(data)
          mindmapMarkdown.value = parsed.markdown || ''
        } catch (e) { /* ignore parse error */ }
      },
      quota: (data) => {
        try { quotaInfo.value = JSON.parse(data) } catch {}
      },
      done: () => {
        loading.value = false
      },
      error: (data) => {
        loading.value = false
        try {
          const parsed = JSON.parse(data)
          if (parsed.need_login) {
            emit('need-login')
            return
          }
          if (parsed.need_vip) {
            emit('need-vip')
            return
          }
          alert(parsed.message || '总结失败')
        } catch (e) {
          alert('总结失败: ' + data)
        }
      },
    })
  } catch (err) {
    loading.value = false
    alert('请求失败: ' + err.message)
  }
}

async function sendQuestion() {
  const question = chatInput.value.trim()
  if (!question || chatLoading.value) return

  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: question })

  const aiMessage = { role: 'assistant', content: '', loading: true }
  chatMessages.value.push(aiMessage)
  chatLoading.value = true

  await nextTick()
  scrollChatToBottom()

  try {
    await chatWithVideo(
      props.videoUrl,
      question,
      subtitleData.value.full_text || '',
      {
        answer: (data) => {
          try { aiMessage.content += JSON.parse(data) } catch { aiMessage.content += data }
          scrollChatToBottom()
        },
        done: () => {
          aiMessage.loading = false
          chatLoading.value = false
        },
        error: (data) => {
          aiMessage.loading = false
          chatLoading.value = false
          try {
            const parsed = JSON.parse(data)
            aiMessage.content = '❌ ' + (parsed.message || '回答失败')
          } catch (e) {
            aiMessage.content = '❌ 回答失败'
          }
        },
      }
    )
  } catch (err) {
    aiMessage.loading = false
    chatLoading.value = false
    aiMessage.content = '❌ 请求失败: ' + err.message
  }
}

function scrollChatToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  startSummarize()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('webkitfullscreenchange', onFullscreenChange)
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', onFullscreenChange)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 总结摘要 Markdown 排版定制 */
.summary-prose :deep(h1) {
  font-size: 1.5rem;
  font-weight: 900;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #fff;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--color-primary-light);
  letter-spacing: -0.025em;
}
.summary-prose :deep(h2) {
  font-size: 1.25rem;
  font-weight: 800;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #fff;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.summary-prose :deep(p) {
  margin-bottom: 1rem;
  line-height: 1.8;
  color: var(--color-text-secondary);
}
.summary-prose :deep(ul), .summary-prose :deep(ol) {
  margin-bottom: 1rem;
  padding-left: 1.5rem;
}
.summary-prose :deep(li) {
  margin-bottom: 0.5rem;
  line-height: 1.8;
  color: var(--color-text-secondary);
}
.summary-prose :deep(li::marker) {
  color: var(--color-primary);
}
.summary-prose :deep(strong) {
  color: #fff;
  font-weight: 700;
}
.summary-prose :deep(blockquote) {
  border-left: 4px solid var(--color-primary);
  padding: 1rem 1.25rem;
  color: var(--color-text-secondary);
  font-style: italic;
  margin: 1.5rem 0;
  background: rgba(255,255,255,0.03);
  border-radius: 0 12px 12px 0;
}
.summary-prose :deep(code) {
  background: rgba(255,255,255,0.05);
  padding: 0.2rem 0.4rem;
  border-radius: 6px;
  font-size: 0.9em;
  color: var(--color-primary);
  font-weight: 600;
}

/* 思维导图：确保 markmap 的 foreignObject 文字正常显示 */
.mindmap-wrapper :deep(.markmap-foreign) {
  display: inline-block !important;
}
.mindmap-wrapper :deep(foreignObject) {
  overflow: visible !important;
}
.mindmap-wrapper :deep(foreignObject div) {
  font: 700 14px/20px sans-serif;
  color: #fff !important;
  text-shadow: 0 0 10px rgba(0,209,255,0.3);
}
.mindmap-wrapper :deep(path.markmap-link) {
  stroke: rgba(0,209,255,0.4) !important;
  stroke-width: 2px !important;
}
.mindmap-wrapper :deep(circle.markmap-node) {
  fill: var(--color-primary) !important;
  stroke: #020617 !important;
  stroke-width: 2px !important;
}

/* 思维导图全屏样式 */
.mindmap-fullscreen {
  position: fixed !important;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  border-radius: 0 !important;
  border: none !important;
  background: #020617;
}

/* AI 问答 Markdown 排版（更紧凑） */
.chat-prose :deep(p) {
  margin-bottom: 0.5rem;
  line-height: 1.6;
}
.chat-prose :deep(ul), .chat-prose :deep(ol) {
  padding-left: 1.25rem;
}
.chat-prose :deep(li) {
  margin-bottom: 0.25rem;
}
</style>
