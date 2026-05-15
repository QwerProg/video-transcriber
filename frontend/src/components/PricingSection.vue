<template>
  <section id="pricing" class="py-24 sm:py-32 relative overflow-hidden">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 relative z-10">
      <div class="text-center mb-16">
        <h2 class="text-3xl sm:text-5xl font-black text-white mb-6 tracking-tight uppercase">
          选择您的 <span class="text-primary text-glow">会员方案</span>
        </h2>
        <p class="text-text-secondary text-lg max-w-2xl mx-auto leading-relaxed">
          开通高级版，释放 AI 视频处理的全部潜能。
        </p>
      </div>

      <div class="flex flex-col lg:flex-row items-center justify-center gap-8 mb-20">
        <!-- 免费版 -->
        <div class="w-full max-w-md glass-effect rounded-3xl p-8 border border-white/5 flex flex-col hover:border-white/10 transition-all">
          <div class="mb-8">
            <h3 class="text-xl font-black text-white mb-2 uppercase tracking-widest">基础免费版</h3>
            <p class="text-text-muted text-[10px] font-black uppercase tracking-widest">创作者的基础选择</p>
          </div>
          <div class="mb-8">
            <span class="text-5xl font-black text-white">¥0</span>
            <span class="text-text-muted text-xs ml-2 uppercase font-black tracking-widest">/ 终身</span>
          </div>
          <ul class="space-y-4 mb-10 flex-1">
            <li v-for="item in freePlan" :key="item" class="flex items-center gap-3 text-sm text-text-secondary">
              <svg class="w-5 h-5 text-success/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              {{ item }}
            </li>
          </ul>
          <button
            @click="!user && $emit('need-login')"
            class="w-full py-4 rounded-xl border border-white/10 text-white font-black tracking-widest uppercase hover:bg-white/5 transition-all cursor-pointer text-xs"
          >
            {{ user ? '当前方案' : '立即注册' }}
          </button>
        </div>

        <!-- VIP 版 -->
        <div class="w-full max-w-md glass-effect rounded-3xl p-10 border border-primary/30 flex flex-col relative overflow-hidden group hover:border-primary transition-all shadow-[0_0_40px_rgba(0,209,255,0.15)]">
          <div class="absolute top-0 right-0 px-4 py-1.5 bg-primary text-slate-950 text-[10px] font-black uppercase tracking-widest rounded-bl-xl shadow-[0_0_20px_rgba(0,209,255,0.5)]">
            最受欢迎
          </div>
          <div class="mb-8">
            <h3 class="text-2xl font-black text-primary mb-2 uppercase tracking-widest text-glow">高级专业版</h3>
            <p class="text-text-muted text-[10px] font-black uppercase tracking-widest">极致性能与优先权</p>
          </div>
          <div class="mb-8 flex items-baseline">
            <span class="text-6xl font-black text-white">¥9.9</span>
            <span class="text-text-muted text-xs ml-2 uppercase font-black tracking-widest">/ 月</span>
            <span class="ml-3 text-[10px] bg-primary/20 text-primary border border-primary/30 px-2 py-0.5 rounded-full font-black tracking-tighter">优惠</span>
          </div>
          <ul class="space-y-4 mb-10 flex-1">
            <li v-for="item in vipPlan" :key="item" class="flex items-center gap-3 text-sm text-white font-bold">
              <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
              </svg>
              {{ item }}
            </li>
          </ul>
          <div class="relative group/btn">
            <div class="absolute -inset-1 bg-gradient-to-r from-primary to-blue-600 rounded-xl blur opacity-30 group-hover/btn:opacity-60 transition duration-500"></div>
            <button @click="handleVipClick" class="relative w-full py-4 bg-primary text-slate-950 font-black tracking-widest uppercase rounded-xl transition-all cursor-pointer shadow-[0_0_20px_rgba(0,209,255,0.4)] text-xs">
              {{ user?.is_vip ? '续费方案' : '立即开通' }}
            </button>
          </div>
        </div>
      </div>

      <!-- FAQ 简单说明 -->
      <div class="max-w-3xl mx-auto glass-effect rounded-3xl p-8 border border-white/5 text-center">
        <p class="text-text-muted text-xs font-bold uppercase tracking-widest leading-relaxed">
          支付成功后立即生效。由 Stripe 提供安全支付加密。24/7 技术监控保障。
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
const props = defineProps({
  user: { type: Object, default: null },
})

const emit = defineEmits(['open-vip', 'need-login'])

const freePlan = [
  '无限次视频下载',
  '1800+ 平台解析支持',
  '标准解析速度',
  '每日 3 次 AI 总结',
]

const vipPlan = [
  '无限次 AI 视频总结',
  '4K 思维导图生成',
  '针对视频的 AI 问答',
  '完整字幕文本导出',
  '服务器解析优先权',
]

function handleVipClick() {
  if (!props.user) {
    emit('need-login')
    return
  }
  emit('open-vip')
}
</script>
