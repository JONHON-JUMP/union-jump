<template>
  <div class="app-wrapper portal-shell" :style="{'--current-color': theme}">
    <div class="main-container">
      <app-main v-if="isPortalHome" />
      <portal-shell v-else>
        <app-main />
      </portal-shell>
    </div>
  </div>
</template>

<script>
import { AppMain } from './components'
import PortalShell from './components/PortalShell'
import { mapState } from 'vuex'
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'Layout',
  components: {
    AppMain,
    PortalShell
  },
  computed: {
    ...mapState({
      theme: state => state.settings.theme
    }),
    variables() {
      return variables;
    },
    isPortalHome() {
      return this.$route.path === '/index'
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mixin.scss";
@import "~@/assets/styles/variables.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.portal-shell {
  min-height: 100%;

  .main-container {
    width: 100%;
    min-height: 100%;
    margin-left: 0 !important;
  }
}
</style>
