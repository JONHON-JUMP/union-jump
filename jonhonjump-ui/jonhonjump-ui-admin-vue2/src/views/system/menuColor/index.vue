<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="颜色名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入颜色名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="MES 大类" prop="mesCategory">
        <el-input v-model="queryParams.mesCategory" placeholder="如 M02-生产执行" clearable @keyup.enter.native="handleQuery" />
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
                   v-hasPermi="['system:menu-style:create']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="!checkedIds.length"
                   @click="handleDeleteBatch" v-hasPermi="['system:menu-style:delete']">批量删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="预览" width="80" align="center">
        <template v-slot="scope">
          <span class="color-swatch" :style="previewStyle(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="颜色名称" prop="name" min-width="140" />
      <el-table-column label="主色" prop="color" width="100" />
      <el-table-column label="MES 大类" prop="mesCategory" min-width="120" />
      <el-table-column label="适用说明" prop="remark" min-width="220" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['system:menu-style:update']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['system:menu-style:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="颜色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：产线绿·生产执行" />
        </el-form-item>
        <el-form-item label="主色" prop="color">
          <el-color-picker v-model="form.color" color-format="hex" />
          <el-input v-model="form.color" placeholder="#087CE5" style="width: 160px; margin-left: 12px;" />
          <div class="form-tip">门户图标底色与图标色：主色底 + 白色图标</div>
        </el-form-item>
        <el-form-item label="MES 大类" prop="mesCategory">
          <el-input v-model="form.mesCategory" placeholder="如：M02-生产执行" />
        </el-form-item>
        <el-form-item label="适用说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3"
                    placeholder="说明该颜色适合哪些一级菜单，如：生产执行、报工、在制品查询等" />
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
        <el-form-item label="预览">
          <span class="color-swatch color-swatch--large" :style="previewStyle(form)" />
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
import { createMenuStyle, deleteMenuStyle, deleteMenuStyleList, getMenuStyle, getMenuStylePage, updateMenuStyle } from '@/api/system/menuStyle'
import { buildMenuStylePreview } from '@/utils/menuIconStyle'
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

export default {
  name: 'SystemMenuStyle',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      checkedIds: [],
      open: false,
      title: '',
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        name: undefined,
        mesCategory: undefined,
        status: undefined
      },
      form: {},
      rules: {
        name: [{ required: true, message: '颜色名称不能为空', trigger: 'blur' }],
        color: [{ required: true, message: '主色不能为空', trigger: 'blur' }],
        sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
      },
      DICT_TYPE,
      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    previewStyle(row) {
      if (!row || !row.color) return {}
      return buildMenuStylePreview(row)
    },
    getList() {
      this.loading = true
      getMenuStylePage(this.queryParams).then(res => {
        this.list = res.data.list || []
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
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.checkedIds = selection.map(item => item.id)
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        color: '#087CE5',
        mesCategory: undefined,
        remark: undefined,
        sort: 0,
        status: CommonStatusEnum.ENABLE
      }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增菜单颜色'
    },
    handleUpdate(row) {
      const id = row && row.id
      if (id === undefined || id === null) {
        this.$modal.msgError('缺少样式编号')
        return
      }
      getMenuStyle(id).then(res => {
        const data = res.data || {}
        this.form = {
          id: data.id,
          name: data.name,
          color: data.color,
          mesCategory: data.mesCategory,
          remark: data.remark,
          sort: Number(data.sort),
          status: Number(data.status)
        }
        this.open = true
        this.title = '修改菜单样式'
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate()
          }
        })
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = { ...this.form, shape: 'rounded' }
        const req = payload.id ? updateMenuStyle(payload) : createMenuStyle(payload)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除颜色「${row.name}」吗？`).then(() => deleteMenuStyle(row.id)).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('确认删除选中的菜单颜色吗？').then(() => deleteMenuStyleList(this.checkedIds)).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.color-swatch {
  display: inline-block;
  width: 28px;
  height: 28px;
  border-radius: 8px;
}
.color-swatch--large {
  width: 48px;
  height: 48px;
  border-radius: 12px;
}
.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
</style>
