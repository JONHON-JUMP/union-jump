<template>
  <div class="app-wrapper portal-shell" :style="{'--current-color': theme}">
    <div class="main-container">
      <!-- 始终使用同一套壳，避免首页/业务页顶栏拆装造成闪屏 -->
      <portal-shell>
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
      return variables
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
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;

  .main-container {
    width: 100%;
    height: 100%;
    min-height: 0;
    margin-left: 0 !important;
    display: flex;
    flex-direction: column;
  }
}
</style>
