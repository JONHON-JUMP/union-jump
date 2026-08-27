<template>
  <div class="app-container">
    <!-- 返回变更通知管理页面的快捷入口 -->
    <el-row class="mb8">
      <el-button type="primary" plain icon="el-icon-back" size="mini" @click="handleBack">返回变更通知管理</el-button>
    </el-row>

    <!-- 当前通知对应的操作处理链路 -->
    <el-table v-loading="loading" :data="operationLogList">
      <el-table-column label="操作时间" prop="operationTime" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.operationTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作类型" prop="operationTypeName" />
      <el-table-column label="MPM通知ID" prop="notifyId" />
      <el-table-column label="车间" prop="workshopCode"/>
      <el-table-column label="操作人" prop="operator" />
      <el-table-column label="操作结果" prop="operationResultName"/>
      <el-table-column label="错误信息" prop="errorMsg" show-overflow-tooltip />
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { getRecipeChangeOperationLogPage } from '@/api/rm/recipeChangeLog'

export default {
  name: 'RecipeChangeOperationLog',
  data() {
    return {
      // 列表加载状态
      loading: true,
      // 当前通知主键，由变更通知管理页面传入
      noticeId: undefined,
      // 操作日志总记录数
      total: 0,
      // 操作日志列表数据
      operationLogList: [],
      // 分页参数
      queryParams: {
        pageNo: 1,
        pageSize: 10
      }
    }
  },
  created() {
    // 从路由查询参数取得需要查看日志的通知主键
    this.noticeId = this.$route.query.noticeId
    this.getList()
  },
  methods: {
    /** 查询当前通知的操作日志 */
    getList() {
      if (!this.noticeId) {
        // 未携带通知主键时不能确定日志归属，提示后不发起无效请求
        this.loading = false
        this.$modal.msgError('缺少通知主键，无法查询操作日志')
        return
      }
      this.loading = true
      getRecipeChangeOperationLogPage({
        noticeId: this.noticeId,
        pageNo: this.queryParams.pageNo,
        pageSize: this.queryParams.pageSize
      }).then(response => {
        // 将后端通用分页结构写入表格和分页组件
        this.operationLogList = response.data.list
        this.total = response.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    /** 返回变更通知管理页面 */
    handleBack() {
      // this.$router.push({ path: '/rm/recipeChange/index' })
      this.$router.push({ name: 'RecipeChangeNotice' })
    }
  }
}
</script>
