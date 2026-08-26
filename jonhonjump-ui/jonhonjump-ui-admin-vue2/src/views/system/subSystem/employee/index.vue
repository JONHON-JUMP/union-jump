<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 已配置接口的外部系统列表 -->
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
        <div class="head-container sub-system-list" v-loading="clientsLoading">
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
              <el-tag size="mini" type="success">{{ configMap[item.id] ? configMap[item.id].apiType : '未配置' }}</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无已配置接口的系统" :image-size="60" />
        </div>
      </el-col>
      <!-- 人员数据 -->
      <el-col :span="20" :xs="24">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择系统；只有已在【子系统接口配置】中配置并启用的系统才会出现在这里"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="车间" prop="workshopCode">
            <el-select v-model="queryParams.workshopCode" placeholder="全部车间" clearable filterable style="width: 180px">
              <el-option
                v-for="item in workshopOptions"
                :key="item.workshopCode"
                :label="item.workshopName + ' (' + item.workshopCode + ')'"
                :value="item.workshopCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="工号" prop="userCode">
            <el-input v-model="queryParams.userCode" placeholder="请输入工号" clearable style="width: 160px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="姓名" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入姓名" clearable style="width: 160px"
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
                       v-hasPermi="['sub-system:employee:create']">新增</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
        </el-row>
        <el-table v-loading="loading" :data="employeeList">
          <el-table-column label="工号" align="center" prop="userCode" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="姓名" align="center" prop="userName" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="车间" align="center" prop="workshopName" :show-overflow-tooltip="true">
            <template v-slot="scope">
              <span>{{ scope.row.workshopName || scope.row.workshopCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="班组" align="center" prop="teamName" :show-overflow-tooltip="true">
            <template v-slot="scope">
              <span>{{ scope.row.teamName || scope.row.teamCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="域账号" align="center" prop="domainName" :show-overflow-tooltip="true" />
          <el-table-column label="ERP号" align="center" prop="erpNo" width="110" :show-overflow-tooltip="true" />
          <el-table-column label="卡号" align="center" prop="cardNo" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:employee:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:employee:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                    @pagination="getList"/>
      </el-col>
    </el-row>
    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="外部系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="车间" prop="workshopCode">
          <el-select v-model="form.workshopCode" placeholder="请选择车间" filterable style="width: 100%"
                     :disabled="!!form.editing" @change="handleWorkshopChange">
            <el-option
              v-for="item in workshopOptions"
              :key="item.workshopCode"
              :label="item.workshopName + ' (' + item.workshopCode + ')'"
              :value="item.workshopCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班组" prop="teamCode">
          <el-select v-model="form.teamCode" placeholder="请选择班组（可选）" clearable filterable style="width: 100%"
                     :loading="teamComboLoading" @focus="loadTeamCombo">
            <el-option
              v-for="item in teamOptions"
              :key="item.teamCode"
              :label="item.teamName + ' (' + item.teamCode + ')'"
              :value="item.teamCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工号" prop="userCode">
          <el-input v-model="form.userCode" placeholder="请输入工号" :disabled="!!form.editing" />
        </el-form-item>
        <el-form-item label="姓名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="域账号" prop="domainName">
          <el-input v-model="form.domainName" placeholder="请输入域账号" />
        </el-form-item>
        <el-form-item label="ERP号" prop="erpNo">
          <el-input v-model="form.erpNo" placeholder="请输入 ERP 号" />
        </el-form-item>
        <el-form-item label="卡号" prop="cardNo">
          <el-input v-model="form.cardNo" placeholder="请输入刷卡卡号" />
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
  createSubSystemEmployee,
  deleteSubSystemEmployee,
  getSubSystemClientSimpleList,
  getSubSystemEmployeePage,
  getSubSystemEmployeeTeamCombo,
  updateSubSystemEmployee
} from '@/api/system/subSystemEmployee'
import { getSubSystemApiConfigList } from '@/api/system/subSystemApiConfig'
import { getSubSystemWorkshopSimpleList } from '@/api/system/subSystemWorkshop'
import subSystemImportGate from '@/utils/subSystemImportGate'
export default {
  name: 'SubSystemEmployee',
  mixins: [subSystemImportGate],
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      employeeList: [],
      clientList: [],
      configMap: {},
      clientKeyword: '',
      selectedClient: null,
      workshopOptions: [],
      teamOptions: [],
      teamComboLoading: false,
      title: '',
      open: false,
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        workshopCode: undefined,
        userCode: undefined,
        userName: undefined
      },
      form: {},
      rules: {
        workshopCode: [{ required: true, message: '车间不能为空', trigger: 'change' }],
        userCode: [{ required: true, message: '工号不能为空', trigger: 'blur' }],
        userName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    filteredClientList() {
      const keyword = (this.clientKeyword || '').trim().toLowerCase()
      return this.clientList.filter(item => {
        if (!this.configMap[item.id]) {
          return false
        }
        if (!keyword) {
          return true
        }
        return (item.name && item.name.toLowerCase().includes(keyword)) ||
          (item.clientId && item.clientId.toLowerCase().includes(keyword))
      })
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.withClientsLoading(() => {
        return Promise.all([getSubSystemClientSimpleList(), getSubSystemApiConfigList()]).then(([clients, configs]) => {
          this.clientList = clients.data || []
          this.configMap = {}
          ;(configs.data || []).forEach(c => {
            // 只显示启用状态的配置
            if (c.status === 0 || c.status == null) {
              this.configMap[c.subSystemId] = c
            }
          })
          if (this.syncSelectedClientFromList()) {
            return
          }
          if (!this.selectedClient) {
            const first = this.filteredClientList[0]
            if (first) {
              this.handleClientClick(first)
            }
          }
        })
      })
    },
    handleClientClick(item) {
      this.selectedClient = item
      this.queryParams.workshopCode = undefined
      this.queryParams.userCode = undefined
      this.queryParams.userName = undefined
      this.queryParams.pageNo = 1
      this.loadWorkshopOptions()
      this.getList()
    },
    loadWorkshopOptions() {
      this.workshopOptions = []
      if (!this.selectedClient) {
        return
      }
      getSubSystemWorkshopSimpleList(this.selectedClient.id).then(res => {
        this.workshopOptions = res.data || []
      })
    },
    getList() {
      if (!this.selectedClient) {
        this.employeeList = []
        this.total = 0
        return
      }
      this.loading = true
      getSubSystemEmployeePage({
        ...this.queryParams,
        subSystemId: this.selectedClient.id
      }).then(res => {
        this.employeeList = res.data.list || []
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
    loadTeamCombo() {
      if (!this.form.workshopCode || !this.selectedClient || this.teamOptions.length) {
        return
      }
      this.teamComboLoading = true
      getSubSystemEmployeeTeamCombo(this.selectedClient.id, this.form.workshopCode).then(res => {
        this.teamOptions = res.data || []
      }).finally(() => {
        this.teamComboLoading = false
      })
    },
    handleWorkshopChange() {
      this.teamOptions = []
      this.form.teamCode = undefined
    },
    resetFormData() {
      this.form = {
        editing: false,
        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,
        workshopCode: undefined,
        teamCode: undefined,
        userCode: undefined,
        userName: undefined,
        domainName: undefined,
        erpNo: undefined,
        cardNo: undefined
      }
      this.teamOptions = []
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      this.ensureSubSystemBoundBeforeAction('新增人员', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.open = true
        this.title = '新增子系统人员'
      }).catch(() => {})
    },
    handleUpdate(row) {
      this.resetFormData()
      this.form = {
        editing: true,
        subSystemId: this.selectedClient.id,
        workshopCode: row.workshopCode || row.workshopName,
        teamCode: row.teamCode,
        userCode: row.userCode,
        userName: row.userName,
        domainName: row.domainName,
        erpNo: row.erpNo,
        cardNo: row.cardNo
      }
      this.open = true
      this.title = '修改子系统人员'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const promise = this.form.editing
          ? updateSubSystemEmployee(this.form)
          : createSubSystemEmployee(this.form)
        promise.then(() => {
          this.$modal.msgSuccess(this.form.editing ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const tip = (this.configMap[this.selectedClient.id] && this.configMap[this.selectedClient.id].deleteTip)
        ? this.configMap[this.selectedClient.id].deleteTip
        : '删除后不可恢复'
      this.$modal.confirm('是否确认删除人员【' + row.userName + '（' + row.userCode + '）】？\n\n' + tip).then(() => {
        return deleteSubSystemEmployee(this.selectedClient.id, row.userCode)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
