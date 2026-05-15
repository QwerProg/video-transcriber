<template>
  <div class="min-h-screen flex flex-col bg-bg-main selection:bg-primary/30 selection:text-white overflow-x-hidden">
    <AppHeader
      :user="currentUser"
      @login="showAuthModal('login')"
      @register="showAuthModal('register')"
      @logout="handleLogout"
      @open-vip="handleOpenVip"
    />
    <main class="flex-1 relative">
      <!-- Background Glows -->
      <div class="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <div class="absolute -top-[20%] -right-[10%] w-[60%] h-[60%] bg-primary/10 rounded-full blur-[120px] animate-pulse"></div>
        <div class="absolute top-[40%] -left-[10%] w-[50%] h-[50%] bg-blue-600/5 rounded-full blur-[100px]"></div>
      </div>

      <div class="relative z-10">
        <HeroSection
          @parse="handleParse"
          :loading="loading"
          :compact="!!videoData"
          :showSlogan="!videoData || demoMode"
          class="animate-fade-in-up"
        />

        <!-- 视频信息 + AI 总结 -->
        <transition
          enter-active-class="transition duration-700 ease-out"
          enter-from-class="opacity-0 translate-y-10"
          enter-to-class="opacity-100 translate-y-0"
        >
          <section v-if="videoData" class="py-8 sm:py-12 relative">
            <div class="max-w-7xl mx-auto px-4 sm:px-6">
              <div class="flex flex-col lg:flex-row gap-8">
                <!-- 左栏：视频信息 -->
                <div class="w-full lg:w-2/5 lg:flex-shrink-0">
                  <VideoResult
                    :video="videoData"
                    :downloading="downloading"
                    :summarizing="summarizing"
                    @download="handleDownload"
                    @summarize="handleSummarize"
                  />
                </div>
                <!-- 右栏：AI 总结 -->
                <div class="w-full lg:w-3/5 min-w-0">
                  <VideoSummary
                    :videoUrl="currentUrl"
                    :videoTitle="videoData.title"
                    :user="currentUser"
                    :key="summaryKey"
                    @loading-change="handleSummarizeLoadingChange"
                    @need-login="showAuthModal('login')"
                    @need-vip="handleOpenVip"
                  />
                </div>
              </div>
            </div>
          </section>
        </transition>

        <FeatureSection class="animate-fade-in-up [animation-delay:200ms]" />
        <PricingSection
          :user="currentUser"
          @open-vip="handleOpenVip"
          @need-login="showAuthModal('login')"
          class="animate-fade-in-up [animation-delay:400ms]"
        />
        <PlatformSection class="animate-fade-in-up [animation-delay:600ms]" />
      </div>
    </main>
    <AppFooter class="relative z-10" />

    <!-- 支付成功/取消提示 -->
    <Teleport to="body">
      <div v-if="paymentToast" class="fixed top-24 left-1/2 -translate-x-1/2 z-[200] animate-toast-in">
        <div :class="[
          'flex items-center gap-4 px-6 py-4 rounded-2xl glass-effect shadow-2xl border transition-all duration-300',
          paymentToast === 'success' ? 'border-success/30 text-success' : 'border-orange-500/30 text-orange-500'
        ]">
          <div :class="['p-2 rounded-full', paymentToast === 'success' ? 'bg-success/10' : 'bg-orange-500/10']">
            <svg v-if="paymentToast === 'success'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <span class="font-bold text-sm tracking-wide text-white">
            {{ paymentToast === 'success' ? 'VIP 已激活！所有高级功能已解锁' : '支付已取消。您可以随时再次尝试' }}
          </span>
        </div>
      </div>
    </Teleport>

    <AuthModal
      :visible="authModalVisible"
      :initialMode="authModalMode"
      @close="authModalVisible = false"
      @success="handleAuthSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import AppHeader from './components/AppHeader.vue'
