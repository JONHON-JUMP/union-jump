<template>
  <div class="app-container">
    <doc-alert title="工作流手册" url="https://doc.iocoder.cn/bpm/" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分类名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入分类名" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="分类标志" prop="code">
        <el-input v-model="queryParams.code" placeholder="请输入分类标志" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="分类状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择分类状态" clearable>
          <el-option v-for="dict in getDictDatas(DICT_TYPE.COMMON_STATUS)"
                     :key="dict.value" :label="dict.label" :value="parseInt(dict.value)"/>
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss" type="daterange"
                        range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :default-time="['00:00:00', '23:59:59']" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['bpm:category:create']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="分类编号" align="center" prop="id" />
      <el-table-column label="分类名" align="center" prop="name" />
      <el-table-column label="分类标志" align="center" prop="code" />
      <el-table-column label="分类描述" align="center" prop="description" show-overflow-tooltip />
      <el-table-column label="分类状态" align="center" prop="status">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="分类排序" align="center" prop="sort" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['bpm:category:update']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['bpm:category:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名" />
        </el-form-item>
        <el-form-item label="分类标志" prop="code">
          <el-input v-model="form.code" placeholder="请输入分类标志" />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入分类描述" />
        </el-form-item>
        <el-form-item label="分类状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in getDictDatas(DICT_TYPE.COMMON_STATUS)"
                      :key="dict.value" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类排序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
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
  createCategory,
  deleteCategory,
  getCategory,
  getCategoryPage,
  updateCategory
} from '@/api/bpm/category'
import { CommonStatusEnum } from '@/utils/constants'

export default {
  name: 'BpmCategory',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      title: '',
      open: false,
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        name: null,
        code: null,
        status: null,
        createTime: []
      },
      form: {},
      rules: {
        name: [{ required: true, message: '分类名不能为空', trigger: 'blur' }],
        code: [{ required: true, message: '分类标志不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '分类状态不能为空', trigger: 'change' }],
        sort: [{ required: true, message: '分类排序不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      getCategoryPage(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        code: undefined,
        description: undefined,
        status: CommonStatusEnum.ENABLE,
        sort: 0
      }
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
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增流程分类'
    },
    handleUpdate(row) {
      this.reset()
      getCategory(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改流程分类'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.id) {
          updateCategory(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
          return
        }
        createCategory(this.form).then(() => {
          this.$modal.msgSuccess('新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该流程分类？').then(() => {
        return deleteCategory(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
