/**
 * Camstar 预热：只做本机 Cookie + 源站探活。
 * 对齐 4200：不预挂整页、不阻塞等待跨机种 bridge。
 */
import { ensureLocalCamstarCookie } from '@/utils/camstarCookie'

const warmedOrigins = {}

function isCamstarEntry(entry) {
  if (!entry || !entry.link) {
    return false
  }
  if (entry.kind === 'camstar') {
    return true
  }
  const link = String(entry.link)
  return /^https?:\/\//i.test(link) && link.indexOf('/#/') < 0 && link.indexOf('#') < 0
}

export function collectCamstarPrefetchEntries(pathLinkMap, limit = 6) {
  const byLink = {}
  Object.keys(pathLinkMap || {}).forEach(path => {
    if (!path || path.indexOf('/portal/') !== 0) {
      return
    }
    if (!path.endsWith('/index') && pathLinkMap[`${path}/index`]) {
      return
    }
    const entry = pathLinkMap[path]
    if (!isCamstarEntry(entry)) {
      return
    }
    const link = String(entry.link).replace(/\/+$/, '')
    const prev = byLink[link]
    if (!prev || (path.endsWith('/index') && !prev.path.endsWith('/index'))) {
      byLink[link] = {
        path,
        title: entry.title || entry.menuTitle || 'Camstar',
        link: entry.link,
        meta: {
          link: entry.link,
          title: entry.title || entry.menuTitle,
          menuTitle: entry.menuTitle || entry.title,
          portalKind: 'camstar',
          icon: entry.icon
        }
      }
    }
  })
  return Object.keys(byLink).map(k => byLink[k]).slice(0, Math.max(0, limit))
}

export function warmCamstarOrigin(httpUrl) {
  ensureLocalCamstarCookie()
  let origin = ''
  try {
    origin = new URL(httpUrl, window.location.href).origin
  } catch (e) {
    return
  }
  if (!origin || warmedOrigins[origin]) {
    return
  }
  warmedOrigins[origin] = true
  try {
    const img = new Image()
    img.referrerPolicy = 'no-referrer-when-downgrade'
    img.src = `${origin}/?_jump_camstar_warm=${Date.now()}`
  } catch (e) { /* ignore */ }
}

export function prepareCamstarSessionFromEntries(entries) {
  ensureLocalCamstarCookie()
  const list = entries || []
  list.forEach(item => {
    if (item && item.link) {
      warmCamstarOrigin(item.link)
    }
  })
  return Promise.resolve(list.length)
}
