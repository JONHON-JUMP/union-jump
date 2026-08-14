<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择分类" clearable filterable style="width: 180px">
          <el-option v-for="dict in getDictDatas(DICT_TYPE.SYSTEM_FAQ_CATEGORY)" :key="dict.value"
                     :label="dict.label" :value="parseInt(dict.value)"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发布人" prop="publisherName">
        <el-input v-model="queryParams.publisherName" placeholder="请输入发布人" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" class="faq-list-table" @row-click="handleRowClick">
      <el-table-column label="分类" align="center" prop="category" width="120">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.SYSTEM_FAQ_CATEGORY" :value="scope.row.category"/>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" min-width="220" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span class="link-type">{{ scope.row.title || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发布人" align="center" prop="publisherName" width="120" :show-overflow-tooltip="true"/>
      <el-table-column label="发布部门" align="center" prop="deptName" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="发布时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>
  </div>
</template>

<script>
import { listFaqWorkbench } from '@/api/system/faq'

export default {
  name: 'MyFaq',
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
        category: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listFaqWorkbench(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
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
    handleRowClick(row) {
      if (!row || !row.id) {
        return
      }
      this.$router.push({ name: 'MyFaqDetail', query: { faqId: row.id } }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.faq-list-table ::v-deep .el-table__row {
  cursor: pointer;
}

.faq-list-table ::v-deep .link-type:hover {
  text-decoration: underline;
}
</style>
