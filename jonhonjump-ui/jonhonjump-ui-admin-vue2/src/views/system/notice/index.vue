<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="通知标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入通知标题" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="通知类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择通知类型" clearable filterable style="width: 180px">
          <el-option v-for="dict in notifyTemplateTypeDictDatas" :key="parseInt(dict.value)" :label="dict.label"
                     :value="parseInt(dict.value)"/>
        </el-select>
      </el-form-item>
      <el-form-item label="通知状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="通知状态" clearable style="width: 120px">
          <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发布部门" prop="deptName">
        <el-input v-model="queryParams.deptName" placeholder="请输入发布部门" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="创建者" prop="creator">
        <el-input v-model="queryParams.creator" placeholder="请输入创建者" clearable @keyup.enter.native="handleQuery"/>
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
                   v-hasPermi="['system:notice:create']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="isEmpty(checkedIds)"
          @click="handleDeleteBatch"
          v-hasPermi="['system:notice:delete']"
        >
          批量删除
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" :selectable="rowSelectable"/>
      <el-table-column label="序号" align="center" prop="id" width="80"/>
      <el-table-column label="通知标题" align="center" prop="title" :show-overflow-tooltip="true"/>
      <el-table-column label="通知类型" align="center" prop="type" width="100">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template v-slot="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布部门" align="center" prop="deptName" width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="创建者" align="center" width="100" :show-overflow-tooltip="true">
        <template v-slot="scope">
          {{ scope.row.publisherName || scope.row.creator || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="110">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handlePreview(scope.row)"
                     v-hasPermi="['system:notice:query']">预览
          </el-button>
          <el-button
            v-if="isDraft(scope.row) || isPublished(scope.row)"
            size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['system:notice:update']">修改
          </el-button>
          <el-button
            v-if="isDraft(scope.row)"
            size="mini" type="text" icon="el-icon-s-promotion" @click="handlePublish(scope.row)"
            v-hasPermi="['system:notice:update']">发布
          </el-button>
          <el-button
            v-if="isPublished(scope.row)"
            size="mini" type="text" icon="el-icon-refresh-left" @click="handleRevoke(scope.row)"
            v-hasPermi="['system:notice:update']">撤回
          </el-button>
          <el-button
            v-if="!isDeleted(scope.row)"
            size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:notice:delete']">删除
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
            <el-form-item label="通知标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入通知标题"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通知类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择">
                <el-option
                    v-for="dict in notifyTemplateTypeDictDatas"
                    :key="parseInt(dict.value)"
                    :label="dict.label"
                    :value="parseInt(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                    v-for="item in editableStatusOptions"
                    :key="item.value"
                    :label="item.value"
                >{{ item.label }}
                </el-radio>
              </el-radio-group>
              <div class="form-tip">新增默认草稿；选「已发布」保存后普通用户即可在工作台看到</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件">
              <file-upload v-model="form.attachmentUrls" :limit="5" :file-size="20" />
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
  addNotice,
  delNotice,
  getNotice,
  listNotice,
  updateNotice,
  delNoticeList,
  publishNotice,
  revokeNotice
} from '@/api/system/notice'
import Editor from '@/components/Editor'
import FileUpload from '@/components/FileUpload'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'

/** 0草稿 1已发布 2已删除 —— 与后端 NoticeStatusEnum 一致 */
const NOTICE_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  DELETED: 2
}

const NOTICE_STATUS_OPTIONS = [
  { value: NOTICE_STATUS.DRAFT, label: '草稿', tag: 'info' },
  { value: NOTICE_STATUS.PUBLISHED, label: '已发布', tag: 'success' },
  { value: NOTICE_STATUS.DELETED, label: '已删除', tag: 'danger' }
]

