<template>
  <section
    :class="{
      'portal-embedded': true,
      'portal-iframe': isPortalIframeRoute
    }"
    class="app-main"
  >
    <!-- 对齐 4200 AppMain：router-view 与 iframe-toggle 同时常驻 -->
    <!-- 首页/主系统内容常驻可见：业务页时由绝对定位的 iframe 宿主整块盖住。
         不再 display:none / visibility 切换，避免首页↔业务页切换时闪白。 -->
    <div class="app-main__body">
      <transition :name="routeTransitionName" :mode="routeTransitionMode">
        <keep-alive :include="cachedViews">
          <router-view :key="key" />
        </keep-alive>
      </transition>
    </div>
    <!-- 不要用 v-show 切换 iframe 宿主：display:none→block 会让 iframe 白闪一下（Chrome 82 尤其明显）。
         宿主常驻、绝对定位，用 visibility 隐藏；已打开的 iframe 不重建，符合「已打开页面不刷新」。 -->
    <iframe-toggle
      class="app-main__iframe-host"
      :class="{ 'is-portal-hidden': !isPortalIframeRoute }"
    />
  </section>
</template>

<script>
import iframeToggle from './IframeToggle/index'
import { resolvePortalFrameRoute, parsePortalClientId } from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'

export default {
  name: 'AppMain',
  components: { iframeToggle },
  computed: {
    cachedViews() {
      const list = this.$store.state.tagsView.cachedViews
      // 门户布局没有 TagsView，cachedViews 恒为空 → keep-alive 实际什么都没缓存，
      // 每次回首页都全量重建快捷导航（低配 82 上数百毫秒，正撞抽屉关闭的黑影淡出）。
      // 恒定缓存门户首页；其他页面维持现状不缓存，避免多标签内存累积
      return list.includes('JumpPortalHome') ? list : [...list, 'JumpPortalHome']
    },
    key() {
      return this.$route.path
    },
    routeTransitionName() {
      return ''
    },
    routeTransitionMode() {
      return undefined
    },
    isPortalIframeRoute() {
      const route = resolvePortalFrameRoute(
        this.$route,
        this.$store.state.portal.pathLinkMap,
        this.$store.state.portal.systemList
      )
      if (route.meta && route.meta.link) {
        return true
      }
      // path 归一化/二次 replace 的瞬间 link 可能还没算出来；
      // 不能因此把 iframe 宿主隐藏，否则切换瞬间会白闪
      const clientId = parsePortalClientId(this.$route.path)
      return !!(clientId && (this.$route.name === 'PortalFrame' || this.$route.name === 'PortalFrameLegacy'))
    }
  },
  watch: {
    // 唯一登记走 syncPortalIframeView（与 IframeToggle / permission 共用）
    $route: {
      immediate: true,
      handler(route) {
        syncPortalIframeView(this.$store, route)
      }
    },
    '$store.state.portal.pathLinkMap': {
      deep: true,
      handler() {
        syncPortalIframeView(this.$store, this.$route)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  overflow: hidden;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .app-main {
    min-height: calc(100vh - 84px);
  }

  .fixed-header + .app-main {
    padding-top: 84px;
  }
}

.app-main.portal-embedded {
  flex: 1 1 auto;
  align-self: stretch;
  width: 100%;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-main__body {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  /* 数据加载把内容撑高时不要触发滚动锚定，避免整页跳动 */
  overflow-anchor: none;
  /* 首页常驻在 iframe 宿主下面：必须形成独立层叠上下文，
     否则首页里 z-index 较高的卡片/浮层会穿透到 iframe 上面，
     表现成“切到已打开页面时 Camstar 闪一下/被首页内容盖一下”。 */
  position: relative;
  z-index: 0;
}

/* 业务 iframe 页：首页/主系统内容不再显隐，保持原样垫在 iframe 宿主下面。
   这样从业务页回首页时不需要重新可见/重绘，避免闪白。 */

.app-main__iframe-host {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #f5f7fb;
  z-index: 1;
  opacity: 1;
}

/* 非门户路由：用 opacity 隐藏 iframe 宿主，不切 visibility/display。
   visibility 切换会让 iframe 重新合成，回业务页时先白一下；
   opacity 只影响绘制，宿主和 iframe 始终处于“已合成”状态。 */
.app-main__iframe-host.is-portal-hidden {
  opacity: 0;
  pointer-events: none;
  z-index: 0;
}

.app-main.portal-embedded.portal-iframe {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
</style>

<style lang="scss">
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 17px;
  }
}
</style>
