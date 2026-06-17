<template>
  <el-dialog title="配置快捷导航" :visible.sync="visible" width="760px" append-to-body @open="handleOpen">
    <el-tabs v-model="activeTab" class="quick-nav-tabs">
      <el-tab-pane label="统一门户" name="main">
        <main-quick-nav-settings-panel ref="mainPanel" />
      </el-tab-pane>
      <el-tab-pane
        v-for="system in subSystemTabs"
        :key="system.subSystemId"
        :label="system.label"
        :name="tabName(system.subSystemId)"
      >
        <sub-system-quick-nav-settings-panel
          :ref="'subPanel-' + system.subSystemId"
          :sub-system-id="system.subSystemId"
        />
      </el-tab-pane>
    </el-tabs>

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { saveUserQuickNav } from '@/api/system/user/quickNav'
import { saveSubSystemUserQuickNav } from '@/api/system/user/subSystemQuickNav'
import MainQuickNavSettingsPanel from './MainQuickNavSettingsPanel'
import SubSystemQuickNavSettingsPanel from './SubSystemQuickNavSettingsPanel'

export default {
  name: 'PortalQuickNavSettings',
  components: { MainQuickNavSettingsPanel, SubSystemQuickNavSettingsPanel },
  props: {
    value: {
      type: Boolean,
      default: false
    },
    portalSystemList: {
      type: Array,
      default: () => []
    },
    initialTab: {
      type: String,
      default: 'main'
    }
  },
  data() {
    return {
      activeTab: 'main',
      saving: false
    }
  },
  computed: {
    visible: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    subSystemTabs() {
      return (this.portalSystemList || []).map(item => ({
        subSystemId: Number(item.subSystemId),
        label: item.clientName || item.clientId
      })).filter(item => item.subSystemId > 0)
    }
  },
  methods: {
    tabName(subSystemId) {
      return `sub-${subSystemId}`
    },
    handleOpen() {
      this.activeTab = this.initialTab || 'main'
      this.$nextTick(() => {
        this.loadActivePanel()
      })
    },
    loadActivePanel() {
      const panel = this.getActivePanelRef()
      if (panel && panel.loadData) {
        panel.loadData()
      }
    },
    getActivePanelRef() {
      if (this.activeTab === 'main') {
        return this.$refs.mainPanel
      }
      const subSystemId = this.activeTab.replace('sub-', '')
      const ref = this.$refs[`subPanel-${subSystemId}`]
      return Array.isArray(ref) ? ref[0] : ref
    },
    handleSave() {
      this.saving = true
      if (this.activeTab === 'main') {
        const panel = this.$refs.mainPanel
        saveUserQuickNav({ menuIds: panel.getSelectedMenuIds() })
          .then(() => {
            this.$modal.msgSuccess('主系统快捷导航保存成功')
            this.$emit('saved', { scope: 'main', menuIds: panel.getSelectedMenuIds() })
            this.visible = false
          })
          .finally(() => {
            this.saving = false
          })
        return
      }
      const subSystemId = Number(this.activeTab.replace('sub-', ''))
      const panel = this.getActivePanelRef()
      saveSubSystemUserQuickNav({
        subSystemId,
        menuIds: panel.getSelectedMenuIds()
      })
        .then(() => {
          this.$modal.msgSuccess('子系统快捷导航保存成功')
          this.$emit('saved', { scope: 'sub', subSystemId, menuIds: panel.getSelectedMenuIds() })
          this.visible = false
        })
        .finally(() => {
          this.saving = false
        })
    }
  },
  watch: {
    activeTab() {
      this.$nextTick(() => this.loadActivePanel())
    }
  }
}
</script>

<style lang="scss" scoped>
.quick-nav-tabs {
  ::v-deep .el-tabs__content {
    padding-top: 8px;
  }
}
</style>
