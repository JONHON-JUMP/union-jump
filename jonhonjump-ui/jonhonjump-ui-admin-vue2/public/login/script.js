/**
 * login 静态登录页 — 对接 JUMP 后端（与 Vue auth 存储键一致）
 * 现场模式：Token 使用 sessionStorage，关闭浏览器即失效
 */
(function () {
  var STORAGE_TOKEN = 'ACCESS_TOKEN'
  var STORAGE_REFRESH = 'REFRESH_TOKEN'
  var STORAGE_TENANT = 'TENANT_ID'
  var tokenStorage = window.sessionStorage

  var config = window.LOGIN_CONFIG || {}
  var params = new URLSearchParams(window.location.search)
  var portalUrl = config.portalUrl || params.get('redirect') || '/index'

  var state = { loading: false, showPassword: false }

  function getApiBase() {
    if (config.apiBase) {
      return String(config.apiBase).replace(/\/$/, '')
    }
    var qsApi = params.get('api')
    if (qsApi) {
      return qsApi.replace(/\/$/, '')
    }
    var loc = window.location
    var isLocalHost = loc.hostname === 'localhost' || loc.hostname === '127.0.0.1'
    if (isLocalHost && (loc.port === '80' || loc.port === '' || loc.port === '8080')) {
      return 'http://localhost:48080'
    }
    return loc.origin
  }

  function resolveApiUrl(path) {
    return getApiBase() + path
  }

  ;['ACCESS_TOKEN', 'REFRESH_TOKEN', 'PASSWORD', 'REMEMBER_ME'].forEach(function (key) {
    localStorage.removeItem(key)
  })

  document.addEventListener('DOMContentLoaded', function () {
    if (tokenStorage.getItem(STORAGE_TOKEN)) {
      // 已登录误进登录页：直接回门户，勿清 portal_last_system（否则刷新保持被毁掉）
      window.location.href = portalUrl
      return
    }
    // 真正要重新登录：清门户会话，首进走星标默认（含各版快捷导航缓存，防串用户）
    ;[
      'portal_last_system',
      'portal_subsystem_cache',
      'portal_sso_done',
      'portal_quick_nav_cache_v1',
      'portal_quick_nav_cache_v2',
      'portal_quick_nav_cache_v3',
      'portal_main_boot_id'
    ].forEach(function (key) {
      try { sessionStorage.removeItem(key) } catch (e) { /* ignore */ }
    })

    var els = {
      form: document.getElementById('login-form'),
      account: document.getElementById('username'),
      password: document.getElementById('password'),
      errorBox: document.getElementById('error-box'),
      loginBtn: document.getElementById('login-btn'),
      btnText: document.querySelector('#login-btn .btn-text'),
      btnLoader: document.querySelector('#login-btn .btn-loader'),
      toggleBtn: document.getElementById('toggle-password')
    }

    sessionStorage.setItem(STORAGE_TENANT, '1')

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
        showError('请输入工号/域账号和密码')
        return
      }
      setLoading(true)
      loginRequest(account, password).then(function (token) {
        saveToken(token)
        window.location.href = portalUrl
      }).catch(function (error) {
        setLoading(false)
        showError(error.message || '登录失败，请检查账号和密码')
      })
    })

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

    function saveToken(data) {
      tokenStorage.setItem(STORAGE_TOKEN, data.accessToken)
      tokenStorage.setItem(STORAGE_REFRESH, data.refreshToken)
    }

    function loginRequest(account, password) {
      var controller = typeof AbortController !== 'undefined' ? new AbortController() : null
      var timer = null
      var fetchPromise = fetch(resolveApiUrl('/admin-api/system/auth/login'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'tenant-id': sessionStorage.getItem(STORAGE_TENANT) || '1'
        },
        body: JSON.stringify({
          loginType: 'auto',
          username: account,
          password: password,
          captchaVerification: ''
        }),
        signal: controller ? controller.signal : undefined
      }).then(function (response) {
        return response.text().then(function (text) {
          var result
          try {
            result = JSON.parse(text)
          } catch (e) {
            throw new Error('无法连接登录服务，请确认后端已启动且 API 代理配置正确')
          }
          if (!response.ok || result.code !== 0) {
            throw new Error(result.msg || '登录失败，请检查账号和密码')
          }
          return result.data
        })
      })
      if (!controller) {
        return fetchPromise
      }
      var timeoutPromise = new Promise(function (_, reject) {
        timer = setTimeout(function () {
          try { controller.abort() } catch (e) { /* ignore */ }
          reject(new Error('登录超时（15秒），请检查网络或后端是否正常'))
        }, 15000)
      })
      return Promise.race([fetchPromise, timeoutPromise]).then(function (data) {
        if (timer) clearTimeout(timer)
        return data
      }, function (err) {
        if (timer) clearTimeout(timer)
        throw err
      })
    }
  })
})()
