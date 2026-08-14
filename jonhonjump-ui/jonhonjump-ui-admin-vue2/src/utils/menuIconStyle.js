import { getMenuStyleDefault, menuIconRadius } from '@/api/system/menuStyle'

const FALLBACK_STYLE = {
  color: '#3A71A8',
  shape: 'rounded'
}

let cachedDefault = null

export function setMenuStyleDefault(style) {
  cachedDefault = style || null
}

export function getCachedMenuStyleDefault() {
  return cachedDefault
}

export async function loadMenuStyleDefault() {
  try {
    const res = await getMenuStyleDefault()
    cachedDefault = (res && res.data) ? res.data : null
  } catch (e) {
    cachedDefault = null
  }
  return cachedDefault
}

export function resolveMenuColors(source = {}) {
  const defaults = cachedDefault || FALLBACK_STYLE
  return {
    color: source.color || defaults.color,
    shape: source.shape || defaults.shape || 'rounded'
  }
}

export function buildIconStyle(source = {}) {
  const { color, shape } = resolveMenuColors(source)
  const primary = color || FALLBACK_STYLE.color
  return {
    background: primary,
    borderRadius: menuIconRadius(shape),
    color: '#fff'
  }
}

export function buildMenuStylePreview(source = {}) {
  return buildIconStyle(source)
}
