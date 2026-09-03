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
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 角色数据 -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择已登记的外部系统；关联系统信息后，才可新增/维护该系统下的角色与权限"
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
          <el-form-item label="接口注册" prop="roleRegistered">
            <el-select v-model="queryParams.roleRegistered" placeholder="全部" clearable style="width: 140px">
              <el-option label="已注册" value="1" />
              <el-option label="未注册" value="0" />
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
          <el-table-column label="接口注册" align="center" width="90">
            <template v-slot="scope">
              <el-tag
                :type="scope.row.roleRegistered === '1' ? 'success' : 'info'"
                size="mini"
                style="cursor: pointer"
                title="点击切换已注册/未注册"
                @click.native="handleToggleRegister(scope.row)"
              >
                {{ scope.row.roleRegistered === '1' ? '已注册' : '未注册' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
            <template v-slot="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-position"
                :disabled="scope.row.roleRegistered === '1'"
                @click="handleRegister(scope.row)"
                v-hasPermi="['sub-system:role:update']"
              >注册</el-button>
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:role:update']">修改</el-button>
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

    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="外部系统">
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
          <el-form-item label="同步外部">
            <el-checkbox
              v-model="form.syncToExternal"
              :disabled="!roleCreateApiReady"
            >同步到外部系统（调「角色新增」接口）</el-checkbox>
            <div class="form-tip">
              <span v-if="roleCreateApiReady" style="color:#67c23a">可选接口目标：与花名册系统解耦（如 Camstar人员管理）</span>
              <span v-else style="color:#e6a23c">未找到已启用的「角色新增」接口。请到「人员接口接入」配置并启用；若已配在 Camstar人员管理，刷新后应能勾选</span>
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
            <el-select v-model="form.workshopCode" placeholder="请从车间对照中选择" filterable style="width: 100%">
              <el-option
                v-for="item in workshopOptions"
                :key="item.workshopCode"
                :label="workshopOptionLabel(item)"
                :value="item.workshopCode"
              />
            </el-select>
            <div v-if="!workshopOptions.length" class="form-tip">暂无车间对照，请先在「车间对照」中维护</div>
            <div v-if="syncRoleNamePreview" class="form-tip">将同步角色名：<b>{{ syncRoleNamePreview }}</b></div>
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 补注册：选接口目标；角色名无车间前缀时再选车间 -->
    <el-dialog title="注册到外部系统" :visible.sync="registerOpen" width="520px" append-to-body>
      <el-form ref="registerForm" :model="registerForm" :rules="registerRules" label-width="90px">
        <el-form-item label="角色名称">
          <el-input :value="registerForm.name" disabled />
        </el-form-item>
        <el-form-item label="接口目标" prop="apiSubSystemId">
          <el-select v-model="registerForm.apiSubSystemId" placeholder="请选择调用哪个系统的角色新增接口" style="width: 100%">
            <el-option
              v-for="item in roleCreateApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="registerNeedWorkshop" label="车间" prop="workshopCode">
          <el-select v-model="registerForm.workshopCode" placeholder="请选择车间" filterable style="width: 100%">
            <el-option
              v-for="item in workshopOptions"
              :key="item.workshopCode"
              :label="workshopOptionLabel(item)"
              :value="item.workshopCode"
            />
          </el-select>
          <div class="form-tip">角色名无车间前缀，需选择车间后按 车间编号_角色名称 同步</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="registerSubmitting" :disabled="!roleCreateApiReady" @click="submitRegister">确 定</el-button>
        <el-button @click="registerOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配菜单权限（含目录/页面/按钮；数据范围在子系统本地配置） -->
    <el-dialog title="分配菜单权限" :visible.sync="openMenu" width="500px" append-to-body>
      <el-form :model="menuForm" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="menuForm.name" disabled />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="menuForm.code" disabled />
        </el-form-item>
        <el-form-item label="菜单权限">
          <div style="margin-bottom: 8px; color: #909399; font-size: 12px;">可勾选目录、页面及按钮权限；勾选页面时也会自动带上该页按钮。数据范围请在子系统本地配置。</div>
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
          <span>仅允许 xls/xlsx。须先选择并确认关联外部系统。</span>
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
  createSubSystemRole,
  deleteSubSystemRole,
  deleteSubSystemRoleList,
  getSubSystemClientSimpleList,
  getSubSystemMenuSimpleList,
  getSubSystemRole,
  getSubSystemRoleMenuIds,
  getSubSystemRolePage,
  importSubSystemRoleTemplate,
  registerSubSystemRole,
  updateSubSystemRole,
  updateSubSystemRoleRegisterStatus,
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
      title: '',
      open: false,
      openMenu: false,
      openQuickNav: false,
      registerOpen: false,
      registerSubmitting: false,
      registerNeedWorkshop: false,
      registerForm: {
        id: undefined,
        name: '',
        apiSubSystemId: undefined,
        workshopCode: undefined
      },
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
        roleRegistered: undefined,
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
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
        apiSubSystemId: [{ required: true, message: '请选择接口目标', trigger: 'change' }],
        workshopCode: [{ required: true, message: '请选择车间', trigger: 'change' }]
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
        return this.rules
      }
      const { workshopCode, apiSubSystemId, ...rest } = this.rules
      return rest
    },
    registerRules() {
      const rules = {
        apiSubSystemId: [{ required: true, message: '请选择接口目标', trigger: 'change' }]
      }
      if (this.registerNeedWorkshop) {
        rules.workshopCode = [{ required: true, message: '请选择车间', trigger: 'change' }]
      }
      return rules
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
      this.loadRoleCreateApis()
    })
  },
  methods: {
    workshopOptionLabel(item) {
      if (!item) {
        return ''
      }
      const name = item.workshopName || item.deptName || ''
      return name ? (item.workshopCode + ' / ' + name) : item.workshopCode
    },
    loadRoleCreateApis() {
      return getSubSystemRoleCreateApis().then(res => {
        this.roleCreateApis = res.data || []
      }).catch(() => {
        this.roleCreateApis = []
      })
    },
    defaultApiSubSystemId() {
      if (!(this.roleCreateApis || []).length) {
        return undefined
      }
      return this.roleCreateApis[0].subSystemId
    },
    loadWorkshopOptions() {
      if (!this.selectedClient || !this.selectedClient.id) {
        this.workshopOptions = []
        return Promise.resolve()
      }
      return getSubSystemWorkshopSimpleList(this.selectedClient.id).then(res => {
        this.workshopOptions = res.data || []
      }).catch(() => {
        this.workshopOptions = []
      })
    },
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
      this.queryParams.roleRegistered = undefined
      this.queryParams.createTime = []
      this.queryParams.pageNo = 1
      this.workshopOptions = []
      this.getList()
      this.loadWorkshopOptions()
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
        syncToExternal: false,
        apiSubSystemId: undefined,
        workshopCode: undefined,
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
      this.ensureSubSystemBoundBeforeAction('新增角色', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.form.apiSubSystemId = this.defaultApiSubSystemId()
        this.open = true
        this.title = '添加外部系统角色'
        Promise.all([this.loadRoleCreateApis(), this.loadWorkshopOptions()]).then(() => {
          if (!this.form.apiSubSystemId) {
            this.form.apiSubSystemId = this.defaultApiSubSystemId()
          }
        })
      }).catch(() => {})
    },
    handleImport() {
      this.ensureSubSystemBoundBeforeAction('导入').then(() => {
        this.upload.title = '导入外部系统角色 — ' + (this.selectedClient.name || '')
        this.upload.open = true
        this.upload.headers = getBaseHeader()
      }).catch(() => {})
    },
    importTemplate() {
      importSubSystemRoleTemplate().then(response => {
        this.$download.excel(response, '外部系统角色导入模板.xls')
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
      this.resetFormData()
      getSubSystemRole(row.id).then(res => {
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId,
          name: res.data.name,
          code: res.data.code,
          sort: res.data.sort,
          status: res.data.status,
          syncToExternal: false,
          workshopCode: undefined
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
        const payload = {
          id: this.form.id,
          subSystemId: this.form.subSystemId,
          name: this.form.name,
          code: this.form.code,
          sort: this.form.sort,
          status: this.form.status
        }
        if (!this.form.id) {
          payload.syncToExternal = !!this.form.syncToExternal
          if (payload.syncToExternal) {
            payload.workshopCode = this.form.workshopCode
            payload.apiSubSystemId = this.form.apiSubSystemId
          }
        }
        this.submitting = true
        const request = this.form.id ? updateSubSystemRole : createSubSystemRole
        request(payload).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.loadClientList()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    handleToggleRegister(row) {
      const next = row.roleRegistered === '1' ? '0' : '1'
      const action = next === '1' ? '已注册' : '未注册'
      this.$modal.confirm('将角色「' + row.name + '」的接口注册状态改为【' + action + '】？').then(() => {
        return updateSubSystemRoleRegisterStatus(row.id, next)
      }).then(() => {
        row.roleRegistered = next
        this.$modal.msgSuccess('已改为' + action)
      }).catch(() => {})
    },
    handleRegister(row) {
      if (!row || row.roleRegistered === '1') {
        return
      }
      const name = row.name || ''
      const idx = name.indexOf('_')
      const parsedWorkshop = idx > 0 ? name.substring(0, idx) : ''
      Promise.all([this.loadRoleCreateApis(), this.loadWorkshopOptions()]).then(() => {
        if (!this.roleCreateApiReady) {
          this.$modal.msgWarning('未找到已启用的「角色新增」接口，请先在「人员接口接入」配置并启用')
          return
        }
        this.registerNeedWorkshop = !parsedWorkshop
        this.registerForm = {
          id: row.id,
          name,
          apiSubSystemId: this.defaultApiSubSystemId(),
          workshopCode: parsedWorkshop || undefined
        }
        this.registerOpen = true
        this.$nextTick(() => {
          if (this.$refs.registerForm) {
            this.$refs.registerForm.clearValidate()
          }
        })
      })
    },
    submitRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) {
          return
        }
        this.registerSubmitting = true
        registerSubSystemRole(this.registerForm.id, {
          apiSubSystemId: this.registerForm.apiSubSystemId,
          workshopCode: this.registerForm.workshopCode
        }).then(() => {
          this.$modal.msgSuccess('注册成功')
          this.registerOpen = false
          this.getList()
        }).finally(() => {
          this.registerSubmitting = false
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

.form-tip {
  margin-top: 4px;
  line-height: 1.4;
  font-size: 12px;
  color: #909399;
}
</style>
