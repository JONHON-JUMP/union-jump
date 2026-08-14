<template>
  <div class="system-chip">
    <span class="status-dot" />
    <span>当前系统：</span>
    <strong>{{ currentSubsystem }}</strong>
    <el-dropdown trigger="click" placement="bottom-end" @command="handleSubsystemChange" @visible-change="handleDropdownVisible">
      <button class="system-switch" type="button">
        切换
        <i class="el-icon-arrow-down" />
      </button>
      <el-dropdown-menu slot="dropdown" class="system-dropdown">
        <el-dropdown-item
          v-for="system in subsystemOptions"
          :key="system.value"
          :command="system.value"
          :class="{ 'is-current': currentSystem === system.value }"
        >
          <span class="system-option">
            <button
              v-if="showDefaultStar"
              type="button"
              class="system-default-star"
              :class="{ pinned: isDefaultPortalSystem(system.value) }"
              :title="isDefaultPortalSystem(system.value) ? '登录后默认打开' : '设为登录后默认打开'"
              :aria-label="isDefaultPortalSystem(system.value) ? system.label + '为默认打开' : '将' + system.label + '设为默认打开'"
              @click.stop="toggleDefaultPortalSystem(system.value)"
            >
              <svg class="system-default-star__icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
                  :fill="isDefaultPortalSystem(system.value) ? 'currentColor' : 'none'"
                  :stroke="isDefaultPortalSystem(system.value) ? 'none' : 'currentColor'"
                  stroke-width="1.6"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
            <i :class="system.icon" class="system-option__icon" />
            <span class="system-option__copy">
              <strong>{{ system.label }}</strong>
              <small>{{ system.description }}</small>
            </span>
            <i v-if="currentSystem === system.value" class="el-icon-check system-option__check" />
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  buildSubsystemOptions,
  resolveCurrentSubsystemLabel,
  resolveRuleBasedPortalDefault,
  shouldShowPortalDefaultSettings
} from '@/utils/portalSubsystem'
import {
  saveUserPortalDefault
} from '@/api/system/user/portalDefault'

export default {
  name: 'PortalSystemSwitch',
  data() {
    return {
      portalDefaultConfigured: false,
      portalDefaultSystem: 'main',
      portalDefaultSubSystemId: null,
      portalDefaultSaving: false
    }
  },
  computed: {
    ...mapGetters(['currentSystem', 'portalSystemList']),
    subsystemOptions() {
      return buildSubsystemOptions(this.portalSystemList)
    },
    currentSubsystem() {
      return resolveCurrentSubsystemLabel(this.currentSystem, this.portalSystemList)
    },
    showDefaultStar() {
      return shouldShowPortalDefaultSettings(this.portalSystemList)
    }
  },
  mounted() {
    this.loadPortalDefault()
  },
  watch: {
    portalSystemList() {
      this.loadPortalDefault()
    }
  },
  methods: {
    loadPortalDefault() {
      return this.$store.dispatch('portal/fetchPortalDefault').then(config => {
        const data = config || {}
        this.portalDefaultConfigured = !!data.configured
        this.portalDefaultSubSystemId = data.subSystemId != null ? data.subSystemId : null
        this.portalDefaultSystem = data.defaultSystem || resolveRuleBasedPortalDefault(this.portalSystemList)
      }).catch(() => {
        this.portalDefaultConfigured = false
        this.portalDefaultSubSystemId = null
        this.portalDefaultSystem = resolveRuleBasedPortalDefault(this.portalSystemList)
      })
    },
    handleDropdownVisible(visible) {
      if (visible) {
        this.loadPortalDefaultForce()
      }
    },
    loadPortalDefaultForce() {
      return this.$store.dispatch('portal/fetchPortalDefault', { force: true }).then(config => {
        const data = config || {}
        this.portalDefaultConfigured = !!data.configured
        this.portalDefaultSubSystemId = data.subSystemId != null ? data.subSystemId : null
        this.portalDefaultSystem = data.defaultSystem || resolveRuleBasedPortalDefault(this.portalSystemList)
      }).catch(() => {})
    },
    isDefaultPortalSystem(systemValue) {
      return this.portalDefaultSystem === systemValue
    },
    async toggleDefaultPortalSystem(systemValue) {
      if (!this.showDefaultStar || this.portalDefaultSaving || this.isDefaultPortalSystem(systemValue)) {
        return
      }
      this.portalDefaultSaving = true
      try {
        const subSystemId = this.resolveSubSystemId(systemValue)
        await saveUserPortalDefault({ subSystemId })
        this.portalDefaultConfigured = true
        this.portalDefaultSubSystemId = subSystemId
        this.portalDefaultSystem = systemValue
        this.$store.commit('portal/SET_PORTAL_DEFAULT_CACHE', {
          configured: true,
          subSystemId,
          defaultSystem: systemValue
        })
        // 星标只影响下次登录默认进入，不切换当前正在使用的系统
        await this.$store.dispatch('portal/rememberSystemChoice', systemValue)
        this.$message.success(`已将「${this.resolveSystemLabel(systemValue)}」设为登录后默认打开`)
        this.$emit('default-change', {
          configured: this.portalDefaultConfigured,
          subSystemId: this.portalDefaultSubSystemId,
          defaultSystem: this.portalDefaultSystem
        })
      } catch (error) {
        console.error('[portal] save default system failed:', error)
        this.$message.error('默认打开系统保存失败')
      } finally {
        this.portalDefaultSaving = false
      }
    },
    resolveSystemLabel(systemValue) {
      const option = this.subsystemOptions.find(item => item.value === systemValue)
      return option ? option.label : systemValue
    },
    resolveSubSystemId(systemValue) {
      if (systemValue === 'main') {
        return null
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === systemValue)
      return sys ? sys.subSystemId : null
    },
    handleSubsystemChange(value) {
      this.$emit('switch', value)
    }
  }
}
</script>

