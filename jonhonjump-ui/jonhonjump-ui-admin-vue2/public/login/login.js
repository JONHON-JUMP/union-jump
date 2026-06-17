(function () {
  var STORAGE_ACCOUNT = 'JUMP_LOGIN_ACCOUNT'
  var STORAGE_REMEMBER = 'JUMP_LOGIN_REMEMBER'
  var STORAGE_TOKEN = 'ACCESS_TOKEN'
  var STORAGE_REFRESH = 'REFRESH_TOKEN'
  var STORAGE_TENANT = 'TENANT_ID'

  var config = window.LOGIN_CONFIG || {}
  var params = new URLSearchParams(window.location.search)
  var apiBase = config.apiBase || params.get('api') || ''
  var portalUrl = config.portalUrl || params.get('redirect') || '/index'

  var state = { loading: false }

  var els = {
    form: document.getElementById('loginForm'),
    account: document.getElementById('account'),
    password: document.getElementById('password'),
    rememberMe: document.getElementById('rememberMe'),
    errorBox: document.getElementById('errorBox'),
    submitBtn: document.getElementById('submitBtn'),
    btnText: document.querySelector('.btn-text'),
    btnLoading: document.querySelector('.btn-loading')
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

  function restoreRemembered() {
    var remembered = localStorage.getItem(STORAGE_REMEMBER) === 'true'
    els.rememberMe.checked = remembered
    if (remembered) {
      els.account.value = localStorage.getItem(STORAGE_ACCOUNT) || ''
    }
    localStorage.setItem(STORAGE_TENANT, '1')
  }

  function persistRemembered(account, password) {
    if (els.rememberMe.checked) {
      localStorage.setItem(STORAGE_REMEMBER, 'true')
      localStorage.setItem(STORAGE_ACCOUNT, account)
    } else {
      localStorage.removeItem(STORAGE_REMEMBER)
      localStorage.removeItem(STORAGE_ACCOUNT)
    }
  }

  function saveToken(data) {
    localStorage.setItem(STORAGE_TOKEN, data.accessToken)
    localStorage.setItem(STORAGE_REFRESH, data.refreshToken)
  }

  async function loginRequest(account, password) {
    var response = await fetch(resolveApiUrl('/admin-api/system/auth/login'), {
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
      showError('请输入用户名/工号/域账号')
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
      persistRemembered(account, password)
      window.location.href = portalUrl
    } catch (error) {
      showError(error.message || '登录失败')
      setLoading(false)
    }
  })

  restoreRemembered()
})()
