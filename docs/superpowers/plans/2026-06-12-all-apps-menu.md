# 全部应用菜单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将首页“全部应用”改为沿用首页视觉的分层权限菜单，并以应用文件夹聚合全部三级菜单。

**Architecture:** 新建 `AllAppsDrawer.vue` 负责 drawer、一级菜单、二级图标、文件夹弹层和搜索；新建 CommonJS 纯函数模块负责权限路由规范化，便于直接使用 Node 做测试。`index.vue` 仅传入 `permission_routes`、同步显示状态，并复用现有 `openApp` 跳转。

**Tech Stack:** Vue 2.7、Vuex、Vue Router、Element UI 2、SCSS、Node `assert`

---

## 文件结构

- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/allAppsMenu.js`
  - 规范化权限路由、折叠布局节点、生成一级/二级/三级菜单、搜索叶子和文件夹。
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/allAppsMenu.test.cjs`
  - 使用 Node `assert` 覆盖隐藏菜单、路径拼接、外链、三级聚合和不限数量预览。
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/AllAppsDrawer.vue`
  - 渲染全部应用抽屉和文件夹弹层，发出 `open` 与 `update:visible`。
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/index.vue`
  - 引入新组件，删除旧 drawer 模板、状态、计算属性、方法和样式。

### Task 1: 权限菜单规范化

**Files:**
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/allAppsMenu.test.cjs`
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/allAppsMenu.js`

- [ ] **Step 1: 编写失败测试**

测试构造两组一级菜单：一个包含普通二级菜单和 12 个三级菜单的文件夹，一个包含隐藏项和外链。核心断言：

```js
const assert = require('assert')
const { normalizeMenuTree, searchMenus, folderPreviewStyle } = require('./allAppsMenu')

const children = Array.from({ length: 12 }, (_, index) => ({
  path: `child-${index + 1}`,
  meta: { title: `三级菜单${index + 1}`, icon: 'component' }
}))

const routes = [{
  path: '/production',
  meta: { title: '生产管理', icon: 'skill' },
  children: [
    { path: 'plan', meta: { title: '生产计划', icon: 'form' } },
    { path: 'orders', meta: { title: '工单管理', icon: 'folder' }, children }
  ]
}, {
  path: '/system',
  meta: { title: '系统管理' },
  children: [
    { path: 'hidden', hidden: true, meta: { title: '隐藏菜单' } },
    { path: 'https://example.com', meta: { title: '外部系统' } }
  ]
}]

const groups = normalizeMenuTree(routes)
assert.strictEqual(groups.length, 2)
assert.strictEqual(groups[0].children[0].path, '/production/plan')
assert.strictEqual(groups[0].children[1].children.length, 12)
assert.strictEqual(groups[0].children[1].children[11].path, '/production/orders/child-12')
assert.strictEqual(groups[1].children.length, 1)
assert.strictEqual(searchMenus(groups, '三级菜单12')[0].name, '三级菜单12')
assert.ok(folderPreviewStyle(12).columns >= 4)
assert.ok(folderPreviewStyle(12).size > 0)
console.log('allAppsMenu tests passed')
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```powershell
node src/views/components/allAppsMenu.test.cjs
```

Expected: FAIL，提示无法找到 `./allAppsMenu`。

- [ ] **Step 3: 实现最小纯函数模块**

实现并导出：

```js
function resolveMenuPath(basePath, routePath) {}
function normalizeMenuTree(routes) {}
function searchMenus(groups, keyword) {}
function folderPreviewStyle(count) {}

module.exports = {
  resolveMenuPath,
  normalizeMenuTree,
  searchMenus,
  folderPreviewStyle
}
```

实现规则：

- 过滤 `hidden`、`*`、`/404`、`/index`。
- 标题取 `route.meta.title`，图标取 `route.meta.icon || 'component'`。
- HTTP/HTTPS 不拼接；绝对路径保持不变；相对路径逐层拼接。
- 二级节点有子节点时保留为文件夹，递归收集其全部可打开后代到 `children`。
- 无标题布局节点不展示自身，将可见子节点提升。
- `searchMenus` 返回匹配名称的二级叶子、二级文件夹和三级叶子。
- `folderPreviewStyle` 根据数量返回 `{ columns, size }`，不截断菜单数组。

- [ ] **Step 4: 运行测试并确认通过**

Run:

```powershell
node src/views/components/allAppsMenu.test.cjs
```

Expected: PASS，并输出 `allAppsMenu tests passed`。

### Task 2: 全部应用抽屉组件

**Files:**
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/components/AllAppsDrawer.vue`

- [ ] **Step 1: 建立组件接口和状态**

组件声明：

```js
export default {
  name: 'AllAppsDrawer',
  props: {
    visible: { type: Boolean, default: false },
    routes: { type: Array, default: () => [] }
  },
  data() {
    return {
      keyword: '',
      activeGroupKey: '',
      openedFolder: null
    }
  }
}
```

使用 `require('./allAppsMenu')` 引入纯函数。计算 `menuGroups`、`activeGroup`、`displayMenus` 和搜索结果；监听菜单组变化，自动选择首个有效一级菜单。

- [ ] **Step 2: 渲染 drawer 和一级/二级菜单**

模板使用：

```vue
<el-drawer
  title="全部应用"
  :visible="visible"
  direction="btt"
  size="78%"
  custom-class="all-apps-drawer"
  :append-to-body="true"
  @update:visible="$emit('update:visible', $event)"
>
  <div class="all-apps-shell">
    <header class="all-apps-toolbar">...</header>
    <div class="all-apps-layout">
      <nav class="menu-groups">...</nav>
      <main class="menu-apps">...</main>
    </div>
  </div>
</el-drawer>
```