<style lang="scss" scoped>
.system-chip {
  display: flex;
  height: 48px;
  padding: 0 8px 0 15px;
  align-items: center;
  border-radius: 24px;
  color: #44607f;
  background: #e3f1fd;
  font-size: 14px;
  white-space: nowrap;
}

.system-chip strong {
  color: #075eb5;
}

.system-switch {
  height: 30px;
  margin-left: 9px;
  padding: 0 10px;
  border: 0;
  border-radius: 15px;
  color: #075eb5;
  background: rgba(255, 255, 255, .72);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.system-switch:hover,
.system-switch:focus {
  outline: none;
  background: #fff;
}

.status-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 8px;
  border-radius: 50%;
  background: #11a574;
  box-shadow: 0 0 0 4px rgba(17, 165, 116, .12);
}
</style>

<style lang="scss">
.system-dropdown {
  min-width: 280px;
  padding: 8px;
  border: 0;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(41, 81, 117, .16);
}

.system-dropdown .el-dropdown-menu__item {
  height: auto;
  padding: 9px 10px;
  border-radius: 10px;
  line-height: 1.4;
}

.system-dropdown .el-dropdown-menu__item:hover {
  color: #075eb5;
  background: #edf6fd;
}

.system-dropdown .el-dropdown-menu__item.is-current {
  color: #075eb5;
  background: #e5f2fd;
}

.system-option {
  display: flex;
  align-items: center;
}
.system-option > * + * {
  margin-left: 8px;
}

.system-default-star {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  color: #54708f;
  background: #fff;
  box-shadow: 0 2px 6px rgba(38, 78, 113, .14);
  font-size: 13px;
  line-height: 0;
  cursor: pointer;
  transition: color .18s ease, background .18s ease, transform .18s ease;
}

.system-default-star__icon {
  display: block;
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
}

.system-default-star:hover,
.system-default-star:focus {
  outline: 3px solid rgba(8, 124, 229, .14);
  color: #075eb5;
  transform: scale(1.05);
}

.system-default-star.pinned {
  color: #fff;
  background: #087ce5;
}

.system-option__icon {
  width: 28px;
  color: #2785d1;
  font-size: 18px;
  text-align: center;
}

.system-option__copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.system-option__copy strong {
  color: #183653;
  font-size: 13px;
}

.system-option__copy small {
  margin-top: 2px;
  color: #71859b;
  font-size: 11px;
}

.system-option__check {
  color: #087ce5;
  font-weight: 700;
}
</style>
