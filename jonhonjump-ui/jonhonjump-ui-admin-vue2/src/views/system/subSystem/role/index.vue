<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 业务系统列表 -->
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
        <div class="head-container sub-system-list" v-loading="clientsLoading">
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
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无业务系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 角色数据 -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择已登记的业务系统；关联系统信息后，才可新增/维护该系统下的角色与权限"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
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
            <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport"
                       v-hasPermi="['sub-system:role:create']">导入</el-button>
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
          <el-table-column label="外部关联" align="center" width="110">
            <template v-slot="scope">
              <el-tag :type="scope.row.externalRoleId ? 'success' : 'info'" size="mini">
                {{ scope.row.externalRoleId ? '已关联外部' : '未关联' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="外部角色ID" prop="externalRoleId" min-width="170" :show-overflow-tooltip="true">
            <template v-slot="scope">
              <span>{{ scope.row.externalRoleId || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="330" class-name="small-padding fixed-width">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:role:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-connection" @click="handleBind(scope.row)"
                         v-hasPermi="['sub-system:role:update']">关联外部</el-button>
              <el-button size="mini" type="text" icon="el-icon-circle-check" @click="handleMenu(scope.row)"
                         v-hasPermi="['sub-system:role:update']">菜单权限</el-button>
              <el-button size="mini" type="text" icon="el-icon-menu" @click="handleQuickNav(scope.row)"
                         v-hasPermi="['sub-system:role:update']">快捷导航</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:role:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                    @pagination="getList"/>
      </el-col>
    </el-row>

    <!-- 新增/修改：关闭销毁表单，避免下次 resetFields 踩到上次残留字段 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body destroy-on-close>
      <el-form ref="form" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="业务系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色标识" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色顺序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="!form.id">
          <el-form-item label="同步注册">
            <el-checkbox
              v-model="form.syncToExternal"
              :disabled="!roleCreateApiReady"
              @change="handleSyncToExternalChange"
            >同步注册到外部系统（调「角色新增」接口）</el-checkbox>
            <div class="form-tip">
              <span v-if="roleCreateApiReady">新建后同时在对方系统建同名角色，成功后自动回存外部角色 ID</span>
              <span v-else style="color:#e6a23c">未找到已启用的「角色新增」接口。请到【接口管理】配置并启用；若已配在 Camstar人员管理，刷新后应能勾选</span>
            </div>
          </el-form-item>
          <el-form-item v-if="form.syncToExternal" label="接口目标" prop="apiSubSystemId">
            <el-select v-model="form.apiSubSystemId" placeholder="请选择调用哪个系统的角色新增接口" style="width: 100%">
              <el-option
                v-for="item in roleCreateApis"
                :key="item.subSystemId"
                :label="item.systemName"
                :value="item.subSystemId"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.syncToExternal" label="车间" prop="workshopCode">
            <el-input v-if="workshopOptions.length <= 1" :value="workshopFixedText()" disabled placeholder="暂无车间对照" />
            <el-select
              v-else
              v-model="form.workshopCode"
              placeholder="请选择已对照的部门车间"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in workshopOptions"
                :key="item.workshopCode"
                :label="workshopOptionLabel(item)"
                :value="item.workshopCode"
              />
            </el-select>
            <div v-if="workshopOptions.length === 1" class="form-tip">车间与部门是一对一，已确定</div>
            <div class="form-tip">对方系统角色名固定为 车间编号_角色名称</div>
            <div v-if="syncRoleNamePreview" class="form-tip">将同步角色名：<b>{{ syncRoleNamePreview }}</b></div>
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 关联外部角色：绑定外部系统已有角色的 roleId，之后分配角色按 ID 同步 -->
    <el-dialog title="关联外部角色" :visible.sync="bindOpen" width="560px" append-to-body
               custom-class="bind-external-role-dialog" :close-on-click-modal="false">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="角色名称">
          <el-input :value="bindForm.name + (bindForm.externalRoleId ? '（已关联 ' + bindForm.externalRoleId + '）' : '')" disabled />
        </el-form-item>
        <el-form-item label="接口目标" required>
          <el-select v-model="bindForm.apiSubSystemId" placeholder="请选择外部系统" style="width: 100%"
                     :disabled="bindLoading" @change="onBindFilterChange">
            <el-option
              v-for="item in roleCreateApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="车间" required>
          <el-input v-if="workshopOptions.length <= 1" :value="workshopFixedText()" disabled placeholder="暂无车间对照" />
          <el-select
            v-else
            v-model="bindForm.workshopCode"
            placeholder="请选择已对照的部门车间"
            filterable
            style="width: 100%"
            :disabled="bindLoading"
            @change="onBindFilterChange"
          >
            <el-option
              v-for="item in workshopOptions"
              :key="item.workshopCode"
              :label="workshopOptionLabel(item)"
              :value="item.workshopCode"
            />
          </el-select>
          <div v-if="workshopOptions.length === 1" class="form-tip">车间与部门是一对一，已确定</div>
        </el-form-item>
        <el-form-item label="外部角色">
          <div style="margin-bottom: 6px">
            <el-button size="mini" type="primary" plain :loading="bindLoading"
                       :disabled="!bindForm.apiSubSystemId || !bindForm.workshopCode"
                       @click="loadExternalRoles">查询外部角色</el-button>
            <el-button size="mini" type="primary" plain :loading="bindRegistering"
                       :disabled="!bindForm.apiSubSystemId || !bindForm.workshopCode"
                       @click="pushRegisterFromBind">新增外部角色</el-button>
          </div>
          <el-select
            ref="externalRoleSelect"
            :key="'ext-role-' + externalRoles.length + '-' + (bindForm.workshopCode || '')"
            v-model="bindForm.externalRoleId"
            filterable
            clearable
            :loading="bindLoading"
            :placeholder="bindLoading ? '正在查询外部角色…' : '选择对应的角色（角色名 + roleId）'"
            popper-class="external-role-dropdown"
            style="width: 100%"
          >
            <el-option
              v-for="item in externalRoles"
              :key="item.roleId"
              :label="item.roleName + '（' + item.roleId + '）'"
              :value="item.roleId"
            />
          </el-select>
          <div v-if="externalRoleTip" class="form-tip">{{ externalRoleTip }}</div>
          <div class="form-tip">绑定后「分配角色」勾选同步时按此 ID 上挂；对方系统手工挂的其他角色不受影响</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="bindForm.bound" type="danger" plain :loading="bindSubmitting" @click="submitBind(true)">解除关联</el-button>
        <el-button type="primary" :loading="bindSubmitting" :disabled="!bindForm.externalRoleId" @click="submitBind(false)">保存关联</el-button>
        <el-button @click="bindOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配菜单权限（含目录/页面/按钮；数据范围在业务系统本地配置） -->
    <el-dialog title="分配菜单权限" :visible.sync="openMenu" width="500px" append-to-body>
      <el-form :model="menuForm" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="menuForm.name" disabled />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="menuForm.code" disabled />
        </el-form-item>
        <el-form-item label="菜单权限">
          <div style="margin-bottom: 8px; color: #909399; font-size: 12px;">可勾选目录、页面及按钮权限；勾选页面时也会自动带上该页按钮。数据范围请在业务系统本地配置。</div>
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

    <role-quick-nav-dialog
      :visible.sync="openQuickNav"
      :role-name="quickNavForm.name"
      :role-code="quickNavForm.code"
      :menu-tree="quickNavMenuTree"
      :leaf-menu-ids="quickNavLeafMenuIds"
      :menu-ids="quickNavMenuIds"
      :saving="quickNavSaving"
      @save="submitQuickNav"
    />

    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-alert
        :title="'当前系统：' + (selectedClient ? (selectedClient.name + ' (' + selectedClient.clientId + ')') : '未选择')"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="uploadAction"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已存在的角色（按角色标识）
          </div>
          <span>仅允许 xls/xlsx。须先选择并确认关联业务系统。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignSubSystemRoleMenu,
  bindSubSystemExternalRole,
  createSubSystemRole,
  deleteSubSystemRole,
  deleteSubSystemRoleList,
  getSubSystemClientSimpleList,
  getSubSystemExternalRoleList,
  getSubSystemMenuSimpleList,
  getSubSystemRole,
  getSubSystemRoleMenuIds,
  getSubSystemRolePage,
  importSubSystemRoleTemplate,
  registerSubSystemRole,
  updateSubSystemRole,
  updateSubSystemRoleStatus
} from '@/api/system/subSystemRole'
import { getSubSystemRoleCreateApis } from '@/api/system/subSystemApiConfig'
import { getSubSystemWorkshopSimpleList } from '@/api/system/subSystemWorkshop'
import { getSubSystemRoleQuickNavList, saveSubSystemRoleQuickNav } from '@/api/system/subSystem/roleQuickNav'
import { buildSubSystemRoleQuickNavCheckTree, getSubSystemQuickNavLeafIds } from '@/utils/roleQuickNavMenus'
import { restoreRoleMenuCheckedKeys } from '@/utils/roleMenuTree'
import RoleQuickNavDialog from '@/views/system/components/RoleQuickNavDialog.vue'
import { refreshPortalMenusAfterAdminChange } from '@/utils/portalMenuRefresh'
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, ensureDictDatas, getDictDatas } from '@/utils/dict'
import { getBaseHeader } from '@/utils/request'
import subSystemImportGate from '@/utils/subSystemImportGate'

export default {
  name: 'SubSystemRole',
  mixins: [subSystemImportGate],
  components: { RoleQuickNavDialog },
  data() {
    return {
      loading: false,
      submitting: false,
      showSearch: true,
      total: 0,
      roleList: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      workshopOptions: [],
      roleCreateApis: [],
      bindOpen: false,
      bindLoading: false,
      bindSubmitting: false,
      bindRegistering: false,
      externalRoles: [],
      externalRoleTip: '',
      bindForm: {},
      title: '',
      open: false,
      openMenu: false,
      openQuickNav: false,
      quickNavSaving: false,
      quickNavForm: {},
      quickNavMenuTree: [],
      quickNavLeafMenuIds: [],
      quickNavMenuIds: [],
      menuExpand: false,
      menuNodeAll: false,
      menuCheckStrictly: true,
      menuOptions: [],
      menuForm: {},
      checkedIds: [],
      flatMenuList: [],
      upload: {
        open: false,
        title: '',
        isUploading: false,
        updateSupport: false,
        headers: getBaseHeader()
      },
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
      }
    }
  },
  computed: {
    statusDictDatas() {
      return getDictDatas(DICT_TYPE.COMMON_STATUS)
    },
    roleCreateApiReady() {
      return (this.roleCreateApis || []).length > 0
    },
    syncRoleNamePreview() {
      if (!this.form || !this.form.syncToExternal) {
        return ''
      }
      const workshop = (this.form.workshopCode || '').trim()
      const name = (this.form.name || '').trim()
      if (!workshop || !name) {
        return ''
      }
      const prefix = workshop + '_'
      return name.startsWith(prefix) ? name : (prefix + name)
    },
    formRules() {
      if (this.form && this.form.syncToExternal && !this.form.id) {
        return Object.assign({}, this.rules, {
          apiSubSystemId: [{ required: true, message: '请选择接口目标', trigger: 'change' }],
          workshopCode: [{ required: true, message: '请选择车间', trigger: 'change' }]
        })
      }
      return this.rules
    },
    uploadAction() {
      const id = this.selectedClient && this.selectedClient.id
      const update = this.upload.updateSupport ? 'true' : 'false'
      return process.env.VUE_APP_BASE_API + '/admin-api/system/sub-system-role/import'
        + '?subSystemId=' + (id || '')
        + '&updateSupport=' + update
    },
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
    ensureDictDatas(DICT_TYPE.COMMON_STATUS).finally(() => {
      this.loadClientList()
    })
  },
  methods: {
    loadClientList() {
      return this.withClientsLoading(() => {
        return getSubSystemClientSimpleList(true).then(res => {
          this.clientList = res.data || []
          if (this.syncSelectedClientFromList()) {
            return
          }
          if (!this.selectedClient && this.clientList.length > 0) {
            this.handleClientClick(this.clientList[0])
          }
        })
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
      }).catch(() => {
        this.roleList = []
        this.total = 0
        this.$modal.msgError('加载角色列表失败，请重试')
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
        menuCheckStrictly: true,
        syncToExternal: false,
        apiSubSystemId: undefined,
        workshopCode: undefined
      }
      // 只清校验，不调用 resetFields。关过一次后再 resetFields，ElementUI 会对没有 prop 的表单项执行 indexOf，直接报错，修改弹窗打不开
      this.$nextTick(() => {
        if (this.$refs.form && typeof this.$refs.form.clearValidate === 'function') {
          this.$refs.form.clearValidate()
        }
      })
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      this.ensureSubSystemBoundBeforeAction('新增角色', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.open = true
        this.title = '添加业务系统角色'
        this.loadRoleCreateApis()
        this.loadWorkshopOptions()
      }).catch(() => {})
    },
    /** 可选「角色新增」接口目标（接口管理中 role_create 已启用） */
    loadRoleCreateApis() {
      return getSubSystemRoleCreateApis().then(res => {
        this.roleCreateApis = res.data || []
        if (!this.form.apiSubSystemId && this.roleCreateApis.length === 1) {
          this.form.apiSubSystemId = this.roleCreateApis[0].subSystemId
        }
      }).catch(() => {
        this.roleCreateApis = []
      })
    },
    loadWorkshopOptions() {
      const id = this.selectedClient ? this.selectedClient.id : null
      if (!id) {
        this.workshopOptions = []
        return Promise.resolve()
      }
      return getSubSystemWorkshopSimpleList(id).then(res => {
        this.workshopOptions = res.data || []
        this.lockWorkshopToMappings()
      }).catch(() => {
        this.workshopOptions = []
      })
    },
    mappedWorkshopCode(current) {
      const codes = (this.workshopOptions || []).map(w => w.workshopCode)
      if (codes.length === 1) {
        return codes[0]
      }
      return current && codes.indexOf(current) >= 0 ? current : undefined
    },
    workshopFixedText() {
      const item = (this.workshopOptions || [])[0]
      return item ? this.workshopOptionLabel(item) : ''
    },
    lockWorkshopToMappings() {
      if (this.open) {
        this.form.workshopCode = this.mappedWorkshopCode(this.form.workshopCode)
      }
      if (this.bindOpen && this.bindForm) {
        this.bindForm.workshopCode = this.mappedWorkshopCode(this.bindForm.workshopCode)
      }
    },
    workshopOptionLabel(item) {
      const name = item.workshopName || '车间'
      const code = item.workshopCode || ''
      const dept = item.deptName ? (' / ' + item.deptName) : ''
      return name + '（' + code + '）' + dept
    },
    handleSyncToExternalChange() {
      if (!this.form.syncToExternal) {
        return
      }
      if (!this.roleCreateApis.length) {
        this.loadRoleCreateApis()
      }
      if (!this.workshopOptions.length) {
        this.loadWorkshopOptions()
      }
    },
    handleImport() {
      this.ensureSubSystemBoundBeforeAction('导入').then(() => {
        this.upload.title = '导入业务系统角色 — ' + (this.selectedClient.name || '')
        this.upload.open = true
        this.upload.headers = getBaseHeader()
      }).catch(() => {})
    },
    importTemplate() {
      importSubSystemRoleTemplate().then(response => {
        this.$download.excel(response, '业务系统角色导入模板.xls')
      })
    },
    handleFileUploadProgress() {
      this.upload.isUploading = true
    },
    handleFileSuccess(response) {
      this.upload.open = false
      this.upload.isUploading = false
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
      if (response.code !== 0) {
        this.$modal.msgError(response.msg || '导入失败')
        return
      }
      const data = response.data || {}
      let text = '新建：' + ((data.createKeys && data.createKeys.length) || 0)
      ;(data.createKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + k })
      text += '<br />更新：' + ((data.updateKeys && data.updateKeys.length) || 0)
      ;(data.updateKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + k })
      const failMap = data.failureKeys || {}
      const failKeys = Object.keys(failMap)
      text += '<br />失败：' + failKeys.length
      failKeys.forEach(k => { text += '<br />&nbsp;&nbsp;' + k + '：' + failMap[k] })
      this.$alert(text, '导入结果', { dangerouslyUseHTMLString: true })
      this.getList()
      this.loadClientList()
    },
    submitFileForm() {
      this.$refs.upload.submit()
    },
    handleUpdate(row) {
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
        this.title = '修改业务系统角色'
        this.$nextTick(() => {
          if (this.$refs.form && typeof this.$refs.form.clearValidate === 'function') {
            this.$refs.form.clearValidate()
          }
        })
      }).catch(() => {
        this.$modal.msgError('加载角色信息失败，请重试')
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const payload = {
          id: this.form.id,
          subSystemId: this.form.subSystemId,
          name: this.form.name,
          code: this.form.code,
          sort: this.form.sort,
          status: this.form.status
        }
        if (!this.form.id && this.form.syncToExternal) {
          payload.syncToExternal = true
          payload.apiSubSystemId = this.form.apiSubSystemId
          payload.workshopCode = this.form.workshopCode
        }
        this.submitting = true
        const request = this.form.id ? updateSubSystemRole : createSubSystemRole
        request(payload).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功'
            : (this.form.syncToExternal ? '新增成功，已同步注册并关联外部角色' : '新增成功'))
          this.open = false
          this.getList()
          this.loadClientList()
        }).catch(() => {
          this.$modal.msgError(this.form.id ? '修改失败，请重试' : '新增失败，请重试')
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    /** 关联外部角色：把对方系统已有角色的 roleId 绑到本地角色 */
    handleBind(row) {
      this.bindForm = {
        id: row.id,
        subSystemId: row.subSystemId,
        name: row.name,
        externalRoleId: row.externalRoleId || undefined,
        apiSubSystemId: undefined,
        workshopCode: this.inferWorkshopFromRoleName(row.name),
        bound: !!row.externalRoleId,
        boundRoleId: row.externalRoleId || undefined
      }
      this.externalRoles = []
      this.externalRoleTip = ''
      this.bindOpen = true
      Promise.all([this.loadRoleCreateApis(), this.loadWorkshopOptions()]).then(() => {
        if (!this.bindForm.apiSubSystemId && this.roleCreateApis.length === 1) {
          this.bindForm.apiSubSystemId = this.roleCreateApis[0].subSystemId
        }
        this.bindForm.workshopCode = this.mappedWorkshopCode(this.bindForm.workshopCode)
      })
    },
    onBindFilterChange() {
      this.bindForm.externalRoleId = this.bindForm.boundRoleId || undefined
      this.externalRoles = []
      this.externalRoleTip = ''
    },
    /** 角色名带「车间_」前缀时直接取前缀当车间 */
    inferWorkshopFromRoleName(name) {
      const n = (name || '').trim()
      const idx = n.indexOf('_')
      return idx > 0 ? n.substring(0, idx) : undefined
    },
    loadExternalRoles(silent) {
      if (!this.bindForm.apiSubSystemId || !this.bindForm.workshopCode) {
        this.externalRoleTip = '请先选择接口目标和车间'
        if (!silent) {
          this.$modal.msgWarning(this.externalRoleTip)
        }
        return
      }
      this.bindLoading = true
      this.externalRoleTip = ''
      getSubSystemExternalRoleList(this.bindForm.apiSubSystemId, this.bindForm.workshopCode).then(res => {
        this.externalRoles = res.data || []
        if (!this.externalRoles.length) {
          this.externalRoleTip = '该车间在对方系统没有查到角色'
          if (!silent) {
            this.$modal.msgWarning(this.externalRoleTip)
          }
          return
        }
        this.externalRoleTip = '共 ' + this.externalRoles.length + ' 个，点开下拉按角色名或 roleId 选择'
        const current = this.bindForm.externalRoleId
        const stillThere = current && this.externalRoles.some(item => item.roleId === current)
        if (stillThere) {
          return
        }
        const wanted = (this.bindForm.name || '').trim().toLowerCase()
        const hit = this.externalRoles.find(item => (item.roleName || '').trim().toLowerCase() === wanted)
        if (hit) {
          this.bindForm.externalRoleId = hit.roleId
        }
      }).catch(() => {
        this.externalRoles = []
        this.externalRoleTip = '查询外部角色失败，请确认接口目标和车间后点「查询外部角色」'
      }).finally(() => {
        this.bindLoading = false
      })
    },
    /** 对方系统没有对应角色时，直接调「角色新增」注册（后端自动按 车间_角色名 推送并回存 roleId） */
    pushRegisterFromBind() {
      if (!this.bindForm.apiSubSystemId || !this.bindForm.workshopCode) {
        this.$modal.msgWarning('请先选择接口目标和车间')
        return
      }
      this.$modal.confirm('将调「角色新增」接口按 车间编号_角色名称 注册到对方系统？').then(() => {
        this.bindRegistering = true
        return registerSubSystemRole(this.bindForm.id, {
          apiSubSystemId: this.bindForm.apiSubSystemId,
          workshopCode: this.bindForm.workshopCode
        })
      }).then(() => {
        this.$modal.msgSuccess('注册成功')
        this.getList()
        return this.loadExternalRoles()
      }).catch(() => {}).finally(() => {
        this.bindRegistering = false
      })
    },
    /** 保存/解除关联（解除后该角色不参与外部同步） */
    submitBind(unbind) {
      const action = unbind ? '解除关联' : '保存关联'
      const roleId = unbind ? '' : this.bindForm.externalRoleId
      this.bindSubmitting = true
      bindSubSystemExternalRole(this.bindForm.id, roleId).then(() => {
        this.$modal.msgSuccess(action + '成功')
        this.bindOpen = false
        this.getList()
      }).catch(() => {
        this.$modal.msgError(action + '失败，请重试')
      }).finally(() => {
        this.bindSubmitting = false
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
      this.$modal.confirm('是否确认批量删除选中的业务系统角色？').then(() => {
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
        const allMenus = res.data || []
        this.flatMenuList = allMenus
        this.menuOptions = this.handleTree(allMenus, 'id')
        this.$nextTick(() => {
          getSubSystemRoleMenuIds(row.id).then(menuRes => {
            restoreRoleMenuCheckedKeys(
              this,
              this.$refs.menu,
              menuRes.data || [],
              value => { this.menuCheckStrictly = value }
            )
          })
        })
      })
    },
    handleQuickNav(row) {
      this.quickNavForm = {
        roleId: row.id,
        subSystemId: row.subSystemId,
        name: row.name,
        code: row.code
      }
      this.quickNavMenuTree = []
      this.quickNavLeafMenuIds = []
      this.quickNavMenuIds = []
      this.openQuickNav = true
      Promise.all([
        getSubSystemMenuSimpleList(row.subSystemId),
        getSubSystemRoleMenuIds(row.id),
        getSubSystemRoleQuickNavList(row.id)
      ]).then(([menuRes, roleMenuRes, quickNavRes]) => {
        const allMenus = menuRes.data || []
        const roleMenuIds = roleMenuRes.data || []
        this.quickNavMenuTree = buildSubSystemRoleQuickNavCheckTree(allMenus, roleMenuIds)
        this.quickNavLeafMenuIds = getSubSystemQuickNavLeafIds(allMenus, roleMenuIds)
        this.quickNavMenuIds = (quickNavRes.data && quickNavRes.data.menuIds) || []
      }).catch(() => {
        this.$modal.msgError('加载快捷导航菜单失败')
        this.openQuickNav = false
      })
    },
    submitQuickNav(menuIds) {
      this.quickNavSaving = true
      saveSubSystemRoleQuickNav({
        subSystemId: this.quickNavForm.subSystemId,
        roleId: this.quickNavForm.roleId,
        menuIds: menuIds || []
      }).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.openQuickNav = false
        refreshPortalMenusAfterAdminChange({
          scope: 'sub',
          clientId: this.selectedClient && this.selectedClient.clientId,
          subSystemId: this.selectedClient && this.selectedClient.id
        })
      }).finally(() => {
        this.quickNavSaving = false
      })
    },
    handleCheckedTreeExpand(value, type) {
      if (type === 'menu') {
        const treeList = this.menuOptions
        for (let i = 0; i < treeList.length; i++) {
          this.$refs.menu.store.nodesMap[treeList[i].id].expanded = value
        }
      }
    },
    handleCheckedTreeNodeAll(value, type) {
      if (type === 'menu') {
        this.$refs.menu.setCheckedNodes(value ? this.menuOptions : [])
      }
    },
    submitMenu() {
      assignSubSystemRoleMenu({
        roleId: this.menuForm.roleId,
        menuIds: [...this.$refs.menu.getCheckedKeys(), ...this.$refs.menu.getHalfCheckedKeys()]
      }).then(() => {
        this.$modal.msgSuccess('分配成功')
        this.openMenu = false
        refreshPortalMenusAfterAdminChange({
          scope: 'sub',
          clientId: this.selectedClient && this.selectedClient.clientId,
          subSystemId: this.selectedClient && this.selectedClient.id
        })
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

::v-deep .bind-external-role-dialog .el-dialog__body {
  overflow: visible;
}
</style>

<style lang="scss">
/* 下拉挂到 body，必须压过对话框，否则选项点不到 */
.external-role-dropdown {
  z-index: 4000 !important;
}
</style>
