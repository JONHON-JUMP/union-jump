import axios from 'axios'
import { Message } from 'element-ui'

// 公共查询与后台会话隔离：不发送 token、租户或跨域 Cookie，也不触发自动重登。
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API + '/admin-api/',
  timeout: 30000,
  withCredentials: false,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

service.interceptors.response.use(response => {
  const data = response.data
  if (data && (data.code === 0 || data.code === 200)) return data
  const message = (data && data.msg) || '工艺查询失败，请稍后重试'
  Message.error(message)
  return Promise.reject(new Error(message))
}, error => {
  const data = error.response && error.response.data
  Message.error((data && data.msg) || '工艺服务连接失败，请稍后重试')
  return Promise.reject(error)
})

export default service
