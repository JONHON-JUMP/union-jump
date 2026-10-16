<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="系统" prop="systemName">
        <el-input v-model="queryParams.systemName" placeholder="系统名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="IP" prop="ip">
        <el-input v-model="queryParams.ip" placeholder="IP" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input v-model="queryParams.port" placeholder="端口" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="框架" prop="framework">
        <el-select v-model="queryParams.framework" placeholder="框架" clearable>
          <el-option label="若依" value="若依"/>
          <el-option label="芋道" value="芋道"/>
          <el-option label="C#" value="C#"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['system:biz-system:create']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="checkedIds.length === 0"
                   @click="handleDeleteBatch" v-hasPermi="['system:biz-system:delete']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50"/>
      <el-table-column label="序号" align="center" prop="sortNo" width="70"/>
      <el-table-column label="IP" align="center" prop="ip" width="150"/>
      <el-table-column label="端口" align="center" prop="port" width="90"/>
      <el-table-column label="系统" align="center" prop="systemName" min-width="180" show-overflow-tooltip/>
      <el-table-column label="后端明细" align="center" prop="backendDetail" min-width="220" show-overflow-tooltip/>
      <el-table-column label="系统类型" align="center" prop="systemType" width="120"/>
      <el-table-column label="框架" align="center" prop="framework" width="80"/>
      <el-table-column label="nginx映射端口" align="center" prop="nginxPort" width="130"/>
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['system:biz-system:update']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['system:biz-system:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="1" controls-position="right"/>
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input v-model="form.ip" placeholder="例如 192.168.240.127"/>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right"/>
        </el-form-item>
        <el-form-item label="系统" prop="systemName">
          <el-input v-model="form.systemName" placeholder="系统名称"/>
        </el-form-item>
        <el-form-item label="后端明细" prop="backendDetail">
          <el-input v-model="form.backendDetail" placeholder="例如 192.168.240.127:4222/prod-api/"/>
        </el-form-item>
        <el-form-item label="系统类型" prop="systemType">
          <el-select v-model="form.systemType" placeholder="请选择" clearable style="width: 100%">
            <el-option label="前后端分离" value="前后端分离"/>
            <el-option label="前后端一体" value="前后端一体"/>
          </el-select>
        </el-form-item>
        <el-form-item label="框架" prop="framework">
          <el-select v-model="form.framework" placeholder="请选择" clearable style="width: 100%">
            <el-option label="若依" value="若依"/>
            <el-option label="芋道" value="芋道"/>
            <el-option label="C#" value="C#"/>
          </el-select>
        </el-form-item>
        <el-form-item label="nginx映射端口" prop="nginxPort">
          <el-input-number v-model="form.nginxPort" :min="1" :max="65535" controls-position="right"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBizSystem, getBizSystem, addBizSystem, updateBizSystem, delBizSystem, delBizSystemList } from '@/api/system/bizSystem'

export default {
  name: 'BizSystem',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      title: '',
      open: false,
      checkedIds: [],
      queryParams: {
        pageNo: 1,
        pageSize: 20,
        systemName: undefined,
        ip: undefined,
        port: undefined,
        framework: undefined
      },
      form: {},
      rules: {
        sortNo: [{ required: true, message: '序号不能为空', trigger: 'blur' }],
        ip: [{ required: true, message: 'IP不能为空', trigger: 'blur' }],
        port: [{ required: true, message: '端口不能为空', trigger: 'blur' }],
        systemName: [{ required: true, message: '系统名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const query = { ...this.queryParams }
      if (query.port === '' || query.port === undefined) {
        delete query.port
      } else {
        query.port = Number(query.port)
      }
      listBizSystem(query).then(res => {
        this.list = res.data.list || []
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
      this.handleQuery()
    },
    reset() {
      this.form = {
        id: undefined,
        sortNo: (this.total || 0) + 1,
        ip: undefined,
        port: undefined,
        systemName: undefined,
        backendDetail: undefined,
        systemType: undefined,
        framework: undefined,
        nginxPort: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增系统明细'
    },
    handleUpdate(row) {
      getBizSystem(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改系统明细'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const req = this.form.id ? updateBizSystem : addBizSystem
        req(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除系统「' + row.systemName + '」？').then(() => {
        return delBizSystem(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认删除选中的系统明细？').then(() => {
        return delBizSystemList(this.checkedIds.join(','))
      }).then(() => {
        this.checkedIds = []
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleSelectionChange(rows) {
      this.checkedIds = rows.map(item => item.id)
    }
  }
}
</script>
