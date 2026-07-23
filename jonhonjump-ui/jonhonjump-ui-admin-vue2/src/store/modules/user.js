import {login, logout, getInfo, socialLogin, smsLogin} from '@/api/login'
import {setToken, removeToken} from '@/utils/auth'
import { resolveUserAvatar, loadRoleAvatarConfig } from '@/utils/defaultAvatar'

function normalizePermissions(permissions) {
  if (!permissions) {
    return []
  }
  const list = Array.isArray(permissions) ? permissions : Object.values(permissions)
  return list.filter(permission => permission && String(permission).trim())
}

const user = {
  state: {
    id: 0, // 用户编号
    name: '',
    avatar: '',
    rawAvatar: '',
    roles: [],
    permissions: []
  },

  mutations: {
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_NICKNAME: (state, nickname) => {
      state.nickname = nickname
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_RAW_AVATAR: (state, rawAvatar) => {
      state.rawAvatar = rawAvatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const captchaVerification = userInfo.captchaVerification
      const socialCode = userInfo.socialCode
      const socialState = userInfo.socialState
      const socialType = userInfo.socialType
      const loginType = userInfo.loginType || 'auto'
      return new Promise((resolve, reject) => {
        login(username, password, captchaVerification, socialType, socialCode, socialState, loginType).then(res => {
          res = res.data;
          // 新登录清门户会话：首进走星标默认；避免 Token 过期未走 LogOut 时仍按上次系统进
          commit('portal/RESET_PORTAL', null, { root: true })
          setToken(res)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 社交登录
    SocialLogin({ commit }, userInfo) {
      const code = userInfo.code
      const state = userInfo.state
      const type = userInfo.type
      return new Promise((resolve, reject) => {
        socialLogin(type, code, state).then(res => {
          res = res.data;
          commit('portal/RESET_PORTAL', null, { root: true })
          setToken(res)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 短信登录
    SmsLogin({ commit }, userInfo) {
      const mobile = userInfo.mobile.trim()
      const mobileCode = userInfo.mobileCode
      return new Promise((resolve, reject) => {
        smsLogin(mobile,mobileCode).then(res => {
          res = res.data;
          commit('portal/RESET_PORTAL', null, { root: true })
          setToken(res)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    // 获取用户信息（默认轻量：不含主系统菜单树）
    GetInfo({ commit, state }, options = {}) {
      const includeMenus = options && options.includeMenus === true
      const redisOnly = options && options.redisOnly === true
      return new Promise((resolve, reject) => {
        getInfo(includeMenus, redisOnly).then(res => {
          // redisOnly 未命中时 data 可能为 null
          if (!res || res.data == null) {
            if (redisOnly) {
              resolve(null)
              return
            }
            res = {
              data: {
                roles: [],
                menus: [],
                permissions: [],
                user: {
                  id: '',
                  avatar: '',
                  userName: '',
                  nickname: ''
                }
              }
            }
          }

          res = res.data
          const user = res.user || {}
          const roles = res.roles && res.roles.length > 0 ? res.roles : ['ROLE_DEFAULT']
          commit('SET_ROLES', roles)
          commit('SET_PERMISSIONS', normalizePermissions(res.permissions))
          commit('SET_ID', user.id)
          commit('SET_NAME', user.userName || user.username)
          commit('SET_NICKNAME', user.nickname)
          commit('SET_RAW_AVATAR', user.avatar || '')
          commit('SET_AVATAR', resolveUserAvatar(user.avatar, roles))
          resolve(res)
          loadRoleAvatarConfig().then(() => {
            commit('SET_AVATAR', resolveUserAvatar(user.avatar, roles))
          }).catch(() => {})
        }).catch(error => {
          reject(error)
        })
      })
    },

    /**
     * 加载主系统菜单树并注入路由。
     * - redisOnly / background：仅 Redis，未命中返回 null（不打库）
     * - preferRedis：先 Redis，未命中再打库写回（后台懒加载用）
     */
    LoadMainMenus({ dispatch, rootState }, options = {}) {
      const background = !!(options && options.background)
      const redisOnly = background || !!(options && options.redisOnly)
      const preferRedis = !!(options && options.preferRedis)
      if (rootState.permission.defaultRoutes && rootState.permission.defaultRoutes.length) {
        return Promise.resolve(rootState.permission.defaultRoutes)
      }
      const applyMenus = userInfo => {
        if (!userInfo || !userInfo.menus || !userInfo.menus.length) {
          return null
        }
        return dispatch('GenerateRoutes', userInfo.menus).then(accessRoutes => {
          const router = require('@/router').default
          router.addRoutes(accessRoutes)
          dispatch('portal/ensureMainSidebarCached', null, { root: true })
          return accessRoutes
        })
      }
      if (preferRedis) {
        return dispatch('GetInfo', { includeMenus: true, redisOnly: true }).then(cached => {
          const applied = applyMenus(cached)
          if (applied) {
            return applied
          }
          return dispatch('GetInfo', { includeMenus: true, redisOnly: false }).then(applyMenus)
        })
      }
      return dispatch('GetInfo', { includeMenus: true, redisOnly }).then(applyMenus)
    },

    // 退出系统（即使后端失败也清本地登录态，避免现场换人后仍残留 Token）
    LogOut({ commit }) {
      return new Promise((resolve) => {
        logout().finally(() => {
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_ID', 0)
          commit('SET_NAME', '')
          commit('SET_NICKNAME', '')
          commit('portal/RESET_PORTAL', null, { root: true })
          removeToken()
          resolve()
        })
      })
    }
  }
}

export default user
