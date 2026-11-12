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
          style="width: 100%"
          @change="handleHangLeafChange"
        />
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
            <el-select
              v-model="form.username"
              filterable
              allow-create
              default-first-option
              :disabled="isBindMode && !form.workshopId"
              placeholder="请选择业务用户名"
              style="width: 100%"
              @change="onBizUsernameChange"
            >
              <el-option
                v-for="item in bizUsernameOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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
              placeholder=""
              @change="handleWorkshopChange"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">可同步到业务系统</el-divider>
      <el-row :gutter="16">
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
        <el-col :span="12">
          <el-form-item label="域账号">
            <el-input :value="form.domainNo || ''" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="刷卡卡号">
            <el-input v-model="form.cardNo" maxlength="64" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="ERP号">
            <el-input v-model="form.erpNo" maxlength="64" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="同步业务系统">
        <el-checkbox v-model="form.syncAccount">是否更新数据至业务系统</el-checkbox>
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
  updateSubSystemUser,
  syncSubSystemUserAccount
} from '@/api/system/subSystemUsers'
import { registerSubSystemEmployee } from '@/api/system/subSystemEmployee'
import { getHangableExternalTree } from '@/api/system/subSystemWorkshop'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

const DEFAULT_USERNAME_PATTERNS = [
  { label: 'JUMP用户名', value: '{username}', sort: 1 },
  { label: '车间编号_工号', value: '{workshop}_{username}', sort: 2 },
  { label: '工号车间编号', value: '{username}{workshop}', sort: 3 }
]

