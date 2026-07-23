(function () {
  var STORAGE_TOKEN = 'ACCESS_TOKEN'
  var STORAGE_REFRESH = 'REFRESH_TOKEN'
  var STORAGE_TENANT = 'TENANT_ID'
  var tokenStorage = window.sessionStorage

  var config = window.LOGIN_CONFIG || {}
  var params = new URLSearchParams(window.location.search)
  var apiBase = config.apiBase || params.get('api') || ''
  var portalUrl = config.portalUrl || params.get('redirect') || '/index'

  var state = { loading: false }

  var els = {
    form: document.getElementById('loginForm'),
    account: document.getElementById('account'),
    password: document.getElementById('password'),
    errorBox: document.getElementById('errorBox'),
    submitBtn: document.getElementById('submitBtn'),
    btnText: document.querySelector('.btn-text'),
    btnLoading: document.querySelector('.btn-loading')
  }

  ;['ACCESS_TOKEN', 'REFRESH_TOKEN', 'PASSWORD', 'REMEMBER_ME', 'JUMP_LOGIN_REMEMBER', 'JUMP_LOGIN_ACCOUNT'].forEach(function (key) {
    localStorage.removeItem(key)
  })

  if (tokenStorage.getItem(STORAGE_TOKEN)) {
    // 已登录误进登录页：回门户，保留当前系统会话
    window.location.href = portalUrl
  } else {
    // 真正重新登录：清门户会话，首进走星标默认
    ;['portal_last_system', 'portal_subsystem_cache', 'portal_sso_done', 'portal_quick_nav_cache_v1'].forEach(function (key) {
      try { sessionStorage.removeItem(key) } catch (e) { /* ignore */ }
    })
  }

  function resolveApiUrl(path) {
    var base = apiBase || window.location.origin
    return base.replace(/\/$/, '') + path
  }

  function showError(message) {
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
    els.submitBtn.disabled = loading
    els.btnText.hidden = loading
    els.btnLoading.hidden = !loading
  }

  function saveToken(data) {
    tokenStorage.setItem(STORAGE_TOKEN, data.accessToken)
    tokenStorage.setItem(STORAGE_REFRESH, data.refreshToken)
  }

  sessionStorage.setItem(STORAGE_TENANT, '1')

  async function loginRequest(account, password) {
    var response = await fetch(resolveApiUrl('/admin-api/system/auth/login'), {
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
      })
    })
    var result = await response.json()
    if (!response.ok || result.code !== 0) {
      throw new Error(result.msg || '登录失败，请检查账号和密码')
    }
    return result.data
  }

  els.form.addEventListener('submit', async function (event) {
    event.preventDefault()
    if (state.loading) return

    var account = els.account.value.trim()
    var password = els.password.value
    if (!account) {
      showError('请输入工号/域账号')
      return
    }
    if (!password) {
      showError('请输入密码')
      return
    }

    setLoading(true)
    showError('')
    try {
      var token = await loginRequest(account, password)
      saveToken(token)
      window.location.href = portalUrl
    } catch (error) {
      showError(error.message || '登录失败')
      setLoading(false)
    }
  })
})()
