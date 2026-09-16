/**
 * 自检：打开页只认路由地址（发版前本地跑）
 * node scripts/check-portal-menu-kind.js
 */

function isCamstarLikeUrl(url) {
  const s = String(url || '')
  return /:4200\b/i.test(s) || /\/4200\//i.test(s) || /camstarportal/i.test(s) || /\/camstar\//i.test(s)
}
function isHttpUrl(url) {
  return /^https?:\/\//i.test(String(url || ''))
}
function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}
function classifyPortalMenu({ path, link } = {}) {
  const p = String(path || '')
  const l = String(link || '')
  if (isHttpUrl(p) || isHttpUrl(l) || isExternal(p) || isCamstarLikeUrl(p) || isCamstarLikeUrl(l)) {
    return 'camstar'
  }
  return 'ruoyi'
}

const cases = [
  {
    name: '完整 http 路由',
    input: { path: 'http://192.168.240.127:4200/WorkOrder/x', component: '' },
    expect: 'camstar'
  },
  {
    name: '完整 http 即使填了组件路径也只认路由地址',
    input: { path: 'http://192.168.240.129:9100/system/user', component: 'system/user/index' },
    expect: 'camstar'
  },
  {
    name: '完整 http 即使填了组件名称也只认路由地址',
    input: { path: 'http://192.168.240.129:9100/system/role', component: 'system/role/index', link: '' },
    expect: 'camstar'
  },
  {
    name: '路由地址带 hash 仍按该 URL 打开',
    input: { path: 'http://192.168.240.129:9100/#/system/user' },
    expect: 'camstar'
  },
  {
    name: 'Camstar 已解套 link',
    input: { path: '192/168/240/127/4200/WorkOrder/x', link: 'http://192.168.240.127:4200/WorkOrder/x' },
    expect: 'camstar'
  },
  {
    name: '相对路由无 http 才不是直开',
    input: { path: 'system/dept', component: 'system/dept/index' },
    expect: 'ruoyi'
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
