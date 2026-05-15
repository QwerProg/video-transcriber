<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-slate-950/80 backdrop-blur-md" @click="$emit('close')"></div>
      <div class="relative glass-effect rounded-3xl border border-white/10 shadow-2xl w-full max-w-md overflow-hidden animate-modal-in">
        <!-- Header -->
        <div class="px-8 pt-8 pb-4">
          <div class="flex items-center justify-between mb-2">
            <h2 class="text-xl font-black text-white uppercase tracking-widest">
              {{ isLogin ? '身份验证' : '创建账户' }}
            </h2>
            <button @click="$emit('close')" class="p-2 rounded-xl hover:bg-white/5 transition-colors cursor-pointer border border-transparent hover:border-white/10">
              <svg class="w-5 h-5 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p class="text-[10px] font-bold text-text-muted uppercase tracking-widest">
            {{ isLogin ? '连接至安全处理核心' : '初始化您的会员档案' }}
          </p>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleSubmit" class="px-8 pb-8">
          <div class="space-y-5">
            <div>
              <label class="block text-[10px] font-black text-text-muted uppercase tracking-[0.2em] mb-2">邮箱地址</label>
              <input
                v-model="email"
                type="email"
                required
                placeholder="你的邮箱地址"
                class="w-full h-12 px-5 rounded-2xl border border-white/10 bg-white/5 text-sm text-white placeholder:text-text-muted/30 focus:outline-none focus:border-primary/50 focus:bg-white/[0.08] transition-all font-mono"
                :disabled="submitting"
              />
            </div>
            <div>
              <label class="block text-[10px] font-black text-text-muted uppercase tracking-[0.2em] mb-2">访问密码</label>
              <div class="relative">
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  minlength="6"
                  placeholder="••••••••"
                  class="w-full h-12 px-5 pr-12 rounded-2xl border border-white/10 bg-white/5 text-sm text-white placeholder:text-text-muted/30 focus:outline-none focus:border-primary/50 focus:bg-white/[0.08] transition-all font-mono"
                  :disabled="submitting"
                />
                <button type="button" @click="showPassword = !showPassword" class="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted hover:text-primary transition-colors cursor-pointer">
                  <svg v-if="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div v-if="error" class="mt-6 p-4 rounded-2xl bg-error/10 border border-error/20">
            <p class="text-xs font-bold text-error uppercase tracking-widest text-center">{{ error }}</p>
          </div>

          <div class="relative group/btn mt-8">
            <div class="absolute -inset-1 bg-gradient-to-r from-primary to-blue-600 rounded-2xl blur opacity-30 group-hover/btn:opacity-60 transition duration-500"></div>
            <button
              type="submit"
              :disabled="submitting"
              class="relative w-full h-12 bg-primary text-slate-950 font-black tracking-[0.2em] uppercase rounded-2xl transition-all shadow-[0_0_20px_rgba(0,209,255,0.2)] disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-3 text-xs"
            >
              <svg v-if="submitting" class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              {{ submitting ? '同步中...' : (isLogin ? '立即登录' : '立即注册') }}
            </button>
          </div>

          <p class="mt-6 text-center text-[10px] font-black text-text-muted uppercase tracking-widest">
            {{ isLogin ? '新用户？' : '已有账户？' }}
            <button type="button" @click="toggleMode" class="text-primary hover:text-white transition-colors cursor-pointer ml-1">
              {{ isLogin ? '注册新账户' : '前往登录' }}
            </button>
          </p>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { register, login } from '../api/auth'

const props = defineProps({
  visible: Boolean,
  initialMode: { type: String, default: 'login' },
})

const emit = defineEmits(['close', 'success'])

const isLogin = ref(props.initialMode === 'login')
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const error = ref('')

function toggleMode() {
  isLogin.value = !isLogin.value
  error.value = ''
}

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    const user = isLogin.value
      ? await login(email.value, password.value)
      : await register(email.value, password.value)
    emit('success', user)
    emit('close')
    email.value = ''
    password.value = ''
  } catch (err) {
    const msg = err.response?.data?.detail || err.message || '网关错误'
    error.value = (typeof msg === 'string' ? msg : JSON.stringify(msg)).toUpperCase()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@keyframes modal-in {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.animate-modal-in {
  animation: modal-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
</style>
