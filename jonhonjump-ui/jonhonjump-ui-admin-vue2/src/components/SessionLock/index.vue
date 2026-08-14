<template>
  <transition name="session-lock-fade">
    <div v-if="visible" class="session-lock">
      <div class="session-lock__panel">
        <div class="session-lock__icon">
          <i class="el-icon-lock" />
        </div>
        <h2>会话已锁定</h2>
        <p class="session-lock__hint">超过 {{ idleMinutes }} 分钟无操作，请重新登录后继续</p>
        <p v-if="displayName" class="session-lock__user">当前用户：{{ displayName }}</p>
        <el-button type="primary" size="medium" class="session-lock__btn" @click="$emit('relogin')">
          重新登录
        </el-button>
      </div>
    </div>
  </transition>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'SessionLock',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    idleMinutes: {
      type: Number,
      default: 60
    }
  },
  computed: {
    ...mapGetters(['nickname', 'name']),
    displayName() {
      return this.nickname || this.name || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.session-lock {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(16, 35, 62, 0.72);
  backdrop-filter: blur(6px);
}

.session-lock__panel {
  width: 360px;
  max-width: calc(100vw - 48px);
  padding: 36px 32px 32px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 64px rgba(16, 35, 62, 0.24);
  text-align: center;
}

.session-lock__icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #fff7e6;
  color: #e6a23c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.session-lock__panel h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #10233e;
}

.session-lock__hint {
  margin: 0 0 12px;
  color: #5d718c;
  font-size: 14px;
  line-height: 1.6;
}

.session-lock__user {
  margin: 0 0 24px;
  color: #087ce5;
  font-size: 15px;
  font-weight: 600;
}

.session-lock__btn {
  min-width: 160px;
}

.session-lock-fade-enter-active,
.session-lock-fade-leave-active {
  transition: opacity 0.25s ease;
}

.session-lock-fade-enter,
.session-lock-fade-leave-to {
  opacity: 0;
}
</style>
