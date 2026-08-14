<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择分类" clearable filterable style="width: 180px">
          <el-option
            v-for="dict in faqCategoryDictDatas"
            :key="dict.value"
            :label="dict.label"
            :value="toOptionValue(dict.value)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="item in faqStatusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发布部门" prop="deptName">
        <el-input v-model="queryParams.deptName" placeholder="请输入发布部门" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss"
                        type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"
                        :default-time="['00:00:00', '23:59:59']"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['system:faq:create']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="isEmpty(checkedIds)"
                   @click="handleDeleteBatch" v-hasPermi="['system:faq:delete']">批量删除
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="faqList" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" :selectable="rowSelectable"/>
      <el-table-column label="序号" align="center" prop="id" width="80"/>
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true"/>
      <el-table-column label="分类" align="center" prop="category" width="120">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.SYSTEM_FAQ_CATEGORY" :value="scope.row.category"/>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80"/>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template v-slot="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布人" align="center" prop="publisherName" width="120" :show-overflow-tooltip="true"/>
      <el-table-column label="发布部门" align="center" prop="deptName" width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handlePreview(scope.row)"
                     v-hasPermi="['system:faq:query']">预览
          </el-button>
          <el-button
            v-if="isDraft(scope.row) || isPublished(scope.row)"
            size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['system:faq:update']">修改
          </el-button>
          <el-button
            v-if="isDraft(scope.row)"
            size="mini" type="text" icon="el-icon-s-promotion" @click="handlePublish(scope.row)"
            v-hasPermi="['system:faq:update']">发布
          </el-button>
          <el-button
            v-if="isPublished(scope.row)"
            size="mini" type="text" icon="el-icon-refresh-left" @click="handleRevoke(scope.row)"
            v-hasPermi="['system:faq:update']">撤回
          </el-button>
          <el-button
            v-if="!isDeleted(scope.row)"
            size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:faq:delete']">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入标题"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择">
                <el-option
                  v-for="dict in faqCategoryDictDatas"
                  :key="dict.value"
                  :label="dict.label"
                  :value="toOptionValue(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="item in editableStatusOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
        </el-row>
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
  addFaq,
  delFaq,
  delFaqList,
  getFaq,
  listFaq,
  updateFaq,
  publishFaq,
  revokeFaq
} from '@/api/system/faq'
import Editor from '@/components/Editor'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

const FAQ_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  DELETED: 2
}

const FAQ_STATUS_OPTIONS = [
  { value: FAQ_STATUS.DRAFT, label: '草稿', tag: 'info' },
  { value: FAQ_STATUS.PUBLISHED, label: '已发布', tag: 'success' },
  { value: FAQ_STATUS.DELETED, label: '已删除', tag: 'danger' }
]

export default {
  name: 'SystemFaq',
  components: { Editor },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      faqList: [],
      title: '',
      open: false,
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        title: undefined,
        category: undefined,
        status: undefined,
        deptName: undefined,
        createTime: []
      },
      form: {},
      rules: {
        title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
        category: [{ required: true, message: '分类不能为空', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      faqStatusOptions: FAQ_STATUS_OPTIONS,
      editableStatusOptions: FAQ_STATUS_OPTIONS.filter(item => item.value !== FAQ_STATUS.DELETED)
    }
  },
  computed: {
    // 字典懒加载：必须用 computed，避免 data() 固化空数组导致「无数据」
    faqCategoryDictDatas() {
      return getDictDatas(DICT_TYPE.SYSTEM_FAQ_CATEGORY)
    },
    DICT_TYPE() {
      return DICT_TYPE
    }
  },
  created() {
    this.getList()
  },
  methods: {
    toOptionValue(value) {
      const num = Number(value)
      return Number.isNaN(num) ? value : num
    },
    statusLabel(status) {
      const hit = this.faqStatusOptions.find(item => item.value === Number(status))
      return hit ? hit.label : '-'
    },
    statusTagType(status) {
      const hit = this.faqStatusOptions.find(item => item.value === Number(status))
      return hit ? hit.tag : 'info'
    },
    isDraft(row) {
      return Number(row.status) === FAQ_STATUS.DRAFT
    },
    isPublished(row) {
      return Number(row.status) === FAQ_STATUS.PUBLISHED
    },
    isDeleted(row) {
      return Number(row.status) === FAQ_STATUS.DELETED
    },
    rowSelectable(row) {
      return !this.isDeleted(row)
    },
    getList() {
      this.loading = true
      listFaq(this.queryParams).then(response => {
        this.faqList = response.data.list
        this.total = response.data.total
        this.loading = false
      }).catch(() => {
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
        title: undefined,
        category: undefined,
        content: undefined,
        sort: 0,
        status: FAQ_STATUS.DRAFT
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
      this.title = '添加常见 QA'
    },
    handlePreview(row) {
      this.$router.push({ name: 'MyFaqDetail', query: { faqId: row.id } }).catch(() => {})
    },
    handleUpdate(row) {
      this.reset()
      getFaq(row.id).then(response => {
        const data = response.data || {}
        this.form = {
          ...data,
          status: Number(data.status) === FAQ_STATUS.DELETED ? FAQ_STATUS.DRAFT : data.status
        }
        this.open = true
        this.title = '修改常见 QA'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.id !== undefined ? updateFaq(this.form) : addFaq(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.id !== undefined ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handlePublish(row) {
      this.$modal.confirm('确认发布该常见 QA？发布后普通用户可在工作台看到。').then(() => {
        return publishFaq(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('发布成功')
      }).catch(() => {})
    },
    handleRevoke(row) {
      this.$modal.confirm('确认撤回？撤回后将变为草稿，工作台不再展示。').then(() => {
        return revokeFaq(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已撤回')
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除？删除后为「已删除」状态，仍可在筛选中查看。').then(() => {
        return delFaq(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认删除选中的常见 QA?').then(() => {
        return delFaqList(this.checkedIds)
      }).then(() => {
        this.checkedIds = []
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleRowCheckboxChange(selection) {
      this.checkedIds = selection.map(item => item.id)
    }
  }
}
</script>
