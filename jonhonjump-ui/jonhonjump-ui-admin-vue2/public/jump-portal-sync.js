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
})()