export default {
  name: 'SystemNotice',
  components: {
    Editor,
    FileUpload
  },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      noticeList: [],
      title: '',
      open: false,
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        title: undefined,
        type: undefined,
        status: undefined,
        deptName: undefined,
        creator: undefined,
        createTime: []
      },
      form: {},
      rules: {
        title: [
          { required: true, message: '通知标题不能为空', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '通知类型不能为空', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      },
      NOTICE_STATUS,
      // 本地兜底，避免字典未配置时「无数据」
      noticeStatusOptions: NOTICE_STATUS_OPTIONS,
      editableStatusOptions: NOTICE_STATUS_OPTIONS.filter(item => item.value !== NOTICE_STATUS.DELETED)
    }
  },
  computed: {
    notifyTemplateTypeDictDatas() {
      return getDictDatas(DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE)
    },
    DICT_TYPE() {
      return DICT_TYPE
    }
  },
  created() {
    this.getList()
  },
  methods: {
    statusLabel(status) {
      const hit = this.noticeStatusOptions.find(item => item.value === Number(status))
      return hit ? hit.label : '-'
    },
    statusTagType(status) {
      const hit = this.noticeStatusOptions.find(item => item.value === Number(status))
      return hit ? hit.tag : 'info'
    },
    isDraft(row) {
      return Number(row.status) === NOTICE_STATUS.DRAFT
    },
    isPublished(row) {
      return Number(row.status) === NOTICE_STATUS.PUBLISHED
    },
    isDeleted(row) {
      return Number(row.status) === NOTICE_STATUS.DELETED
    },
    rowSelectable(row) {
      return !this.isDeleted(row)
    },
    getList() {
      this.loading = true
      listNotice(this.queryParams).then(response => {
        this.noticeList = response.data.list
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
        type: undefined,
        content: undefined,
        status: NOTICE_STATUS.DRAFT,
        attachmentUrls: ''
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
      this.title = '添加通知'
    },
    handlePreview(row) {
      this.$router.push({
        name: 'MyNotifyMessageDetail',
        query: { noticeId: row.id }
      }).catch(() => {})
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id
      getNotice(id).then(response => {
        const data = response.data || {}
        this.form = {
          ...data,
          status: data.status === NOTICE_STATUS.DELETED ? NOTICE_STATUS.DRAFT : data.status,
          attachmentUrls: (data.attachments || []).map(item => item.url).join(',')
        }
        this.open = true
        this.title = '修改通知'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        const payload = {
          ...this.form,
          attachments: this.buildAttachments(this.form.attachmentUrls)
        }
        delete payload.attachmentUrls
        const req = this.form.id !== undefined ? updateNotice(payload) : addNotice(payload)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id !== undefined ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    buildAttachments(attachmentUrls) {
      if (!attachmentUrls) {
        return []
      }
      return attachmentUrls.split(',').filter(Boolean).map(url => ({
        name: this.getAttachmentName(url),
        url
      }))
    },
    getAttachmentName(url) {
      if (!url) {
        return '附件'
      }
      const cleanUrl = url.split('?')[0]
      return cleanUrl.slice(cleanUrl.lastIndexOf('/') + 1) || '附件'
    },
    handlePublish(row) {
      this.$modal.confirm('确认发布该通知？发布后普通用户可在工作台看到。').then(() => {
        return publishNotice(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('发布成功')
      }).catch(() => {})
    },
    handleRevoke(row) {
      this.$modal.confirm('确认撤回该通知？撤回后将变为草稿，工作台不再展示。').then(() => {
        return revokeNotice(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已撤回')
      }).catch(() => {})
    },
    handleDelete(row) {
      const ids = row.id
      this.$modal.confirm('确认删除该通知？删除后为「已删除」状态，仍可在筛选中查看。').then(() => {
        return delNotice(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    async handleDeleteBatch() {
      await this.$modal.confirm('是否确认批量删除选中的通知？')
      try {
        await delNoticeList(this.checkedIds)
        this.checkedIds = []
        await this.getList()
        this.$modal.msgSuccess('删除成功')
      } catch (e) { /* ignore */ }
    },
    handleRowCheckboxChange(records) {
      this.checkedIds = records.map(item => item.id)
    }
  }
}
</script>

<style scoped>
.form-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
</style>
