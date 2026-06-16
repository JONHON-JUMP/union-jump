<template>
  <section
    :class="{
      'portal-embedded': $route.path !== '/index',
      'portal-iframe': isPortalIframeRoute
    }"
    class="app-main"
  >
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view v-if="!isPortalIframeRoute" :key="key" />
      </keep-alive>
    </transition>
    <iframe-toggle />
  </section>
</template>

<script>
import iframeToggle from "./IframeToggle/index"
import { resolvePortalFrameRoute } from '@/utils/portalRoute'

export default {
  name: 'AppMain',
  components: { iframeToggle },
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    },
    isPortalIframeRoute() {
      const route = resolvePortalFrameRoute(this.$route, this.$store.state.portal.pathLinkMap)
      return Boolean(route.meta && route.meta.link)
    }
  },
  watch: {
    '$route': {
      immediate: true,
      handler(route) {
        const resolved = resolvePortalFrameRoute(route, this.$store.state.portal.pathLinkMap)
        if (resolved.meta && resolved.meta.link && resolved.name) {
          this.$store.dispatch('tagsView/addIframeView', resolved)
        }
      }
    },
    '$store.state.portal.pathLinkMap': {
      deep: true,
      handler() {
        const resolved = resolvePortalFrameRoute(this.$route, this.$store.state.portal.pathLinkMap)
        if (resolved.meta && resolved.meta.link && resolved.name) {
          this.$store.dispatch('tagsView/addIframeView', resolved)
        }
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
  flex: 1;
  min-height: 0;
  height: auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-main.portal-embedded.portal-iframe {
  height: 100%;
  min-height: 0;
}
</style>

<style lang="scss">
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 17px;
  }
}
</style>
