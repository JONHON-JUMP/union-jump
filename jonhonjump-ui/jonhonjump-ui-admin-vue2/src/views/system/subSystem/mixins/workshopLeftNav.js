/**
 * 业务人员/角色左侧：以人为中心后改为「按业务系统」平铺；
 * 车间仅作筛选（数据来自 sub_system_workshop 字典），不再做对照树主导航。
 */
import { getSubSystemWorkshopList } from '@/api/system/subSystemWorkshop'

export default {
  data() {
    return {
      workshopList: [],
      selectedWorkshop: null,
      leftNavMode: 'system',
      leftNavTreeProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  computed: {
    leftNavItems() {
      return this.leftNavTreeData
    },
    /** 系统平铺列表（不再用车间树作主入口） */
    leftNavTreeData() {
      const keyword = (this.clientKeyword || '').trim().toLowerCase()
      const countKey = this.leftNavCountKey || 'userCount'
      return (this.clientList || [])
        .map(c => ({
          nodeKey: 'sys-' + c.id,
          id: c.id,
          label: c.name || c.clientId,
          navType: 'system',
          clientId: c.clientId,
          name: c.name,
          workshopScopeEnabled: !!c.workshopScopeEnabled,
          userCount: c.userCount || 0,
          roleCount: c.roleCount || 0,
          count: Number(c[countKey]) || 0
        }))
        .filter(c => this.matchLeftNavKeyword(c, keyword))
    },
    leftNavEmptyText() {
      return '暂无业务系统'
    },
    leftNavCurrentKey() {
      if (this.selectedClient) {
        return 'sys-' + this.selectedClient.id
      }
      return null
    },
    /** 当前系统下的车间字典，供顶部筛选 */
    workshopFilterOptions() {
      if (!this.selectedClient || !this.selectedClient.id) {
        return []
      }
      return (this.workshopList || []).filter(w => w.subSystemId === this.selectedClient.id)
    }
  },
  watch: {
    clientKeyword(val) {
      if (this.$refs.leftNavTree) {
        this.$refs.leftNavTree.filter(val)
      }
    },
    leftNavCurrentKey(key) {
      this.$nextTick(() => {
        if (this.$refs.leftNavTree && key) {
          this.$refs.leftNavTree.setCurrentKey(key)
        }
      })
    }
  },
  methods: {
    matchLeftNavKeyword(item, keyword) {
      if (!keyword) {
        return true
      }
      const blob = [item.label, item.name, item.clientId].filter(Boolean).join(' ').toLowerCase()
      return blob.includes(keyword)
    },
    loadWorkshopList() {
      return getSubSystemWorkshopList().then(res => {
        this.workshopList = res.data || []
      }).catch(() => {
        this.workshopList = []
      })
    },
    filterLeftNavNode(value, data) {
      if (!value) {
        return true
      }
      return this.matchLeftNavKeyword(data, String(value).trim().toLowerCase())
    },
    handleLeftNavTreeClick(data) {
      this.handleLeftNavClick(data)
    },
    handleLeftNavClick(item) {
      if (!item || item.navType === 'group') {
        return
      }
      this.leftNavMode = 'system'
      this.selectedWorkshop = null
      this.selectedClient = {
        id: item.id,
        clientId: item.clientId,
        name: item.name,
        workshopScopeEnabled: item.workshopScopeEnabled,
        userCount: item.userCount,
        roleCount: item.roleCount
      }
      this.onSystemNavSelected(item)
    },
    onWorkshopNavSelected() {},
    onSystemNavSelected() {}
  }
}
