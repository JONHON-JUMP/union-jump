const assert = require('assert')
const {
  normalizeMenuTree,
  searchMenus,
  folderPreviewStyle,
  resolveMenuPath
} = require('./allAppsMenu')

function route(path, title, options = {}) {
  return {
    path,
    hidden: options.hidden,
    redirect: options.redirect,
    meta: title ? { title, icon: options.icon } : undefined,
    children: options.children
  }
}

const deepLeaves = Array.from({ length: 12 }, (_, index) =>
  route(`item-${index + 1}`, `三级菜单 ${index + 1}`)
)

const routes = [
  {
    path: '/system',
    meta: { title: '系统管理', icon: 'system' },
    children: [
      route('users', '用户管理', { icon: 'user' }),
      {
        path: 'tools',
        meta: { title: '开发工具' },
        children: deepLeaves
      },
      {
        path: 'layout',
        children: [
          route('promoted', '提升菜单'),
          route('secret', '隐藏菜单', { hidden: true })
        ]
      },
      route('https://example.com/docs', '外部文档'),
      route('/absolute/report', '绝对报表'),
      route('hidden', '隐藏项', { hidden: true }),
      route('*', '通配页'),
      route('/404', '错误页'),
      route('/index', '首页')
    ]
  }
]

const groups = normalizeMenuTree(routes)

assert.equal(groups.length, 1)
assert.equal(groups[0].name, '系统管理')
assert.equal(groups[0].icon, 'system')
assert.equal(groups[0].key, 'group:/system:系统管理')

const users = groups[0].children.find(item => item.name === '用户管理')
assert.deepEqual(users, {
  type: 'leaf',
  key: 'leaf:/system/users:用户管理',
  name: '用户管理',
  icon: 'user',
  path: '/system/users'
})

const tools = groups[0].children.find(item => item.name === '开发工具')
assert.equal(tools.type, 'folder')
assert.equal(tools.icon, 'component')
assert.equal(tools.key, 'folder:/system/tools:开发工具')
assert.equal(tools.children.length, 12)
assert.deepEqual(tools.children[11], {
  type: 'leaf',
  key: 'leaf:/system/tools/item-12:三级菜单 12',
  name: '三级菜单 12',
  icon: 'component',
  path: '/system/tools/item-12'
})

assert.equal(groups[0].children.some(item => item.name === '隐藏项'), false)
assert.equal(groups[0].children.some(item => item.name === '隐藏菜单'), false)
assert.equal(groups[0].children.some(item => item.name === '通配页'), false)
assert.equal(groups[0].children.some(item => item.name === '错误页'), false)
assert.equal(groups[0].children.some(item => item.name === '首页'), false)

const promoted = groups[0].children.find(item => item.name === '提升菜单')
assert.equal(promoted.path, '/system/layout/promoted')

const external = groups[0].children.find(item => item.name === '外部文档')
assert.equal(external.path, 'https://example.com/docs')
assert.equal(resolveMenuPath('/system', 'http://example.com/app'), 'http://example.com/app')
assert.equal(resolveMenuPath('/system', 'mailto:test@example.com'), 'mailto:test@example.com')
assert.equal(resolveMenuPath('/system', 'tel:10086'), 'tel:10086')
assert.equal(
  resolveMenuPath('https://example.com/base', 'child'),
  'https://example.com/base/child'
)

const absolute = groups[0].children.find(item => item.name === '绝对报表')
assert.equal(absolute.path, '/absolute/report')

assert.deepEqual(
  searchMenus(groups, '用户').map(item => item.name),
  ['用户管理']
)
assert.deepEqual(
  searchMenus(groups, '开发').map(item => item.name),
  ['开发工具']
)
assert.deepEqual(
  searchMenus(groups, '三级菜单 12').map(item => item.name),
  ['三级菜单 12']
)
assert.deepEqual(
  searchMenus(groups, '系统管理').map(item => item.name),
  ['用户管理', '开发工具', '提升菜单', '外部文档', '绝对报表']
)

const topLevelFallbacks = normalizeMenuTree([
  route('/standalone', '独立入口'),
  route('/hidden-only', '隐藏子项父入口', {
    children: [route('secret', '隐藏子项', { hidden: true })]
  }),
  route('/blocked', '不可点击空组', {
    redirect: 'noRedirect',
    children: [route('secret', '隐藏子项', { hidden: true })]
  }),
  route('', '无路径空组', {
    children: [route('secret', '隐藏子项', { hidden: true })]
  })
])

assert.deepEqual(
  topLevelFallbacks.map(group => ({
    name: group.name,
    childNames: group.children.map(item => item.name)
  })),
  [
    { name: '独立入口', childNames: ['独立入口'] },
    { name: '隐藏子项父入口', childNames: ['隐藏子项父入口'] }
  ]
)
assert.equal(topLevelFallbacks[0].children[0].path, '/standalone')

const relativeIndexGroups = normalizeMenuTree([
  {
    path: '',
    children: [route('index', '相对首页')]
  },
  route('/business/index', '业务索引')
])

assert.deepEqual(
  relativeIndexGroups.map(group => group.name),
  ['业务索引']
)

const duplicateNames = normalizeMenuTree([
  {
    path: '/one',
    meta: { title: '重复分组' },
    children: [route('same', '重复菜单')]
  },
  {
    path: '/two',
    meta: { title: '重复分组' },
    children: [route('same', '重复菜单')]
  }
])

assert.notEqual(duplicateNames[0].key, duplicateNames[1].key)
assert.notEqual(duplicateNames[0].children[0].key, duplicateNames[1].children[0].key)

const samePathDifferentTitles = normalizeMenuTree([
  {
    path: '/duplicates',
    meta: { title: '同路径分组' },
    children: [
      route('shared', '同路径菜单甲'),
      route('shared', '同路径菜单乙')
    ]
  }
])

assert.notEqual(
  samePathDifferentTitles[0].children[0].key,
  samePathDifferentTitles[0].children[1].key
)
assert.deepEqual(
  searchMenus(samePathDifferentTitles, '同路径菜单').map(item => item.name),
  ['同路径菜单甲', '同路径菜单乙']
)

assert.deepEqual(folderPreviewStyle(0), { columns: 1, size: 0 })
assert.deepEqual(folderPreviewStyle(12), { columns: 4, size: 12 })
assert.deepEqual(folderPreviewStyle(17), { columns: 5, size: 17 })

console.log('allAppsMenu tests passed')
