<template>
  <section
    :class="{
      'portal-embedded': true,
      'portal-iframe': isPortalIframeRoute
    }"
    class="app-main"
  >
    <!-- 对齐 4200 AppMain：router-view 与 iframe-toggle 同时常驻 -->
    <div v-show="!isPortalIframeRoute" class="app-main__body">
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
}

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
}

/* 非门户路由：只隐藏（visibility），不用 display:none。
   display 切换会让 iframe 重新合成、出现白闪，且可能触发子页重排。 */
.app-main__iframe-host.is-portal-hidden {
  visibility: hidden;
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
