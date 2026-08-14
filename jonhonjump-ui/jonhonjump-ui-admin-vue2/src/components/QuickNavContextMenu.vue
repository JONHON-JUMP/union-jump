<template>
  <div>
    <transition name="quick-nav-menu-fade">
      <div
        v-if="visible"
        class="quick-nav-context-backdrop"
        @click="$emit('close')"
        @contextmenu.prevent
      >
        <div
          class="quick-nav-context-menu"
          :style="menuStyle"
          @click.stop
        >
          <button
            v-if="showReorder"
            type="button"
            class="quick-nav-context-item"
            @click="$emit('reorder')"
          >
            <i class="el-icon-rank" />
            <span>调整顺序</span>
          </button>
          <button
            v-if="showSubscribe"
            type="button"
            class="quick-nav-context-item"
            @click="$emit('subscribe')"
          >
            <i class="el-icon-star-off" />
            <span>加入快捷导航</span>
          </button>
          <button
            v-if="showUnsubscribe"
            type="button"
            class="quick-nav-context-item"
            :class="{ 'is-disabled': unsubscribeDisabled }"
            @click="$emit('unsubscribe')"
          >
            <i class="el-icon-close" />
            <span>取消订阅</span>
          </button>
          <button
            type="button"
            class="quick-nav-context-item"
            @click="$emit('view-manual')"
          >
            <i class="el-icon-download" />
            <span>下载说明书</span>
          </button>
        </div>
      </div>
    </transition>

    <el-dialog
      :title="manualTitle"
      :visible.sync="manualVisibleProxy"
      width="480px"
      append-to-body
      @closed="$emit('manual-closed')"
    >
      <div class="quick-nav-manual-content">{{ manualContent }}</div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'QuickNavContextMenu',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    menuStyle: {
      type: Object,
      default: () => ({})
    },
    showReorder: {
      type: Boolean,
      default: false
    },
    showSubscribe: {
      type: Boolean,
      default: false
    },
    showUnsubscribe: {
      type: Boolean,
      default: false
    },
    unsubscribeDisabled: {
      type: Boolean,
      default: false
    },
    manualVisible: {
      type: Boolean,
      default: false
    },
    manualTitle: {
      type: String,
      default: '说明书'
    },
    manualContent: {
      type: String,
      default: ''
    }
  },
  computed: {
    manualVisibleProxy: {
      get() {
        return this.manualVisible
      },
      set(value) {
        this.$emit('update:manualVisible', value)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.quick-nav-context-backdrop {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 3200;
  background: rgba(16, 35, 62, .18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.quick-nav-context-menu {
  position: fixed;
  z-index: 3201;
  overflow: hidden;
  border-radius: 16px;
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 18px 48px rgba(16, 35, 62, .18);
}

.quick-nav-context-item {
  display: flex;
  width: 100%;
  min-height: 56px;
  padding: 0 18px;
  align-items: center;
  border: 0;
  border-bottom: 1px solid rgba(16, 35, 62, .08);
  color: #10233e;
  background: transparent;
  font-size: 15px;
  text-align: left;
  cursor: pointer;
}
.quick-nav-context-item > * + * {
  margin-left: 12px;
}

.quick-nav-context-item:last-child {
  border-bottom: 0;
}

.quick-nav-context-item i {
  width: 18px;
  color: #087ce5;
  font-size: 18px;
}

.quick-nav-context-item:hover {
  background: rgba(8, 124, 229, .06);
}

.quick-nav-context-item.is-disabled {
  color: #9eb1c5;
  cursor: not-allowed;
}

.quick-nav-context-item.is-disabled i {
  color: #9eb1c5;
}

.quick-nav-manual-content {
  min-height: 80px;
  color: #4f6478;
  font-size: 14px;
  line-height: 1.8;
}

.quick-nav-menu-fade-enter-active,
.quick-nav-menu-fade-leave-active {
  transition: opacity .18s ease;
}

.quick-nav-menu-fade-enter,
.quick-nav-menu-fade-leave-to {
  opacity: 0;
}
</style>
