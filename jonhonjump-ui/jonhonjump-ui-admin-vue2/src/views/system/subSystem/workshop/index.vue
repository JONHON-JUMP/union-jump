<template>
  <div class="app-container">
    <el-row :gutter="20">
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
              <el-tag v-if="item.workshopScopeEnabled" size="mini" type="warning">多车间</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无业务系统" :image-size="60" />
        </div>
      </el-col>

      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择业务系统；本页维护该系统下可挂接的叶子（一对一也要维护一条）"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-alert
          v-else
          title="叶子 = 人员挂接与角色作用域单位；多叶子系统请在「业务系统登记」开启车间维度。有用户/角色占用的叶子不能删、不能改编号。"
          type="info"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="叶子编号" prop="workshopCode">
            <el-input v-model="queryParams.workshopCode" placeholder="车间编号" clearable style="width: 160px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="叶子名称" prop="workshopName">
            <el-input v-model="queryParams.workshopName" placeholder="名称" clearable style="width: 180px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
              <el-option v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)"/>
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
          <el-table-column label="叶子编号" align="center" prop="workshopCode" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="叶子名称" align="center" prop="workshopName" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column label="产品线" align="center" prop="productLine" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="排序" align="center" prop="sort" width="70" />
          <el-table-column label="用户数" align="center" prop="userCount" width="80" />
          <el-table-column label="角色数" align="center" prop="roleCount" width="80" />
          <el-table-column label="状态" align="center" prop="status" width="90">
            <template v-slot="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="170">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" fixed="right">
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

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="业务系统">
          <el-input :value="selectedClient ? selectedClient.name + ' (' + selectedClient.clientId + ')' : ''" disabled />
        </el-form-item>
        <el-form-item label="叶子编号" prop="workshopCode">
          <el-input
            v-model="form.workshopCode"
            placeholder="如 5501、4200；全局唯一"
            maxlength="50"
            :disabled="!!form.id"
          />
          <div v-if="form.id" class="form-tip">编号创建后不可改；需调整请新建叶子并迁移人员/角色</div>
        </el-form-item>
        <el-form-item label="叶子名称" prop="workshopName">
          <el-input v-model="form.workshopName" placeholder="展示名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="产品线" prop="productLine">
          <el-input v-model="form.productLine" placeholder="可选，如 环控 / 液冷" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
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
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, ensureDictDatas, getDictDatas } from '@/utils/dict'
import subSystemImportGate from '@/utils/subSystemImportGate'

export default {
  name: 'SubSystemWorkshop',
  mixins: [subSystemImportGate],
  data() {
    return {
      loading: false,
      submitting: false,
      showSearch: true,
      total: 0,
      workshopList: [],
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
        workshopName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        workshopCode: [{ required: true, message: '叶子编号不能为空', trigger: 'blur' }],
        workshopName: [{ required: true, message: '叶子名称不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
      }
    }
  },
  computed: {
    DICT_TYPE() {
      return DICT_TYPE
    },
    statusDictDatas() {
      return getDictDatas(DICT_TYPE.COMMON_STATUS)
    },
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
    ensureDictDatas(DICT_TYPE.COMMON_STATUS).finally(() => {
      this.loadClientList()
    })
  },
  methods: {
    loadClientList() {
      return this.withClientsLoading(() => {
        return getSubSystemClientSimpleList(true).then(res => {
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
    handleClientClick(item) {
      this.selectedClient = item
      this.queryParams.workshopCode = undefined
      this.queryParams.workshopName = undefined
      this.queryParams.status = undefined
      this.queryParams.pageNo = 1
      this.getList()
    },
    getList() {
      if (!this.selectedClient) {
        this.workshopList = []
        this.total = 0
        return
      }
      const seq = (this._listSeq = (this._listSeq || 0) + 1)
      this.loading = true
      getSubSystemWorkshopPage({
        ...this.queryParams,
        subSystemId: this.selectedClient.id
      }).then(res => {
        if (seq !== this._listSeq) {
          return
        }
        this.workshopList = res.data.list || []
        this.total = res.data.total || 0
      }).catch(() => {
        if (seq === this._listSeq) {
          this.workshopList = []
          this.total = 0
        }
        // 加载失败（含切页主动取消）由请求拦截器统一处理，不再弹通用 toast
      }).finally(() => {
        if (seq === this._listSeq) {
          this.loading = false
        }
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
        productLine: undefined,
        sort: 0,
        status: CommonStatusEnum.ENABLE,
        remark: undefined
      }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    handleAdd() {
      this.ensureSubSystemBoundBeforeAction('新增叶子', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.open = true
        this.title = '新增业务系统叶子'
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
          productLine: res.data.productLine,
          sort: res.data.sort == null ? 0 : res.data.sort,
          status: res.data.status,
          remark: res.data.remark
        }
        this.open = true
        this.title = '修改业务系统叶子'
      }).catch(() => {
        this.$modal.msgError('加载叶子信息失败')
      })
    },
    submitForm() {
      if (this.submitting) {
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.submitting = true
        const req = this.form.id ? updateSubSystemWorkshop : createSubSystemWorkshop
        const payload = {
          ...this.form,
          subSystemId: this.selectedClient.id
        }
        req(payload).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        }).catch(() => {
          // 具体原因由请求拦截器提示（叶子编号重复、仍在使用等）
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除叶子「' + row.workshopCode + '」？').then(() => {
        return deleteSubSystemWorkshop(row.id)
      }).then(() => {
        if (this.workshopList && this.workshopList.length === 1 && this.queryParams.pageNo > 1) {
          this.queryParams.pageNo -= 1
        }
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认删除选中的 ' + this.checkedIds.length + ' 条叶子？').then(() => {
        return deleteSubSystemWorkshopList(this.checkedIds)
      }).then(() => {
        const removing = (this.checkedIds || []).length
        if (this.workshopList && this.workshopList.length <= removing && this.queryParams.pageNo > 1) {
          this.queryParams.pageNo -= 1
        }
        this.$modal.msgSuccess('删除成功')
        this.getList()
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
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  &.is-active {
    border-color: #409eff;
    background: #ecf5ff;
  }
  &__name {
    font-weight: 600;
    margin-bottom: 4px;
  }
  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
  }
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
