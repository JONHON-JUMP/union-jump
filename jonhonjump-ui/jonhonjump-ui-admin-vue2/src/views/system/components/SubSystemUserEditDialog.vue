<template>
  <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" append-to-body @close="handleClose">
    <el-form ref="form" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="外部系统" prop="subSystemId">
        <el-select
          v-if="isCreateMode"
          v-model="form.subSystemId"
          placeholder="请选择要添加的外部系统"
          filterable
          no-data-text="暂无可添加的外部系统"
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
        title="当前用户已关联全部外部系统，无可继续添加"
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
      <el-divider content-position="left">子系统用户信息</el-divider>
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
          <el-form-item label="车间编号">
            <el-input v-model="form.workshopId" placeholder="请输入车间编号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="班组">
            <el-select v-model="form.teamId" placeholder="请选择班组" clearable filterable style="width: 100%">
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
      <el-divider content-position="left">岗位 / 角色 / 主页面</el-divider>
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
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import {
  bindSubSystemMainUser,
  createSubSystemUser,
  getSubSystemClientSimpleList,
  getSubSystemPostSimpleList,
  getSubSystemRoleSimpleList,
  getSubSystemTeamSimpleList,
  getSubSystemUser,
  getSubSystemUserByUsername,
  getSubSystemUserHomeMenuTree,
  updateSubSystemUser
} from '@/api/system/subSystemUsers'

export default {
  name: 'SubSystemUserEditDialog',
  components: { Treeselect },
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
      menuPageOptions: [],
      matchOk: false,
      matchHint: '',
      rules: {
        subSystemId: [{ required: true, message: '请选择外部系统', trigger: 'change' }],
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
    /** 从主用户详情开通：只挂接同名子系统用户 */
    isBindMode() {
      return this.isCreateMode && !!this.mainUserId
    },
    /** 挂接流程中：未匹配成功前锁定姓名；匹配后可改车间班组角色等 */
    identityLocked() {
      return this.isBindMode && !this.matchOk
    },
    availableClientList() {
      const excludeSet = new Set(this.excludeSubSystemIds || [])
      return this.clientList.filter(item => !excludeSet.has(item.id))
    },
    dialogTitle() {
      if (this.isBindMode) {
        return '开通外部系统访问'
      }
      if (this.isCreateMode) {
        return '添加外部系统用户'
      }
      return this.clientName ? `修改外部系统用户 - ${this.clientName}` : '修改外部系统用户'
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
      this.menuPageOptions = []
      this.matchOk = false
      this.matchHint = ''
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    loadClientList() {
      return getSubSystemClientSimpleList().then(res => {
        this.clientList = res.data || []
      })
    },
    loadSubOptions(subSystemId) {
      if (!subSystemId) {
        this.roleOptions = []
        this.postOptions = []
        this.teamOptions = []
        this.menuPageOptions = []
        return Promise.resolve()
      }
      return Promise.all([
        getSubSystemRoleSimpleList(subSystemId).then(res => { this.roleOptions = res.data || [] }),
        getSubSystemPostSimpleList(subSystemId).then(res => { this.postOptions = res.data || [] }),
        getSubSystemTeamSimpleList(subSystemId).then(res => { this.teamOptions = res.data || [] })
      ])
    },
    handleSubSystemChange(subSystemId) {
      this.form.teamId = undefined
      this.form.postIds = []
      this.form.roleIds = []
      this.form.homeMenuId = undefined
      this.matchOk = false
      this.matchHint = ''
      this.loadSubOptions(subSystemId).then(() => {
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
        this.matchHint = '缺少主用户登录名，无法匹配子系统用户'
        return Promise.resolve()
      }
      return getSubSystemUserByUsername(subSystemId, username).then(res => {
        const data = res.data
        if (!data || !data.id) {
          this.matchOk = false
          this.matchHint = `子系统中不存在同名用户「${username}」，请先在外部系统用户中导入或新增`
          this.form.username = username
          this.form.nickname = undefined
          this.form.workshopId = undefined
          this.form.teamId = undefined
          this.form.id = undefined
          return
        }
        this.matchOk = true
        this.matchHint = `已匹配子系统用户「${data.username}」，确认后写入 main_user_id；可同时修改车间、班组、岗位、角色等`
        this.applyRosterToForm(data)
        return this.loadHomeMenuOptions()
      }).catch(() => {
        this.matchOk = false
        this.matchHint = '匹配子系统用户失败'
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
    },
    loadHomeMenuOptions() {
      const subSystemId = this.form.subSystemId
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
          return this.loadHomeMenuOptions()
        })
      }).catch(() => {
        this.dialogVisible = false
      })
    },
    submitForm() {
      if (this.isCreateMode && this.availableClientList.length === 0) {
        this.$modal.msgWarning('当前用户已关联全部外部系统')
        return
      }
      if (this.isBindMode && !this.matchOk) {
        this.$modal.msgWarning(this.matchHint || '请先匹配到子系统同名用户')
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
