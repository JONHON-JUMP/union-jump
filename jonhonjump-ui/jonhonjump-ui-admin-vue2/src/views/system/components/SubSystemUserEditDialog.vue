<template>
  <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" append-to-body @close="handleClose">
    <el-form ref="form" :model="form" :rules="rules" label-width="110px">
      <el-form-item v-if="isCreateMode" label="挂接目标" prop="hangLeafKey">
        <el-cascader
          v-model="hangPath"
          :options="hangableCascaderOptions"
          :props="cascaderProps"
          filterable
          clearable
          placeholder="请选择子表叶子（一对一为系统本身一条，多车间选具体车间）"
          style="width: 100%"
          @change="handleHangLeafChange"
        />
        <div class="form-tip">人员只挂 sub_system_workshop 子表记录；多叶子共用父系统菜单，每个叶子一套角色</div>
      </el-form-item>
      <el-form-item v-else label="业务系统">
        <el-input :value="displayClientLabel" disabled />
      </el-form-item>
      <el-alert
        v-if="isCreateMode && hangableCascaderOptions.length === 0"
        title="暂无可挂接的外部系统/车间"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-alert
        v-if="isBindMode && matchHint"
        :title="matchHint"
        :type="matchOk ? 'success' : 'info'"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-divider content-position="left">业务系统用户信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :disabled="isBindMode" placeholder="与主用户登录名同名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="form.nickname" placeholder="用户姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="车间编号" prop="workshopId">
            <el-input
              v-model="form.workshopId"
              clearable
              maxlength="50"
              placeholder="可空；人员车间编号可直接填写"
              @change="handleWorkshopChange"
            />
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
        <el-col :span="24">
          <el-form-item label="关联车间编号" prop="usernameWithWorkshop">
            <el-radio-group v-model="form.usernameWithWorkshop" size="mini">
              <el-radio label="0" border>工号</el-radio>
              <el-radio label="1" border>车间编号_工号</el-radio>
            </el-radio-group>
            <div class="form-tip">选「车间编号_工号」且车间编号非空时，切换系统 Cookie 用 车间_工号；否则用工号</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">岗位</el-divider>
      <el-form-item label="岗位">
        <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width: 100%">
          <el-option
            v-for="item in postOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <div class="form-tip">角色请在挂接后通过「分配角色」设置（车间叶子下角色须带对应车间）</div>
      </el-form-item>
      <el-form-item label="启用状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="0">启用</el-radio>
          <el-radio label="1">停用</el-radio>
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
        :disabled="isCreateMode && !form.subSystemId"
        @click="submitForm"
      >确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  createSubSystemUser,
  getSubSystemPostSimpleList,
  getSubSystemTeamSimpleList,
  getSubSystemUser,
  getSubSystemUserByUsername,
  updateSubSystemUser
} from '@/api/system/subSystemUsers'
import { getHangableExternalTree } from '@/api/system/subSystemWorkshop'

