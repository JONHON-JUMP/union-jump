import { getAccessToken } from '@/utils/auth'

const ACTIVITY_EVENTS = ['mousedown', 'mousemove', 'keydown', 'touchstart', 'scroll', 'click']

let idleTimer = null
let onLockCallback = null
let idleTimeoutMs = 0

function clearIdleTimer() {
  if (idleTimer) {
    clearTimeout(idleTimer)
    idleTimer = null
  }
}

function scheduleIdleTimer() {
  clearIdleTimer()
  if (!getAccessToken() || !onLockCallback || idleTimeoutMs <= 0) {
    return
  }
  idleTimer = setTimeout(() => {
    onLockCallback()
  }, idleTimeoutMs)
}

function handleActivity() {
  scheduleIdleTimer()
}

export function initSessionGuard(onLock, timeoutMs) {
  destroySessionGuard()
  onLockCallback = onLock
  idleTimeoutMs = Number(timeoutMs) || 0
  if (idleTimeoutMs <= 0) {
    return
  }
  ACTIVITY_EVENTS.forEach(event => {
    document.addEventListener(event, handleActivity, { passive: true })
  })
  scheduleIdleTimer()
}

export function destroySessionGuard() {
  ACTIVITY_EVENTS.forEach(event => {
    document.removeEventListener(event, handleActivity)
  })
  clearIdleTimer()
  onLockCallback = null
  idleTimeoutMs = 0
}

export function resetSessionGuardTimer() {
  scheduleIdleTimer()
}

export function pauseSessionGuard() {
  clearIdleTimer()
}

export function resumeSessionGuard() {
  scheduleIdleTimer()
}
