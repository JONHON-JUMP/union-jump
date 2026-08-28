<template>
  <div class="app-container">
    <!-- 变更通知筛选条件 -->
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="100px">
      <el-form-item label="MPM通知ID" prop="notifyId">
        <el-input v-model="queryParams.notifyId" placeholder="请输入通知ID" clearable style="width: 220px"
                  @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="车间" prop="workshopCode">
        <el-input v-model="queryParams.workshopCode" placeholder="请输入车间编码" clearable style="width: 180px"
                  @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss"
                        range-separator="-" start-placeholder="开始时间" end-placeholder="结束时间"
                        :default-time="['00:00:00', '23:59:59']" style="width: 360px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
      <el-form-item style="float: right">
        <el-button type="primary" icon="el-icon-refresh" :disabled="selectedNoticeIds.length === 0" @click="handleManualRetry">批量重发</el-button>
        <el-button type="success" icon="el-icon-finished" :disabled="selectedNoticeIds.length === 0" @click="handleBatchMarkComplete">批量标记完成</el-button>
        <el-button type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
      </el-form-item>
    </el-form>

    <!-- 变更通知列表 -->
    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" :selectable="isRetrySelectable" />
      <el-table-column type="index" label="序号" width="55" />
      <el-table-column label="MPM通知ID" prop="notifyId" />
      <el-table-column label="车间" prop="workshopCode" />
      <el-table-column label="当前状态" prop="statusName" />
      <el-table-column label="自动重试次数" >
        <template slot-scope="scope">
          {{ scope.row.retryCount }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-document" @click="handleContentQuery(scope.row)">内容查询</el-button>
          <el-button type="text" size="mini" icon="el-icon-tickets" @click="handleOperationLogQuery(scope.row)">日志查询</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <!-- 单独展示原始工艺变更 JSON 内容 -->
    <el-dialog title="工艺变更内容" :visible.sync="contentDialogVisible" width="75%" top="5vh" append-to-body>
      <el-form label-width="100px" size="small">
        <el-form-item label="MPM通知ID">{{ contentForm.notifyId }}</el-form-item>
        <el-form-item label="车间">{{ contentForm.workshopCode }}</el-form-item>
        <el-form-item label="变更内容">
          <pre class="change-content">{{ formattedChangeContent }}</pre>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 展示本次批量人工重发的实际投递结果，失败记录由后端排在最上方 -->
    <el-dialog :title="manualRetryResultDialogTitle" :visible.sync="manualRetryResultDialogVisible" width="75%" top="8vh" append-to-body>
      <el-row class="retry-result-summary" :gutter="20">
        <el-col :span="8">本次操作总数：{{ manualRetryResultTotal }}</el-col>
        <el-col :span="8">成功数量：{{ manualRetryResult.successCount }}</el-col>
        <el-col :span="8">失败数量：{{ manualRetryResult.failureCount }}</el-col>
      </el-row>
      <el-table :data="manualRetryResult.retryResults" border max-height="480" class="retry-result-table">
        <el-table-column type="index" label="序号" width="55" />
        <el-table-column label="MPM通知ID" prop="notifyId" />
        <el-table-column label="本次重发结果" width="140">
          <template slot-scope="scope">
            <el-tag :type="scope.row.success ? 'success' : 'danger'">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="360">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.errorMsg" placement="top-start" :disabled="!scope.row.errorMsg" effect="dark">
              <span class="retry-error-message">{{ scope.row.errorMsg || '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import {
  batchMarkCompleteRecipeChangeNotice,
  exportRecipeChangeNoticeExcel,
  getRecipeChangeNoticeContent,
  getRecipeChangeNoticePage,
  manualRetryRecipeChangeNotice
} from '@/api/rm/recipeChangeLog'

export default {
  name: 'RecipeChangeNotice',
  data() {
    return {
      // 列表加载状态
      loading: true,
      // 变更通知总记录数
      total: 0,
      // Excel 导出执行状态
      exportLoading: false,
      // 变更通知列表数据
      noticeList: [],
      // 当前选中的、允许人工重发的通知主键
      selectedNoticeIds: [],
      // 人工重发结果弹窗显示状态
      manualRetryResultDialogVisible: false,
      // 批量操作结果弹窗标题
      manualRetryResultDialogTitle: '',
      // 人工重发的汇总数量和逐条结果
      manualRetryResult: {
        successCount: 0,
        failureCount: 0,
        retryResults: []
      },
      // 内容查询弹窗显示状态
      contentDialogVisible: false,
      // 内容查询弹窗数据
      contentForm: {},
      // 页面状态筛选项，与后端已支持的状态保持一致
      statusOptions: [
        { label: '接收成功', value: 5 },
        { label: '已发送MQ', value: 10 },
        { label: '发送失败', value: 15 },
        { label: 'MES处理中', value: 18 },
        { label: '车间处理失败', value: 25 },
        { label: '待人工处理', value: 30 },
        { label: '已标记完成', value: 35 }
      ],
      // 分页和筛选参数
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        notifyId: undefined,
        workshopCode: undefined,
        status: undefined,
        createTime: []
      }
    }
  },
  computed: {
    /** 计算本次批量人工操作的通知总数 */
    manualRetryResultTotal() {
      return this.manualRetryResult.successCount + this.manualRetryResult.failureCount
    },
    /** 将 changeContent 格式化为可读 JSON 文本 */
    formattedChangeContent() {
      if (this.contentForm.changeContent === null || this.contentForm.changeContent === undefined) {
        return '暂无变更内容'
      }
      // 接口正常返回对象，直接按两个空格缩进美化 JSON
      if (typeof this.contentForm.changeContent !== 'string') {
        return JSON.stringify(this.contentForm.changeContent, null, 2)
      }
      try {
        // 兼容 JSONB 被序列化为字符串的情况，解析后再美化展示
        return JSON.stringify(JSON.parse(this.contentForm.changeContent), null, 2)
      } catch (error) {
        // 非 JSON 文本时保留原始内容，避免内容无法查看
        return this.contentForm.changeContent
      }
    }
  },
  created() {
    // 页面加载后查询首屏数据
    this.getList()
  },
  methods: {
    /** 查询变更通知管理列表 */
    getList() {
      this.loading = true
      getRecipeChangeNoticePage(this.queryParams).then(response => {
        // 将后端通用分页结构写入表格和分页组件
        this.noticeList = response.data.list
        this.total = response.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    /** 执行查询并回到第一页 */
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    /** 清空筛选条件后重新查询 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 按当前筛选条件导出全部工艺变更通知 Excel */
    handleExport() {
      this.$modal.confirm('确认导出当前查询条件下的全部工艺变更通知吗？').then(() => {
        // 创建独立导出参数并清除分页字段，确保后端按筛选条件导出全量数据
        const exportParams = { ...this.queryParams }
        exportParams.pageNo = undefined
        exportParams.pageSize = undefined
        this.exportLoading = true
        return exportRecipeChangeNoticeExcel(exportParams)
      }).then(response => {
        // 将后端返回的二进制 Excel 文件交由统一下载插件保存
        const exportTime = new Date()
        // 将月、日、时、分、秒补齐为两位数，生成便于排序的导出时间戳
        const padTimePart = (value) => String(value).padStart(2, '0')
        const exportTimestamp = `${exportTime.getFullYear()}${padTimePart(exportTime.getMonth() + 1)}${padTimePart(exportTime.getDate())}`
          + `${padTimePart(exportTime.getHours())}${padTimePart(exportTime.getMinutes())}${padTimePart(exportTime.getSeconds())}`
        // 下载插件以此处传入的文件名为准，因此在前端追加时间戳避免重复下载时覆盖文件
        this.$download.excel(response, `工艺变更通知数据_${exportTimestamp}.xlsx`)
      }).finally(() => {
        this.exportLoading = false
      })
    },
    /** 查询并展示指定通知的 changeContent */
    handleContentQuery(row) {
      getRecipeChangeNoticeContent(row.id).then(response => {
        // 后端单独返回 JSON 内容，避免列表接口传输大字段
        this.contentForm = response.data || {}
        this.contentDialogVisible = true
      })
    },
    /** 跳转到操作日志查看页面并传递通知主键 */
    handleOperationLogQuery(row) {
      this.$router.push({
        path: '/rm/recipeChange/operationLog',
        query: { noticeId: row.id }
      })
    },
    /** 仅允许选择发送失败、车间处理失败或待人工处理的通知 */
    isRetrySelectable(row) {
      return row.status === 15 || row.status === 25 || row.status === 30
    },
    /** 保存当前表格勾选的通知主键 */
    handleSelectionChange(selection) {
      this.selectedNoticeIds = selection.map(row => row.id)
    },
    /** 确认后对选中的失败通知执行人工重发 */
    handleManualRetry() {
      this.$modal.confirm('确认重发选中的工艺变更通知吗？').then(() => {
        return manualRetryRecipeChangeNotice(this.selectedNoticeIds)
      }).then(response => {
        this.showBatchOperationResult('人工重发', response.data)
        this.getList()
      })
    },
    /** 确认后将选中的异常通知标记完成，不再参与后续自动重试 */
    handleBatchMarkComplete() {
      this.$modal.confirm('确认将选中的工艺变更通知标记完成吗？标记后将不再自动重试').then(() => {
        return batchMarkCompleteRecipeChangeNotice(this.selectedNoticeIds)
      }).then(response => {
        this.showBatchOperationResult('批量标记完成', response.data)
        this.getList()
      })
    },
    /** 打开弹窗展示批量人工操作的实际处理结果 */
    showBatchOperationResult(operationName, operationResult) {
      this.manualRetryResultDialogTitle = `${operationName}结果`
      this.manualRetryResult = operationResult
      this.manualRetryResultDialogVisible = true
    }
  }
}
</script>

<style scoped>
.change-content {
  height: 480px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  color: #303133;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

.retry-result-summary {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 32px;
}

.retry-result-table {
  width: 100%;
}

.retry-error-message {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
