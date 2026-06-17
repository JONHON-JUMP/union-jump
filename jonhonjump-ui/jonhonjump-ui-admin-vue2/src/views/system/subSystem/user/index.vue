<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 外部系统列表（sub_system） -->
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
              <el-tag size="mini" type="info">{{ item.userCount || 0 }} 人</el-tag>
            </div>
          </div>
          <el-empty v-if="filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 用户数据（子表 sub_system_users） -->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="外部系统" prop="subSystemId">
            <el-select
              v-model="queryParams.subSystemId"
              placeholder="请选择外部系统"
              clearable
              filterable
              style="width: 240px"
            >
              <el-option
                v-for="item in clientList"
                :key="item.id"
                :label="item.name + ' (' + item.clientId + ')'"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="queryParams.nickname" placeholder="请输入用户姓名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="域账号" prop="domainNo">
            <el-input v-model="queryParams.domainNo" placeholder="请输入域账号" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="用户工号" prop="employeeNo">
            <el-input v-model="queryParams.employeeNo" placeholder="请输入用户工号" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="班组名称" prop="teamName">
            <el-input v-model="queryParams.teamName" placeholder="请输入班组名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                         v-hasPermi="['sub-system:user:create']">新增</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button
                type="danger"
                plain
                icon="el-icon-delete"
                size="mini"
                :disabled="checkedIds.length === 0"
                @click="handleDeleteBatch"
                v-hasPermi="['sub-system:user:delete']"
              >批量删除</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
          </el-row>

          <el-table v-loading="loading" :data="userList" @selection-change="handleRowCheckboxChange">
            <el-table-column type="selection" width="55" fixed="left" />
            <el-table-column label="系统" align="center" prop="clientName" width="120" fixed="left" :show-overflow-tooltip="true" />
            <el-table-column label="用户姓名" prop="nickname" :show-overflow-tooltip="true" width="100" fixed="left" />
            <el-table-column label="用户工号" prop="employeeNo" :show-overflow-tooltip="true" width="100" fixed="left" />
            <el-table-column label="刷卡卡号" prop="cardNo" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="ERP账号" width="120" :show-overflow-tooltip="true">
              <template v-slot="scope">
                <span>{{ formatErpNos(scope.row.erpNos) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="域账号" prop="domainNo" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="车间编号" prop="workshopId" width="100" />
            <el-table-column label="班组编号" prop="teamId" width="110" />
            <el-table-column label="班组名称" prop="teamName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="主页面" prop="homeMenuName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="岗位" prop="postNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="角色" prop="roleNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="状态" align="center" width="100">
              <template v-slot="scope">
                <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleStatusChange(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
              <template v-slot="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="200" fixed="right" class-name="small-padding fixed-width">
              <template v-slot="scope">
                <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                           v-hasPermi="['sub-system:user:update']">修改</el-button>
                <el-button size="mini" type="text" icon="el-icon-circle-check" @click="handleRole(scope.row)"
                           v-hasPermi="['sub-system:user:update']">分配角色</el-button>
                <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['sub-system:user:delete']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                      @pagination="getList"/>
      </el-col>
    </el-row>

    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="外部系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item v-if="form.id === undefined" label="主数据人员ID" prop="mainUserId">
          <el-input-number
            v-model="form.mainUserId"
            :min="1"
            controls-position="right"
            placeholder="请输入主数据人员ID"
            style="width: 100%"
            @change="handleMainUserIdChange"
          />
        </el-form-item>
        <el-divider content-position="left">主数据人员信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户姓名">
              <el-input v-model="mainUserInfo.nickname" disabled placeholder="请先选择主数据人员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户工号">
              <el-input v-model="mainUserInfo.employeeNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="刷卡卡号">
              <el-input v-model="mainUserInfo.cardNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ERP账号">
              <el-input :value="formatErpNos(mainUserInfo.erpNos)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="域账号">
              <el-input v-model="mainUserInfo.domainNo" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">外部系统关联信息</el-divider>
        <el-form-item label="车间编号" prop="workshopId">
          <el-input v-model="form.workshopId" placeholder="请输入车间编号" />
        </el-form-item>
        <el-form-item label="班组" prop="teamId">
          <el-select v-model="form.teamId" placeholder="请选择班组" clearable filterable style="width: 100%">
            <el-option
              v-for="item in teamOptions"
              :key="item.teamCode"
              :label="item.teamName + ' (' + item.teamCode + ')'"
              :value="item.teamCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width: 100%">
            <el-option
              v-for="item in postOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%"
                     @change="handleRoleIdsChange">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主页面" prop="homeMenuId">
          <treeselect
            v-model="form.homeMenuId"
            :options="menuPageOptions"
            :normalizer="homeMenuNormalizer"
            :show-count="true"
            :disable-branch-nodes="true"
            :disabled="!form.roleIds || form.roleIds.length === 0"
            placeholder="请先选择角色，再选择主页面"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog title="分配角色" :visible.sync="openRole" width="500px" append-to-body>
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="用户姓名">
          <el-input v-model="roleForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRole">确 定</el-button>
        <el-button @click="openRole = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import {
  assignSubSystemUserRole,
  createSubSystemUser,
  deleteSubSystemUser,
  deleteSubSystemUserList,
  getSubSystemClientSimpleList,
  getSubSystemPostSimpleList,
  getSubSystemRoleSimpleList,
  getSubSystemTeamSimpleList,
  getSubSystemUser,
  getSubSystemUserHomeMenuTree,
  getSubSystemUserPage,
  getSubSystemUserRoleIds,
  updateSubSystemUser,
  updateSubSystemUserStatus
} from '@/api/system/subSystemUsers'
import { getUser } from '@/api/system/user'

export default {
  name: 'SubSystemUser',
  components: { Treeselect },
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      userList: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      listSubSystemId: null,
      roleOptions: [],
      postOptions: [],
      teamOptions: [],
      menuPageOptions: [],
      title: '',
      open: false,
      openRole: false,
      form: {},
      roleForm: {},
      mainUserInfo: {},
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        subSystemId: undefined,
        mainUserId: undefined,
        workshopId: undefined,
        employeeNo: undefined,
        domainNo: undefined,
        nickname: undefined,
        teamName: undefined,
        status: undefined
      },
      rules: {
        mainUserId: [{ required: true, message: '主数据人员ID不能为空', trigger: 'blur' }]
      }
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
    formatErpNos(erpNos) {
      if (!erpNos) {
        return ''
      }
      if (Array.isArray(erpNos)) {
        return erpNos.join('、')
      }
      return String(erpNos)
    },
    resetMainUserInfo() {
      this.mainUserInfo = {
        nickname: undefined,
        employeeNo: undefined,
        cardNo: undefined,
        erpNos: undefined,
        domainNo: undefined
      }
    },
    fillMainUserInfo(data) {
      this.mainUserInfo = {
        nickname: data.nickname,
        employeeNo: data.employeeNo,
        cardNo: data.cardNo,
        erpNos: data.erpNos,
        domainNo: data.domainNo
      }
    },
    handleMainUserIdChange(mainUserId) {
      if (!mainUserId) {
        this.resetMainUserInfo()
        return
      }
      getUser(mainUserId).then(res => {
        this.fillMainUserInfo(res.data || {})
      }).catch(() => {
        this.resetMainUserInfo()
      })
    },
    loadClientList() {
      getSubSystemClientSimpleList().then(res => {
        this.clientList = res.data || []
        if (!this.selectedClient && this.clientList.length > 0) {
          this.handleClientClick(this.clientList[0])
        }
      })
    },
    loadSubOptions(subSystemId) {
      const id = subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      if (!id) {
        this.roleOptions = []
        this.postOptions = []
        this.teamOptions = []
        this.menuPageOptions = []
        return Promise.resolve()
      }
      return Promise.all([
        getSubSystemRoleSimpleList(id).then(res => { this.roleOptions = res.data || [] }),
        getSubSystemPostSimpleList(id).then(res => { this.postOptions = res.data || [] }),
        getSubSystemTeamSimpleList(id).then(res => { this.teamOptions = res.data || [] })
      ])
    },
    homeMenuNormalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.id,
        label: node.name,
        children: node.children
      }
    },
    loadHomeMenuOptions() {
      const subSystemId = this.form.subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      const roleIds = this.form.roleIds || []
      if (!subSystemId || roleIds.length === 0) {
        this.menuPageOptions = []
        return Promise.resolve()
      }
      return getSubSystemUserHomeMenuTree(subSystemId, roleIds).then(res => {
        this.menuPageOptions = res.data || []
      })
    },
    handleRoleIdsChange() {
      const currentHomeMenuId = this.form.homeMenuId
      this.loadHomeMenuOptions().then(() => {
        if (currentHomeMenuId && !this.isHomeMenuInTree(currentHomeMenuId, this.menuPageOptions)) {
          this.form.homeMenuId = undefined
        }
      })
    },
    isHomeMenuInTree(menuId, nodes) {
      if (!nodes || nodes.length === 0) {
        return false
      }
      for (const node of nodes) {
        if (node.id === menuId) {
          return true
        }
        if (this.isHomeMenuInTree(menuId, node.children)) {
          return true
        }
      }
      return false
    },
    handleClientClick(item) {
      this.selectedClient = item
      this.queryParams.subSystemId = item.id
      this.queryParams.nickname = undefined
      this.queryParams.domainNo = undefined
      this.queryParams.employeeNo = undefined
      this.queryParams.teamName = undefined
      this.queryParams.pageNo = 1
      this.listSubSystemId = item.id
      this.getList()
    },
    getList() {
      const params = { ...this.queryParams }
      if (this.listSubSystemId != null) {
        params.subSystemId = this.listSubSystemId
      } else if (params.subSystemId == null || params.subSystemId === '') {
        delete params.subSystemId
      }
      this.loading = true
      getSubSystemUserPage(params).then(res => {
        this.userList = res.data.list || []
        this.total = res.data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.listSubSystemId = null
      if (this.queryParams.subSystemId == null || this.queryParams.subSystemId === '') {
        this.queryParams.subSystemId = undefined
      }
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.subSystemId = this.selectedClient ? this.selectedClient.id : undefined
      this.listSubSystemId = this.selectedClient ? this.selectedClient.id : null
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetFormData() {
      this.form = {
        id: undefined,
        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,
        mainUserId: undefined,
        workshopId: undefined,
        teamId: undefined,
        homeMenuId: undefined,
        status: '0',
        remark: undefined,
        roleIds: [],
        postIds: []
      }
      this.resetMainUserInfo()
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
      this.loadSubOptions().then(() => {
        this.menuPageOptions = []
        this.open = true
        this.title = '添加外部系统用户'
      })
    },
    handleUpdate(row) {
      this.resetFormData()
      this.loadSubOptions(row.subSystemId).then(() => {
        getSubSystemUser(row.id).then(res => {
          this.form = {
            id: res.data.id,
            subSystemId: res.data.subSystemId,
            mainUserId: res.data.mainUserId,
            workshopId: res.data.workshopId,
            teamId: res.data.teamId,
            homeMenuId: res.data.homeMenuId,
            status: res.data.status || '0',
            remark: res.data.remark,
            roleIds: res.data.roleIds || [],
            postIds: res.data.postIds || []
          }
          this.fillMainUserInfo(res.data)
          return this.loadHomeMenuOptions()
        }).then(() => {
          this.open = true
          this.title = '修改外部系统用户'
        })
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.id ? updateSubSystemUser : createSubSystemUser
        request(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.loadClientList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该外部系统用户？').then(() => {
        return deleteSubSystemUser(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的外部系统用户？').then(() => {
        return deleteSubSystemUserList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '"编号为"' + row.id + '"的用户吗?').then(() => {
        return updateSubSystemUserStatus(row.id, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    handleRole(row) {
      this.loadSubOptions(row.subSystemId).then(() => {
        this.roleForm = {
          id: row.id,
          nickname: row.nickname,
          roleIds: []
        }
        getSubSystemUserRoleIds(row.id).then(res => {
          this.roleForm.roleIds = res.data || []
          this.openRole = true
        })
      })
    },
    submitRole() {
      assignSubSystemUserRole({
        id: this.roleForm.id,
        roleIds: this.roleForm.roleIds || []
      }).then(() => {
        this.$modal.msgSuccess('分配成功')
        this.openRole = false
        this.getList()
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
</style>
