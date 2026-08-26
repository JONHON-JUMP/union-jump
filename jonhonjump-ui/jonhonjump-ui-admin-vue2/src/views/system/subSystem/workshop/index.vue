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
              <el-tag size="mini" type="info">{{ item.workshopCount || 0 }} 车间</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>
      <!-- 车间映射数据 -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择已登记的外部系统；维护 JUMP 部门与该系统车间的对应关系（用户创建联动时按部门换算车间）"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="车间编码" prop="workshopCode">
            <el-input v-model="queryParams.workshopCode" placeholder="请输入车间编码" clearable style="width: 200px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="车间名称" prop="workshopName">
            <el-input v-model="queryParams.workshopName" placeholder="请输入车间名称" clearable style="width: 200px"
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
                       v-hasPermi="['sub-system:workshop:create']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="checkedIds.length === 0"
              @click="handleDeleteBatch"
              v-hasPermi="['sub-system:workshop:delete']"
            >批量删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
        </el-row>
        <el-table v-loading="loading" :data="workshopList" @selection-change="handleRowCheckboxChange">
          <el-table-column type="selection" width="55"/>
          <el-table-column label="编号" align="center" prop="id" width="80" />
          <el-table-column label="车间编码" align="center" prop="workshopCode" :show-overflow-tooltip="true" />
          <el-table-column label="车间名称" align="center" prop="workshopName" :show-overflow-tooltip="true" />
          <el-table-column label="关联部门" align="center" prop="deptName" width="160" :show-overflow-tooltip="true" />
          <el-table-column label="车间描述" align="center" prop="description" :show-overflow-tooltip="true" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:workshop:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:workshop:delete']">删除</el-button>
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
        <el-form-item label="车间编码" prop="workshopCode">
          <el-input v-model="form.workshopCode" placeholder="请输入子系统车间编码（如 4200）" />
        </el-form-item>
        <el-form-item label="车间名称" prop="workshopName">
          <el-input v-model="form.workshopName" placeholder="请输入子系统车间名称" />
        </el-form-item>
        <el-form-item label="关联部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择 JUMP 部门（可空）" clearable filterable style="width: 100%">
            <el-option
              v-for="item in deptOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <div class="el-form-item__tip">JUMP 部门与子系统车间对应；多个部门可映射同一车间</div>
        </el-form-item>
        <el-form-item label="车间描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入车间描述" :rows="3" />
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
  createSubSystemWorkshop,
  deleteSubSystemWorkshop,
  deleteSubSystemWorkshopList,
  getSubSystemClientSimpleList,
  getSubSystemWorkshop,
  getSubSystemWorkshopPage,
  updateSubSystemWorkshop
} from '@/api/system/subSystemWorkshop'
import { listSimpleDepts } from '@/api/system/dept'
import subSystemImportGate from '@/utils/subSystemImportGate'
export default {
  name: 'SubSystemWorkshop',
  mixins: [subSystemImportGate],
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      workshopList: [],
      deptOptions: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      title: '',
      open: false,
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        workshopCode: undefined,
        workshopName: undefined
      },
      form: {},
      rules: {
        workshopCode: [{ required: true, message: '车间编码不能为空', trigger: 'blur' }],
        workshopName: [{ required: true, message: '车间名称不能为空', trigger: 'blur' }]
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
    this.loadDeptOptions()
  },
  methods: {
    loadClientList() {
      return this.withClientsLoading(() => {
        return getSubSystemClientSimpleList().then(res => {
          this.clientList = res.data || []
          if (this.syncSelectedClientFromList()) {
            return
          }
          if (!this.selectedClient && this.clientList.length > 0) {
            this.handleClientClick(this.clientList[0])
          }
        })
      })
    },
    loadDeptOptions() {
      listSimpleDepts().then(res => {
        this.deptOptions = res.data || []
      })
    },
    handleClientClick(item) {
      this.selectedClient = item
      this.queryParams.workshopCode = undefined
      this.queryParams.workshopName = undefined
      this.queryParams.pageNo = 1
      this.getList()
    },
    getList() {
      if (!this.selectedClient) {
        this.workshopList = []
        this.total = 0
        return
      }
      this.loading = true
      getSubSystemWorkshopPage({
        ...this.queryParams,
        subSystemId: this.selectedClient.id
      }).then(res => {
        this.workshopList = res.data.list || []
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
        workshopCode: undefined,
        workshopName: undefined,
        deptId: undefined,
        description: undefined
      }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      this.ensureSubSystemBoundBeforeAction('新增车间', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.open = true
        this.title = '添加外部系统车间'
      }).catch(() => {})
    },
    handleUpdate(row) {
      this.resetFormData()
      getSubSystemWorkshop(row.id).then(res => {
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId,
          workshopCode: res.data.workshopCode,
          workshopName: res.data.workshopName,
          deptId: res.data.deptId,
          description: res.data.description
        }
        this.open = true
        this.title = '修改外部系统车间'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.id) {
          updateSubSystemWorkshop(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          createSubSystemWorkshop(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const confirmText = '是否确认删除车间【' + row.workshopName + '】？已分配用户的车间无法删除。'
      this.$modal.confirm(confirmText).then(() => {
        return deleteSubSystemWorkshop(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleDeleteBatch() {
      const names = this.workshopList.filter(w => this.checkedIds.includes(w.id)).map(w => w.workshopName)
      this.$modal.confirm('是否确认删除选中的车间【' + names.join('、') + '】？').then(() => {
        return deleteSubSystemWorkshopList(this.checkedIds)
      }).then(() => {
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
<style scoped>
.el-form-item__tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
