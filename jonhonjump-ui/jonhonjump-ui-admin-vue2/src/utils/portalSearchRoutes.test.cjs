const assert = require('assert')
const { resolveCurrentSystemSearchRoutes, resolveCurrentSystemLabel } = require('./portalSearchRoutes')

const mainRoutes = [
  { path: '/login', hidden: true },
  { path: '/system', hidden: false, meta: { title: '系统管理' }, children: [] },
  { path: '/portal/mes/plan', hidden: false, meta: { title: 'MES计划', clientId: 'mes' } }
]

const mesRoutes = [
  { path: '/portal/mes/plan', hidden: false, meta: { title: '生产计划', clientId: 'mes' } },
  { path: '/portal/scada/board', hidden: false, meta: { title: 'SCADA看板', clientId: 'scada' } }
]

const state = {
  portal: {
    currentSystem: 'main',
    mainSidebarRouters: mainRoutes,
    subSystemSidebarCache: {
      mes: mesRoutes
    },
    systemList: [
      { clientId: 'mes', systemName: 'MES车间' }
    ]
  },
  permission: {
    sidebarRouters: mainRoutes
  }
}

assert.deepEqual(
  resolveCurrentSystemSearchRoutes(state).map(route => route.path),
  ['/system']
)

state.portal.currentSystem = 'mes'
state.permission.sidebarRouters = mesRoutes

assert.deepEqual(
  resolveCurrentSystemSearchRoutes(state).map(route => route.path),
  ['/portal/mes/plan']
)

assert.equal(resolveCurrentSystemLabel(state), 'MES车间')

state.portal.currentSystem = 'main'
assert.equal(resolveCurrentSystemLabel(state), 'JUMP 主系统')

console.log('portalSearchRoutes tests passed')
