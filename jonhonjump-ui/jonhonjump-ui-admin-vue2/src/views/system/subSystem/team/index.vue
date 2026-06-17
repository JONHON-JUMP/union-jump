<template>

  <div class="app-container">

    <el-row :gutter="20">

      <!-- 外部系统列表 -->

      <el-col :span="4" :xs="24">

        <div class="head-container">

          <el-input

            v-model="clientKeyword"

            placeholder="请输入系统名称"

            clearable

            size="small"

            prefix-icon="el-icon-search"

            style="margin-bottom: 20px"

          />

        </div>

        <div class="head-container sub-system-list">

          <div

            v-for="item in filteredClientList"

            :key="item.id"

            class="sub-system-item"

            :class="{ 'is-active': selectedClient && selectedClient.id === item.id }"

            @click="handleClientClick(item)"

          >

            <div class="sub-system-item__name">{{ item.name }}</div>

            <div class="sub-system-item__meta">

              <span>{{ item.clientId }}</span>

              <el-tag size="mini" type="info">{{ item.teamCount || 0 }} 班组</el-tag>

            </div>

          </div>

          <el-empty v-if="filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />

        </div>

      </el-col>



      <!-- 班组数据 -->

      <el-col :span="20" :xs="24">

        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">

          <el-form-item label="班组编码" prop="teamCode">

            <el-input v-model="queryParams.teamCode" placeholder="请输入班组编码" clearable style="width: 240px"

                      @keyup.enter.native="handleQuery"/>

          </el-form-item>

          <el-form-item label="班组名称" prop="teamName">

            <el-input v-model="queryParams.teamName" placeholder="请输入班组名称" clearable style="width: 240px"

                      @keyup.enter.native="handleQuery"/>

          </el-form-item>

          <el-form-item>

            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>

            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>

          </el-form-item>

        </el-form>



        <el-row :gutter="10" class="mb8">

          <el-col :span="1.5">

            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"

                       v-hasPermi="['sub-system:team:create']">新增</el-button>

          </el-col>

          <el-col :span="1.5">

            <el-button

              type="danger"

              plain

              icon="el-icon-delete"

              size="mini"

              :disabled="checkedIds.length === 0"

              @click="handleDeleteBatch"

              v-hasPermi="['sub-system:team:delete']"

            >批量删除</el-button>

          </el-col>

          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />

        </el-row>



        <el-table v-loading="loading" :data="teamList" @selection-change="handleRowCheckboxChange">

          <el-table-column type="selection" width="55"/>

          <el-table-column label="班组编号" align="center" prop="id" width="100" />

          <el-table-column label="班组编码" align="center" prop="teamCode" :show-overflow-tooltip="true" />

          <el-table-column label="班组名称" align="center" prop="teamName" :show-overflow-tooltip="true" />

          <el-table-column label="班组描述" align="center" prop="description" :show-overflow-tooltip="true" />

          <el-table-column label="班组长" align="center" prop="teamLeaderName" width="120" :show-overflow-tooltip="true" />

          <el-table-column label="创建时间" align="center" prop="createTime" width="180">

            <template v-slot="scope">

              <span>{{ parseTime(scope.row.createTime) }}</span>

            </template>

          </el-table-column>

          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">

            <template v-slot="scope">

              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"

                         v-hasPermi="['sub-system:team:update']">修改</el-button>

              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"

                         v-hasPermi="['sub-system:team:delete']">删除</el-button>

            </template>

          </el-table-column>

        </el-table>



        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"

                    @pagination="getList"/>

      </el-col>

    </el-row>



    <!-- 新增/修改 -->

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>

      <el-form ref="form" :model="form" :rules="rules" label-width="90px">

        <el-form-item label="外部系统">

          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />

        </el-form-item>

        <el-form-item label="班组编码" prop="teamCode">

          <el-input v-model="form.teamCode" placeholder="请输入班组编码" />

        </el-form-item>

        <el-form-item label="班组名称" prop="teamName">

          <el-input v-model="form.teamName" placeholder="请输入班组名称" />

        </el-form-item>

        <el-form-item label="班组描述" prop="description">

          <el-input v-model="form.description" type="textarea" placeholder="请输入班组描述" :rows="3" />

        </el-form-item>

        <el-form-item label="班组长" prop="teamLeaderId">

          <el-select v-model="form.teamLeaderId" placeholder="请选择班组长" clearable filterable style="width: 100%">

            <el-option

              v-for="item in userOptions"

              :key="item.id"

              :label="item.nickname"

              :value="item.id"

            />

          </el-select>

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

  createSubSystemTeam,

  deleteSubSystemTeam,

  deleteSubSystemTeamList,

  getSubSystemClientSimpleList,

  getSubSystemTeam,

  getSubSystemTeamPage,

  getSubSystemTeamUserSimpleList,

  updateSubSystemTeam

} from '@/api/system/subSystemTeam'



