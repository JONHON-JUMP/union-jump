/**
 * 自检：Camstar / 若依二分不能互踩（发版前本地跑）
 * node scripts/check-portal-menu-kind.js
 */
const path = require('path')

// 与 portalMenuKind.js 保持同规则的纯 JS 副本，避免 ESM 加载问题
function isCamstarLikeUrl(url) {
  const s = String(url || '')
  return /:4200\b/i.test(s) || /\/4200\//i.test(s) || /camstarportal/i.test(s) || /\/camstar\//i.test(s)
}
function isRuoyiComponent(component) {
  const c = String(component || '').trim()
  if (!c) return false
  const lower = c.toLowerCase()
  if (lower === 'innerlink' || lower.includes('empty') || lower.includes('portal/')) return false
  return true
}
function isPureHttpUrl(url) {
  const s = String(url || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}
function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}
function classifyPortalMenu({ path, component, link } = {}) {
  if (isRuoyiComponent(component)) return 'ruoyi'
  const p = String(path || '')
  const l = String(link || '')
  if (l.indexOf('/#/') >= 0 || (l.indexOf('#') >= 0 && !isPureHttpUrl(l))) return 'ruoyi'
  if (isPureHttpUrl(p) || isPureHttpUrl(l)) return 'camstar'
  if (isExternal(p) || isCamstarLikeUrl(p) || isCamstarLikeUrl(l)) return 'camstar'
  return 'ruoyi'
}

const cases = [
  {
    name: 'Camstar http 路由',
    input: { path: 'http://192.168.240.127:4200/WorkOrder/x', component: '' },
    expect: 'camstar'
  },
  {
    name: '若依有组件路径',
    input: { path: 'system/user', component: 'system/user/index' },
    expect: 'ruoyi'
  },
  {
    name: '若依有组件即使 path 像 http 也不走 Camstar',
    input: { path: 'http://evil', component: 'system/user/index' },
    expect: 'ruoyi'
  },
  {
    name: '若依 hash link',
    input: { path: 'system/user', link: 'http://192.168.240.129:8088/mes4200/#/system/user' },
    expect: 'ruoyi'
  },
  {
    name: '相对路由无组件 → 若依',
    input: { path: 'system/dept', component: '' },
    expect: 'ruoyi'
  },
  {
    name: 'Camstar 已解套 link',
    input: { path: '192/168/240/127/4200/WorkOrder/x', link: 'http://192.168.240.127:4200/WorkOrder/x' },
    expect: 'camstar'
  }
]

let failed = 0
cases.forEach(c => {
  const got = classifyPortalMenu(c.input)
  const ok = got === c.expect
  if (!ok) {
    failed += 1
    console.error('FAIL', c.name, 'expect', c.expect, 'got', got)
  } else {
    console.log('OK  ', c.name)
  }
})
if (failed) {
  console.error('\n' + failed + ' failed')
  process.exit(1)
}
console.log('\nall ' + cases.length + ' passed')
