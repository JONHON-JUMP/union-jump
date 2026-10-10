<template>
  <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" append-to-body @close="handleClose">
    <el-form ref="form" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="业务系统" prop="subSystemId">
        <el-select
          v-if="isCreateMode"
          v-model="form.subSystemId"
          placeholder="请选择要添加的业务系统"
          filterable
          no-data-text="暂无可添加的业务系统"
          style="width: 100%"
          @change="handleSubSystemChange"
        >
          <el-option
            v-for="item in availableClientList"
            :key="item.id"
            :label="item.name + ' (' + item.clientId + ')'"
            :value="item.id"
          />
        </el-select>
        <el-input v-else :value="clientName || '-'" disabled />
      </el-form-item>
      <el-alert
        v-if="isCreateMode && availableClientList.length === 0"
        title="当前用户已关联全部业务系统，无可继续添加"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-alert
        v-if="isBindMode && matchHint"
        :title="matchHint"
        :type="matchOk ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-divider content-position="left">业务系统用户信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" disabled placeholder="与主用户登录名同名匹配" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="form.nickname" :disabled="identityLocked" placeholder="用户姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="车间">
            <el-input v-if="workshopOptions.length <= 1" :value="workshopFixedText()" disabled placeholder="暂无车间对照" />
            <el-select
              v-else
              v-model="form.workshopId"
              placeholder="请选择已对照的部门车间"
              filterable
              style="width: 100%"
              :disabled="identityLocked"
              @change="handleWorkshopChange"
            >
              <el-option
                v-for="item in workshopOptions"
                :key="item.workshopCode"
                :label="workshopOptionLabel(item)"
                :value="item.workshopCode"
              />
            </el-select>
            <div v-if="workshopOptions.length === 1" class="form-tip">车间与部门是一对一，已确定</div>
            <div v-else-if="workshopHint" class="form-tip">{{ workshopHint }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="班组">
            <el-select v-model="form.teamId" placeholder="请选择班组" clearable filterable style="width: 100%"
                       :disabled="identityLocked">
              <el-option
                v-for="item in teamOptions"
                :key="item.teamCode"
                :label="item.teamName + ' (' + item.teamCode + ')'"
                :value="item.teamCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">岗位 / 角色</el-divider>
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
        <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
          <el-option
            v-for="item in roleOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
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
      <el-button
        type="primary"
        :loading="submitLoading"
        :disabled="(isCreateMode && availableClientList.length === 0) || (isBindMode && !matchOk)"
        @click="submitForm"
      >确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  bindSubSystemMainUser,
  createSubSystemUser,
  getSubSystemClientSimpleList,
  getSubSystemPostSimpleList,
  getSubSystemRoleSimpleList,
  getSubSystemTeamSimpleList,
  getSubSystemUser,
  getSubSystemUserByUsername,
  updateSubSystemUser
} from '@/api/system/subSystemUsers'
import { getSubSystemWorkshopByDept, getSubSystemWorkshopSimpleList } from '@/api/system/subSystemWorkshop'