import HeroSection from './components/HeroSection.vue'
import VideoResult from './components/VideoResult.vue'
import VideoSummary from './components/VideoSummary.vue'
import FeatureSection from './components/FeatureSection.vue'
import PricingSection from './components/PricingSection.vue'
import PlatformSection from './components/PlatformSection.vue'
import AppFooter from './components/AppFooter.vue'
import AuthModal from './components/AuthModal.vue'
import { parseVideo, downloadViaServer } from './api/video.js'
import { getSavedUser, fetchMe, logout as logoutApi, isLoggedIn } from './api/auth.js'
import { createCheckoutSession } from './api/payment.js'

const demoMode = ref(false)
let enterCount = 0
let enterTimer = null

function onKeyDown(e) {
  if (e.key === 'Enter' && !e.target.matches('input, textarea, [contenteditable]')) {
    enterCount++
    clearTimeout(enterTimer)
    if (enterCount >= 3) {
      demoMode.value = !demoMode.value
      enterCount = 0
    } else {
      enterTimer = setTimeout(() => { enterCount = 0 }, 800)
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeyDown)
  restoreUser()
  checkPaymentResult()
})
onBeforeUnmount(() => { document.removeEventListener('keydown', onKeyDown) })

// ===== 用户状态管理 =====
const currentUser = ref(null)
const authModalVisible = ref(false)
const authModalMode = ref('login')

function showAuthModal(mode = 'login') {
  authModalMode.value = mode
  authModalVisible.value = true
}

function handleAuthSuccess(user) {
  currentUser.value = user
}

function handleLogout() {
  logoutApi()
  currentUser.value = null
}

async function restoreUser() {
  if (!isLoggedIn()) return
  const saved = getSavedUser()
  if (saved) currentUser.value = saved
  try {
    currentUser.value = await fetchMe()
  } catch {
    handleLogout()
  }
}

// ===== VIP 购买 =====
async function handleOpenVip() {
  if (!isLoggedIn()) {
    showAuthModal('login')
    return
  }
  try {
    const { checkout_url } = await createCheckoutSession('monthly')
    window.location.href = checkout_url
  } catch (err) {
    const msg = err.response?.data?.detail || err.message || '创建支付失败'
    alert(typeof msg === 'string' ? msg : JSON.stringify(msg))
  }
}

// ===== 支付结果处理 =====
const paymentToast = ref(null)

function checkPaymentResult() {
  const params = new URLSearchParams(window.location.search)
  const payment = params.get('payment')
  if (payment === 'success' || payment === 'cancel') {
    paymentToast.value = payment
    window.history.replaceState({}, '', window.location.pathname)
    if (payment === 'success' && isLoggedIn()) {
      setTimeout(async () => {
        try { currentUser.value = await fetchMe() } catch {}
      }, 1000)
    }
    setTimeout(() => { paymentToast.value = null }, 5000)
  }
}

// ===== 视频功能 =====
const loading = ref(false)
const downloading = ref(false)
const videoData = ref(null)
const currentUrl = ref('')
const summaryKey = ref(0)
const summarizing = ref(false)

function handleSummarize() {
  summaryKey.value++
}

function handleSummarizeLoadingChange(isLoading) {
  summarizing.value = isLoading
}

async function handleParse(url) {
  loading.value = true
  videoData.value = null
  currentUrl.value = url
  try {
    const res = await parseVideo(url)
    if (res.success) {
      videoData.value = res.data
      summaryKey.value++
    } else {
      alert('解析失败：' + (res.error || '未知错误'))
    }
  } catch (err) {
    const msg = err.response?.data?.detail?.error || err.response?.data?.detail || err.message
    alert('解析失败：' + msg)
  } finally {
    loading.value = false
  }
}

async function handleDownload(formatId) {
  downloading.value = true
  try {
    const response = await downloadViaServer(currentUrl.value, formatId)
    const contentDisposition = response.headers['content-disposition']
    let filename = 'video.mp4'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?([^;\n]+)/i)
      if (match) filename = decodeURIComponent(match[1].replace(/"/g, ''))
    }
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    alert('下载失败：' + (err.message || '请稍后重试'))
  } finally {
    downloading.value = false
  }
}
</script>

<style>
@keyframes toast-in {
  from { opacity: 0; transform: translate(-50%, -10px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}
.animate-toast-in {
  animation: toast-in 0.3s ease-out;
}
</style>
