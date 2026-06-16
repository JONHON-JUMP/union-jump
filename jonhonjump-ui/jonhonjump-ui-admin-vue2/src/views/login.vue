<template>
  <div class="jump-login-mian-page">
    <div class="login-container">
      <div ref="brandSection" class="brand-section">
        <canvas ref="particleCanvas" id="particle-canvas" />
        <div class="brand-overlay">
          <div class="system-badge">
            <div class="badge-glow" />
            <h2 class="system-name">JUMP</h2>
            <p class="system-fullname">Jonhon Uniform Manufacturing Platform</p>
            <p class="system-cn">JONHON统一制造管理平台</p>
          </div>
          <div class="slogan">
            <span class="slogan-icon">⚡</span>
            <p class="slogan-text">连接世界，制造未来</p>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <p class="form-welcome">欢迎登录</p>
          </div>

          <form autocomplete="off" @submit.prevent="handleSubmit">
            <div class="input-group" :class="{ shake: shakeUsername }">
              <div class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
              <input
                id="username"
                v-model.trim="loginForm.username"
                type="text"
                placeholder=" "
                autocomplete="username"
              >
              <label for="username">用户名 / 工号 / 域账号</label>
              <div class="input-highlight" />
            </div>

            <div class="input-group" :class="{ shake: shakePassword }">
              <div class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              </div>
              <input
                id="password"
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder=" "
                autocomplete="current-password"
                @keyup.enter="handleSubmit"
              >
              <label for="password">密码</label>
              <div class="input-highlight" />
              <button type="button" class="toggle-password" aria-label="显示/隐藏密码" @click="showPassword = !showPassword">
                <svg v-show="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-show="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                  <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>

            <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

            <div class="form-options">
              <label class="remember-me">
                <input v-model="loginForm.rememberMe" type="checkbox">
                <span class="checkmark" />
                <span class="remember-text">记住我</span>
              </label>
            </div>

            <button type="submit" class="login-btn" :disabled="loading">
              <span v-if="!loading" class="btn-text">登 录</span>
              <span v-else class="btn-loader">
                <span class="loader-dot" />
                <span class="loader-dot" />
                <span class="loader-dot" />
              </span>
              <div class="btn-glow" />
            </button>
          </form>

          <div class="form-footer">
            <p class="copyright">© 中航光电科技股份有限公司</p>
          </div>
        </div>
      </div>
    </div>

    <Verify
      ref="verify"
      :captcha-type="'blockPuzzle'"
      :img-size="{ width: '400px', height: '200px' }"
      @success="handleLogin"
    />
  </div>
</template>

<script>
import { getCaptchaEnable } from '@/utils/ruoyi'
import {
  getPassword,
  getRememberMe,
  getUsername,
  removePassword,
  removeRememberMe,
  removeUsername,
  setPassword,
  setRememberMe,
  setTenantId,
  setUsername
} from '@/utils/auth'
import { ParticleNetwork } from '@/utils/loginParticle'
import Verify from '@/components/Verifition/Verify'

export default {
  name: 'Login',
  components: { Verify },
  data() {
    return {
      captchaEnable: false,
      loading: false,
      showPassword: false,
      errorMessage: '',
      shakeUsername: false,
      shakePassword: false,
      redirect: undefined,
      particleNetwork: null,
      loginForm: {
        loginType: 'auto',
        username: '',
        password: '',
        captchaVerification: '',
        rememberMe: false
      }
    }
  },
  created() {
    setTenantId(1)
    this.captchaEnable = getCaptchaEnable()
    this.redirect = this.$route.query.redirect ? decodeURIComponent(this.$route.query.redirect) : undefined
    this.restoreRemembered()
  },
  mounted() {
    if (this.$refs.particleCanvas && this.$refs.brandSection) {
      this.particleNetwork = new ParticleNetwork(this.$refs.particleCanvas, this.$refs.brandSection)
    }
  },
  beforeDestroy() {
    if (this.particleNetwork) {
      this.particleNetwork.destroy()
      this.particleNetwork = null
    }
  },
  methods: {
    restoreRemembered() {
      this.loginForm.username = getUsername() || ''
      this.loginForm.password = getPassword() || ''
      this.loginForm.rememberMe = !!getRememberMe()
    },
    shakeField(field) {
      if (field === 'username') {
        this.shakeUsername = true
        setTimeout(() => { this.shakeUsername = false }, 500)
      } else {
        this.shakePassword = true
        setTimeout(() => { this.shakePassword = false }, 500)
      }
    },
    handleSubmit() {
      this.errorMessage = ''
      let hasError = false
      if (!this.loginForm.username) {
        this.shakeField('username')
        hasError = true
      }
      if (!this.loginForm.password) {
        this.shakeField('password')
        hasError = true
      }
      if (hasError) {
        this.errorMessage = '请输入用户名/工号/域账号和密码'
        return
      }
      if (!this.captchaEnable) {
        this.handleLogin({})
        return
      }
      this.$refs.verify.show()
    },
    handleLogin(captchaParams) {
      this.errorMessage = ''
      this.loading = true
      if (this.loginForm.rememberMe) {
        setUsername(this.loginForm.username)
        setPassword(this.loginForm.password)
        setRememberMe(true)
      } else {
        removeUsername()
        removePassword()
        removeRememberMe()
      }
      this.loginForm.captchaVerification = captchaParams.captchaVerification
      this.$store.dispatch('Login', this.loginForm).then(() => {
        this.$router.push({ path: this.redirect || '/' }).catch(() => {})
      }).catch((error) => {
        this.loading = false
        if (typeof error === 'string') {
          this.errorMessage = '登录失败，请检查账号和密码'
        } else {
          this.errorMessage = (error && error.message) || '登录失败，请检查账号和密码'
        }
      })
    }
  }
}
</script>

<style src="@/assets/styles/login-mian.css"></style>
