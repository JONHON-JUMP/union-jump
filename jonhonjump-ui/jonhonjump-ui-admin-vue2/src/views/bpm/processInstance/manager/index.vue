<template>
  <div class="app-container">
    <doc-alert title="工作流手册" url="https://doc.iocoder.cn/bpm/" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="发起人" prop="startUserId">
        <el-select v-model="queryParams.startUserId" placeholder="请选择发起人" clearable filterable style="width: 240px">
          <el-option v-for="user in userList" :key="user.id" :label="user.nickname" :value="user.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="流程名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入流程名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="流程分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择流程分类" clearable style="width: 240px">
          <el-option v-for="category in categoryList" :key="category.code" :label="category.name" :value="category.code"/>
        </el-select>
      </el-form-item>
      <el-form-item label="流程状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择流程状态" clearable>
          <el-option v-for="dict in getDictDatas(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS)"
                     :key="dict.value" :label="dict.label" :value="parseInt(dict.value)"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发起时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss" type="daterange"
                        range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :default-time="['00:00:00', '23:59:59']" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="流程名称" align="center" prop="name" min-width="200" fixed />
      <el-table-column label="流程分类" align="center" prop="categoryName" min-width="100" />
      <el-table-column label="流程发起人" align="center" width="120">
        <template v-slot="scope">
          <span>{{ scope.row.startUser && scope.row.startUser.nickname }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发起部门" align="center" width="120">
        <template v-slot="scope">
          <span>{{ scope.row.startUser && scope.row.startUser.deptName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="流程状态" align="center" prop="status" width="120">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="发起时间" align="center" prop="startTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" prop="durationInMillis" width="169">
        <template v-slot="scope">
          <span>{{ scope.row.durationInMillis > 0 ? getDateStar(scope.row.durationInMillis) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前审批任务" align="center" prop="tasks" min-width="120">
        <template v-slot="scope">
          <el-button v-for="task in scope.row.tasks" :key="task.id" type="text">
            <span>{{ task.name }}</span>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="流程编号" align="center" prop="id" min-width="320" show-overflow-tooltip />
      <el-table-column label="操作" align="center" fixed="right" width="140">
        <template v-slot="scope">
          <el-button size="mini" type="text" @click="handleDetail(scope.row)"
                     v-hasPermi="['bpm:process-instance:manager-query']">详情</el-button>
          <el-button size="mini" type="text" v-if="scope.row.status === 1" @click="handleCancel(scope.row)"
                     v-hasPermi="['bpm:process-instance:cancel-by-admin']">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>
  </div>
</template>

<script>
import { cancelProcessInstanceByAdmin, getProcessInstanceManagerPage } from '@/api/bpm/processInstance'
import { getCategorySimpleList } from '@/api/bpm/category'
import { listSimpleUsers } from '@/api/system/user'
import { getDate } from '@/utils/dateUtils'

export default {
  name: 'BpmProcessInstanceManager',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      categoryList: [],
      userList: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        startUserId: undefined,
        name: null,
        category: null,
        status: null,
        createTime: []
      }
    }
  },
  created() {
    this.getList()
    getCategorySimpleList().then(response => {
      this.categoryList = response.data || []
    })
    listSimpleUsers().then(response => {
      this.userList = response.data || []
    })
  },
  methods: {
    getList() {
      this.loading = true
      getProcessInstanceManagerPage(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
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
    getDateStar(ms) {
      return getDate(ms)
    },
    handleDetail(row) {
      this.$router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.id } })
    },
    handleCancel(row) {
      this.$prompt('请输入取消原因', '取消流程', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S/,
        inputErrorMessage: '取消原因不能为空'
      }).then(({ value }) => {
        return cancelProcessInstanceByAdmin(row.id, value)
      }).then(() => {
        this.$modal.msgSuccess('取消成功')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>