export default {
  name: 'SubSystemUserEditDialog',
  props: {
    visible: { type: Boolean, default: false },
    recordId: { type: Number, default: undefined },
    mainUserId: { type: Number, default: undefined },
    portalUsername: { type: String, default: '' },
    portalNickname: { type: String, default: '' },
    mainUserDeptId: { type: Number, default: undefined },
    /** 已挂接叶子：[{ subSystemId, workshopId }] */
    excludeLeaves: { type: Array, default: () => [] },
    clientName: { type: String, default: '' }
  },
  data() {
    return {
      submitLoading: false,
      form: {},
      hangPath: [],
      hangableTree: [],
      postOptions: [],
      teamOptions: [],
      matchOk: false,
      matchHint: '',
      createNewAllowed: false,
      cascaderProps: {
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: true,
        checkStrictly: false
      },
      rules: {
        hangLeafKey: [{ required: true, message: '请选择要挂接的系统或车间', trigger: 'change' }],
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(value) { this.$emit('update:visible', value) }
    },
    isCreateMode() { return !this.recordId },
    isBindMode() { return this.isCreateMode && !!this.mainUserId },
    displayClientLabel() {
      if (this.form.workshopId) {
        return (this.clientName || '') + (this.clientName ? ' / ' : '') + this.form.workshopId
      }
      return this.clientName || '-'
    },
    hangableCascaderOptions() {
      const exclude = new Set(
        (this.excludeLeaves || []).map(item => this.leafKey(item.subSystemId, item.workshopId))
      )
      return (this.hangableTree || []).map(node => {
        if (node.nodeType === 'group') {
          const children = (node.children || [])
            .filter(c => !exclude.has(this.leafKey(c.subSystemId, c.workshopCode)))
            .map(c => ({
              value: this.leafKey(c.subSystemId, c.workshopCode),
              label: c.label,
              leafMeta: c
            }))
          if (!children.length) {
            return null
          }
          return {
            value: 'g-' + node.subSystemId,
            label: node.label,
            children
          }
        }
        const key = this.leafKey(node.subSystemId, node.workshopCode)
        if (exclude.has(key)) {
          return null
        }
        return {
          value: key,
          label: node.label,
          leafMeta: node
        }
      }).filter(Boolean)
    },
    dialogTitle() {
      if (this.isBindMode) {
        return '挂接业务系统 / 车间'
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
    handleWorkshopChange() {
      this.form.teamId = undefined
      this.loadTeamOptions(this.form.subSystemId)
    },
    leafKey(subSystemId, workshopId) {
      const w = workshopId == null || workshopId === '' ? '' : String(workshopId)
      return subSystemId + '|' + w
    },
    resetFormData() {
      this.form = {
        id: undefined,
        hangLeafKey: undefined,
        subSystemId: undefined,
        mainUserId: undefined,
        username: undefined,
        nickname: undefined,
        workshopId: undefined,
        teamId: undefined,
        homeMenuId: undefined,
        status: '0',
        // 新用户默认按 JUMP 规则「车间编号_工号」注册/种 Cookie；横线多账号仅存量兼容
        usernameWithWorkshop: '1',
        remark: undefined,
        roleIds: [],
        postIds: []
      }
      this.hangPath = []
      this.postOptions = []
      this.teamOptions = []
      this.matchOk = false
      this.matchHint = ''
      this.createNewAllowed = false
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    loadHangableTree() {
      return getHangableExternalTree().then(res => {
        this.hangableTree = res.data || []
      })
    },
    loadTeamOptions(subSystemId) {
      if (!subSystemId) {
        this.teamOptions = []
        return Promise.resolve()
      }
      return getSubSystemTeamSimpleList(subSystemId).then(res => {
        this.teamOptions = res.data || []
      }).catch(() => { this.teamOptions = [] })
    },
    loadSubOptions(subSystemId) {
      if (!subSystemId) {
        this.postOptions = []
        this.teamOptions = []
        return Promise.resolve()
      }
      return Promise.all([
        getSubSystemPostSimpleList(subSystemId).then(res => { this.postOptions = res.data || [] }),
        this.loadTeamOptions(subSystemId)
      ])
    },
    findLeafMeta(key) {
      for (const node of this.hangableTree || []) {
        if (node.nodeType === 'group') {
          const hit = (node.children || []).find(c => this.leafKey(c.subSystemId, c.workshopCode) === key)
          if (hit) {
            return hit
          }
        } else if (this.leafKey(node.subSystemId, node.workshopCode) === key) {
          return node
        }
      }
      return null
    },
    handleHangLeafChange(path) {
      const key = path && path.length ? path[path.length - 1] : null
      this.form.hangLeafKey = key
      const meta = key ? this.findLeafMeta(key) : null
      this.form.subSystemId = meta ? meta.subSystemId : undefined
      this.form.workshopId = meta && meta.workshopCode ? meta.workshopCode : undefined
      this.form.teamId = undefined
      this.form.postIds = []
      this.form.homeMenuId = undefined
      this.matchOk = false
      this.matchHint = ''
      this.createNewAllowed = false
      if (!this.form.subSystemId) {
        return
      }
      this.loadSubOptions(this.form.subSystemId).then(() => {
        if (this.isBindMode) {
          return this.matchRosterUser(this.form.subSystemId)
        }
        return null
      })
    },
    matchRosterUser(subSystemId) {
      const username = (this.portalUsername || '').trim()
      if (!subSystemId || !username) {
        this.matchOk = false
        this.createNewAllowed = false
        this.matchHint = '缺少主用户登录名'
        return Promise.resolve()
      }
      this.form.username = username
      return getSubSystemUserByUsername(subSystemId, username, this.form.workshopId).then(res => {
        const data = res.data
        if (data && data.id) {
          this.matchOk = true
          this.createNewAllowed = false
          this.matchHint = `已匹配业务用户「${data.username}」，确认后关联 JUMP 用户`
          const keptWorkshop = this.form.workshopId
          this.applyRosterToForm(data)
          this.form.workshopId = keptWorkshop || data.workshopId
          return this.loadTeamOptions(subSystemId)
        }
        this.matchOk = false
        this.createNewAllowed = true
        this.form.id = undefined
        this.matchHint = `将新建业务成员「${username}」并挂接所选叶子，挂接后请分配角色方可进入门户`
        return this.loadTeamOptions(subSystemId)
      }).catch(() => {
        this.matchOk = false
        this.createNewAllowed = true
        this.matchHint = '将新建业务成员并挂接'
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
      this.form.usernameWithWorkshop = data.usernameWithWorkshop === '1' ? '1' : '0'
      this.form.remark = data.remark
      this.form.roleIds = data.roleIds || []
      this.form.postIds = data.postIds || []
    },
    openBind() {
      this.resetFormData()
      this.form.mainUserId = this.mainUserId
      this.form.username = this.portalUsername
      this.form.nickname = this.portalNickname
      this.loadHangableTree().catch(() => { this.dialogVisible = false })
    },
    openCreate() {
      this.resetFormData()
      this.loadHangableTree().catch(() => { this.dialogVisible = false })
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
        })
      }).catch(() => { this.dialogVisible = false })
    },
    submitForm() {
      if (this.isCreateMode && !this.form.subSystemId) {
        this.$modal.msgWarning('请选择要挂接的系统或车间叶子')
        return
      }
      if (this.isBindMode && !this.matchOk && !this.createNewAllowed) {
        this.$modal.msgWarning(this.matchHint || '无法挂接')
        return
      }
      this.$refs.form.validate(valid => {
        // 校验不通过一律拦截（如 username 为空），不再穿透提交
        if (!valid) {
          return
        }
        if (this.isCreateMode && !this.form.hangLeafKey) {
          this.$modal.msgWarning('请选择要挂接的系统或车间叶子')
          return
        }
        this.submitLoading = true
        const payload = {
          ...this.form,
          mainUserId: this.isBindMode ? this.mainUserId : this.form.mainUserId,
          usernameWithWorkshop: this.form.usernameWithWorkshop === '1' ? '1' : '0'
        }
        delete payload.hangLeafKey
        const run = this.form.id
          ? updateSubSystemUser({
            ...payload,
            mainUserId: this.isBindMode ? this.mainUserId : payload.mainUserId
          })
          : createSubSystemUser(payload)
        run.then(() => {
          this.$modal.msgSuccess(this.isBindMode ? '挂接成功' : (this.form.id ? '修改成功' : '新增成功'))
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
