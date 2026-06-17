<template>
  <el-dialog title="配置快捷导航" :visible.sync="visible" width="720px" append-to-body @open="handleOpen">
    <div v-loading="loading" class="quick-nav-settings">
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="panel-title">可选菜单</div>
          <el-input
            v-model="keyword"
            placeholder="搜索菜单名称"
            prefix-icon="el-icon-search"
            clearable
            size="small"
            class="panel-search"
            @input="filterCandidateTree"
          />
          <div class="candidate-list">
            <el-tree
              ref="candidateTree"
              :data="candidateTree"
              :props="treeProps"
              node-key="id"
              default-expand-all
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              empty-text="暂无可选菜单"
            >
              <span slot-scope="{ node, data }" class="tree-node">
                <span class="tree-node__label">{{ node.label }}</span>
                <i
                  v-if="isMenuNode(data) && !selectedMenuIds.includes(data.id)"
                  class="el-icon-circle-plus-outline tree-node__add"
                  title="添加"
                  @click.stop="addMenu(data.id)"
                />
              </span>
            </el-tree>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="panel-title">我的快捷导航（拖拽排序）</div>
          <draggable v-model="selectedMenuIds" class="selected-list" handle=".drag-handle" animation="200">
            <div v-for="menuId in selectedMenuIds" :key="menuId" class="selected-item">
              <i class="el-icon-rank drag-handle" />
              <span class="selected-item__name">{{ getCandidateName(menuId) }}</span>
              <i class="el-icon-close selected-item__remove" @click="removeMenu(menuId)" />
            </div>
          </draggable>
          <el-empty v-if="selectedMenuIds.length === 0" description="请从左侧添加菜单" :image-size="60" />
        </el-col>
      </el-row>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import draggable from 'vuedraggable'
import { getUserQuickNavCandidates, getUserQuickNavList, saveUserQuickNav } from '@/api/system/user/quickNav'
import { SystemMenuTypeEnum } from '@/utils/constants'

export default {
  name: 'QuickNavSettings',
  components: { draggable },
  props: {
    value: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      saving: false,
      keyword: '',
      candidateTree: [],
      candidateMap: {},
      selectedMenuIds: [],
      treeProps: {
        label: 'name',
        children: 'children'
      }
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
    }
  },
  methods: {
    handleOpen() {
      this.keyword = ''
      this.loadData()
    },
    loadData() {
      this.loading = true
      Promise.all([getUserQuickNavList(), getUserQuickNavCandidates()])
        .then(([configRes, candidateRes]) => {
          const config = configRes.data || {}
          this.candidateTree = candidateRes.data || []
          this.candidateMap = this.buildCandidateMap(this.candidateTree)
          this.selectedMenuIds = config.menuIds || []
          this.$nextTick(() => this.filterCandidateTree())
        })
        .finally(() => {
          this.loading = false
        })
    },
    buildCandidateMap(nodes, map = {}) {
      if (!nodes) {
        return map
      }
      nodes.forEach(node => {
        map[node.id] = node
        if (node.children && node.children.length) {
          this.buildCandidateMap(node.children, map)
        }
      })
      return map
    },
    isMenuNode(data) {
      return data.type === SystemMenuTypeEnum.MENU
    },
    filterNode(value, data) {
      if (!value) {
        return true
      }
      return data.name && data.name.toLowerCase().includes(value.toLowerCase())
    },
    filterCandidateTree() {
      if (this.$refs.candidateTree) {
        this.$refs.candidateTree.filter(this.keyword.trim())
      }
    },
    getCandidateName(menuId) {
      const item = this.candidateMap[menuId]
      return item ? item.name : `菜单#${menuId}`
    },
    addMenu(menuId) {
      if (!this.selectedMenuIds.includes(menuId)) {
        this.selectedMenuIds.push(menuId)
      }
    },
    removeMenu(menuId) {
      this.selectedMenuIds = this.selectedMenuIds.filter(id => id !== menuId)
    },
    handleSave() {
      this.saving = true
      saveUserQuickNav({ menuIds: this.selectedMenuIds })
        .then(() => {
          this.$modal.msgSuccess('保存成功')
          this.visible = false
          this.$emit('saved', [...this.selectedMenuIds])
        })
        .finally(() => {
          this.saving = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.panel-search {
  margin-bottom: 10px;
}

.candidate-list,
.selected-list {
  min-height: 280px;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}

.selected-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 4px;
  margin-bottom: 6px;
  background: #fafafa;
}

.selected-item__name {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.selected-item__remove {
  cursor: pointer;
  color: #999;

  &:hover {
    color: #f56c6c;
  }
}

.drag-handle {
  cursor: move;
  color: #999;
  margin-right: 8px;
}

.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  padding-right: 8px;
  font-size: 13px;
}

.tree-node__label {
  flex: 1;
}

.tree-node__add {
  color: #1890ff;
  font-size: 16px;
  margin-left: 8px;

  &:hover {
    color: #40a9ff;
  }
}
</style>
