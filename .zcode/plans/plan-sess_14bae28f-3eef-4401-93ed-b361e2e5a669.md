# 门户 tab 切换不刷新（刷新交给浏览器 F5）

## 根因
iframe 保活池（`IframeToggle` v-show + `InnerLink` camstar-stable 模式）本来就现成可用，切换本不该刷新。元凶是：

`PortalDock.vue` 的 `activateTab`（L213-217）**每次点击** portal root tab 都 commit `tagsView/RESTORE_PORTAL_IFRAME`，该 mutation 无条件 bump `portalSrcNonce` → iframe 的 `:key` 变化 → Vue 销毁重建 InnerLink → 整页重载。原意图只是"列表停在详情子页时点回叶子 tab 拉回列表"，但没做条件判断，变成每次切换必刷、点当前 tab 也刷。

## 改动（仅 1 处）

`src/layout/components/PortalDock.vue` — `activateTab` 删除 `RESTORE_PORTAL_IFRAME` 那段 commit，只保留 `router.push` + collapse：

```js
activateTab(tab) {
  // 切换 tab 只切路由，iframe 帧由 IframeToggle 用 v-show 保活，保持现场不重载；
  // 需要刷新时用户按浏览器 F5（或打开/关闭详情子页时既有逻辑会刷新父列表）
  if (!this.isActive(tab)) {
    this.$router.push(tab.fullPath || tab.path)
  }
  this.$emit('collapse')
}
```

**不动的部分**：
- `RESTORE_PORTAL_IFRAME` mutation 保留（"关闭详情子 tab 拉回父列表" portal.js:955 还在用）
- `BUMP_COVERING_PORTAL_IFRAME` 保留（打开详情子页时刷新父列表，非切换场景）
- IframeToggle / InnerLink / tagsView store 全部不动

## 行为对照
| 动作 | 之前 | 之后 |
|---|---|---|
| tab 间切换 | 每次整页重载 | v-show 切换，保持现场 |
| 点当前激活 tab | 重载 | 无操作 |
| 想刷新 | 被迫被动刷新 | 浏览器 F5（原生，无需开发） |
| iframe 内跳详情后切走再切回 | 重载回列表 | 保持详情现场，F5 回列表 |
| 打开/关闭详情子页刷新父列表 | 既有逻辑 | 不变 |

## 验证
eslint 基线比对。