二级叶子点击 `openMenu(menu)`；文件夹点击 `openFolder(menu)`。应用图标使用当前首页 `.app-icon` 的渐变、圆角、阴影、字号和 hover 位移。

- [ ] **Step 3: 实现不限数量的文件夹预览**

文件夹图标内部遍历全部 `menu.children`：

```vue
<span
  class="folder-preview"
  :style="{ '--folder-columns': previewStyle(menu).columns, '--folder-icon-size': previewStyle(menu).size + 'px' }"
>
  <span
    v-for="child in menu.children"
    :key="child.path"
    class="folder-preview__item"
    :style="{ background: child.color }"
  >
    <svg-icon :icon-class="child.icon" />
  </span>
</span>
```

禁止 `slice(0, 9)` 或其他截断。数量越多，小图标与间距自动缩小。

- [ ] **Step 4: 实现文件夹弹层**

在 drawer 内加入遮罩和居中面板：

```vue
<transition name="folder-zoom">
  <div v-if="openedFolder" class="folder-overlay" @click.self="closeFolder">
    <section class="folder-panel" role="dialog" :aria-label="openedFolder.name">
      <button class="folder-close" type="button" @click="closeFolder">
        <i class="el-icon-close" />
      </button>
      <h2>{{ openedFolder.name }}</h2>
      <div class="folder-app-grid">...</div>
    </section>
  </div>
</transition>
```

挂载时监听 Escape，销毁时移除监听。三级菜单点击调用 `openMenu`，清空弹层并发出 `open`。

- [ ] **Step 5: 实现搜索和响应式样式**

搜索框沿用首页浅蓝输入样式。存在关键字时隐藏一级导航选择逻辑，右侧展示匹配图标；文件夹结果继续打开弹层。

SCSS 断点：

- `> 820px`：左侧一级菜单固定宽度，右侧自适应图标网格。
- `<= 820px`：一级菜单改为横向滚动标签。
- `<= 560px`：减少网格列数，文件夹面板接近全宽。

### Task 3: 接入首页

**Files:**
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/index.vue`

- [ ] **Step 1: 替换旧 drawer**

导入并注册：

```js
import AllAppsDrawer from './components/AllAppsDrawer.vue'

export default {
  name: 'JumpPortalHome',
  components: { AllAppsDrawer },
  // ...
}
```

模板替换为：

```vue
<all-apps-drawer
  :visible.sync="drawerVisible"
  :routes="permission_routes || []"
  @open="openApp"
/>
```

- [ ] **Step 2: 删除旧 drawer 专属逻辑**

删除：

- `drawerKeyword`
- `drawerFilteredApps`
- 旧 drawer 模板
- `.drawer-content`、`.drawer-toolbar`、`.drawer-grid`、`.drawer-app`、`.drawer-empty`
- 全局 `.app-drawer` 样式

保留 `authorizedApps` 和 `drawerApps`，因为首页顶部搜索和 `openByKeyword` 仍依赖扁平菜单。`openApp` 保持唯一跳转出口。

- [ ] **Step 3: 调整 openApp 状态**

确保 `openApp` 在叶子菜单点击后：

```js
this.searchFocused = false
this.drawerVisible = false
```

外链用 `window.open(app.path, '_blank')`，内部路由用 `this.$router.push(app.path)`。

### Task 4: 静态验证与修正

**Files:**
- Modify as needed: `AllAppsDrawer.vue`, `allAppsMenu.js`, `index.vue`

- [ ] **Step 1: 运行纯函数测试**

Run:

```powershell
node src/views/components/allAppsMenu.test.cjs
```

Expected: PASS。

- [ ] **Step 2: 运行目标文件 ESLint**

Run:

```powershell
npx eslint src/views/index.vue src/views/components/AllAppsDrawer.vue src/views/components/allAppsMenu.js
```

Expected: exit code 0；修复所有错误后重跑。

- [ ] **Step 3: 运行开发构建**

Run:

```powershell
npm run build:dev
```

Expected: 编译成功；不新增模板、SCSS 或模块解析错误。

### Task 5: 浏览器交互验证

**Files:**
- Modify as needed: `AllAppsDrawer.vue`, `index.vue`

- [ ] **Step 1: 启动本地应用并打开首页**

Run:

```powershell
npm run local
```

使用 Browser 打开项目实际 localhost 地址并登录现有测试会话。

- [ ] **Step 2: 验证桌面交互**

检查：

- 点击“全部应用”出现与首页一致的浅蓝视觉。
- 左侧一级菜单切换正确。
- 右侧普通二级菜单显示首页同款大图标。
- 含 12 个以上三级菜单的文件夹预览没有截断。
- 点击文件夹显示全部三级菜单；关闭按钮、遮罩、Escape 均可关闭。
- 点击三级菜单关闭抽屉并进入正确路由。
- 搜索二级、三级菜单均得到正确结果。

- [ ] **Step 3: 验证窄屏交互**

将视口调整至约 `390x844`，确认一级菜单横向滚动、图标网格不溢出、文件夹面板可滚动且可关闭。

- [ ] **Step 4: 最终回归**

再次运行：

```powershell
node src/views/components/allAppsMenu.test.cjs
npx eslint src/views/index.vue src/views/components/AllAppsDrawer.vue src/views/components/allAppsMenu.js
npm run build:dev
```

Expected: 三项全部通过。
