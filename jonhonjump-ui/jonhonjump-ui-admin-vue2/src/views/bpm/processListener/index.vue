<template>
  <div class="app-container">
    <doc-alert title="执行监听器、任务监听器" url="https://doc.iocoder.cn/bpm/listener/" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="85px">
      <el-form-item label="名字" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入名字" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择类型" clearable>
          <el-option v-for="dict in getDictDatas(DICT_TYPE.BPM_PROCESS_LISTENER_TYPE)"
                     :key="dict.value" :label="dict.label" :value="dict.value"/>
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
                   v-hasPermi="['bpm:process-listener:create']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="编号" align="center" prop="id" />
      <el-table-column label="名字" align="center" prop="name" />
      <el-table-column label="类型" align="center" prop="type">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.BPM_PROCESS_LISTENER_TYPE" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="事件" align="center" prop="event" />
      <el-table-column label="值类型" align="center" prop="valueType">
        <template v-slot="scope">
          <dict-tag :type="DICT_TYPE.BPM_PROCESS_LISTENER_VALUE_TYPE" :value="scope.row.valueType"/>
        </template>
      </el-table-column>
      <el-table-column label="值" align="center" prop="value" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template v-slot="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['bpm:process-listener:update']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['bpm:process-listener:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名字" prop="name">
          <el-input v-model="form.name" placeholder="请输入名字" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in getDictDatas(DICT_TYPE.COMMON_STATUS)"
                      :key="dict.value" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%" @change="form.event = undefined">
            <el-option v-for="dict in getDictDatas(DICT_TYPE.BPM_PROCESS_LISTENER_TYPE)"
                       :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="事件" prop="event">
          <el-select v-model="form.event" placeholder="请选择事件" style="width: 100%">
            <el-option v-for="item in eventOptions" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="form.valueType" placeholder="请选择值类型" style="width: 100%">
            <el-option v-for="dict in getDictDatas(DICT_TYPE.BPM_PROCESS_LISTENER_VALUE_TYPE)"
                       :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="form.valueType === 'class' ? '类路径' : '表达式'" prop="value">
          <el-input v-model="form.value" :placeholder="form.valueType === 'class' ? '请输入类路径' : '请输入表达式'" />
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
  createProcessListener,
  deleteProcessListener,
  getProcessListener,
  getProcessListenerPage,
  updateProcessListener
} from '@/api/bpm/processListener'
import { CommonStatusEnum } from '@/utils/constants'

const EXECUTION_EVENTS = [
  { label: '开始', value: 'start' },
  { label: '结束', value: 'end' }
]
const TASK_EVENTS = [
  { label: '创建', value: 'create' },
  { label: '指派', value: 'assignment' },
  { label: '完成', value: 'complete' },
  { label: '删除', value: 'delete' },
  { label: '更新', value: 'update' },
  { label: '超时', value: 'timeout' }
]

export default {
  name: 'BpmProcessListener',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      title: '',
      open: false,
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        name: null,
        type: null
      },
      form: {},
      rules: {
        name: [{ required: true, message: '名字不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '类型不能为空', trigger: 'change' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
        event: [{ required: true, message: '监听事件不能为空', trigger: 'change' }],
        valueType: [{ required: true, message: '值类型不能为空', trigger: 'change' }],
        value: [{ required: true, message: '值不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    eventOptions() {
      if (this.form.type === 'execution') {
        return EXECUTION_EVENTS
      }
      if (this.form.type === 'task') {
        return TASK_EVENTS
      }
      return []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      getProcessListenerPage(this.queryParams).then(response => {
        this.list = response.data.list
        this.total = response.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        type: undefined,
        status: CommonStatusEnum.ENABLE,
        event: undefined,
        valueType: undefined,
        value: undefined
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增流程监听器'
    },
    handleUpdate(row) {
      this.reset()
      getProcessListener(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改流程监听器'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.id) {
          updateProcessListener(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
          return
        }
        createProcessListener(this.form).then(() => {
          this.$modal.msgSuccess('新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该流程监听器？').then(() => {
        return deleteProcessListener(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
