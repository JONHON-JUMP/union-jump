<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="JUMP 部门" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="全部部门" clearable filterable style="width: 200px">
          <el-option
            v-for="item in deptOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="车间编码" prop="workshopCode">
        <el-input v-model="queryParams.workshopCode" placeholder="请输入车间编码" clearable style="width: 160px"
                  @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="车间名称" prop="workshopName">
        <el-input v-model="queryParams.workshopName" placeholder="请输入车间名称" clearable style="width: 160px"
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
                   v-hasPermi="['sub-system:workshop:create']">新增对照</el-button>
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
      <el-table-column label="JUMP 部门" align="center" prop="deptName" min-width="160" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span>{{ scope.row.deptName || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="车间编码" align="center" prop="workshopCode" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="车间名称" align="center" prop="workshopName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="说明" align="center" prop="description" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
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

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="JUMP 部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" filterable style="width: 100%">
            <el-option
              v-for="item in deptOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <div class="el-form-item__tip">用户管理新增用户时按部门带出此车间编码</div>
        </el-form-item>
        <el-form-item label="车间编码" prop="workshopCode">
          <el-input v-model="form.workshopCode" placeholder="如 4200" />
        </el-form-item>
        <el-form-item label="车间名称" prop="workshopName">
          <el-input v-model="form.workshopName" placeholder="车间名称" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="可选备注" :rows="3" />
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

export default {
  name: 'SubSystemWorkshop',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      workshopList: [],
      deptOptions: [],
      /** 库表仍要 subSystemId：后台静默绑定默认业务系统，页面不展示 */
      defaultSubSystemId: undefined,
      title: '',
      open: false,
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        deptId: undefined,
        workshopCode: undefined,
        workshopName: undefined
      },
      form: {},
      rules: {
        deptId: [{ required: true, message: '请选择 JUMP 部门', trigger: 'change' }],
        workshopCode: [{ required: true, message: '车间编码不能为空', trigger: 'blur' }],
        workshopName: [{ required: true, message: '车间名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadDefaultSubSystem().then(() => {
      this.getList()
    })
    this.loadDeptOptions()
  },
  methods: {
    /** 优先 mes4200（Camstar人员管理门户客户端），否则取第一个已登记系统；仅用于落库，不在页面展示 */
    loadDefaultSubSystem() {
      return getSubSystemClientSimpleList(true).then(res => {
        const list = res.data || []
        const preferred = list.find(item => item.clientId === 'mes4200') || list[0]
        this.defaultSubSystemId = preferred ? preferred.id : undefined
      }).catch(() => {
        this.defaultSubSystemId = undefined
      })
    },
    loadDeptOptions() {
      listSimpleDepts().then(res => {
        this.deptOptions = res.data || []
      })
    },
    getList() {
      this.loading = true
      getSubSystemWorkshopPage({ ...this.queryParams }).then(res => {
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
        subSystemId: this.defaultSubSystemId,
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
      if (!this.defaultSubSystemId) {
        this.$modal.msgWarning('请先在业务系统登记中至少登记一个系统（页面不展示，仅落库需要）')
        return
      }
      this.resetFormData()
      this.open = true
      this.title = '新增车间对照'
    },
    handleUpdate(row) {
      this.resetFormData()
      getSubSystemWorkshop(row.id).then(res => {
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId || this.defaultSubSystemId,
          workshopCode: res.data.workshopCode,
          workshopName: res.data.workshopName,
          deptId: res.data.deptId,
          description: res.data.description
        }
        this.open = true
        this.title = '修改车间对照'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.form.subSystemId) {
          this.form.subSystemId = this.defaultSubSystemId
        }
        if (!this.form.subSystemId) {
          this.$modal.msgWarning('缺少默认业务系统，无法保存')
          return
        }
        const request = this.form.id ? updateSubSystemWorkshop : createSubSystemWorkshop
        request(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除车间对照【' + row.workshopName + '】？').then(() => {
        return deleteSubSystemWorkshop(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleDeleteBatch() {
      const names = this.workshopList.filter(w => this.checkedIds.includes(w.id)).map(w => w.workshopName)
      this.$modal.confirm('是否确认删除选中的对照【' + names.join('、') + '】？').then(() => {
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
