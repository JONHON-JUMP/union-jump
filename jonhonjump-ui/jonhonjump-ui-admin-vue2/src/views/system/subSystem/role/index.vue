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
              <el-tag size="mini" type="info">{{ item.roleCount || 0 }} 角色</el-tag>
            </div>
          </div>
          <el-empty v-if="filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 角色数据 -->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="queryParams.name" placeholder="请输入角色名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="角色标识" prop="code">
            <el-input v-model="queryParams.code" placeholder="请输入角色标识" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="角色状态" clearable style="width: 240px">
              <el-option v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)"/>
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间" prop="createTime">
            <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss" type="daterange"
                            range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :default-time="['00:00:00', '23:59:59']" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                       v-hasPermi="['sub-system:role:create']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="checkedIds.length === 0"
              @click="handleDeleteBatch"
              v-hasPermi="['sub-system:role:delete']"
            >批量删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
        </el-row>

        <el-table v-loading="loading" :data="roleList" @selection-change="handleRowCheckboxChange">
          <el-table-column type="selection" width="55"/>
          <el-table-column label="角色编号" prop="id" width="120" />
          <el-table-column label="角色名称" prop="name" :show-overflow-tooltip="true" width="150" />
          <el-table-column label="角色标识" prop="code" :show-overflow-tooltip="true" width="150" />
          <el-table-column label="角色类型" prop="type" width="80">
            <template v-slot="scope">
              <dict-tag :type="DICT_TYPE.SYSTEM_ROLE_TYPE" :value="scope.row.type"/>
            </template>
          </el-table-column>
          <el-table-column label="显示顺序" prop="sort" width="100" />
          <el-table-column label="状态" align="center" width="100">
            <template v-slot="scope">
              <el-switch v-model="scope.row.status" :active-value="0" :inactive-value="1" @change="handleStatusChange(scope.row)"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:role:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-circle-check" @click="handleMenu(scope.row)"
                         v-hasPermi="['sub-system:role:update']">菜单权限</el-button>
              <el-button size="mini" type="text" icon="el-icon-circle-check" @click="handleDataScope(scope.row)"
                         v-hasPermi="['sub-system:role:update']">数据权限</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:role:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                    @pagination="getList"/>
      </el-col>
    </el-row>

    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="外部系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色标识" />
        </el-form-item>
        <el-form-item label="角色顺序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配数据权限 -->
    <el-dialog title="分配数据权限" :visible.sync="openDataScope" width="500px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="form.code" disabled />
        </el-form-item>
        <el-form-item label="权限范围">
          <el-select v-model="form.dataScope">
            <el-option
              v-for="item in dataScopeDictDatas"
              :key="parseInt(item.value)"
              :label="item.label"
              :value="parseInt(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据权限" v-show="form.dataScope === SysDataScopeEnum.DEPT_CUSTOM">
          <el-checkbox :checked="!form.deptCheckStrictly" @change="handleCheckedTreeConnect($event, 'dept')">父子联动(选中父节点，自动选择子节点)</el-checkbox>
          <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpand($event, 'dept')">展开/折叠</el-checkbox>
          <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAll($event, 'dept')">全选/全不选</el-checkbox>
          <el-tree
            class="tree-border"
            :data="deptOptions"
            show-checkbox
            default-expand-all
            ref="dept"
            node-key="id"
            :check-strictly="form.deptCheckStrictly"
            empty-text="加载中，请稍后"
            :props="defaultProps"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitDataScope">确 定</el-button>
        <el-button @click="openDataScope = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配菜单权限 -->
    <el-dialog title="分配菜单权限" :visible.sync="openMenu" width="500px" append-to-body>
      <el-form :model="menuForm" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="menuForm.name" disabled />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="menuForm.code" disabled />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
          <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
          <el-tree
            class="tree-border"
            :data="menuOptions"
            show-checkbox
            ref="menu"
            node-key="id"
            :check-strictly="menuCheckStrictly"
            empty-text="加载中，请稍后"
            :props="defaultProps"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitMenu">确 定</el-button>
        <el-button @click="openMenu = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignSubSystemRoleDataScope,
  assignSubSystemRoleMenu,
  createSubSystemRole,
  deleteSubSystemRole,
  deleteSubSystemRoleList,
  getSubSystemClientSimpleList,
  getSubSystemMenuSimpleList,
  getSubSystemRole,
  getSubSystemRoleMenuIds,
  getSubSystemRolePage,
  updateSubSystemRole,
  updateSubSystemRoleStatus
} from '@/api/system/subSystemRole'
import { listSimpleDepts } from '@/api/system/dept'
import { CommonStatusEnum, SystemDataScopeEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

export default {
  name: 'SubSystemRole',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      roleList: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      title: '',
      open: false,
      openDataScope: false,
      openMenu: false,
      menuExpand: false,
      menuNodeAll: false,
      deptExpand: true,
      deptNodeAll: false,
      menuCheckStrictly: true,
      menuOptions: [],
      deptOptions: [],
      depts: [],
      menuForm: {},
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        name: undefined,
        code: undefined,
        status: undefined,
        createTime: []
      },
      form: {},
      defaultProps: {
        label: 'name',
        children: 'children'
      },
      rules: {
        name: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
        code: [{ required: true, message: '角色标识不能为空', trigger: 'blur' }],
        sort: [{ required: true, message: '角色顺序不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
      },
      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS),
      dataScopeDictDatas: getDictDatas(DICT_TYPE.SYSTEM_DATA_SCOPE),
      SysDataScopeEnum: SystemDataScopeEnum
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
    }
  },
  created() {
    this.loadClientList()
  },
  methods: {
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
      this.queryParams.code = undefined
      this.queryParams.status = undefined
      this.queryParams.createTime = []
      this.queryParams.pageNo = 1
      this.getList()
    },
    getList() {
      if (!this.selectedClient) {
        this.roleList = []
        this.total = 0
        return
      }
      this.loading = true
      getSubSystemRolePage({
        ...this.queryParams,
        subSystemId: this.selectedClient.id
      }).then(res => {
        this.roleList = res.data.list || []
        this.total = res.data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetFormData() {
      this.form = {
        id: undefined,
        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,
        name: undefined,
        code: undefined,
        sort: 0,
        status: CommonStatusEnum.ENABLE,
        dataScope: undefined,
        deptCheckStrictly: false,
        menuCheckStrictly: true
      }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      if (!this.selectedClient) {
        this.$modal.msgWarning('请先选择外部系统')
        return
      }
      this.resetFormData()
      this.open = true
      this.title = '添加外部系统角色'
    },
    handleUpdate(row) {
      this.resetFormData()
      getSubSystemRole(row.id).then(res => {
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId,
          name: res.data.name,
          code: res.data.code,
          sort: res.data.sort,
          status: res.data.status
        }
        this.open = true
        this.title = '修改外部系统角色'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.id ? updateSubSystemRole : createSubSystemRole
        request(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.loadClientList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除角色"' + row.name + '"？').then(() => {
        return deleteSubSystemRole(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的外部系统角色？').then(() => {
        return deleteSubSystemRoleList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.checkedIds = []
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '""' + row.name + '"角色吗?').then(() => {
        return updateSubSystemRoleStatus(row.id, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
      })
    },
    handleMenu(row) {
      this.menuForm = {
        roleId: row.id,
        name: row.name,
        code: row.code
      }
      this.menuExpand = false
      this.menuNodeAll = false
      this.openMenu = true
      getSubSystemMenuSimpleList(row.subSystemId).then(res => {
        this.menuOptions = this.handleTree(res.data || [], 'id')
        this.$nextTick(() => {
          getSubSystemRoleMenuIds(row.id).then(menuRes => {
            this.menuCheckStrictly = true
            this.$refs.menu.setCheckedKeys(menuRes.data || [])
            this.menuCheckStrictly = false
          })
        })
      })
    },
    handleDataScope(row) {
      this.form = {
        id: row.id,
        name: row.name,
        code: row.code,
        dataScope: undefined,
        deptCheckStrictly: false
      }
      this.deptExpand = true
      this.deptNodeAll = false
      this.openDataScope = true
      listSimpleDepts().then(res => {
        this.deptOptions = this.handleTree(res.data || [], 'id')
        this.depts = res.data || []
        getSubSystemRole(row.id).then(roleRes => {
          this.form.dataScope = roleRes.data.dataScope
          this.form.deptCheckStrictly = roleRes.data.deptCheckStrictly === 1
          this.$nextTick(() => {
            if (this.$refs.dept) {
              this.$refs.dept.setCheckedKeys(roleRes.data.dataScopeDeptIds || [], false)
            }
          })
        })
      })
    },
    handleCheckedTreeExpand(value, type) {
      if (type === 'menu') {
        const treeList = this.menuOptions
        for (let i = 0; i < treeList.length; i++) {
          this.$refs.menu.store.nodesMap[treeList[i].id].expanded = value
        }
      } else if (type === 'dept') {
        const treeList = this.deptOptions
        for (let i = 0; i < treeList.length; i++) {
          this.$refs.dept.store.nodesMap[treeList[i].id].expanded = value
        }
      }
    },
    handleCheckedTreeNodeAll(value, type) {
      if (type === 'menu') {
        this.$refs.menu.setCheckedNodes(value ? this.menuOptions : [])
      } else if (type === 'dept') {
        this.$refs.dept.setCheckedNodes(value ? this.depts : [])
      }
    },
    handleCheckedTreeConnect(value, type) {
      if (type === 'dept') {
        this.form.deptCheckStrictly = !value
      }
    },
    submitDataScope() {
      if (this.form.id === undefined) {
        return
      }
      assignSubSystemRoleDataScope({
        roleId: this.form.id,
        dataScope: this.form.dataScope,
        dataScopeDeptIds: this.form.dataScope !== SystemDataScopeEnum.DEPT_CUSTOM ? [] :
          this.$refs.dept.getCheckedKeys()
      }).then(() => {
        this.$modal.msgSuccess('修改成功')
        this.openDataScope = false
        this.getList()
      })
    },
    submitMenu() {
      assignSubSystemRoleMenu({
        roleId: this.menuForm.roleId,
        menuIds: [...this.$refs.menu.getCheckedKeys(), ...this.$refs.menu.getHalfCheckedKeys()]
      }).then(() => {
        this.$modal.msgSuccess('分配成功')
        this.openMenu = false
      })
    },
    handleRowCheckboxChange(selection) {
      this.checkedIds = selection.map(item => item.id)
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

.tree-border {
  margin-top: 5px;
  border: 1px solid #e5e6e7;
  background: #fff none;
  border-radius: 4px;
  width: 100%;
}
</style>