export default {
  name: 'SubSystemUserEditDialog',
  props: {
    visible: { type: Boolean, default: false },
    recordId: { type: Number, default: undefined },
    mainUserId: { type: Number, default: undefined },
    portalUsername: { type: String, default: '' },
    portalNickname: { type: String, default: '' },
    portalDomainNo: { type: String, default: '' },
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
      pickedPattern: '{username}',
      cascaderProps: {
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: true,
        checkStrictly: false
      },
      rules: {
        hangLeafKey: [{ required: true, message: '请选择要挂接的系统或车间', trigger: 'change' }],
        username: [{ required: true, message: '用户名不能为空', trigger: 'change' }]
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
    bizUsernameOptions() {
      const base = (this.portalUsername || '').trim()
      const workshop = (this.form.workshopId || '').trim()
      const options = []
      const seen = {}
      this.usernamePatterns.forEach(item => {
        const text = this.renderUsernamePattern(item.value, base, workshop)
        if (!text || seen[text]) {
          return
        }
        seen[text] = true
        options.push({
          value: text,
          pattern: item.value,
          label: text
        })
      })
      const current = (this.form.username || '').trim()
      if (current && !seen[current]) {
        options.push({ value: current, pattern: '', label: current })
      }
      return options
    },
    usernamePatterns() {
      const rows = getDictDatas(DICT_TYPE.SUB_SYSTEM_BIZ_USERNAME_PATTERN) || []
      const enabled = rows.filter(item => item && (item.status === undefined || item.status === 0 || item.status === '0'))
      const source = enabled.length ? enabled : DEFAULT_USERNAME_PATTERNS
      return source.slice().sort((a, b) => (a.sort || 0) - (b.sort || 0))
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
      this.applyPickedUsername()
      this.loadTeamOptions(this.form.subSystemId)
      if (this.isBindMode && this.form.subSystemId) {
        this.matchRosterUser(this.form.subSystemId)
      }
    },
    renderUsernamePattern(pattern, username, workshop) {
      if (!pattern || !username) {
        return ''
      }
      if (pattern.indexOf('{workshop}') >= 0 && !workshop) {
        return ''
      }
      return pattern.replace(/\{username\}/g, username).replace(/\{workshop\}/g, workshop)
    },
    applyPickedUsername() {
      const base = (this.portalUsername || '').trim()
      const workshop = (this.form.workshopId || '').trim()
      if (this.pickedPattern) {
        const text = this.renderUsernamePattern(this.pickedPattern, base, workshop)
        if (text) {
          this.form.username = text
          return
        }
      }
      if (!workshop && base) {
        this.form.username = base
        this.pickedPattern = '{username}'
      }
    },
    onBizUsernameChange(value) {
      const hit = this.bizUsernameOptions.find(item => item.value === value)
      this.pickedPattern = hit && hit.pattern ? hit.pattern : ''
      if (this.isBindMode && this.form.subSystemId) {
        this.matchRosterUser(this.form.subSystemId)
      }
    },
    syncPickedPattern() {
      const base = (this.portalUsername || '').trim()
      const workshop = (this.form.workshopId || '').trim()
      const current = (this.form.username || '').trim()
      const hit = this.usernamePatterns.find(item => this.renderUsernamePattern(item.value, base, workshop) === current)
      this.pickedPattern = hit ? hit.value : ''
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
        // 用户名即业务系统真实账号，不再按「关联车间编号」拼接
        usernameWithWorkshop: '0',
        remark: undefined,
        cardNo: undefined,
        erpNo: undefined,
        domainNo: undefined,
        syncAccount: false,
        roleIds: [],
        postIds: []
      }
      this.hangPath = []
      this.postOptions = []
      this.teamOptions = []
      this.matchOk = false
      this.matchHint = ''
      this.createNewAllowed = false
      this.pickedPattern = '{username}'
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
        this.pickedPattern = '{username}'
        this.applyPickedUsername()
        return
      }
      this.applyPickedUsername()
      this.loadSubOptions(this.form.subSystemId).then(() => {
        if (this.isBindMode) {
          return this.matchRosterUser(this.form.subSystemId)
        }
        return null
      })
    },
    matchRosterUser(subSystemId) {
      const username = (this.form.username || this.portalUsername || '').trim()
      if (!subSystemId || !username) {
        this.matchOk = false
        this.createNewAllowed = false
        this.matchHint = '请填写业务用户名'
        return Promise.resolve()
      }
      this.form.username = username
      const workshop = (this.form.workshopId || '').trim()
      const bare = (this.portalUsername || '').trim()
      const prefixed = workshop && username === bare ? (workshop + '_' + username) : ''
      const lookup = (name) => getSubSystemUserByUsername(subSystemId, name, this.form.workshopId)
      return lookup(username).then(res => {
        const data = res.data
        if (data && data.id) {
          return data
        }
        if (!prefixed) {
          return null
        }
        return lookup(prefixed).then(second => (second.data && second.data.id) ? second.data : null)
      }).then(data => {
        if (data && data.id) {
          this.matchOk = true
          this.createNewAllowed = false
          const keptWorkshop = this.form.workshopId
          this.applyRosterToForm(data)
          this.form.workshopId = keptWorkshop || data.workshopId
          this.matchHint = `已匹配业务用户「${this.form.username}」，确认后关联 JUMP 用户`
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
      const workshop = (data.workshopId || '').trim()
      const name = (data.username || '').trim()
      // 旧数据只存工号、并用「关联车间编号」拼 车间_工号。打开时展开成完整用户名，保存后 Cookie 直接用它
      this.form.username = (data.usernameWithWorkshop === '1' && workshop && name && name.indexOf(workshop + '_') !== 0)
        ? (workshop + '_' + name)
        : name
      this.form.nickname = data.nickname
      this.form.workshopId = data.workshopId
      this.form.teamId = data.teamId
      this.form.homeMenuId = data.homeMenuId
      this.form.status = data.status || '0'
      this.form.usernameWithWorkshop = '0'
      this.form.cardNo = data.cardNo
      this.form.erpNo = data.erpNo
      this.form.domainNo = data.domainNo || this.portalDomainNo
      this.form.remark = data.remark
      this.form.roleIds = data.roleIds || []
      this.form.postIds = data.postIds || []
      this.syncPickedPattern()
    },
    openBind() {
      this.resetFormData()
      this.form.mainUserId = this.mainUserId
      this.form.username = this.portalUsername
      this.form.nickname = this.portalNickname
      this.form.domainNo = this.portalDomainNo
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
        const syncAccount = !!this.form.syncAccount
        const hasAccount = [this.form.domainNo, this.form.cardNo, this.form.erpNo, this.form.teamId]
          .some(value => value != null && String(value).trim() !== '')
        const payload = {
          ...this.form,
          mainUserId: this.isBindMode ? this.mainUserId : this.form.mainUserId,
          usernameWithWorkshop: '0'
        }
        delete payload.hangLeafKey
        delete payload.domainNo
        delete payload.syncAccount
        delete payload.postIds
        const run = this.form.id
          ? updateSubSystemUser({
            ...payload,
            mainUserId: this.isBindMode ? this.mainUserId : payload.mainUserId
          })
          : createSubSystemUser(payload)
        run.then(res => {
          const savedId = this.form.id || res.data
          if (!syncAccount) {
            this.$modal.msgSuccess(this.isBindMode ? '挂接成功' : (this.form.id ? '修改成功' : '新增成功'))
            this.dialogVisible = false
            this.$emit('success')
            return
          }
          const done = (syncRes) => {
            const message = (syncRes.data && syncRes.data.message) || '已同步到业务系统'
            this.$modal.msgSuccess(message)
            this.dialogVisible = false
            this.$emit('success')
          }
          const keep = (message) => {
            this.form.id = savedId
            this.$modal.msgError(message)
            this.$emit('success')
          }
          // 先按叶子「新增人员」绑定。已注册的会跳过。有域账号、刷卡卡号、ERP 号或班组时，再按修改人员更新
          return registerSubSystemEmployee({
            workshopCode: payload.workshopId,
            ids: [savedId]
          }).then(regRes => {
            const results = regRes.data || []
            const fail = results.filter(item => !item.success)
            if (!results.length || fail.length) {
              keep('已保存在 JUMP。新增人员失败：' + ((fail[0] && fail[0].message) || '接口调用失败'))
              return null
            }
            if (!hasAccount) {
              return { data: { message: (results[0] && results[0].message) || '已同步到业务系统' } }
            }
            return syncSubSystemUserAccount({ id: savedId }).catch(() => {
              keep('人员已在业务系统。域账号、刷卡卡号、ERP号、班组同步失败')
              return null
            })
          }).catch(() => {
            keep('已保存在 JUMP。新增人员失败')
            return null
          }).then(syncRes => {
            if (!syncRes) {
              return
            }
            done(syncRes)
          })
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
