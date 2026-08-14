/**
 * 预置静态头像：扫描 src/assets/images/avatar 目录（构建时打包）。
 * 数据库存储格式：static:文件名（不含扩展名），如 static:super_admin
 */
export const STATIC_AVATAR_PREFIX = 'static:'

const avatarContext = require.context('@/assets/images/avatar', false, /\.(png|jpe?g|gif|webp)$/i)

function parseFileName(key) {
  return key.replace(/^\.\//, '').replace(/\.(png|jpe?g|gif|webp)$/i, '')
}

const staticUrlMap = {}
avatarContext.keys().forEach(key => {
  const name = parseFileName(key)
  staticUrlMap[`${STATIC_AVATAR_PREFIX}${name}`] = avatarContext(key)
})

/** 可选静态头像列表（配置页、展示用） */
export function getStaticAvatarOptions() {
  return avatarContext.keys()
    .sort()
    .map(key => {
      const name = parseFileName(key)
      return {
        name,
        url: avatarContext(key),
        value: `${STATIC_AVATAR_PREFIX}${name}`
      }
    })
}

/** 将 DB 中的 avatarUrl 转为可展示的地址 */
export function resolveAvatarUrl(avatarUrl) {
  if (!avatarUrl) {
    return ''
  }
  const value = String(avatarUrl).trim()
  if (value.startsWith(STATIC_AVATAR_PREFIX)) {
    return staticUrlMap[value] || ''
  }
  return value
}

export function isStaticAvatarValue(avatarUrl) {
  return !!(avatarUrl && String(avatarUrl).startsWith(STATIC_AVATAR_PREFIX))
}

export function getStaticAvatarUrl(name) {
  return staticUrlMap[`${STATIC_AVATAR_PREFIX}${name}`] || ''
}
