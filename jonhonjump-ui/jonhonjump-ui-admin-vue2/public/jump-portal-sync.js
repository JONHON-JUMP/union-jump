/**
 * 业务页：路由变化通知 JUMP；点击按钮时带上按钮文字（查看参数 / 新增版本）。
 * 由 9100 nginx location = /jump-portal-sync.js 直接 return。
 */
(function () {
  if (window.__JUMP_PORTAL_SYNC__) {
    return
  }
  window.__JUMP_PORTAL_SYNC__ = true
  var last = ''
  var lastClick = ''

  function clickText(el) {
    var i = 0
    while (el && i < 10) {
      var tag = String(el.tagName || '').toLowerCase()
      var cls = String(el.className || '')
      var role = el.getAttribute ? String(el.getAttribute('role') || '') : ''
      if (tag === 'button' || tag === 'a' || role === 'button'
        || cls.indexOf('el-button') >= 0
        || cls.indexOf('el-dropdown-menu__item') >= 0
        || cls.indexOf('el-link') >= 0) {
        var text = String(el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim()
        if (text && text.length <= 24) {
          return text
        }
      }
      el = el.parentElement
      i++
    }
    return ''
  }

  document.addEventListener('click', function (e) {
    var t = clickText(e.target)
    if (t) {
      lastClick = t
    }
  }, true)

  function report() {
    try {
      var href = String(window.location.href || '')
      if (!href || href.indexOf('about:blank') >= 0) {
        return
      }
      if (href === last) {
        return
      }
      last = href
      var label = lastClick
      lastClick = ''
      if (window.parent && window.parent !== window) {
        window.parent.postMessage({
          type: 'JUMP_PORTAL_LOCATION',
          href: href,
          title: label,
          clickLabel: label
        }, '*')
      }
    } catch (e) { /* ignore */ }
  }
  report()
  window.addEventListener('hashchange', report)
  window.addEventListener('popstate', report)
  try {
    var push = history.pushState
    var replace = history.replaceState
    history.pushState = function () {
      var ret = push.apply(this, arguments)
      setTimeout(report, 0)
      return ret
    }
    history.replaceState = function () {
      var ret = replace.apply(this, arguments)
      setTimeout(report, 0)
      return ret
    }
  } catch (e2) { /* ignore */ }
  setInterval(report, 200)

  /**
   * 业务系统按钮打开新窗口时，如果目标 host 和当前业务 host 不同，
   * 新窗口开始时是没有 Camstar 会话 Cookie 的（Cookie 不跨 host）。
   * 这里先开 about:blank，通知门户给目标 host 种 Cookie，再跳转。
   */
  var nativeOpen = window.open
  function crossHost(url) {
    try {
      var target = new URL(String(url), window.location.href)
      return /^https?:$/i.test(target.protocol)
        && target.hostname !== window.location.hostname
        ? target
        : null
    } catch (e) {
      return null
    }
  }
  window.open = function (url, name, features) {
    try {
      if (url && window.parent && window.parent !== window) {
        var target = crossHost(url)
        if (target) {
          // 去掉 noopener/noreferrer 才能拿到窗口引用后续跳转；跳转后再置空 opener
          var safeFeatures = features
            ? String(features).replace(/noopener/gi, '').replace(/noreferrer/gi, '')
            : features
          var win = nativeOpen.call(window, 'about:blank', name || '_blank', safeFeatures)
          if (win) {
            try {
              win.document.write('<p style="font-family:Microsoft YaHei;padding:24px;color:#10233e">正在打开业务页面…</p>')
              win.document.close()
            } catch (e) { /* ignore */ }
            try {
              window.parent.postMessage({ type: 'JUMP_PORTAL_SEED_URL', url: target.href }, '*')
            } catch (e) { /* ignore */ }
            setTimeout(function () {
              try {
                win.location.href = target.href
                win.opener = null
              } catch (e) { /* ignore */ }
            }, 1200)
            return win
          }
        }
      }
    } catch (e) { /* ignore */ }
    return nativeOpen.apply(window, arguments)
  }

  // <a target="_blank"> 不经过 window.open，这里拦截后走上面的逻辑
  document.addEventListener('click', function (e) {
    var el = e.target
    while (el && String(el.tagName || '').toLowerCase() !== 'a') {
      el = el.parentElement
    }
    if (!el || String(el.getAttribute('target') || '') !== '_blank') {
      return
    }
    var href = el.href
    if (!href || !crossHost(href)) {
      return
    }
    e.preventDefault()
    window.open(href, '_blank')
  }, true)
})()
