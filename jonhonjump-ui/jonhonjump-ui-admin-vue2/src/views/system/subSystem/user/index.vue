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
              <el-tag size="mini" type="info">{{ item.userCount || 0 }} 人</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无业务系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 用户数据（子表 sub_system_users） -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择已登记的业务系统，再新增或导入该系统用户（无需关联主系统用户）"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="业务系统" prop="subSystemId">
            <el-select
              v-model="queryParams.subSystemId"
              placeholder="请选择业务系统"
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
          <el-form-item label="用户名" prop="username">
            <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="queryParams.nickname" placeholder="请输入用户姓名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="班组名称" prop="teamName">
            <el-input v-model="queryParams.teamName" placeholder="请输入班组名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
              <el-option label="未关联" value="unlinked" />
              <el-option label="正常" value="0" />
              <el-option label="禁用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="接口注册" prop="employeeRegistered">
            <el-select v-model="queryParams.employeeRegistered" placeholder="请选择" clearable style="width: 130px">
              <el-option label="未注册" value="0" />
              <el-option label="已注册" value="1" />
            </el-select>
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
              <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport"
                         v-hasPermi="['sub-system:user:create']">导入</el-button>
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
            <el-col :span="1.5">
              <el-button
                type="warning"
                plain
                icon="el-icon-position"
                size="mini"
                :disabled="checkedIds.length === 0"
                @click="handleRegisterBatch"
                v-hasPermi="['sub-system:employee:create', 'sub-system:user:update']"
              >批量注册</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
          </el-row>

          <el-table v-loading="loading" :data="userList" @selection-change="handleRowCheckboxChange">
            <el-table-column type="selection" width="55" fixed="left" />
            <el-table-column label="用户名" prop="username" :show-overflow-tooltip="true" width="110" fixed="left" />
            <el-table-column label="用户姓名" prop="nickname" :show-overflow-tooltip="true" width="100" fixed="left" />
            <el-table-column label="车间" prop="workshopId" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="班组编码" prop="teamId" width="110" />
            <el-table-column label="班组名称" prop="teamName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="岗位" prop="postNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="角色" prop="roleNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="状态" align="center" width="100">
              <template v-slot="scope">
                <el-tag :type="displayStatusType(scope.row)" size="mini">
                  {{ displayStatusLabel(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="接口注册" align="center" width="90">
              <template v-slot="scope">
                <el-tag
                  :type="scope.row.employeeRegistered === '1' ? 'success' : 'info'"
                  size="mini"
                  style="cursor: pointer"
                  title="点击切换已注册/未注册"
                  @click.native="handleToggleRegister(scope.row)"
                >
                  {{ scope.row.employeeRegistered === '1' ? '已注册' : '未注册' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
              <template v-slot="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="240" fixed="right" class-name="small-padding fixed-width">
              <template v-slot="scope">
                <el-button size="mini" type="text" icon="el-icon-position" @click="handleRegister(scope.row)"
                           :disabled="scope.row.employeeRegistered === '1'"
                           v-hasPermi="['sub-system:employee:create', 'sub-system:user:update']">注册</el-button>
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
        <el-form-item label="业务系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入业务系统用户名" maxlength="64" />
        </el-form-item>
        <el-form-item label="用户姓名" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入用户姓名" maxlength="64" />
        </el-form-item>
        <el-divider content-position="left">组织与权限</el-divider>
        <el-form-item label="车间" prop="workshopId">
          <el-select
            v-model="form.workshopId"
            placeholder="请从车间对照中选择"
            clearable
            filterable
            style="width: 100%"
            @change="handleWorkshopChange"
          >
            <el-option
              v-for="item in workshopOptions"
              :key="item.workshopCode"
              :label="workshopOptionLabel(item)"
              :value="item.workshopCode"
            />
          </el-select>
          <div v-if="!workshopOptions.length" class="form-tip">暂无车间对照，请先在「车间对照」中维护</div>
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
          <div v-if="!form.mainUserId" style="color:#909399;font-size:12px;margin-top:4px">
            未挂接主用户时，列表展示为「未关联」
          </div>
        </el-form-item>
        <el-form-item label="接口注册" prop="employeeRegistered">
          <el-radio-group v-model="form.employeeRegistered">
            <el-radio label="0">未注册</el-radio>
            <el-radio label="1">已注册</el-radio>
          </el-radio-group>
          <div class="form-tip">
            标记是否已在对方系统建人（正常由「注册」调接口成功后自动置已注册）；人工在对方系统建过人可标已注册，改回未注册后可在列表重新推送
          </div>
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

    <!-- 子系统用户导入（须先选择并确认已关联的外部系统） -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="460px" append-to-body>
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
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已存在的用户名数据
          </div>
          <span>仅允许 xls/xlsx。按用户名写入子系统花名册，可不关联主系统用户。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 手动调「新增人员」接口注册（接口目标与花名册系统解耦；逐项返回结果） -->
    <el-dialog
      title="调用「新增人员」接口注册"
      :visible.sync="registerOpen"
      width="640px"
      append-to-body
      :close-on-click-modal="!registerSubmitting"
    >
      <el-form label-width="110px">
        <el-form-item label="花名册系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="新增人员接口">
          <el-select
            v-model="registerForm.apiSubSystemId"
            placeholder="请选择接口目标"
            filterable
            style="width: 100%"
            :disabled="registerSubmitting || registerResults.length > 0"
          >
            <el-option
              v-for="item in registerApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
          <div v-if="!registerApis.length" class="form-tip" style="color:#f56c6c">
            没有已启用「新增人员」的接口目标，请先在【接口管理】接入并启用
          </div>
          <div v-else class="form-tip">接口来自【接口管理】中「新增」用途已启用的系统，可与左侧花名册系统不同</div>
        </el-form-item>
        <el-form-item label="待注册用户">
          <div class="register-users">
            <div v-for="u in registerForm.users" :key="u.id" class="register-user-row">
              <span>{{ u.username }}（{{ u.nickname || '-' }}）</span>
              <el-tag v-if="u.employeeRegistered === '1'" size="mini" type="info">已注册，将跳过</el-tag>
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="registerResults.length" label="注册结果">
          <div class="register-users">
            <div v-for="r in registerResults" :key="r.id" class="register-user-row">
              <span>{{ r.username || ('#' + r.id) }}</span>
              <el-tag size="mini" :type="r.success ? 'success' : 'danger'">{{ r.success ? '成功' : '失败' }}</el-tag>
              <span v-if="r.message" class="register-result-msg">{{ r.message }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button
          v-if="!registerResults.length"
          type="primary"
          :loading="registerSubmitting"
          :disabled="!registerForm.apiSubSystemId"
          @click="submitRegister"
        >调接口注册</el-button>
        <el-button v-else type="primary" @click="registerOpen = false">关 闭</el-button>
        <el-button v-if="!registerResults.length" @click="registerOpen = false">取 消</el-button>
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
  importSubSystemUserTemplate,
  updateSubSystemUser,
  updateSubSystemUserRegisterStatus
} from '@/api/system/subSystemUsers'
import {
  getSubSystemRegisterableApis,
  registerSubSystemEmployee
} from '@/api/system/subSystemEmployee'
import { getSubSystemWorkshopSimpleList } from '@/api/system/subSystemWorkshop'
import { getUser } from '@/api/system/user'
import { getBaseHeader } from '@/utils/request'
import subSystemImportGate from '@/utils/subSystemImportGate'

export default {
  name: 'SubSystemUser',
  components: { Treeselect },
  mixins: [subSystemImportGate],
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
      workshopOptions: [],
      menuPageOptions: [],
      workshopDeptId: undefined,
      title: '',
      open: false,
      openRole: false,
      form: {},
      roleForm: {},
      mainUserInfo: {},
      checkedIds: [],
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
        subSystemId: undefined,
        username: undefined,
        mainUserId: undefined,
        workshopId: undefined,
        nickname: undefined,
        teamName: undefined,
        status: undefined,
        employeeRegistered: undefined
      },
      // 手动调「新增人员」接口注册
      registerOpen: false,
      registerSubmitting: false,
      registerApis: [],
      registerResults: [],
      registerForm: {
        apiSubSystemId: undefined,
        users: []
      },
      rules: {
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    uploadAction() {
      const id = this.selectedClient && this.selectedClient.id
      const update = this.upload.updateSupport ? 'true' : 'false'
      return process.env.VUE_APP_BASE_API + '/admin-api/system/sub-system-users/import'
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
    this.loadClientList()
    this.loadRegisterableApis()
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
    loadSubOptions(subSystemId) {
      const id = subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      if (!id) {
        this.roleOptions = []
        this.postOptions = []
        this.teamOptions = []
        this.workshopOptions = []
        this.menuPageOptions = []
        this.workshopDeptId = undefined
        return Promise.resolve()
      }
      return Promise.all([
        getSubSystemRoleSimpleList(id).then(res => { this.roleOptions = res.data || [] }),
        getSubSystemPostSimpleList(id).then(res => { this.postOptions = res.data || [] }),
        getSubSystemWorkshopSimpleList(id).then(res => { this.workshopOptions = res.data || [] })
      ]).then(() => this.reloadTeams(id))
    },
    workshopOptionLabel(item) {
      const name = item.workshopName || '车间'
      const code = item.workshopCode || ''
      const dept = item.deptName ? (' / ' + item.deptName) : ''
      return name + '（' + code + '）' + dept
    },
    handleWorkshopChange(workshopCode) {
      this.form.teamId = undefined
      const hit = (this.workshopOptions || []).find(w => w.workshopCode === workshopCode)
      this.workshopDeptId = hit ? hit.deptId : undefined
      const id = this.form.subSystemId || (this.selectedClient && this.selectedClient.id)
      this.reloadTeams(id)
    },
    reloadTeams(subSystemId) {
      if (!subSystemId) {
        this.teamOptions = []
        return Promise.resolve()
      }
      return getSubSystemTeamSimpleList(subSystemId, this.workshopDeptId).then(res => {
        const list = res.data || []
        if (list.length > 0 || !this.workshopDeptId) {
          this.teamOptions = list
          return
        }
        // 历史班组可能未挂 deptId，按部门过滤为空时回退到该系统全部班组
        return getSubSystemTeamSimpleList(subSystemId).then(allRes => {
          this.teamOptions = allRes.data || []
        })
      })
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
      this.queryParams.username = undefined
      this.queryParams.nickname = undefined
      this.queryParams.teamName = undefined
      this.queryParams.status = undefined
      this.queryParams.employeeRegistered = undefined
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
        username: undefined,
        nickname: undefined,
        workshopId: undefined,
        teamId: undefined,
        homeMenuId: undefined,
        status: '0',
        employeeRegistered: '0',
        remark: undefined,
        roleIds: [],
        postIds: []
      }
      this.workshopDeptId = undefined
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      this.ensureSubSystemBoundBeforeAction('新增', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.loadSubOptions().then(() => {
          this.menuPageOptions = []
          this.open = true
          this.title = '添加业务系统用户'
        })
      }).catch(() => {})
    },
    handleImport() {
      this.ensureSubSystemBoundBeforeAction('导入').then(() => {
        this.upload.title = '导入子系统用户 — ' + (this.selectedClient.name || '')
        this.upload.open = true
        this.upload.headers = getBaseHeader()
      }).catch(() => {})
    },
    importTemplate() {
      importSubSystemUserTemplate().then(response => {
        this.$download.excel(response, '子系统用户导入模板.xls')
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
      let text = '新建绑定：' + ((data.createKeys && data.createKeys.length) || 0)
      ;(data.createKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + k })
      text += '<br />更新绑定：' + ((data.updateKeys && data.updateKeys.length) || 0)
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
      this.loadSubOptions(row.subSystemId).then(() => {
        getSubSystemUser(row.id).then(res => {
          this.form = {
            id: res.data.id,
            subSystemId: res.data.subSystemId,
            mainUserId: res.data.mainUserId,
            username: res.data.username,
            nickname: res.data.nickname,
            workshopId: res.data.workshopId,
            teamId: res.data.teamId,
            homeMenuId: res.data.homeMenuId,
            status: res.data.status || '0',
            employeeRegistered: res.data.employeeRegistered || '0',
            remark: res.data.remark,
            roleIds: res.data.roleIds || [],
            postIds: res.data.postIds || []
          }
          if (this.form.workshopId && !(this.workshopOptions || []).some(w => w.workshopCode === this.form.workshopId)) {
            this.workshopOptions = (this.workshopOptions || []).concat([{
              workshopCode: this.form.workshopId,
              workshopName: this.form.workshopId + '（未在对照中）',
              deptId: undefined,
              deptName: undefined
            }])
          }
          const hit = (this.workshopOptions || []).find(w => w.workshopCode === this.form.workshopId)
          this.workshopDeptId = hit ? hit.deptId : undefined
          return this.reloadTeams(this.form.subSystemId).then(() => this.loadHomeMenuOptions())
        }).then(() => {
          this.open = true
          this.title = '修改业务系统用户'
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
      this.$modal.confirm('是否确认删除该业务系统用户？').then(() => {
        return deleteSubSystemUser(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的业务系统用户？').then(() => {
        return deleteSubSystemUserList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    /** main_user_id 为空 → 未关联；否则按 status 显示正常/禁用 */
    displayStatusLabel(row) {
      if (!row || row.mainUserId == null || row.mainUserId === '') {
        return '未关联'
      }
      return row.status === '1' ? '禁用' : '正常'
    },
    displayStatusType(row) {
      if (!row || row.mainUserId == null || row.mainUserId === '') {
        return 'info'
      }
      return row.status === '1' ? 'danger' : 'success'
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
    },
    /** 可选「新增人员」接口目标（接口管理中 create 已启用；与花名册系统解耦） */
    loadRegisterableApis() {
      return getSubSystemRegisterableApis().then(res => {
        this.registerApis = res.data || []
        if (!this.registerForm.apiSubSystemId && this.registerApis.length === 1) {
          this.registerForm.apiSubSystemId = this.registerApis[0].subSystemId
        }
      }).catch(() => {
        this.registerApis = []
      })
    },
    /** 列表内点击切换 已注册/未注册（人工修正标记用） */
    handleToggleRegister(row) {
      const next = row.employeeRegistered === '1' ? '0' : '1'
      const action = next === '1' ? '已注册（人工确认已在对方系统建人，避免重复推送）' : '未注册（之后可在列表重新推送）'
      this.$modal.confirm('将用户 ' + row.username + ' 的接口注册状态改为【' + action + '】？').then(() => {
        return updateSubSystemUserRegisterStatus(row.id, next)
      }).then(() => {
        this.$modal.msgSuccess('修改成功')
        this.getList()
      }).catch(() => {})
    },
    /** 行内「注册」：单个用户调「新增人员」接口 */
    handleRegister(row) {
      this.openRegisterDialog([row])
    },
    /** 工具栏「批量注册」：勾选多个用户 */
    handleRegisterBatch() {
      const rows = this.userList.filter(item => this.checkedIds.indexOf(item.id) !== -1)
      if (!rows.length) {
        this.$modal.msgWarning('请先勾选要注册的用户')
        return
      }
      this.openRegisterDialog(rows)
    },
    openRegisterDialog(rows) {
      if (!this.registerApis.length) {
        this.loadRegisterableApis().then(() => {
          if (!this.registerApis.length) {
            this.$modal.msgWarning('没有已启用「新增人员」的接口目标，请先在【接口管理】接入并启用')
          }
        })
      }
      this.registerResults = []
      this.registerForm.users = rows
      this.registerOpen = true
    },
    submitRegister() {
      if (!this.registerForm.apiSubSystemId) {
        this.$modal.msgWarning('请选择「新增人员」接口目标')
        return
      }
      this.registerSubmitting = true
      registerSubSystemEmployee({
        apiSubSystemId: this.registerForm.apiSubSystemId,
        ids: this.registerForm.users.map(u => u.id)
      }).then(res => {
        this.registerResults = res.data || []
        const failCount = this.registerResults.filter(r => !r.success).length
        if (failCount) {
          this.$modal.msgError('注册完成：' + (this.registerResults.length - failCount) + ' 成功 / ' + failCount + ' 失败')
        } else {
          this.$modal.msgSuccess('注册完成')
        }
        this.getList()
      }).finally(() => {
        this.registerSubmitting = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.sub-system-list {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}

.register-users {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 12px;
}

.register-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  font-size: 13px;
  color: #303133;
}

.register-result-msg {
  color: #909399;
  font-size: 12px;
  word-break: break-all;
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
