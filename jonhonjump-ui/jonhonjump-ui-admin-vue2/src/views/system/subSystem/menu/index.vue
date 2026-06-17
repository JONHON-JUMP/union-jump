<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 外部系统列表 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="clientKeyword"
            placeholder="请输入系统名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container sub-system-list">
          <div
            v-for="item in filteredClientList"
            :key="item.id"
            class="sub-system-item"
            :class="{ 'is-active': selectedClient && selectedClient.id === item.id }"
            @click="handleClientClick(item)"
          >
            <div class="sub-system-item__name">{{ item.name }}</div>
            <div class="sub-system-item__meta">
              <span>{{ item.clientId }}</span>
              <el-tag size="mini" type="info">{{ item.menuCount || 0 }} 菜单</el-tag>
            </div>
          </div>
          <el-empty v-if="filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 菜单数据 -->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="菜单名称" prop="name">
            <el-input v-model="queryParams.name" placeholder="请输入菜单名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="菜单状态" clearable style="width: 240px">
              <el-option v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd()"
                       v-hasPermi="['sub-system:menu:create']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="checkedIds.length === 0"
              @click="handleDeleteBatch"
              v-hasPermi="['sub-system:menu:delete']"
            >批量删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
        </el-row>

        <el-table v-if="refreshTable" v-loading="loading" :data="menuList" row-key="id"
                  :default-expand-all="isExpandAll"
                  :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
                  @selection-change="handleRowCheckboxChange">
          <el-table-column type="selection" width="55"/>
          <el-table-column prop="name" label="菜单名称" :show-overflow-tooltip="true" width="200"/>
          <el-table-column prop="icon" label="图标" align="center" width="80">
            <template v-slot="scope">
              <svg-icon :icon-class="scope.row.icon" />
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="60"/>
          <el-table-column prop="permission" label="权限标识" :show-overflow-tooltip="true" />
          <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true" />
          <el-table-column prop="status" label="状态" width="80">
            <template v-slot="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:menu:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)"
                         v-hasPermi="['sub-system:menu:create']">新增</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:menu:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="外部系统">
              <el-input :value="formSubSystemLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <treeselect v-model="form.parentId" :options="menuOptions" :normalizer="normalizer" :show-count="true"
                          placeholder="选择上级菜单"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio v-for="dict in menuTypeDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="菜单图标">
              <el-popover placement="bottom-start" width="460" trigger="click" @show="$refs['iconSelect'].reset()">
                <IconSelect ref="iconSelect" @selected="selected" />
                <el-input slot="reference" v-model="form.icon" placeholder="点击选择图标" readonly>
                  <svg-icon v-if="form.icon" slot="prefix" :icon-class="form.icon" class="el-input__icon"
                            style="height: 32px;width: 16px;"/>
                  <i v-else slot="prefix" class="el-icon-search el-input__icon" />
                </el-input>
              </el-popover>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.DIR" label="权限标识">
              <el-input v-model="form.permission" placeholder="请输入权限标识" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.type === MenuTypeEnum.MENU">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="例如说：system/user/index" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.type === MenuTypeEnum.MENU">
            <el-form-item label="组件名称" prop="componentName">
              <el-input v-model="form.componentName" placeholder="例如说：SystemUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in statusDictDatas" :key="dict.value" :label="parseInt(dict.value)">
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio :label="true">显示</el-radio>
                <el-radio :label="false">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type === MenuTypeEnum.MENU" label="是否缓存">
              <el-radio-group v-model="form.keepAlive">
                <el-radio :label="true">缓存</el-radio>
                <el-radio :label="false">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="总是显示">
              <el-radio-group v-model="form.alwaysShow">
                <el-radio :label="true">总是</el-radio>
                <el-radio :label="false">不是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import IconSelect from '@/components/IconSelect'