export default {
  name: 'SubSystemUserEditDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    recordId: {
      type: Number,
      default: undefined
    },
    mainUserId: {
      type: Number,
      default: undefined
    },
    portalUsername: {
      type: String,
      default: ''
    },
    /** 主用户归属部门，用于默认选中车间对照、过滤班组 */
    mainUserDeptId: {
      type: Number,
      default: undefined
    },
    excludeSubSystemIds: {
      type: Array,
      default: () => []
    },
    clientName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      submitLoading: false,
      form: {},
      clientList: [],
      roleOptions: [],
      postOptions: [],
      teamOptions: [],
      workshopOptions: [],
      matchOk: false,
      matchHint: '',
      workshopHint: '',
      workshopDeptId: undefined,
      rules: {
        subSystemId: [{ required: true, message: '请选择业务系统', trigger: 'change' }],
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    isCreateMode() {
      return !this.recordId
    },
    isBindMode() {
      return this.isCreateMode && !!this.mainUserId
    },
    identityLocked() {
      return this.isBindMode && !this.matchOk
    },
    availableClientList() {
      const excludeSet = new Set(this.excludeSubSystemIds || [])
      return this.clientList.filter(item => !excludeSet.has(item.id))
    },
    dialogTitle() {
      if (this.isBindMode) {
        return '开通业务系统访问'
      }
      if (this.isCreateMode) {
        return '添加业务系统用户'
      }
      return this.clientName ? `修改业务系统用户 - ${this.clientName}` : '修改业务系统用户'
    }
  },
  watch: {
    visible(value) {
      if (!value) {
        return
      }
      if (this.recordId) {
        this.loadRecord()
        return
      }
      if (this.mainUserId) {
        this.openBind()
        return
      }
      this.openCreate()
    }
  },
  methods: {
    resetFormData() {
      this.form = {
        id: undefined,
        subSystemId: undefined,
        mainUserId: undefined,
        username: undefined,
        nickname: undefined,
        workshopId: undefined,
        teamId: undefined,
        homeMenuId: undefined,
        status: '0',
        remark: undefined,
        roleIds: [],
        postIds: []
      }
      this.roleOptions = []
      this.postOptions = []
      this.teamOptions = []
      this.workshopOptions = []
      this.matchOk = false
      this.matchHint = ''
      this.workshopHint = ''
      this.workshopDeptId = undefined
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    workshopFixedText() {
      const item = (this.workshopOptions || [])[0]
      return item ? this.workshopOptionLabel(item) : ''
    },
    workshopOptionLabel(item) {
      const name = item.workshopName || '车间'
      const code = item.workshopCode || ''
      const dept = item.deptName ? (' / ' + item.deptName) : ''
      return name + '（' + code + '）' + dept
    },
    loadClientList() {
      return getSubSystemClientSimpleList(true).then(res => {
        this.clientList = res.data || []
      })
    },
    loadWorkshopOptions(subSystemId) {
      if (!subSystemId) {
        this.workshopOptions = []
        return Promise.resolve()
      }
      const deptId = this.mainUserDeptId
      const load = (id) => getSubSystemWorkshopSimpleList(subSystemId, id).then(res => res.data || [])
      const apply = (list) => {
        this.workshopOptions = list || []
        if (this.workshopOptions.length === 1) {
          this.form.workshopId = this.workshopOptions[0].workshopCode
          this.workshopDeptId = this.workshopOptions[0].deptId
          this.workshopHint = ''
        } else if (!this.workshopOptions.length) {
          this.workshopHint = '暂无车间对照，请先在「车间对照」中维护'
        }
      }
      const request = deptId
        ? load(deptId).then(list => (list.length ? list : load(undefined)))
        : load(undefined)
      return request.then(apply).catch(() => {
        this.workshopOptions = []
        this.workshopHint = '车间对照加载失败'
      })
    },
    loadTeamOptions(subSystemId, deptId) {
      if (!subSystemId) {
        this.teamOptions = []
        return Promise.resolve()
      }
      return getSubSystemTeamSimpleList(subSystemId, deptId || undefined).then(res => {
        const list = res.data || []
        if (list.length > 0 || !deptId) {
          this.teamOptions = list
          return
        }
        return getSubSystemTeamSimpleList(subSystemId).then(allRes => {
          this.teamOptions = allRes.data || []
        })
      })
    },
    loadSubOptions(subSystemId) {
      if (!subSystemId) {
        this.roleOptions = []
        this.postOptions = []
        this.teamOptions = []
        this.workshopOptions = []
        return Promise.resolve()
      }
      return Promise.all([
        getSubSystemRoleSimpleList(subSystemId).then(res => { this.roleOptions = res.data || [] }),
        getSubSystemPostSimpleList(subSystemId).then(res => { this.postOptions = res.data || [] }),
        this.loadWorkshopOptions(subSystemId)
      ]).then(() => this.reloadTeamsForCurrentWorkshop(subSystemId))
    },
    reloadTeamsForCurrentWorkshop(subSystemId) {
      const deptId = this.workshopDeptId || this.mainUserDeptId
      return this.loadTeamOptions(subSystemId, deptId)
    },
    /** 只接受已对照的车间。一对一直接用那一条，对照外的编号不放进下拉 */
    ensureWorkshopOption(workshopCode) {
      const codes = (this.workshopOptions || []).map(w => w.workshopCode)
      if (codes.length === 1) {
        this.form.workshopId = codes[0]
        return
      }
      if (workshopCode && codes.indexOf(workshopCode) < 0) {
        this.form.workshopId = undefined
      }
    },
    /**
     * 同步车间选中与班组过滤。
     * clearTeam=true 表示用户主动改车间；加载已有数据时不要清班组。
     */
    syncWorkshopSelection(workshopCode, clearTeam) {
      const codes = (this.workshopOptions || []).map(w => w.workshopCode)
      let code = workshopCode
      if (codes.length === 1) {
        code = codes[0]
      } else if (code && codes.indexOf(code) < 0) {
        code = undefined
      }
      const hit = (this.workshopOptions || []).find(w => w.workshopCode === code)
      this.form.workshopId = code || undefined
      this.workshopDeptId = hit ? hit.deptId : undefined
      if (clearTeam) {
        this.form.teamId = undefined
      }
      if (hit && hit.deptId) {
        this.workshopHint = '已选车间对照：' + this.workshopOptionLabel(hit)
      } else if (workshopCode && hit && !hit.deptId) {
        this.workshopHint = '该车间编号不在当前对照中，建议改选正式对照项'
      } else if (!workshopCode) {
        this.workshopHint = this.workshopOptions.length
          ? '请从车间对照中选择车间'
          : '暂无车间对照，请先在「车间对照」中维护'
      }
      return this.reloadTeamsForCurrentWorkshop(this.form.subSystemId)
    },
    handleWorkshopChange(workshopCode) {
      this.syncWorkshopSelection(workshopCode, true)
    },
    /** 优先保留已有车间；否则按主用户归属部门从对照默认选中 */
    preferWorkshopFromDept(subSystemId, preferExisting) {
      const existing = preferExisting ? this.form.workshopId : undefined
      if (!subSystemId) {
        return Promise.resolve()
      }
      if (existing) {
        return this.syncWorkshopSelection(existing, false)
      }
      if (!this.mainUserDeptId) {
        this.workshopHint = this.workshopOptions.length
          ? '请从车间对照中选择车间'
          : '暂无车间对照，请先在「车间对照」中维护'
        return this.reloadTeamsForCurrentWorkshop(subSystemId)
      }
      return getSubSystemWorkshopByDept(subSystemId, this.mainUserDeptId).then(res => {
        const data = res.data
        if (data && data.workshopCode) {
          return this.syncWorkshopSelection(data.workshopCode, true).then(() => {
            this.workshopHint = '已按主用户归属部门自动选中车间对照'
          })
        }
        this.workshopHint = '该归属部门未维护车间对照，请从下拉选择或先维护对照'
        return this.reloadTeamsForCurrentWorkshop(subSystemId)
      }).catch(() => {
        this.workshopHint = '车间对照加载失败，请从下拉选择'
        return this.reloadTeamsForCurrentWorkshop(subSystemId)
      })
    },
    handleSubSystemChange(subSystemId) {
      this.form.teamId = undefined
      this.form.postIds = []
      this.form.roleIds = []
      this.form.homeMenuId = undefined
      this.form.workshopId = undefined
      this.workshopDeptId = undefined
      this.workshopHint = ''
      this.matchOk = false
      this.matchHint = ''
      this.loadSubOptions(subSystemId).then(() => {
        return this.preferWorkshopFromDept(subSystemId, false)
      }).then(() => {
        if (this.isBindMode) {
          return this.matchRosterUser(subSystemId)
        }
        return null
      })
    },
    matchRosterUser(subSystemId) {
      const username = (this.portalUsername || '').trim()
      if (!subSystemId || !username) {
        this.matchOk = false
        this.matchHint = '缺少主用户登录名，无法匹配业务系统用户'
        return Promise.resolve()
      }
      return getSubSystemUserByUsername(subSystemId, username).then(res => {
        const data = res.data
        if (!data || !data.id) {
          this.matchOk = false
          this.matchHint = `业务系统中不存在同名用户「${username}」，请先在业务系统用户中导入或新增`
          this.form.username = username
          this.form.nickname = undefined
          this.form.teamId = undefined
          this.form.id = undefined
          return this.preferWorkshopFromDept(subSystemId, false)
        }
        this.matchOk = true
        this.matchHint = `已匹配业务系统用户「${data.username}」，确认后写入关联；可同时修改车间、班组、岗位、角色等`
        this.applyRosterToForm(data)
        return this.preferWorkshopFromDept(subSystemId, !!data.workshopId)
      }).catch(() => {
        this.matchOk = false
        this.matchHint = '匹配业务系统用户失败'
      })
    },
    applyRosterToForm(data) {
      this.form.id = data.id
      this.form.subSystemId = data.subSystemId
      this.form.username = data.username
      this.form.nickname = data.nickname
      this.form.workshopId = data.workshopId
      this.form.teamId = data.teamId
      this.form.homeMenuId = data.homeMenuId
      this.form.status = data.status || '0'
      this.form.remark = data.remark
      this.form.roleIds = data.roleIds || []
      this.form.postIds = data.postIds || []
      this.workshopHint = ''
      this.workshopDeptId = undefined
    },
    openBind() {
      this.resetFormData()
      this.form.mainUserId = this.mainUserId
      this.form.username = this.portalUsername
      this.loadClientList().catch(() => {
        this.dialogVisible = false
      })
    },
    openCreate() {
      this.resetFormData()
      this.loadClientList().catch(() => {
        this.dialogVisible = false
      })
    },
    loadRecord() {
      if (!this.recordId) {
        return
      }
      this.resetFormData()
      getSubSystemUser(this.recordId).then(res => {
        const data = res.data || {}
        return this.loadSubOptions(data.subSystemId).then(() => {
          this.applyRosterToForm(data)
          this.form.mainUserId = data.mainUserId
          return this.preferWorkshopFromDept(data.subSystemId, !!data.workshopId)
        })
      }).catch(() => {
        this.dialogVisible = false
      })
    },
    submitForm() {
      if (this.isCreateMode && this.availableClientList.length === 0) {
        this.$modal.msgWarning('当前用户已关联全部业务系统')
        return
      }
      if (this.isBindMode && !this.matchOk) {
        this.$modal.msgWarning(this.matchHint || '请先匹配到业务系统同名用户')
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.submitLoading = true
        const run = this.isBindMode
          ? bindSubSystemMainUser(this.form.subSystemId, this.mainUserId).then(res => {
            this.form.id = res.data
            this.form.mainUserId = this.mainUserId
            return updateSubSystemUser(this.form)
          })
          : (this.form.id ? updateSubSystemUser(this.form) : createSubSystemUser(this.form))
        run.then(() => {
          this.$modal.msgSuccess(this.isBindMode ? '开通成功' : (this.form.id ? '修改成功' : '新增成功'))
          this.dialogVisible = false
          this.$emit('success')
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleClose() {
      this.resetFormData()
    }
  }
}
</script>

<style scoped>
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
