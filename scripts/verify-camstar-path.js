/**
 * Camstar：iframe 直开业务 http（对齐 4200 meta.link；Camstar 独立不经 4221）
 * node scripts/verify-camstar-path.js
 */
function buildIframeLink(systemUrl, leafPath) {
  const baseUrl = String(systemUrl || '').replace(/\/+$/, '')
  if (/^https?:\/\//i.test(leafPath)) return leafPath
  return baseUrl + '/#/' + String(leafPath || '')
}

const camstar = 'http://192.168.240.127:4200/Process/ProcessManager/ProcessIndex'
const iframe = buildIframeLink('http://192.168.240.127:4221', camstar)
const ok = iframe === camstar && iframe.indexOf('camstar-') < 0 && iframe.indexOf('4221/') < 0
console.log(ok ? 'PASS' : 'FAIL', iframe)
process.exit(ok ? 0 : 1)
