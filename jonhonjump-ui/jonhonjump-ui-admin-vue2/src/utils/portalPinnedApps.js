import {
  getPortalPinnedApps,
  savePortalPinnedApps
} from '@/api/portal/pinnedApps'

const STORAGE_KEY = 'jump-portal-pinned-apps'
const REMOTE_API_ENABLED = process.env.VUE_APP_PORTAL_PINNED_APPS_API === 'true'

function normalizePinnedApps(apps) {
  const normalizedApps = []
  const paths = new Set()

  ;(apps || []).forEach(app => {
    if (!app || !app.path || !app.name || paths.has(app.path)) return
    paths.add(app.path)
    normalizedApps.push({
      name: app.name,
      path: app.path,
      icon: app.icon || 'component',
      group: app.group || '授权应用'
    })
  })

  return normalizedApps
}

export function getCachedPinnedApps() {
  try {
    const apps = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(apps) ? normalizePinnedApps(apps) : []
  } catch (error) {
    return []
  }
}

function cachePinnedApps(apps) {
  const normalizedApps = normalizePinnedApps(apps)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizedApps))
  return normalizedApps
}

export async function loadPinnedApps() {
  if (!REMOTE_API_ENABLED) return getCachedPinnedApps()

  try {
    const response = await getPortalPinnedApps()
    return cachePinnedApps(response.data || [])
  } catch (error) {
    return getCachedPinnedApps()
  }
}

export async function setPinnedApps(apps) {
  const normalizedApps = normalizePinnedApps(apps)
  if (REMOTE_API_ENABLED) {
    await savePortalPinnedApps(normalizedApps)
  }
  return cachePinnedApps(normalizedApps)
}

export async function togglePinnedApp(app, currentApps) {
  const apps = normalizePinnedApps(currentApps || getCachedPinnedApps())
  const index = apps.findIndex(item => item.path === app.path)
  if (index > -1) {
    apps.splice(index, 1)
  } else {
    apps.push(app)
  }
  return await setPinnedApps(apps)
}
