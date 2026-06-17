/**
 * 门户 Vue2 使用 src/assets/icons/svg 雪碧图图标。
 * 将 ep:/fa: 前缀、空值、# 等统一为可展示的 svg 图标名。
 */

const EP_ICON_MAP = {
  'ep:tools': 'system',
  'ep:setting': 'system',
  'ep:monitor': 'monitor',
  'ep:avatar': 'user',
  'ep:user': 'peoples',
  'ep:menu': 'tree-table',
  'ep:collection': 'dict',
  'ep:takeaway-box': 'message',
  'ep:document-copy': 'log',
  'ep:coffee-cup': 'server',
  'ep:connection': 'link',
  'ep:position': 'form',
  'ep:promotion': 'logininfor'
}

const FA_ICON_MAP = {
  'fa:address-book-o': 'post',
  'fa:address-card': 'tree',
  'fa:connectdevelop': 'tool',
  'fa:key': 'lock',
  'fa:wpforms': 'form',
  'fa:fighter-jet': 'swagger',
  'fa:reddit-square': 'redis',
  'fa:road': 'guide',
  'fa-solid:tasks': 'job',
  'fa-solid:box': 'server'
}

const NAME_ICON_MAP = {
  '用户管理': 'user',
  '角色管理': 'peoples',
  '菜单管理': 'tree-table',
  '部门管理': 'tree',
  '岗位管理': 'post',
  '班组管理': 'peoples',
  '字典管理': 'dict',
  '系统管理': 'system',
  '系统监控': 'monitor',
  '数据监控': 'online',
  '日志管理': 'log',
  '操作日志': 'form',
  '登录日志': 'logininfor',
  '服务监控': 'server',
  '定时任务': 'job',
  '缓存监控': 'redis',
  '进程列表': 'redis',
  '端口监控': 'monitor',
  '系统工具': 'tool',
  '系统接口': 'swagger',
  '表单构建': 'form',
  '代码生成': 'code',
  '通知公告': 'message',
  '审计日志': 'log',
  '令牌管理': 'lock',
  '配置管理': 'edit',
  '外部系统管理': 'link',
  '文件管理': 'documentation',
  '加工数据查询': 'documentation',
  '加工数据': 'online',
  '运行数据': 'druid',
  '报警数据': 'message',
  '版本管理': 'code',
  '存储管理': 'server',
  '南向设备': 'server',
  '北向应用': 'link',
  '首页': 'dashboard',
  '工作台': 'dashboard'
}

const PATH_ICON_MAP = {
  user: 'user',
  role: 'peoples',
  menu: 'tree-table',
  dept: 'tree',
  post: 'post',
  team: 'peoples',
  dict: 'dict',
  system: 'system',
  monitor: 'monitor',
  job: 'job',
  log: 'log',
  operlog: 'form',
  logininfor: 'logininfor',
  server: 'server',
  cache: 'redis',
  swagger: 'swagger',
  build: 'form',
  gen: 'code',
  codegen: 'code',
  notice: 'message',
  config: 'edit',
  token: 'lock',
  redis: 'redis',
  druid: 'monitor',
  tool: 'tool',
  management: 'documentation',
  process: 'documentation',
  home: 'dashboard',
  index: 'dashboard',
  emqx: 'server',
  north: 'link',
  program: 'code',
  design: 'server',
  cacheList: 'list',
  file: 'code'
}

function isBlankIcon(icon) {
  return !icon || icon === '#' || icon === '-'
}

function resolvePathKey(path) {
  if (!path) return ''
  const segment = String(path).replace(/^\/+/, '').split('/').filter(Boolean).pop()
  return segment || ''
}

export function resolvePortalMenuIcon(icon, { name, path } = {}) {
  if (typeof icon === 'string' && icon.startsWith('el-icon')) {
    return { svgIcon: null, icon, hasIcon: true }
  }

  const trimmed = typeof icon === 'string' ? icon.trim() : ''
  if (!isBlankIcon(trimmed)) {
    if (EP_ICON_MAP[trimmed]) {
      return { svgIcon: EP_ICON_MAP[trimmed], icon: null, hasIcon: true }
    }
    if (FA_ICON_MAP[trimmed]) {
      return { svgIcon: FA_ICON_MAP[trimmed], icon: null, hasIcon: true }
    }
    if (!trimmed.includes(':')) {
      return { svgIcon: trimmed, icon: null, hasIcon: true }
    }
  }

  if (name && NAME_ICON_MAP[name]) {
    return { svgIcon: NAME_ICON_MAP[name], icon: null, hasIcon: true }
  }

  const pathKey = resolvePathKey(path)
  if (pathKey && PATH_ICON_MAP[pathKey]) {
    return { svgIcon: PATH_ICON_MAP[pathKey], icon: null, hasIcon: true }
  }

  return { svgIcon: 'component', icon: null, hasIcon: true }
}
