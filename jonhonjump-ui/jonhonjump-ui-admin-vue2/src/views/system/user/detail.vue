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
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAddSubSystemUser"
          v-hasPermi="['sub-system:user:create']"
        >挂接系统</el-button>
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
              v-for="item in roleOptions"
              :key="item.id"
              :label="roleOptionLabel(item)"
              :value="item.id"
              :disabled="roleOptionDisabled(item)"
            />
          </el-select>
          <div v-if="roleForm.workshopId" class="form-tip">
            当前车间「{{ roleForm.workshopId }}」，请优先选择该车间下的角色
          </div>
        </el-form-item>
        <el-form-item label="同步外部">
          <el-checkbox v-model="roleForm.syncToExternal">调用外部系统接口同步上挂/解除角色</el-checkbox>
          <div class="form-tip">
            勾选的角色须已在「业务角色管理」中关联外部角色；只增删 JUMP 管辖范围内的角色
          </div>
        </el-form-item>
        <el-form-item v-if="roleForm.syncToExternal" label="接口目标">
          <el-select v-model="roleForm.apiSubSystemId" placeholder="请选择调用哪个系统的人员接口" filterable style="width: 100%">
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
        <el-form-item label="接口目标" required>
          <el-select
            v-model="bindForm.apiSubSystemId"
            placeholder="请选择外部系统"
            filterable
            style="width: 100%"
            :disabled="bindLoading"
            @change="onBindFilterChange"
          >
            <el-option
              v-for="item in registerApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
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
              :disabled="!bindForm.apiSubSystemId"
              @click="loadExternalEmployees"
            >查询外部人员</el-button>
            <el-button
              size="mini"
              type="primary"
              plain
              :loading="bindRegistering"
              :disabled="!bindForm.apiSubSystemId || !bindForm.workshopCode"
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
  getSubSystemUsersByMainUserId
} from '@/api/system/subSystemUsers'
import {
  getSubSystemRegisterableApis,
  listExternalEmployees,
  registerSubSystemEmployee
} from '@/api/system/subSystemEmployee'
import SubSystemUserEditDialog from '@/views/system/components/SubSystemUserEditDialog'
import { formatErpNos } from '@/utils/userFieldDisplay'
import { subSystemRoleOptionLabel, isSubSystemRoleOptionDisabled } from '@/utils/subSystemRoleOption'

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
      }
    }
  },
  computed: {
    hungLeaves() {
      return (this.subSystemList || []).map(item => ({
        subSystemId: item.subSystemId,
        workshopId: item.workshopId || ''
      }))
    }
  },
  watch: {
    '$route'(route) {
      if (route.name === 'SystemUserDetail') {
        this.loadDetail()
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
        // 默认不同步外部，需要时手动勾选
        syncToExternal: false,
        apiSubSystemId: undefined,
        rosterSubSystemId: row.subSystemId
      }
      this.openRole = true
      Promise.all([
        getSubSystemRoleSimpleList(row.subSystemId).then(res => {
          this.roleOptions = res.data || []
        }).catch(() => { this.roleOptions = [] }),
        getSubSystemUserRoleIds(row.id).then(res => {
          this.roleForm.roleIds = res.data || []
        }).catch(() => {
          this.$modal.msgError('加载已分配角色失败')
        }),
        this.registerApis.length ? Promise.resolve() : this.loadRegisterableApis()
      ]).then(() => {
        this.applyDefaultAssignApi(row.subSystemId)
      })
    },
    roleOptionLabel(item) {
      return subSystemRoleOptionLabel(item, { syncToExternal: this.roleForm.syncToExternal })
    },
    roleOptionDisabled(item) {
      return isSubSystemRoleOptionDisabled(item, this.roleForm.workshopId)
    },
    submitRole() {
      if (this.roleForm.workshopId) {
        const bad = (this.roleForm.roleIds || []).some(id => {
          const role = this.roleOptions.find(r => r.id === id)
          return role && role.workshopCode && String(role.workshopCode) !== String(this.roleForm.workshopId)
        })
        if (bad) {
          this.$modal.msgError('请只选择当前车间下的角色')
          return
        }
      }
      if (this.roleForm.syncToExternal && !this.roleForm.apiSubSystemId) {
        this.$modal.msgError('勾选同步外部时请选择接口目标')
        return
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
        workshopCode: row.workshopId || undefined,
        userCode: undefined,
        usernameWithWorkshop: row.usernameWithWorkshop === '1',
        rosterSubSystemId: row.subSystemId
      }
      this.bindOpen = true
      const sid = row.subSystemId
      ;(this.registerApis.length ? Promise.resolve() : this.loadRegisterableApis()).then(() => {
        const hit = (this.registerApis || []).find(item => item.subSystemId === sid)
        this.bindForm.apiSubSystemId = hit
          ? hit.subSystemId
          : ((this.registerApis || []).length === 1 ? this.registerApis[0].subSystemId : undefined)
      })
    },
    onBindFilterChange() {
      this.bindForm.userCode = undefined
      this.externalEmployees = []
      this.externalEmployeeTip = ''
    },
    loadExternalEmployees(silent) {
      if (!this.bindForm.apiSubSystemId) {
        if (!silent) {
          this.$modal.msgWarning('请先选择接口目标')
        }
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
        apiSubSystemId: this.bindForm.apiSubSystemId,
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
</style>
