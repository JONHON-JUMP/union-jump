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
    <iframe-toggle v-show="isPortalIframeRoute" class="app-main__iframe-host" />
  </section>
</template>

<script>
import iframeToggle from './IframeToggle/index'
import { resolvePortalFrameRoute } from '@/utils/portalRoute'
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
      return Boolean(route.meta && route.meta.link)
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
}

.app-main__iframe-host {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  background: #f5f7fb;
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
