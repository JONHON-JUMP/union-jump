import { getInfo } from '@/api/login'
import { getMyPortalMenus } from '@/api/system/subSystemUsers'

function normalizeManualUrl(manualUrl) {
  if (!manualUrl) {
    return null
  }
  const url = String(manualUrl).split(',')[0].trim()
  return url || null
}

export function findMenuManualUrlInTree(menus, menuId) {
  const targetId = Number(menuId)
  if (!Array.isArray(menus) || Number.isNaN(targetId)) {
    return null
  }
  for (const menu of menus) {
    if (!menu) {
      continue
    }
    if (Number(menu.id) === targetId) {
      const url = normalizeManualUrl(menu.manualUrl)
      if (url) {
        return url
      }
    }
    if (menu.children && menu.children.length) {
      const nested = findMenuManualUrlInTree(menu.children, targetId)
      if (nested) {
        return nested
      }
    }
  }
  return null
}

export async function fetchMenuManualUrl({ menuId, currentSystem, subSystemId }) {
  if (menuId == null || String(menuId).startsWith('external-')) {
    return null
  }
  if (currentSystem === 'main' || !subSystemId) {
    const res = await getInfo(true)
    return findMenuManualUrlInTree(res.data && res.data.menus, menuId)
  }
  const res = await getMyPortalMenus(subSystemId)
  return findMenuManualUrlInTree(res.data, menuId)
}

function resolveManualDownloadUrl(manualUrl) {
  const url = normalizeManualUrl(manualUrl)
  if (!url) {
    return null
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  const base = (process.env.VUE_APP_BASE_API || '').replace(/\/+$/, '')
  return `${base}${url.startsWith('/') ? '' : '/'}${url}`
}

function resolveManualFileName(manualUrl, menuName) {
  const url = normalizeManualUrl(manualUrl)
  if (!url) {
    return menuName ? `${menuName}说明书` : '说明书'
  }
  try {
    const pathname = new URL(url, window.location.origin).pathname
    const base = pathname.split('/').pop()
    if (base) {
      return decodeURIComponent(base.split('?')[0])
    }
  } catch (e) {
    const parts = url.split('/')
    const last = parts[parts.length - 1]
    if (last) {
      return decodeURIComponent(last.split('?')[0])
    }
  }
  return menuName ? `${menuName}说明书` : '说明书'
}

/** 触发浏览器下载菜单说明书 */
export function downloadMenuManual(manualUrl, menuName) {
  const url = resolveManualDownloadUrl(manualUrl)
  if (!url) {
    return false
  }
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.setAttribute('download', resolveManualFileName(url, menuName))
  link.setAttribute('target', '_blank')
  link.setAttribute('rel', 'noopener noreferrer')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  return true
}
