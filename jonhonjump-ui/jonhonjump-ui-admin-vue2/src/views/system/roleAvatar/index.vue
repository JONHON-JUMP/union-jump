<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="角色标识" prop="roleCode">
        <el-input v-model="queryParams.roleCode" placeholder="如 super_admin" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)" />
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
                   v-hasPermi="['system:role-avatar:create']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="!checkedIds.length"
                   @click="handleDeleteBatch" v-hasPermi="['system:role-avatar:delete']">批量删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="头像" width="90" align="center">
        <template v-slot="scope">
          <img :src="resolveAvatarUrl(scope.row.avatarUrl)" class="avatar-thumb" alt="">
        </template>
      </el-table-column>
      <el-table-column label="角色名称" prop="roleName" min-width="120" />
      <el-table-column label="角色标识" prop="roleCode" min-width="140" />
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['system:role-avatar:update']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['system:role-avatar:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="请选择角色" filterable :disabled="!!form.id" style="width: 100%;">
            <el-option v-for="role in roleOptions" :key="role.code"
                       :label="role.name + ' (' + role.code + ')'" :value="role.code"
                       :disabled="isRoleConfigured(role.code)" />
          </el-select>
          <div class="form-tip">每个角色只能配置一个系统头像；排序越小，多角色时默认优先级越高</div>
        </el-form-item>
        <el-form-item label="头像" prop="avatarUrl">
          <div v-if="legacyAvatarUrl" class="legacy-avatar-tip">
            当前为历史远程地址，请重新从下方静态库中选择并保存。
            <img :src="resolveAvatarUrl(legacyAvatarUrl)" class="legacy-avatar-preview" alt="">
          </div>
          <div class="static-avatar-grid">
            <div
              v-for="item in staticAvatarOptions"
              :key="item.value"
              class="static-avatar-item"
              :class="{ active: form.avatarUrl === item.value }"
              @click="selectStaticAvatar(item)"
            >
              <img :src="item.url" :alt="item.name">
              <div class="static-avatar-name">{{ item.name }}</div>
              <i v-if="form.avatarUrl === item.value" class="el-icon-check static-avatar-check"></i>
            </div>
          </div>
          <div class="form-tip">仅可选择 <code>src/assets/images/avatar</code> 目录下的预置图片；新增图片后需重新打包前端</div>
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createRoleAvatar, deleteRoleAvatar, deleteRoleAvatarList,
  getRoleAvatar, getRoleAvatarPage, updateRoleAvatar
} from '@/api/system/roleAvatar'
import { listSimpleRoles } from '@/api/system/role'
import { loadRoleAvatarConfig, resolveUserAvatar } from '@/utils/defaultAvatar'
import { getStaticAvatarOptions, isStaticAvatarValue, resolveAvatarUrl } from '@/utils/staticAvatar'
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

export default {
  name: 'SystemRoleAvatar',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      checkedIds: [],
      configuredRoleCodes: [],
      roleOptions: [],
      open: false,
      title: '',
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        roleCode: undefined,
        status: undefined
      },
      form: {},
      staticAvatarOptions: getStaticAvatarOptions(),
      legacyAvatarUrl: '',
      rules: {
        roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
        avatarUrl: [
          { required: true, message: '请选择头像', trigger: 'change' },
          {
            validator: (rule, value, callback) => {
              if (isStaticAvatarValue(value)) {
                callback()
                return
              }
              callback(new Error('请从下方静态头像库中点选一张图片'))
            },
            trigger: 'change'
          }
        ],
        sort: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
      },
      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS)
    }
  },
  created() {
    this.getList()
    this.loadRoleOptions()
  },
  methods: {
    resolveAvatarUrl,
    getList() {
      this.loading = true
      getRoleAvatarPage(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
        this.configuredRoleCodes = this.list.map(item => item.roleCode)
        this.loading = false
      })
    },
    loadRoleOptions() {
      listSimpleRoles().then(response => {
        this.roleOptions = response.data || []
      })
    },
    isRoleConfigured(roleCode) {
      if (this.form.id && this.form.roleCode === roleCode) {
        return false
      }
      return this.configuredRoleCodes.includes(roleCode)
    },
    selectStaticAvatar(item) {
      this.form.avatarUrl = item.value
      this.legacyAvatarUrl = ''
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.validateField('avatarUrl')
        }
      })
    },
    syncLegacyAvatarHint() {
      const url = this.form.avatarUrl
      this.legacyAvatarUrl = url && !isStaticAvatarValue(url) ? url : ''
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        roleCode: undefined,
        avatarUrl: undefined,
        sort: 0,
        status: CommonStatusEnum.ENABLE,
        remark: undefined
      }
      this.legacyAvatarUrl = ''
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.checkedIds = selection.map(item => item.id)
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增角色头像'
    },
    handleUpdate(row) {
      this.reset()
      getRoleAvatar(row.id).then(response => {
        this.form = response.data
        this.syncLegacyAvatarHint()
        if (this.legacyAvatarUrl) {
          this.form.avatarUrl = undefined
        }
        this.open = true
        this.title = '修改角色头像'
      })
    },
    buildSubmitPayload() {
      const payload = { ...this.form }
      const url = payload.avatarUrl
      if (isStaticAvatarValue(url)) {
        return payload
      }
      const matched = this.staticAvatarOptions.find(item => item.url === url || item.value === url)
      if (matched) {
        payload.avatarUrl = matched.value
      }
      return payload
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        const payload = this.buildSubmitPayload()
        const request = payload.id ? updateRoleAvatar(payload) : createRoleAvatar(payload)
        request.then(() => {
          this.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.refreshAvatarCache()
        })
      })
    },
    refreshAvatarCache() {
      return loadRoleAvatarConfig().then(() => {
        this.$store.commit('SET_AVATAR', resolveUserAvatar(this.$store.state.user.rawAvatar, this.$store.getters.roles))
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该角色头像配置？').then(() => {
        return deleteRoleAvatar(row.id)
      }).then(() => {
        this.getList()
        this.refreshAvatarCache()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的角色头像配置？').then(() => {
        return deleteRoleAvatarList(this.checkedIds)
      }).then(() => {
        this.getList()
        this.refreshAvatarCache()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.avatar-thumb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}
.static-avatar-grid {
  display: flex;
  flex-wrap: wrap;
  margin: -6px;
}
.static-avatar-grid > * {
  margin: 6px;
}
.static-avatar-item {
  position: relative;
  width: 96px;
  padding: 8px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.static-avatar-item:hover {
  border-color: #409eff;
}
.static-avatar-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff;
}
.static-avatar-item img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}
.static-avatar-name {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
.static-avatar-check {
  position: absolute;
  top: 4px;
  right: 4px;
  color: #409eff;
  font-size: 16px;
  font-weight: bold;
}
.legacy-avatar-tip {
  margin-bottom: 10px;
  padding: 8px 10px;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 12px;
  border-radius: 4px;
  line-height: 1.6;
}
.legacy-avatar-preview {
  display: block;
  width: 48px;
  height: 48px;
  margin-top: 6px;
  border-radius: 50%;
  object-fit: cover;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 8px;
}
.form-tip code {
  color: #606266;
  background: #f4f4f5;
  padding: 0 4px;
  border-radius: 3px;
}
</style>
