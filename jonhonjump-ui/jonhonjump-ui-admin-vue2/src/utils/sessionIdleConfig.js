import { getConfigKey } from '@/api/infra/config'

/** 参数配置键：系统管理 → 基础设施 → 参数配置 */
export const SESSION_IDLE_TIMEOUT_CONFIG_KEY = 'system.auth.session-idle-timeout-minutes'

/** 未配置或读取失败时的默认值（分钟） */
export const DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES = 60

const MIN_MINUTES = 0
const MAX_MINUTES = 480

let cachedIdleTimeoutMinutes = DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES
let loadingPromise = null

function normalizeMinutes(value) {
  if (value === null || value === undefined || value === '') {
    return DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES
  }
  const minutes = parseInt(String(value).trim(), 10)
  if (!Number.isFinite(minutes) || minutes < MIN_MINUTES) {
    return DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES
  }
  return Math.min(minutes, MAX_MINUTES)
}

export function getSessionIdleTimeoutMinutes() {
  return cachedIdleTimeoutMinutes
}

export function getSessionIdleTimeoutMs() {
  if (cachedIdleTimeoutMinutes <= 0) {
    return 0
  }
  return cachedIdleTimeoutMinutes * 60 * 1000
}

/** 从后台参数配置加载空闲锁屏时长（分钟）。0 表示关闭锁屏。 */
export function loadSessionIdleTimeoutConfig(force = false) {
  if (!force && loadingPromise) {
    return loadingPromise
  }
  loadingPromise = getConfigKey(SESSION_IDLE_TIMEOUT_CONFIG_KEY)
    .then(res => {
      cachedIdleTimeoutMinutes = normalizeMinutes(res.data)
      return getSessionIdleTimeoutMs()
    })
    .catch(() => {
      cachedIdleTimeoutMinutes = DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES
      return getSessionIdleTimeoutMs()
    })
    .finally(() => {
      loadingPromise = null
    })
  return loadingPromise
}

export function clearSessionIdleTimeoutCache() {
  cachedIdleTimeoutMinutes = DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES
  loadingPromise = null
}
