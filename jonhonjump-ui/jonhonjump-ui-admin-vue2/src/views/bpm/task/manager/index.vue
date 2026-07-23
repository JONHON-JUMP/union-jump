<template>
  <div class="app-container">
    <doc-alert title="工作流手册" url="https://doc.iocoder.cn/bpm/" />

    <!-- 搜索工作栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任务名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable @keyup.enter.native="handleQuery"/>
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

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list">
      <el-table-column label="流程" align="center" prop="processInstance.name" width="180" />
      <el-table-column label="发起人" align="center" width="100">
        <template v-slot="scope">
          <span>{{ getStartUserNickname(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发起时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前任务" align="center" prop="name" width="180" />
      <el-table-column label="任务开始时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务结束时间" align="center" prop="endTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批人" align="center" width="100">
        <template v-slot="scope">
          <span>{{ scope.row.assigneeUser && scope.row.assigneeUser.nickname }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" align="center" prop="status" width="120">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.BPM_TASK_STATUS" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="审批建议" align="center" prop="reason" min-width="180" show-overflow-tooltip />
      <el-table-column label="耗时" align="center" prop="durationInMillis" width="160">
        <template v-slot="scope">
          <span>{{ getDateStar(scope.row.durationInMillis) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="流程编号" align="center" prop="processInstanceId" show-overflow-tooltip />
      <el-table-column label="任务编号" align="center" prop="id" show-overflow-tooltip />
      <el-table-column label="操作" align="center" fixed="right" width="80" class-name="small-padding fixed-width">
        <template v-slot="scope">
          <el-button size="mini" type="text" @click="handleAudit(scope.row)"
                     v-hasPermi="['bpm:task:manager-query']">历史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页组件 -->
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

  </div>
</template>

<script>
import { getTaskManagerPage } from '@/api/bpm/task'
import { getDate } from '@/utils/dateUtils'

export default {
  name: 'BpmManagerTask',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        name: null,
        createTime: []
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询列表 */
    getList() {
      this.loading = true
      getTaskManagerPage(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    getStartUserNickname(row) {
      const processInstance = row.processInstance
      if (!processInstance) {
        return ''
      }
      if (processInstance.startUser && processInstance.startUser.nickname) {
        return processInstance.startUser.nickname
      }
      return processInstance.startUserNickname || ''
    },
    getDateStar(ms) {
      return getDate(ms)
    },
    /** 查看流程历史 */
    handleAudit(row) {
      if (!row.processInstance || !row.processInstance.id) {
        return
      }
      this.$router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstance.id } })
    }
  }
}
</script>
