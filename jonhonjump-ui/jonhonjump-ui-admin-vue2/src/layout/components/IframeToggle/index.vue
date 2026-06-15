<template>
  <transition-group class="iframe-toggle" name="fade-transform" mode="out-in" tag="div">
    <inner-link
        v-for="(item, index) in iframeViews"
        :key="item.path"
        :iframeId="'iframe' + index"
        v-show="$route.path === item.path"
        :src="item.meta.link"
    ></inner-link>
  </transition-group>
</template>

<script>
import InnerLink from "../InnerLink/index.vue"

export default {
  components: { InnerLink },
  computed: {
    iframeViews() {
      return this.$store.state.tagsView.iframeViews
    }
  }
}
</script>

<style lang="scss" scoped>
.iframe-toggle {
  height: 100%;
  min-height: 0;
}
</style>
