<template>
  <el-dialog title="通用菜单管理" :visible.sync="visible" width="860px" append-to-body @open="loadList">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px"
      title="通用菜单只需定义一次，勾选挂载的子系统后自动同步到各系统；各子系统的显示位置、角色授权由各系统单独调整。" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增通用菜单</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" size="small" border>
      <el-table-column label="菜单名称" prop="name" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" width="70">
        <template v-slot="scope">
          <span>{{ typeLabel(scope.row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="路由地址" prop="path" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="已挂载子系统" min-width="200">
        <template v-slot="scope">
          <template v-if="scope.row.subSystemNames && scope.row.subSystemNames.length">
            <el-tag v-for="(name, i) in scope.row.subSystemNames" :key="i" size="mini" style="margin: 0 4px 4px 0">
              {{ name }}
            </el-tag>
          </template>
          <span v-else style="color: #999">未挂载</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增 / 修改 -->
    <el-dialog :title="form.id ? '修改通用菜单' : '新增通用菜单'" :visible.sync="formOpen" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">目录</el-radio>
            <el-radio :label="2">菜单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 /common/help，外链则以 http(s):// 开头" />
        </el-form-item>
        <el-form-item label="挂载子系统" prop="subSystemIds">
          <el-select v-model="form.subSystemIds" multiple filterable placeholder="选择要挂载的子系统（可多选）" style="width: 100%">
            <el-option v-for="sys in subSystemOptions" :key="sys.id" :label="sys.name" :value="sys.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="formOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import {
  createCommonMenu,
  deleteCommonMenu,
  getCommonMenuList,
  getSubSystemClientSimpleList,
  updateCommonMenu
} from '@/api/system/subSystemMenu'

export default {
  name: 'CommonMenuDialog',
  data() {
    return {
      visible: false,
      loading: false,
      list: [],
      subSystemOptions: [],
      formOpen: false,
      form: {},
      /** 编辑时的原始挂载，用于清空挂载时的二次确认 */
      originalSubSystemIds: [],
      rules: {
        name: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '菜单类型不能为空', trigger: 'change' }]
      }
    }
  },
  methods: {
    open() {
      this.visible = true
    },
    typeLabel(type) {
      if (type === 1) return '目录'
      if (type === 2) return '菜单'
      if (type === 3) return '按钮'
      return String(type)
    },
    loadList() {
      this.loading = true
      // 模板列表与子系统选项分开加载：子系统接口异常不连累模板列表展示
      getCommonMenuList().then(res => {
        this.list = res.data || []
      }).finally(() => {
        this.loading = false
      })
      getSubSystemClientSimpleList().then(res => {
        this.subSystemOptions = (res.data || []).map(item => ({
          id: Number(item.id),
          name: item.name || item.systemName
        })).filter(item => item.id > 0)
      }).catch(() => {})
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        type: 2,
        path: undefined,
        sort: 0,
        status: 0,
        subSystemIds: []
      }
    },
    handleAdd() {
      this.reset()
      this.formOpen = true
    },
    handleUpdate(row) {
      this.reset()
      const subSystemIds = (row.subSystemIds || []).map(Number)
      this.form = {
        id: row.id,
        name: row.name,
        type: row.type,
        path: row.path,
        sort: row.sort,
        status: row.status != null ? row.status : 0,
        subSystemIds
      }
      this.originalSubSystemIds = subSystemIds
      this.formOpen = true
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        // 编辑时把挂载清空 = 删除所有子系统副本，需二次确认防误操作
        const willUnmountAll = this.form.id
          && this.originalSubSystemIds && this.originalSubSystemIds.length > 0
          && (!this.form.subSystemIds || this.form.subSystemIds.length === 0)
        const doSubmit = () => {
          const action = this.form.id ? updateCommonMenu(this.form) : createCommonMenu(this.form)
          action.then(() => {
            this.$modal.msgSuccess(this.form.id ? '修改成功，已同步到各挂载子系统' : '新增成功')
            this.formOpen = false
            this.loadList()
            const ids = [...(this.form.subSystemIds || []), ...(this.originalSubSystemIds || [])]
              .map(Number)
              .filter(id => Number.isFinite(id) && id > 0)
            this.$emit('changed', { subSystemIds: [...new Set(ids)] })
          })
        }
        if (willUnmountAll) {
          this.$modal.confirm('已取消全部子系统挂载，保存后各子系统的菜单副本将被删除（同时解除角色授权与快捷导航引用），确定继续吗？')
            .then(doSubmit)
            .catch(() => {})
          return
        }
        doSubmit()
      })
    },
    handleDelete(row) {
      this.$modal.confirm(`确定删除通用菜单「${row.name}」吗？将同时删除各子系统副本，并解除角色授权与快捷导航引用。`)
        .then(() => deleteCommonMenu(row.id))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.loadList()
          this.$emit('changed', { subSystemIds: (row.subSystemIds || []).map(Number) })
        })
        .catch(() => {})
    }
  }
}
</script>
