/**
 * 系统默认头像：优先从后台「角色头像」配置加载，失败时使用本地兜底资源。
 */
import { getRoleAvatarSimpleList } from '@/api/system/roleAvatar'
import { getStaticAvatarUrl, resolveAvatarUrl } from '@/utils/staticAvatar'

export const SYSTEM_AVATAR_PREFIX = 'system:'

/** 本地兜底（后台未配置或未加载时使用） */
const FALLBACK_PRIORITY = [
  'super_admin',
  'dept_leader',
  'common'
]

const FALLBACK_MAP = Object.fromEntries(
  FALLBACK_PRIORITY.map(code => [code, getStaticAvatarUrl(code)]).filter(([, url]) => url)
)

const FALLBACK_LABELS = {
  super_admin: '超级管理员',
  dept_leader: '部门领导',
  common: '普通角色'
}

const FALLBACK_DEFAULT = FALLBACK_MAP.common || Object.values(FALLBACK_MAP)[0] || ''

/** @type {{ priority: string[], map: Record<string, string>, labels: Record<string, string> } | null} */
let cachedConfig = null

function buildConfigFromList(list) {
  if (!list || !list.length) {
    return null
  }
  const sorted = [...list].sort((a, b) => {
    const sortDiff = (a.sort || 0) - (b.sort || 0)
    return sortDiff !== 0 ? sortDiff : String(a.roleCode).localeCompare(String(b.roleCode))
  })
  return {
    priority: sorted.map(item => item.roleCode),
    map: Object.fromEntries(sorted.map(item => [item.roleCode, resolveAvatarUrl(item.avatarUrl)])),
    labels: Object.fromEntries(sorted.map(item => [item.roleCode, item.roleName || item.roleCode]))
  }
}

export async function loadRoleAvatarConfig() {
  try {
    const res = await getRoleAvatarSimpleList()
    cachedConfig = buildConfigFromList(res && res.data ? res.data : [])
  } catch (e) {
    cachedConfig = null
  }
  return cachedConfig
}

export function clearRoleAvatarConfig() {
  cachedConfig = null
}

function getPriority() {
  return cachedConfig?.priority?.length ? cachedConfig.priority : FALLBACK_PRIORITY
}

function getMap() {
  return cachedConfig?.map && Object.keys(cachedConfig.map).length ? cachedConfig.map : FALLBACK_MAP
}

function getLabels() {
  return cachedConfig?.labels && Object.keys(cachedConfig.labels).length ? cachedConfig.labels : FALLBACK_LABELS
}

function normalizeRoles(roles) {
  if (!roles) {
    return []
  }
  if (Array.isArray(roles)) {
    return roles
  }
  if (roles instanceof Set) {
    return [...roles]
  }
  return Object.values(roles)
}

/** 多角色时取优先级最高的角色 code */
export function resolvePrimaryRoleCode(roles) {
  const roleList = normalizeRoles(roles)
  const priority = getPriority()
  const map = getMap()
  for (const role of priority) {
    if (roleList.includes(role)) {
      return role
    }
  }
  for (const role of roleList) {
    if (map[role]) {
      return role
    }
  }
  return priority[0] || 'common'
}

export function resolveDefaultAvatar(roles) {
  const roleCode = resolvePrimaryRoleCode(roles)
  const map = getMap()
  return map[roleCode] || FALLBACK_DEFAULT
}

export function parseAvatarSource(userAvatar) {
  if (!userAvatar || !String(userAvatar).trim()) {
    return { type: 'auto', value: '' }
  }
  const value = String(userAvatar).trim()
  if (value.startsWith(SYSTEM_AVATAR_PREFIX)) {
    const roleCode = value.slice(SYSTEM_AVATAR_PREFIX.length)
    return { type: 'system', value, roleCode }
  }
  return { type: 'custom', value }
}

export function buildSystemAvatarValue(roleCode) {
  return `${SYSTEM_AVATAR_PREFIX}${roleCode}`
}

export function getSystemAvatarOptions(roles) {
  const roleList = normalizeRoles(roles)
  const priority = getPriority()
  const map = getMap()
  const labels = getLabels()
  const options = []
  for (const code of priority) {
    if (roleList.includes(code) && map[code]) {
      options.push({
        code,
        label: labels[code] || code,
        url: map[code],
        isDefault: options.length === 0
      })
    }
  }
  for (const code of roleList) {
    if (!priority.includes(code) && map[code]) {
      options.push({
        code,
        label: labels[code] || code,
        url: map[code],
        isDefault: options.length === 0
      })
    }
  }
  return options
}

export function resolveUserAvatar(userAvatar, roles) {
  const source = parseAvatarSource(userAvatar)
  const map = getMap()
  if (source.type === 'auto') {
    return resolveDefaultAvatar(roles)
  }
  if (source.type === 'system') {
    return map[source.roleCode] || resolveDefaultAvatar(roles)
  }
  return source.value
}

/** @deprecated 使用 getLabels() 内部逻辑，保留兼容 */
export const ROLE_AVATAR_LABELS = new Proxy({}, {
  get(target, prop) {
    return getLabels()[prop]
  }
})

export function getRoleAvatarLabel(roleCode) {
  const labels = getLabels()
  return labels[roleCode] || roleCode
}
