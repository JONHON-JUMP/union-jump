/**
 * Camstar 预热：只做本机 Cookie + 源站探活。
 * 对齐 4200：不预挂整页、不阻塞等待跨机种 bridge。
 * 仅 4200 / CamstarPortal 才预热源站。
 * 接口平台等「完整 http 路由、走 Camstar 直开」的系统不要预热前 6 个叶子，
 * 否则每次打开任一页面都会把用户/角色/菜单/部门/岗位/字典都探活一遍。
 */
import { ensureLocalCamstarCookie } from '@/utils/camstarCookie'
import { isCamstarLikeUrl } from '@/utils/portalMenuKind'

const warmedOrigins = {}
const prefetchedClients = {}

function isCamstarEntry(entry) {
  if (!entry || !entry.link) {
    return false
  }
  const link = String(entry.link)
  if (link.indexOf('/#/') >= 0 || (link.indexOf('#') >= 0 && !/^https?:\/\/[^#]+$/.test(link))) {
    return false
  }
  return isCamstarLikeUrl(link)
}

export function collectCamstarPrefetchEntries(pathLinkMap, limit = 6, clientId) {
  if (clientId && prefetchedClients[clientId]) {
    return []
  }
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

export function prepareCamstarSessionFromEntries(entries, clientId) {
  ensureLocalCamstarCookie()
  const list = entries || []
  list.forEach(item => {
    if (item && item.link) {
      warmCamstarOrigin(item.link)
    }
  })
  if (clientId && list.length) {
    prefetchedClients[clientId] = true
  }
  return Promise.resolve(list.length)
}
