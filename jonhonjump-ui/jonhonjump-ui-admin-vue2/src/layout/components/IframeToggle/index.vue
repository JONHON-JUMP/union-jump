<template>
  <transition-group class="iframe-toggle" name="fade-transform" mode="out-in" tag="div">
    <inner-link
      v-for="(item, index) in iframeViews"
      v-show="isIframeVisible(item)"
      :key="item.path + '|' + (item.meta.link || '')"
      :iframeId="'iframe' + index"
      :src="item.meta.link"
    />
  </transition-group>
</template>

<script>
import InnerLink from "../InnerLink/index.vue"
import { parsePortalClientId } from '@/utils/portalRoute'

export default {
  components: { InnerLink },
  computed: {
    iframeViews() {
      return this.$store.state.tagsView.iframeViews
    }
  },
  methods: {
    isIframeVisible(item) {
      if (this.$route.path !== item.path) {
        return false
      }
      const clientId = parsePortalClientId(item.path)
      if (!clientId) {
        return true
      }
      return Boolean(this.$store.state.portal.ssoDone[clientId])
    }
  }
}
</script>

<style lang="scss" scoped>
.iframe-toggle {
  flex: 1;
  height: 100%;
  min-height: 0;
}
</style>
