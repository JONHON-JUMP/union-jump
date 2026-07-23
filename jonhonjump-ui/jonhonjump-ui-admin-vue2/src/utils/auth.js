const AccessTokenKey = 'ACCESS_TOKEN'
const RefreshTokenKey = 'REFRESH_TOKEN'

/** 现场模式：Token 仅存 sessionStorage，关闭浏览器即失效 */
const tokenStorage = sessionStorage

// 清理旧版 localStorage 中的持久化登录态
;[AccessTokenKey, RefreshTokenKey, 'PASSWORD', 'REMEMBER_ME'].forEach(key => {
  localStorage.removeItem(key)
})

// ========== Token 相关 ==========

export function getAccessToken() {
  return tokenStorage.getItem(AccessTokenKey)
}

export function getRefreshToken() {
  return tokenStorage.getItem(RefreshTokenKey)
}

export function setToken(token) {
  tokenStorage.setItem(AccessTokenKey, token.accessToken)
  tokenStorage.setItem(RefreshTokenKey, token.refreshToken)
}

export function removeToken() {
  tokenStorage.removeItem(AccessTokenKey)
  tokenStorage.removeItem(RefreshTokenKey)
}

// ========== 账号相关（现场模式不记住密码） ==========

const UsernameKey = 'USERNAME'
const PasswordKey = 'PASSWORD'
const RememberMeKey = 'REMEMBER_ME'
const LoginTypeKey = 'LOGIN_TYPE'

export function getUsername() {
  return sessionStorage.getItem(UsernameKey)
}

export function setUsername(username) {
  sessionStorage.setItem(UsernameKey, username)
}

export function removeUsername() {
  sessionStorage.removeItem(UsernameKey)
}

export function getPassword() {
  return undefined
}

export function setPassword() {
  // 现场模式不保存密码
}

export function removePassword() {
  sessionStorage.removeItem(PasswordKey)
  localStorage.removeItem(PasswordKey)
}

export function getRememberMe() {
  return false
}

export function setRememberMe() {
  // 现场模式不支持记住我
}

export function removeRememberMe() {
  sessionStorage.removeItem(RememberMeKey)
  localStorage.removeItem(RememberMeKey)
}

export function getLoginType() {
  return sessionStorage.getItem(LoginTypeKey)
}

export function setLoginType(loginType) {
  sessionStorage.setItem(LoginTypeKey, loginType)
}

export function removeLoginType() {
  sessionStorage.removeItem(LoginTypeKey)
}

// ========== 租户相关 ==========

const TenantIdKey = 'TENANT_ID'
const TenantNameKey = 'TENANT_NAME'
const VisitTenantIdKey = 'VISIT_TENANT_ID'

export function getTenantName() {
  return sessionStorage.getItem(TenantNameKey)
}

export function setTenantName(username) {
  sessionStorage.setItem(TenantNameKey, username)
}

export function removeTenantName() {
  sessionStorage.removeItem(TenantNameKey)
}

export function getTenantId() {
  return sessionStorage.getItem(TenantIdKey)
}

export function setTenantId(username) {
  sessionStorage.setItem(TenantIdKey, username)
}

export function removeTenantId() {
  sessionStorage.removeItem(TenantIdKey)
}

export function getVisitTenantId() {
  return sessionStorage.getItem(VisitTenantIdKey)
}

export function setVisitTenantId(tenantId) {
  sessionStorage.setItem(VisitTenantIdKey, tenantId)
}

export function removeVisitTenantId() {
  sessionStorage.removeItem(VisitTenantIdKey)
}
