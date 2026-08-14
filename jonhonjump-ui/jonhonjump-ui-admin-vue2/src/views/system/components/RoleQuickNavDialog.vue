<template>
  <el-dialog
    title="默认快捷导航"
    :visible.sync="dialogVisible"
    width="500px"
    append-to-body
    custom-class="role-quick-nav-dialog"
    @closed="handleClosed"
  >
    <el-form label-width="88px">
      <el-form-item label="角色名称">
        <el-input :value="roleName" disabled />
      </el-form-item>
      <el-form-item label="角色标识">
        <el-input :value="roleCode" disabled />
      </el-form-item>
      <el-form-item label="快捷导航">
        <el-checkbox v-model="menuExpand" @change="handleTreeExpand">展开/折叠</el-checkbox>
        <el-checkbox v-model="menuNodeAll" @change="handleCheckAll">全选/全不选</el-checkbox>
        <el-tree
          ref="menuTree"
          class="tree-border role-quick-nav-tree"
          :data="menuTree"
          show-checkbox
          node-key="id"
          :check-strictly="menuCheckStrictly"
          :default-expand-all="menuExpand"
          :props="treeProps"
          empty-text="加载中，请稍后"
          @check="handleTreeCheck"
          @node-expand="refreshDeniedCheckboxMarks"
          @node-collapse="refreshDeniedCheckboxMarks"
        />
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button type="primary" :loading="saving" @click="submit">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { collectOrderedQuickNavMenuIds } from '@/utils/roleQuickNavMenus'
import { restoreQuickNavLeafCheckedKeys } from '@/utils/roleMenuTree'

export default {
  name: 'RoleQuickNavDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    roleName: {
      type: String,
      default: ''
    },
    roleCode: {
      type: String,
      default: ''
    },
    menuTree: {
      type: Array,
      default: () => []
    },
    leafMenuIds: {
      type: Array,
      default: () => []
    },
    menuIds: {
      type: Array,
      default: () => []
    },
    saving: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      menuExpand: false,
      menuNodeAll: false,
      menuCheckStrictly: false,
      syncSeq: 0,
      treeProps: {
        label: 'name',
        children: 'children',
        disabled: 'disabled'
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.$nextTick(() => {
          this.syncCheckedKeys()
        })
      }
    },
    menuIds: {
      handler() {
        if (this.visible) {
          this.$nextTick(() => {
            this.syncCheckedKeys()
          })
        }
      },
      deep: true
    },
    menuTree: {
      handler() {
        if (this.visible) {
          this.$nextTick(() => {
            this.syncCheckedKeys()
          })
        }
      },
      deep: true
    }
  },
  methods: {
    refreshDeniedCheckboxMarks() {
      this.$nextTick(() => {
        const tree = this.$refs.menuTree
        const root = tree && tree.$el
        if (!root || !tree.store || !tree.store.nodesMap) {
          return
        }
        root.querySelectorAll('.el-checkbox__input.quick-nav-denied').forEach(input => {
          input.classList.remove('quick-nav-denied')
        })
        Object.keys(tree.store.nodesMap).forEach(key => {
          const node = tree.store.nodesMap[key]
          if (!node || !node.data || !node.data.disabled || !node.$el) {
            return
          }
          const input = node.$el.querySelector('.el-checkbox__input')
          if (input) {
            input.classList.add('quick-nav-denied')
          }
        })
      })
    },
    handleTreeCheck() {
      this.menuNodeAll = this.isAllLeafChecked()
      this.refreshDeniedCheckboxMarks()
    },
    syncCheckedKeys() {
      if (!this.$refs.menuTree) {
        return
      }
      const seq = ++this.syncSeq
      const leafIdSet = new Set((this.leafMenuIds || []).map(id => Number(id)).filter(id => id > 0))
      const validMenuIds = (this.menuIds || [])
        .map(id => Number(id))
        .filter(id => id > 0 && leafIdSet.has(id))
      restoreQuickNavLeafCheckedKeys(
        this,
        this.$refs.menuTree,
        validMenuIds,
        value => { this.menuCheckStrictly = value },
        () => {
          if (seq !== this.syncSeq) {
            return
          }
          this.menuNodeAll = this.isAllLeafChecked()
          this.refreshDeniedCheckboxMarks()
        }
      )
    },
    isAllLeafChecked() {
      const leafIds = (this.leafMenuIds || []).map(id => Number(id)).filter(id => id > 0)
      if (!leafIds.length) {
        return false
      }
      const checkedSet = new Set(
        (this.$refs.menuTree ? this.$refs.menuTree.getCheckedKeys() : []).map(id => Number(id))
      )
      return leafIds.every(id => checkedSet.has(id))
    },
    handleTreeExpand(expand) {
      this.toggleTreeExpand(this.menuTree, expand)
      this.refreshDeniedCheckboxMarks()
    },
    toggleTreeExpand(nodes, expand) {
      if (!this.$refs.menuTree || !nodes || !nodes.length) {
        return
      }
      nodes.forEach(node => {
        const treeNode = this.$refs.menuTree.store.nodesMap[node.id]
          || this.$refs.menuTree.store.nodesMap[String(node.id)]
          || this.$refs.menuTree.store.nodesMap[Number(node.id)]
        if (treeNode) {
          treeNode.expanded = expand
        }
        if (node.children && node.children.length) {
          this.toggleTreeExpand(node.children, expand)
        }
      })
    },
    handleCheckAll(checked) {
      if (!this.$refs.menuTree) {
        return
      }
      const leafIds = (this.leafMenuIds || []).map(id => Number(id)).filter(id => id > 0)
      restoreQuickNavLeafCheckedKeys(
        this,
        this.$refs.menuTree,
        checked ? leafIds : [],
        value => { this.menuCheckStrictly = value },
        () => {
          this.menuNodeAll = checked
          this.refreshDeniedCheckboxMarks()
        }
      )
    },
    submit() {
      if (!this.$refs.menuTree) {
        this.$emit('save', [])
        return
      }
      const checkedIds = [
        ...this.$refs.menuTree.getCheckedKeys(),
        ...this.$refs.menuTree.getHalfCheckedKeys()
      ]
      const menuIds = collectOrderedQuickNavMenuIds(this.menuTree, checkedIds)
      this.$emit('save', menuIds)
    },
    handleClosed() {
      this.menuExpand = false
      this.menuNodeAll = false
      this.menuCheckStrictly = false
      this.syncSeq += 1
      if (this.$refs.menuTree) {
        this.$refs.menuTree.setCheckedKeys([])
      }
    }
  }
}
</script>

<style scoped>
.role-quick-nav-tree {
  max-height: 420px;
  overflow: auto;
  margin-top: 8px;
}
</style>
