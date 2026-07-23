<template>

  <section
    :class="{
      'portal-embedded': true,
      'portal-iframe': isPortalIframeRoute
    }"
    class="app-main"
  >

    <div v-if="!isPortalIframeRoute" class="app-main__body">

      <transition :name="routeTransitionName" :mode="routeTransitionMode">

        <keep-alive :include="cachedViews">

          <router-view :key="key" />

        </keep-alive>

      </transition>

    </div>

    <iframe-toggle v-if="isPortalIframeRoute" />

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

    isPortalEmbedded() {

      const path = this.$route.path

      return path !== '/index' && path !== '/'

    },

    routeTransitionName() {
      // 门户壳常驻，路由切换不做过渡，避免关页签回首页闪一下
      return ''
    },

    routeTransitionMode() {
      return undefined
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