export default {

  name: 'SubSystemTeam',

  data() {

    return {

      loading: false,

      showSearch: true,

      total: 0,

      teamList: [],

      clientList: [],

      userOptions: [],

      clientKeyword: '',

      selectedClient: null,

      title: '',

      open: false,

      checkedIds: [],

      queryParams: {

        pageNo: 1,

        pageSize: 10,

        teamCode: undefined,

        teamName: undefined

      },

      form: {},

      rules: {

        teamCode: [{ required: true, message: '班组编码不能为空', trigger: 'blur' }],

        teamName: [{ required: true, message: '班组名称不能为空', trigger: 'blur' }]

      }

    }

  },

  computed: {

    filteredClientList() {

      const keyword = (this.clientKeyword || '').trim().toLowerCase()

      if (!keyword) {

        return this.clientList

      }

      return this.clientList.filter(item =>

        (item.name && item.name.toLowerCase().includes(keyword)) ||

        (item.clientId && item.clientId.toLowerCase().includes(keyword))

      )

    }

  },

  created() {

    this.loadClientList()

  },

  methods: {

    loadClientList() {

      getSubSystemClientSimpleList().then(res => {

        this.clientList = res.data || []

        if (!this.selectedClient && this.clientList.length > 0) {

          this.handleClientClick(this.clientList[0])

        }

      })

    },

    loadUserOptions() {

      if (!this.selectedClient) {

        this.userOptions = []

        return

      }

      getSubSystemTeamUserSimpleList(this.selectedClient.id).then(res => {

        this.userOptions = res.data || []

      })

    },

    handleClientClick(item) {

      this.selectedClient = item

      this.queryParams.teamCode = undefined

      this.queryParams.teamName = undefined

      this.queryParams.pageNo = 1

      this.loadUserOptions()

      this.getList()

    },

    getList() {

      if (!this.selectedClient) {

        this.teamList = []

        this.total = 0

        return

      }

      this.loading = true

      getSubSystemTeamPage({

        ...this.queryParams,

        subSystemId: this.selectedClient.id

      }).then(res => {

        this.teamList = res.data.list || []

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

      this.queryParams.pageNo = 1

      this.getList()

    },

    resetFormData() {

      this.form = {

        id: undefined,

        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,

        teamCode: undefined,

        teamName: undefined,

        description: undefined,

        teamLeaderId: undefined

      }

      this.resetForm('form')

    },

    cancel() {

      this.open = false

      this.resetFormData()

    },

    handleAdd() {

      if (!this.selectedClient) {

        this.$modal.msgWarning('请先在左侧选择外部系统')

        return

      }

      this.loadUserOptions()

      this.resetFormData()

      this.open = true

      this.title = '添加外部系统班组'

    },

    handleUpdate(row) {

      this.loadUserOptions()

      this.resetFormData()

      getSubSystemTeam(row.id).then(res => {

        this.form = {

          id: res.data.id,

          subSystemId: res.data.subSystemId,

          teamCode: res.data.teamCode,

          teamName: res.data.teamName,

          description: res.data.description,

          teamLeaderId: res.data.teamLeaderId

        }

        this.open = true

        this.title = '修改外部系统班组'

      })

    },

    submitForm() {

      this.$refs.form.validate(valid => {

        if (!valid) {

          return

        }

        const request = this.form.id ? updateSubSystemTeam : createSubSystemTeam

        request(this.form).then(() => {

          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')

          this.open = false

          this.getList()

          this.loadClientList()

        })

      })

    },

    handleDelete(row) {

      this.$modal.confirm('是否确认删除班组"' + row.teamName + '"？').then(() => {

        return deleteSubSystemTeam(row.id)

      }).then(() => {

        this.$modal.msgSuccess('删除成功')

        this.getList()

        this.loadClientList()

      }).catch(() => {})

    },

    handleDeleteBatch() {

      this.$modal.confirm('是否确认批量删除选中的外部系统班组？').then(() => {

        return deleteSubSystemTeamList(this.checkedIds)

      }).then(() => {

        this.$modal.msgSuccess('删除成功')

        this.checkedIds = []

        this.getList()

        this.loadClientList()

      }).catch(() => {})

    },

    handleRowCheckboxChange(selection) {

      this.checkedIds = selection.map(item => item.id)

    }

  }

}

</script>



<style lang="scss" scoped>

.sub-system-list {

  max-height: calc(100vh - 220px);

  overflow-y: auto;

}



.sub-system-item {

  padding: 12px 14px;

  margin-bottom: 8px;

  border: 1px solid #ebeef5;

  border-radius: 6px;

  cursor: pointer;

  transition: all 0.2s;



  &:hover,

  &.is-active {

    border-color: #409eff;

    background: #ecf5ff;

  }



  &__name {

    font-size: 14px;

    font-weight: 600;

    color: #303133;

    margin-bottom: 6px;

  }



  &__meta {

    display: flex;

    align-items: center;

    justify-content: space-between;

    font-size: 12px;

    color: #909399;

  }

}

</style>

