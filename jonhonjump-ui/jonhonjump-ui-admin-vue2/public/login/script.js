/**
 * login_mian 登录页脚本 — 对接 JUMP 后端
 */
(function () {
  var STORAGE_ACCOUNT = 'USERNAME'
  var STORAGE_PASSWORD = 'PASSWORD'
  var STORAGE_REMEMBER = 'REMEMBER_ME'
  var STORAGE_TOKEN = 'ACCESS_TOKEN'
  var STORAGE_REFRESH = 'REFRESH_TOKEN'
  var STORAGE_TENANT = 'TENANT_ID'

  var config = window.LOGIN_CONFIG || {}
  var params = new URLSearchParams(window.location.search)
  var apiBase = config.apiBase || params.get('api') || ''
  var portalUrl = config.portalUrl || params.get('redirect') || '/'

  var state = { loading: false, showPassword: false }

  document.addEventListener('DOMContentLoaded', function () {
    var particleCanvas = document.getElementById('particle-canvas')
    if (particleCanvas && window.ParticleNetwork) {
      new window.ParticleNetwork(particleCanvas, particleCanvas.parentElement)
    }

    var els = {
      form: document.getElementById('login-form'),
      account: document.getElementById('username'),
      password: document.getElementById('password'),
      rememberMe: document.getElementById('remember'),
      errorBox: document.getElementById('error-box'),
      loginBtn: document.getElementById('login-btn'),
      btnText: document.querySelector('#login-btn .btn-text'),
      btnLoader: document.querySelector('#login-btn .btn-loader'),
      toggleBtn: document.getElementById('toggle-password')
    }

    localStorage.setItem(STORAGE_TENANT, '1')
    restoreRemembered(els)

    if (els.toggleBtn) {
      els.toggleBtn.addEventListener('click', function () {
        state.showPassword = !state.showPassword
        els.password.type = state.showPassword ? 'text' : 'password'
        var eyeOpen = els.toggleBtn.querySelector('.eye-open')
        var eyeClosed = els.toggleBtn.querySelector('.eye-closed')
        if (eyeOpen) eyeOpen.style.display = state.showPassword ? 'none' : 'block'
        if (eyeClosed) eyeClosed.style.display = state.showPassword ? 'block' : 'none'
      })
    }

    els.form.addEventListener('submit', function (event) {
      event.preventDefault()
      if (state.loading) return
      var account = els.account.value.trim()
      var password = els.password.value
      showError('')
      var hasError = false
      if (!account) {
        shakeInput(els.account)
        hasError = true
      }
      if (!password) {
        shakeInput(els.password)
        hasError = true
      }
      if (hasError) {
        showError('请输入用户名/工号/域账号和密码')
        return
      }
      setLoading(true)
      loginRequest(account, password).then(function (token) {
        saveToken(token)
        persistRemembered(els, account, password)
        window.location.href = portalUrl
      }).catch(function (error) {
        setLoading(false)
        showError(error.message || '登录失败')
      })
    })

    function resolveApiUrl(path) {
      var base = apiBase || window.location.origin
      return base.replace(/\/$/, '') + path
    }

    function showError(message) {
      if (!els.errorBox) return
      if (!message) {
        els.errorBox.hidden = true
        els.errorBox.textContent = ''
        return
      }
      els.errorBox.hidden = false
      els.errorBox.textContent = message
    }

    function setLoading(loading) {
      state.loading = loading
      els.loginBtn.disabled = loading
      els.loginBtn.classList.toggle('loading', loading)
      if (els.btnText) els.btnText.style.display = loading ? 'none' : 'inline'
      if (els.btnLoader) els.btnLoader.style.display = loading ? 'inline-flex' : 'none'
    }

    function shakeInput(input) {
      var group = input.closest('.input-group')
      if (!group) return
      group.classList.add('shake')
      setTimeout(function () { group.classList.remove('shake') }, 500)
    }

    function restoreRemembered(els) {
      var remembered = localStorage.getItem(STORAGE_REMEMBER) === 'true'
      els.rememberMe.checked = remembered
      if (remembered) {
        els.account.value = localStorage.getItem(STORAGE_ACCOUNT) || ''
      }
    }

    function persistRemembered(els, account) {
      if (els.rememberMe.checked) {
        localStorage.setItem(STORAGE_REMEMBER, 'true')
        localStorage.setItem(STORAGE_ACCOUNT, account)
      } else {
        localStorage.removeItem(STORAGE_REMEMBER)
        localStorage.removeItem(STORAGE_ACCOUNT)
        localStorage.removeItem(STORAGE_PASSWORD)
      }
    }

    function saveToken(data) {
      localStorage.setItem(STORAGE_TOKEN, data.accessToken)
      localStorage.setItem(STORAGE_REFRESH, data.refreshToken)
    }

    function loginRequest(account, password) {
      return fetch(resolveApiUrl('/admin-api/system/auth/login'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'tenant-id': localStorage.getItem(STORAGE_TENANT) || '1'
        },
        body: JSON.stringify({
          loginType: 'auto',
          username: account,
          password: password,
          captchaVerification: ''
        })
      }).then(function (response) {
        return response.json().then(function (result) {
          if (!response.ok || result.code !== 0) {
            throw new Error(result.msg || '登录失败，请检查账号和密码')
          }
          return result.data
        })
      })
    }
  })
})()
