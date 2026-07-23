<template>

  <div class="app-container">

    <!-- 搜索工作栏 -->

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">

      <el-form-item label="标题" prop="title">

        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery" />

      </el-form-item>

      <el-form-item label="发布人" prop="publisherName">

        <el-input v-model="queryParams.publisherName" placeholder="请输入发布人" clearable @keyup.enter.native="handleQuery" />

      </el-form-item>

      <el-form-item label="发布部门" prop="deptName">

        <el-input v-model="queryParams.deptName" placeholder="请输入发布部门" clearable @keyup.enter.native="handleQuery" />

      </el-form-item>

      <el-form-item label="通知类型" prop="type">

        <el-select v-model="queryParams.type" placeholder="请输入或选择通知类型" clearable filterable style="width: 200px">

          <el-option v-for="dict in this.getDictDatas(DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE)"

                     :key="dict.value" :label="dict.label" :value="dict.value"/>

        </el-select>

      </el-form-item>

      <el-form-item label="发送时间" prop="createTime">

        <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss" type="daterange"

                        range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :default-time="['00:00:00', '23:59:59']" />

      </el-form-item>

      <el-form-item>

        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>

        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>

      </el-form-item>

    </el-form>



    <el-row :gutter="10" class="mb8">

      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>

    </el-row>



    <!-- 列表 -->

    <el-table v-loading="loading" :data="list" class="notify-list-table" @row-click="handleRowClick">

      <el-table-column label="通知类型" align="center" prop="type" width="100">

        <template v-slot="scope">

          <dict-tag :type="DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE" :value="scope.row.type" />

        </template>

      </el-table-column>

      <el-table-column label="标题" align="left" prop="title" min-width="220" :show-overflow-tooltip="true">

        <template v-slot="scope">

          <span class="link-type">{{ scope.row.title || '-' }}</span>

        </template>

      </el-table-column>

      <el-table-column label="发布人" align="center" prop="publisherName" width="120" :show-overflow-tooltip="true">

        <template v-slot="scope">

          <span>{{ scope.row.publisherName || '-' }}</span>

        </template>

      </el-table-column>

      <el-table-column label="发布部门" align="center" prop="deptName" min-width="160" :show-overflow-tooltip="true">

        <template v-slot="scope">

          <span>{{ scope.row.deptName || '-' }}</span>

        </template>

      </el-table-column>

      <el-table-column label="发送时间" align="center" prop="createTime" width="180">

        <template v-slot="scope">

          <span>{{ parseTime(scope.row.createTime) }}</span>

        </template>

      </el-table-column>

    </el-table>

    <!-- 分页组件 -->

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"

                @pagination="getList"/>



  </div>

</template>



<script>

import { listNoticeWorkbench } from "@/api/system/notice";



export default {

  name: "SystemMyNotify",

  data() {

    return {

      loading: true,

      showSearch: true,

      total: 0,

      list: [],

      queryParams: {

        pageNo: 1,

        pageSize: 10,

        title: null,

        publisherName: null,

        deptName: null,

        type: null,

        createTime: []

      },

    };

  },

  created() {

    this.getList();

  },

  methods: {

    getList() {

      this.loading = true;

      listNoticeWorkbench(this.queryParams).then(response => {

        this.list = response.data.list;

        this.total = response.data.total;

        this.loading = false;

      });

    },

    handleQuery() {

      this.queryParams.pageNo = 1;

      this.getList();

    },

    resetQuery() {

      this.resetForm("queryForm");

      this.handleQuery();

    },

    handleRowClick(row) {

      if (!row || !row.id) {

        return

      }

      this.$router.push({ name: 'MyNotifyMessageDetail', query: { noticeId: row.id } }).catch(() => {})

    }

  }

}

</script>



<style scoped>

.notify-list-table ::v-deep .el-table__row {

  cursor: pointer;

}



.notify-list-table ::v-deep .link-type:hover {

  text-decoration: underline;

}

</style>