import {
  createSubSystemMenu,
  deleteSubSystemMenu,
  deleteSubSystemMenuList,
  getSubSystemClientSimpleList,
  getSubSystemMenu,
  getSubSystemMenuList,
  updateSubSystemMenu
} from '@/api/system/subSystemMenu'
import { SystemMenuTypeEnum, CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'
import { isExternal } from '@/utils/validate'

export default {
  name: 'SubSystemMenu',
  components: { Treeselect, IconSelect },
  data() {
    return {
      loading: false,
      showSearch: true,
      menuList: [],
      menuOptions: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      title: '',
      open: false,
      isExpandAll: false,
      refreshTable: true,
      checkedIds: [],
      queryParams: {
        name: undefined,
        status: undefined
      },
      form: {},
      rules: {
        name: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
        sort: [{ required: true, message: '菜单顺序不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'blur' }]
      },
      MenuTypeEnum: SystemMenuTypeEnum,
      menuTypeDictDatas: getDictDatas(DICT_TYPE.SYSTEM_MENU_TYPE),
      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS)
    }
  },
  computed: {
    filteredClientList() {
      const keyword = (this.clientKeyword || '').trim().toLowerCase()
      if (!keyword) {
        return this.clientList
      }
      return this.clientList.filter(item =>
        (item.name && item.name.toLowerCase().includes(keyword)) ||
        (item.clientId && item.clientId.toLowerCase().includes(keyword))
      )
    },
    formSubSystemLabel() {
      const id = this.form.subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      const item = this.clientList.find(client => client.id === id)
      return item ? item.name + ' (' + item.clientId + ')' : ''
    }
  },
  created() {
    this.loadClientList()
  },
  methods: {
    selected(name) {
      this.form.icon = name
    },
    loadClientList() {
      getSubSystemClientSimpleList().then(res => {
        this.clientList = res.data || []
        if (!this.selectedClient && this.clientList.length > 0) {
          this.handleClientClick(this.clientList[0])
        }
      })
    },
    handleClientClick(item) {
      this.selectedClient = item
      this.queryParams.name = undefined
      this.queryParams.status = undefined
      this.getList()
    },
    getList() {
      if (!this.selectedClient) {
        this.menuList = []
        return
      }
      this.loading = true
      getSubSystemMenuList({
        subSystemId: this.selectedClient.id,
        name: this.queryParams.name,
        status: this.queryParams.status
      }).then(res => {
        this.menuList = this.handleTree(res.data || [], 'id')
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.getList()
    },
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.id,
        label: node.name,
        children: node.children
      }
    },
    getTreeselect(subSystemId) {
      const id = subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      if (!id) {
        this.menuOptions = []
        return Promise.resolve()
      }
      return getSubSystemMenuList({ subSystemId: id }).then(res => {
        this.menuOptions = []
        const menu = { id: 0, name: '主类目', children: [] }
        menu.children = this.handleTree(res.data || [], 'id')
        this.menuOptions.push(menu)
      })
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    resetFormData() {
      this.form = {
        id: undefined,
        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,
        parentId: 0,
        name: undefined,
        icon: undefined,
        type: SystemMenuTypeEnum.DIR,
        sort: 0,
        path: undefined,
        permission: undefined,
        component: undefined,
        componentName: undefined,
        status: CommonStatusEnum.ENABLE,
        visible: true,
        keepAlive: true,
        alwaysShow: true
      }
      this.resetForm('form')
    },
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    handleAdd(row) {
      if (!this.selectedClient) {
        this.$modal.msgWarning('请先在左侧选择外部系统')
        return
      }
      this.resetFormData()
      this.getTreeselect(this.selectedClient.id).then(() => {
        if (row != null && row.id) {
          this.form.parentId = row.id
        } else {
          this.form.parentId = 0
        }
        this.open = true
        this.title = '添加外部系统菜单'
      })
    },
    handleUpdate(row) {
      this.resetFormData()
      const subSystemId = row.subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      this.getTreeselect(subSystemId).then(() => {
        getSubSystemMenu(row.id).then(res => {
          this.form = {
            id: res.data.id,
            subSystemId: res.data.subSystemId,
            parentId: res.data.parentId,
            name: res.data.name,
            icon: res.data.icon,
            type: res.data.type,
            sort: res.data.sort,
            path: res.data.path,
            permission: res.data.permission,
            component: res.data.component,
            componentName: res.data.componentName,
            status: res.data.status,
            visible: res.data.visible,
            keepAlive: res.data.keepAlive,
            alwaysShow: res.data.alwaysShow
          }
          this.open = true
          this.title = '修改外部系统菜单'
        })
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.type === SystemMenuTypeEnum.DIR || this.form.type === SystemMenuTypeEnum.MENU) {
          const path = this.form.path
          if (path && !isExternal(path)) {
            if (this.form.parentId === 0 && path.charAt(0) !== '/') {
              this.$modal.msgError('根目录路由地址必须以 / 开头')
              return
            }
            if (this.form.parentId !== 0 && path.charAt(0) === '/') {
              this.$modal.msgError('非根目录路由地址不能以 / 开头')
              return
            }
          }
        }
        const request = this.form.id ? updateSubSystemMenu : createSubSystemMenu
        request(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.loadClientList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.name + '"的菜单？').then(() => {
        return deleteSubSystemMenu(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的外部系统菜单？').then(() => {
        return deleteSubSystemMenuList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.checkedIds = []
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleRowCheckboxChange(records) {
      this.checkedIds = records.map(item => item.id)
    }
  }
}
</script>

<style lang="scss" scoped>
.sub-system-list {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.sub-system-item {
  padding: 12px 14px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover,
  &.is-active {
    border-color: #409eff;
    background: #ecf5ff;
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
  }
}
</style>
