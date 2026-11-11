<template>
  <div v-loading="loading" class="app-container user-detail-page">
    <el-card shadow="never" class="user-detail-card">
      <div slot="header">
        <span>主系统用户信息</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户编号">{{ user.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名称">{{ user.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户姓名">{{ user.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ user.employeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="域账号">{{ user.domainNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="刷卡卡号">{{ user.cardNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ERP账号">{{ formatErpNos(user.erpNos) }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ user.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ user.deptName || (user.dept && user.dept.name) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="user.status === 0 ? 'success' : 'danger'" size="mini">
            {{ user.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(user.createTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ user.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="user-detail-card">
      <div slot="header" class="user-detail-card__header">
        <span>已挂业务系统</span>
        <div>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAddSubSystemUser"
            v-hasPermi="['sub-system:user:create']"
          >挂接系统</el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-key"
            size="mini"
            @click="handleSyncAccount"
            v-hasPermi="['sub-system:user:update']"
          >同步域账号/卡号</el-button>
        </div>
      </div>
      <el-table
        :data="subSystemList"
        border
        stripe
        empty-text="暂未挂接业务系统（也可在业务用户管理中创建后关联本用户）"
        class="sub-system-table"
      >
        <el-table-column label="业务系统" min-width="140" :show-overflow-tooltip="true">
          <template v-slot="scope">
            {{ displaySystemName(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="车间编号" min-width="120" :show-overflow-tooltip="true">
          <template v-slot="scope">
            {{ displayWorkshop(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="业务用户名" prop="username" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="关联车间编号" width="120" align="center">
          <template v-slot="scope">
            <el-tag
              v-if="scope.row.usernameWithWorkshop === '1'"
              type="success"
              size="mini"
            >车间_工号</el-tag>
            <el-tag v-else type="info" size="mini">工号</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="roleNames" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="班组" prop="teamName" width="120" :show-overflow-tooltip="true" />
        <el-table-column label="岗位" prop="postNames" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="外部关联" width="110" align="center">
          <template v-slot="scope">
            <el-tag :type="scope.row.employeeRegistered === '1' ? 'success' : 'info'" size="mini">
              {{ scope.row.employeeRegistered === '1' ? '已关联外部' : '未关联' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="96" align="center">
          <template v-slot="scope">
            <el-tag :type="scope.row.status === '1' ? 'danger' : 'success'" size="mini">
              {{ scope.row.status === '1' ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="280" fixed="right">
          <template v-slot="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-connection"
              @click="handleBind(scope.row)"
              v-hasPermi="['sub-system:user:update']"
            >关联外部</el-button>
            <el-button
              size="mini"
              type="text"
              @click="handleAssignRole(scope.row)"
              v-hasPermi="['sub-system:user:update']"
            >分配角色</el-button>
            <el-button
              size="mini"
              type="text"
              @click="handleEditSubSystemUser(scope.row)"
              v-hasPermi="['sub-system:user:update']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              @click="handleDeleteSubSystemUser(scope.row)"
              v-hasPermi="['sub-system:user:delete']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <sub-system-user-edit-dialog
      :visible.sync="subSystemEditOpen"
      :record-id="subSystemEditId"
      :main-user-id="subSystemEditMainUserId"
      :portal-username="user.username"
      :portal-nickname="user.nickname"
      :main-user-dept-id="user.deptId"
      :exclude-leaves="hungLeaves"
      :client-name="subSystemEditClientName"
      @success="loadSubSystemList"
    />

    <!-- 分配角色：与业务用户管理同源 -->
    <el-dialog title="分配角色" :visible.sync="openRole" width="560px" append-to-body>
      <el-form :model="roleForm" label-width="110px">
        <el-form-item label="挂接目标">
          <el-input :value="roleForm.targetLabel" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width: 100%" filterable>
            <el-option
              v-for="item in visibleRoleOptions"
              :key="item.id"
              :label="roleOptionLabel(item)"
              :value="item.id"
              :disabled="roleOptionDisabled(item)"
            />
          </el-select>
          <div v-if="roleForm.workshopId" class="form-tip">
            <template v-if="!roleForm.crossWorkshop">
              当前车间「{{ roleForm.workshopId }}」，默认仅展示该车间与未绑定车间的全局角色（如超管）；要选其它车间的角色，请勾选下方「跨车间选择角色」
            </template>
            <template v-else>
              已开启跨车间选择：可选用该业务系统全部车间的角色（同一套 MES 管多车间时角色可跨车间分配）
            </template>
          </div>
          <el-checkbox v-if="roleForm.workshopId" v-model="roleForm.crossWorkshop" style="margin-top: 4px">
            跨车间选择角色
          </el-checkbox>
        </el-form-item>
        <el-form-item label="同步外部">
          <el-checkbox v-model="roleForm.syncToExternal">同步上挂或解除角色到业务系统</el-checkbox>
          <div class="form-tip">
            勾选的角色须已在「业务角色管理」中关联外部角色；只增删 JUMP 管辖范围内的角色
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">确 定</el-button>
        <el-button @click="openRole = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 关联外部人员：与业务用户管理同源 -->
    <el-dialog title="关联外部人员" :visible.sync="bindOpen" width="560px" append-to-body :close-on-click-modal="false">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input :value="bindForm.username + (bindForm.nickname ? '（' + bindForm.nickname + '）' : '')" disabled />
        </el-form-item>
        <el-form-item label="车间编号" required>
          <el-input
            v-model="bindForm.workshopCode"
            placeholder="请填写车间编号"
            maxlength="50"
            clearable
            :disabled="bindLoading"
            @change="onBindFilterChange"
          />
          <div class="form-tip">优先用业务用户上的车间编号；新增外部人员时必填</div>
        </el-form-item>
        <el-form-item label="外部人员">
          <div style="margin-bottom: 6px">
            <el-button
              size="mini"
              type="primary"
              plain
              :loading="bindLoading"
              @click="loadExternalEmployees"
            >查询外部人员</el-button>
            <el-button
              size="mini"
              type="primary"
              plain
              :loading="bindRegistering"
              :disabled="!bindForm.workshopCode"
              @click="pushRegisterFromBind"
            >新增外部人员</el-button>
          </div>
          <el-checkbox v-model="bindForm.usernameWithWorkshop" style="margin-bottom: 6px">关联车间编号：注册为 车间编号_工号</el-checkbox>
          <el-select
            v-model="bindForm.userCode"
            filterable
            clearable
            :loading="bindLoading"
            :placeholder="bindLoading ? '正在查询外部人员…' : '选择对应的人员（姓名 + 工号）'"
            style="width: 100%"
          >
            <el-option
              v-for="item in externalEmployees"
              :key="item.userCode"
              :label="(item.userName || item.userCode) + '（' + item.userCode + (item.workshopName || item.workshopCode ? '，' + (item.workshopName || item.workshopCode) : '') + '）'"
              :value="item.userCode"
            />
          </el-select>
          <div class="form-tip">{{ externalEmployeeTip }}</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="bindForm.employeeRegistered === '1'" type="text" :loading="bindSaving" @click="clearExternalEmployee">解除关联</el-button>
        <el-button type="primary" :loading="bindSaving" :disabled="!bindForm.userCode" @click="submitBind">保存关联</el-button>
        <el-button @click="bindOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 同步域账号/刷卡卡号：只读展示用户资料，选择已挂接业务系统 -->
    <el-dialog title="同步域账号/刷卡卡号" :visible.sync="syncOpen" width="600px" append-to-body :close-on-click-modal="false">
      <el-form :model="syncForm" label-width="100px">
        <el-form-item label="业务系统" required>
          <el-select
            v-model="syncForm.rosterId"
            placeholder="请选择已挂接的业务系统"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="item in hungSystemOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
          <div v-if="!hungSystemOptions.length" class="form-tip form-tip--warn">
            该用户还没有挂接业务系统。请先使用「挂接系统」完成挂接。
          </div>
        </el-form-item>
        <el-form-item label="车间编号" required>
          <el-input :value="syncWorkshopCode || '未填写'" disabled />
          <div v-if="syncForm.rosterId && !syncWorkshopCode" class="form-tip form-tip--warn">
            所选业务系统未填写车间编号。同步需要车间编号，请先在该挂接的「修改」中补充。
          </div>
          <div v-else-if="syncWorkshopCode" class="form-tip">
            已按所选业务系统带出车间编号，同步时会一并提交。
          </div>
        </el-form-item>
        <el-form-item v-if="syncTarget" label="业务用户名">
          <el-input :value="syncTarget.userCode" disabled />
          <div class="form-tip">按此账号在业务系统中定位人员。已启用「关联车间编号」时，账号为车间编号_工号。</div>
        </el-form-item>
        <el-form-item label="域账号">
          <el-input :value="user.domainNo || ''" disabled placeholder="未维护" />
        </el-form-item>
        <el-form-item label="刷卡卡号">
          <el-input :value="user.cardNo || ''" disabled placeholder="未维护" />
          <div class="form-tip">域账号和刷卡卡号取自用户资料，此处不能修改。如需变更，请在用户编辑中修改。</div>
        </el-form-item>
        <el-form-item label="ERP账号">
          <div class="sync-erp-row">
            <el-checkbox v-model="syncForm.syncErp" :disabled="!erpNoOptions.length" @change="onSyncErpChange">同步</el-checkbox>
            <el-select
              v-model="syncForm.erpNo"
              :disabled="!syncForm.syncErp"
              placeholder="请选择一个 ERP 账号"
              style="width: 100%"
              clearable
            >
              <el-option v-for="item in erpNoOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </div>
          <div v-if="!erpNoOptions.length" class="form-tip">该用户尚未维护 ERP 账号。如需同步，请先在用户编辑中维护。</div>
          <div v-else-if="!syncForm.syncErp" class="form-tip">未勾选时不同步 ERP 账号，只同步域账号和刷卡卡号。</div>
          <div v-else-if="!syncForm.erpNo" class="form-tip form-tip--warn">已勾选同步 ERP 账号，请从已维护的账号中选择一个。</div>
        </el-form-item>
        <el-form-item label-width="100px">
          <div class="form-tip" :class="{ 'form-tip--warn': !canSync }">{{ syncTip }}</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="syncSubmitting" :disabled="!canSync" @click="submitSyncAccount">确认同步</el-button>
        <el-button @click="syncOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUser } from '@/api/system/user'
import {
  assignSubSystemUserRole,
  bindExternalEmployee,
  deleteSubSystemUser,
  getSubSystemRoleSimpleList,
  getSubSystemUserRoleIds,
  getSubSystemUsersByMainUserId,
  syncSubSystemUserAccount
} from '@/api/system/subSystemUsers'
import {
  getSubSystemRegisterableApis,
  listExternalEmployees,
  registerSubSystemEmployee
} from '@/api/system/subSystemEmployee'
import SubSystemUserEditDialog from '@/views/system/components/SubSystemUserEditDialog'
import { getLeafApiBindings, bindingLabel } from '@/api/system/subSystemApiBinding'
import { formatErpNos } from '@/utils/userFieldDisplay'
import { subSystemRoleOptionLabel, isSubSystemRoleOptionDisabled, filterSubSystemRoleOptions } from '@/utils/subSystemRoleOption'

export default {
  name: 'SystemUserDetail',
  components: { SubSystemUserEditDialog },
  data() {
    return {
      loading: false,
      user: {},
      subSystemList: [],
      subSystemEditOpen: false,
      subSystemEditId: undefined,
      subSystemEditMainUserId: undefined,
      subSystemEditClientName: '',
      openRole: false,
      roleSubmitting: false,
      roleOptions: [],
      registerApis: [],
      roleForm: {
        id: undefined,
        targetLabel: '',
        workshopId: undefined,
        roleIds: [],
        // 默认不同步外部：避免角色未关联外部时默认报“同步失败”
        syncToExternal: false,
        apiSubSystemId: undefined,
        rosterSubSystemId: undefined
      },
      bindOpen: false,
      bindLoading: false,
      bindSaving: false,
      bindRegistering: false,
      externalEmployees: [],
      externalEmployeeTip: '',
      bindForm: {
        id: undefined,
        username: '',
        nickname: '',
        employeeRegistered: '0',
        apiSubSystemId: undefined,
        workshopCode: undefined,
        userCode: undefined,
        usernameWithWorkshop: false,
        rosterSubSystemId: undefined
      },
      syncOpen: false,
      syncSubmitting: false,
      syncForm: {
        rosterId: undefined,
        apiSubSystemId: undefined,
        syncErp: false,
        erpNo: undefined
      }
    }
  },
  computed: {
    /** 角色下拉只展示当前车间可选的角色（其它车间未选中的隐藏，已选中的保留回显） */
    visibleRoleOptions() {
      return filterSubSystemRoleOptions(this.roleOptions, this.roleForm.workshopId, this.roleForm.roleIds,
        !!this.roleForm.crossWorkshop)
    },
    hungLeaves() {
      return (this.subSystemList || []).map(item => ({
        subSystemId: item.subSystemId,
        workshopId: item.workshopId || ''
      }))
    },
    /** 该用户已挂接的业务系统 */
    hungSystemOptions() {
      return (this.subSystemList || []).map(item => ({
        id: item.id,
        label: this.displaySystemName(item) +
          (item.workshopId ? ('（' + item.workshopId + '）') : '') +
          ' / ' + (item.username || '-'),
        row: item
      }))
    },
    syncTarget() {
      const hit = this.hungSystemOptions.find(item => item.id === this.syncForm.rosterId)
      if (!hit) {
        return null
      }
      const row = hit.row
      const workshopCode = (row.workshopId || '').trim()
      return {
        row,
        userCode: row.usernameWithWorkshop === '1' && workshopCode
          ? (workshopCode + '_' + row.username)
          : row.username
      }
    },
    syncWorkshopCode() {
      const row = this.syncTarget && this.syncTarget.row
      return row ? (row.workshopId || '').trim() : ''
    },
    erpNoOptions() {
      return (this.user && Array.isArray(this.user.erpNos) ? this.user.erpNos : []).filter(item => item)
    },
    /** 本次实际提交的字段。未勾选 ERP 时不包含 ERP 账号 */
    syncFields() {
      const fields = []
      if ((this.user.domainNo || '').trim()) {
        fields.push('域账号')
      }
      if ((this.user.cardNo || '').trim()) {
        fields.push('刷卡卡号')
      }
      if (this.syncForm.syncErp && this.syncForm.erpNo) {
        fields.push('ERP账号')
      }
      return fields
    },
    canSync() {
      if (!this.syncForm.rosterId || !this.syncWorkshopCode) {
        return false
      }
      if (this.syncForm.syncErp && !this.syncForm.erpNo) {
        return false
      }
      return this.syncFields.length > 0
    },
    syncTip() {
      if (!this.hungSystemOptions.length) {
        return '请先挂接业务系统，再进行同步。'
      }
      if (!this.syncForm.rosterId) {
        return '请选择已挂接的业务系统。'
      }
      if (!this.syncWorkshopCode) {
        return '所选业务系统未填写车间编号，暂时不能同步。'
      }
      if (!(this.user.domainNo || '').trim() && !(this.user.cardNo || '').trim() && !this.syncForm.syncErp) {
        return '域账号和刷卡卡号都未维护。请先在用户编辑中填写后再同步。'
      }
      if (this.syncForm.syncErp && !this.syncForm.erpNo) {
        return '已勾选同步 ERP 账号，请选择一个 ERP 账号。'
      }
      if (!this.syncFields.length) {
        return '没有可同步的内容。请先在用户编辑中维护域账号或刷卡卡号。'
      }
      const erpNote = this.syncForm.syncErp ? '' : '未勾选 ERP，本次不更新 ERP 账号。'
      return '本次将同步：' + this.syncFields.join('、') + '。' + erpNote
    }
  },
  watch: {
    '$route'(route) {
      if (route.name === 'SystemUserDetail') {
        this.loadDetail()
      }
    },
    'syncForm.rosterId'() {
      if (this.syncOpen) {
        this.refreshSyncBinding()
      }
    }
  },
  created() {
    this.loadRegisterableApis()
    if (this.$route.name === 'SystemUserDetail') {
      this.loadDetail()
    }
  },
  activated() {
    if (this.$route.name === 'SystemUserDetail') {
      this.loadDetail()
    }
  },
  methods: {
    formatErpNos,
    displaySystemName(row) {
      if (!row) {
        return '-'
      }
      const name = (row.clientName || '').trim()
      if (name.indexOf(' / ') >= 0) {
        return name.split(' / ')[0] || row.clientId || '-'
      }
      return name || row.clientId || '-'
    },
    displayWorkshop(row) {
      if (!row) {
        return '-'
      }
      // 与业务用户管理对齐：车间编号列只展示编号，不展示系统/叶子中文名
      return (row.workshopId || '').trim() || '-'
    },
    loadDetail() {
      const userId = Number(this.$route.query.userId)
      if (!userId) {
        this.$message.error('缺少用户编号，请从用户列表进入')
        return
      }
      this.loading = true
      this.user = {}
      this.subSystemList = []
      getUser(userId).then(userResponse => {
        this.user = userResponse.data || {}
        return this.loadSubSystemList(userId)
      }).catch(() => {
        this.user = {}
        this.subSystemList = []
      }).finally(() => {
        this.loading = false
      })
    },
    loadSubSystemList(userId) {
      const mainUserId = userId || Number(this.$route.query.userId)
      if (!mainUserId) {
        this.subSystemList = []
        return Promise.resolve()
      }
      return getSubSystemUsersByMainUserId(mainUserId).then(subSystemResponse => {
        this.subSystemList = subSystemResponse.data || []
      }).catch(() => {
        this.subSystemList = []
      })
    },
    loadRegisterableApis() {
      return getSubSystemRegisterableApis().then(res => {
        this.registerApis = res.data || []
      }).catch(() => {
        this.registerApis = []
      })
    },
    applyDefaultAssignApi(rosterSubSystemId) {
      const apis = this.registerApis || []
      const hit = apis.find(item => item.subSystemId === rosterSubSystemId)
      const picked = hit ? hit.subSystemId : (apis.length === 1 ? apis[0].subSystemId : undefined)
      if (picked && !this.roleForm.apiSubSystemId) {
        this.roleForm.apiSubSystemId = picked
      }
    },
    handleEditSubSystemUser(row) {
      if (!row || !row.id) {
        return
      }
      this.subSystemEditId = row.id
      this.subSystemEditMainUserId = undefined
      this.subSystemEditClientName = this.displaySystemName(row)
      this.subSystemEditOpen = true
    },
    handleAddSubSystemUser() {
      const mainUserId = Number(this.$route.query.userId)
      if (!mainUserId) {
        this.$message.error('缺少用户编号')
        return
      }
      this.subSystemEditId = undefined
      this.subSystemEditMainUserId = mainUserId
      this.subSystemEditClientName = ''
      this.subSystemEditOpen = true
    },
    /** 打开同步弹窗：域账号、刷卡卡号只展示，不在此处修改 */
    handleSyncAccount() {
      const systems = this.hungSystemOptions
      this.syncForm = {
        rosterId: systems.length === 1 ? systems[0].id : undefined,
        apiSubSystemId: undefined,
        apiLabel: '',
        syncErp: false,
        erpNo: undefined
      }
      this.syncOpen = true
      this.refreshSyncBinding()
    },
    refreshSyncBinding() {
      const row = this.syncTarget && this.syncTarget.row
      this.applyLeafBinding(this.syncForm, row && row.subSystemId, row && row.workshopId, 'account_sync')
    },
    applyLeafBinding(target, subSystemId, workshopCode, scene) {
      if (!target) {
        return Promise.resolve()
      }
      target.apiSubSystemId = undefined
      target.apiLabel = ''
      if (!subSystemId) {
        return Promise.resolve()
      }
      return getLeafApiBindings(subSystemId, workshopCode).then(res => {
        const hit = ((res.data && res.data.bindings) || []).find(item => item.scene === scene)
        if (!hit) {
          return
        }
        target.apiSubSystemId = hit.apiSubSystemId
        target.apiLabel = bindingLabel(hit)
      }).catch(() => {})
    },
    onSyncErpChange(checked) {
      if (!checked) {
        this.syncForm.erpNo = undefined
      }
    },
    async submitSyncAccount() {
      if (!this.canSync) {
        return
      }
      this.syncSubmitting = true
      try {
        const res = await syncSubSystemUserAccount({
          id: this.syncForm.rosterId,
          apiSubSystemId: this.syncForm.apiSubSystemId,
          erpNo: this.syncForm.syncErp ? this.syncForm.erpNo : undefined
        })
        this.$modal.msgSuccess((res.data && res.data.message) || '同步成功')
        this.syncOpen = false
        await this.loadDetail()
      } finally {
        this.syncSubmitting = false
      }
    },
    handleDeleteSubSystemUser(row) {
      if (!row || !row.id) {
        return
      }
      this.$modal.confirm('是否确认删除该挂接？').then(() => {
        return deleteSubSystemUser(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        return this.loadSubSystemList()
      }).catch(() => {})
    },
    handleAssignRole(row) {
      if (!row || !row.id) {
        return
      }
      this.roleForm = {
        id: row.id,
        targetLabel: this.displaySystemName(row) + (row.workshopId ? (' / ' + this.displayWorkshop(row)) : ''),
        workshopId: row.workshopId || '',
        roleIds: [],
        // 跨车间选择：默认关闭，仅展示当前车间角色；开启后可选该业务系统全部车间角色
        crossWorkshop: false,
        // 默认不同步外部，需要时手动勾选
        syncToExternal: false,
        apiSubSystemId: undefined,
        apiLabel: '',
        rosterSubSystemId: row.subSystemId
      }
      this.openRole = true
      this.applyLeafBinding(this.roleForm, row.subSystemId, row.workshopId, 'role_assign')
      Promise.all([
        getSubSystemRoleSimpleList(row.subSystemId).then(res => {
          this.roleOptions = res.data || []
        }).catch(() => { this.roleOptions = [] }),
        getSubSystemUserRoleIds(row.id).then(res => {
          this.roleForm.roleIds = res.data || []
        }).catch(() => {
          this.$modal.msgError('加载已分配角色失败')
        })
      ])
    },
    roleOptionLabel(item) {
      return subSystemRoleOptionLabel(item, { syncToExternal: this.roleForm.syncToExternal })
    },
    roleOptionDisabled(item) {
      return isSubSystemRoleOptionDisabled(item, this.roleForm.workshopId, !!this.roleForm.crossWorkshop)
    },
    submitRole() {
      if (this.roleForm.workshopId && !this.roleForm.crossWorkshop) {
        const bad = (this.roleForm.roleIds || []).some(id => {
          const role = this.roleOptions.find(r => r.id === id)
          return role && role.workshopCode && String(role.workshopCode) !== String(this.roleForm.workshopId)
        })
        if (bad) {
          this.$modal.msgError('请只选择当前车间下的角色')
          return
        }
      }
      this.roleSubmitting = true
      assignSubSystemUserRole({
        id: this.roleForm.id,
        roleIds: this.roleForm.roleIds || [],
        syncToExternal: !!this.roleForm.syncToExternal,
        apiSubSystemId: this.roleForm.syncToExternal ? this.roleForm.apiSubSystemId : undefined
      }).then(res => {
        const data = res.data || {}
        if (data.externalSynced && data.externalSuccess === false) {
          this.$modal.msgError('JUMP 分配成功；外部系统同步失败：' + (data.externalMessage || '未知错误'))
        } else if (data.externalSynced) {
          this.$modal.msgSuccess('分配成功，已同步外部系统')
        } else {
          this.$modal.msgSuccess('分配成功')
        }
        this.openRole = false
        return this.loadSubSystemList()
      }).catch(() => {
        this.$modal.msgError('分配角色失败')
      }).finally(() => {
        this.roleSubmitting = false
      })
    },
    handleBind(row) {
      if (!row || !row.id) {
        return
      }
      this.externalEmployees = []
      this.externalEmployeeTip = ''
      this.bindForm = {
        id: row.id,
        username: row.username,
        nickname: row.nickname,
        employeeRegistered: row.employeeRegistered || '0',
        apiSubSystemId: undefined,
        apiLabel: '',
        workshopCode: row.workshopId || undefined,
        userCode: undefined,
        usernameWithWorkshop: row.usernameWithWorkshop === '1',
        rosterSubSystemId: row.subSystemId
      }
      this.bindOpen = true
      this.applyLeafBinding(this.bindForm, row.subSystemId, row.workshopId, 'employee_query')
    },
    onBindFilterChange() {
      this.bindForm.userCode = undefined
      this.externalEmployees = []
      this.externalEmployeeTip = ''
      this.applyLeafBinding(this.bindForm, this.bindForm.rosterSubSystemId, this.bindForm.workshopCode, 'employee_query')
    },
    loadExternalEmployees(silent) {
      if (!this.bindForm.apiSubSystemId) {
        this.applyLeafBinding(this.bindForm, this.bindForm.rosterSubSystemId, this.bindForm.workshopCode, 'employee_query').then(() => {
          if (!this.bindForm.apiSubSystemId) {
            if (!silent) {
              this.$modal.msgWarning('暂时无法查询外部人员')
            }
            return
          }
          this.loadExternalEmployees(silent)
        })
        return
      }
      if (!this.bindForm.username) {
        if (!silent) {
          this.$modal.msgWarning('当前记录没有工号，无法查询外部人员')
        }
        return
      }
      this.bindLoading = true
      listExternalEmployees(this.bindForm.apiSubSystemId, this.bindForm.workshopCode, this.bindForm.username).then(res => {
        const list = res.data || []
        this.externalEmployees = list
        this.externalEmployeeTip = list.length
          ? ('共 ' + list.length + ' 人（含其它车间的 车间_工号，如 4200_14059）。关联请选账号；新增 5500_工号 与其它车间账号互不影响')
          : '没有查到该工号的外部人员（已按裸工号及各车间_工号补查）'
        const bare = (this.bindForm.username || '').trim()
        const workshop = (this.bindForm.workshopCode || '').trim()
        const prefixed = workshop ? (workshop + '_' + bare) : ''
        const sameCode = (a, b) => a && b && String(a).trim().toLowerCase() === String(b).trim().toLowerCase()
        const hit = (prefixed && list.find(item => sameCode(item.userCode, prefixed)))
          || list.find(item => sameCode(item.userCode, bare))
        if (hit) {
          this.bindForm.userCode = hit.userCode
          this.bindForm.usernameWithWorkshop = hit.userCode === prefixed
        }
      }).catch(() => {
        this.externalEmployees = []
        this.externalEmployeeTip = '查询外部人员失败'
      }).finally(() => {
        this.bindLoading = false
      })
    },
    submitBind() {
      if (!this.bindForm.userCode) {
        this.$modal.msgWarning('请选择外部人员')
        return
      }
      this.bindSaving = true
      bindExternalEmployee(this.bindForm.id, this.bindForm.userCode, this.bindForm.workshopCode, this.bindForm.apiSubSystemId)
        .then(() => {
          this.$modal.msgSuccess('已关联外部人员')
          this.bindOpen = false
          return this.loadSubSystemList()
        }).finally(() => {
          this.bindSaving = false
        })
    },
    clearExternalEmployee() {
      this.bindSaving = true
      bindExternalEmployee(this.bindForm.id, '', this.bindForm.workshopCode, this.bindForm.apiSubSystemId)
        .then(() => {
          this.$modal.msgSuccess('已解除关联')
          this.bindOpen = false
          return this.loadSubSystemList()
        }).finally(() => {
          this.bindSaving = false
        })
    },
    pushRegisterFromBind() {
      this.bindRegistering = true
      registerSubSystemEmployee({
        workshopCode: this.bindForm.workshopCode,
        usernameWithWorkshop: !!this.bindForm.usernameWithWorkshop,
        ids: [this.bindForm.id]
      }).then(res => {
        const results = res.data || []
        if (!results.length) {
          this.$modal.msgError('外部系统未返回注册结果，请刷新后重试')
          return
        }
        const fail = results.filter(r => !r.success)
        if (fail.length) {
          this.$modal.msgError(fail[0].message || '注册失败')
          return
        }
        this.$modal.msgSuccess(results[0].message || '已在外部系统建人并关联')
        this.bindForm.employeeRegistered = '1'
        this.loadSubSystemList()
        this.loadExternalEmployees(true)
      }).catch(() => {
      }).finally(() => {
        this.bindRegistering = false
      })
    }
  }
}
</script>

<style scoped>
.user-detail-page {
  padding-bottom: 20px;
}
.user-detail-card {
  margin-bottom: 16px;
}
.user-detail-card ::v-deep .el-card__header {
  font-weight: 600;
}
.user-detail-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sub-system-table ::v-deep .el-table__header th {
  background: #f5f7fa;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
.form-tip--warn {
  color: #f56c6c;
}
.sync-erp-row {
  display: flex;
  align-items: center;
}
.sync-erp-row .el-checkbox {
  margin-right: 12px;
  flex: none;
}
</style>
