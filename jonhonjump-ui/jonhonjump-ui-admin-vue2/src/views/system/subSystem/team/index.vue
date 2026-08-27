<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：已做车间对照的部门 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="deptKeyword"
            placeholder="筛选部门/车间编号"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container dept-list" v-loading="deptsLoading">
          <div
            v-for="item in filteredDeptList"
            :key="item.id"
            class="dept-item"
            :class="{ 'is-active': selectedDept && selectedDept.id === item.id }"
            @click="handleDeptClick(item)"
          >
            <div class="dept-item__name">{{ item.deptName || '未命名部门' }}</div>
            <div class="dept-item__meta">
              <span>{{ item.workshopName || '车间' }}</span>
              <el-tag size="mini" type="info">{{ item.workshopCode }}</el-tag>
            </div>
          </div>
          <el-empty
            v-if="!deptsLoading && filteredDeptList.length === 0"
            description="暂无车间对照，请先在车间对照中维护部门"
            :image-size="60"
          />
        </div>
      </el-col>

      <!-- 右侧：该部门下班组 -->
      <el-col :span="20" :xs="24" v-loading="deptsLoading">
        <el-alert
          v-if="!deptsLoading && !selectedDept"
          title="请先选择左侧部门（须已在车间对照中维护，如：制造二部 / 4200）"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
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
            <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport"
                       v-hasPermi="['sub-system:team:create']">导入</el-button>
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
          <el-table-column label="编号" align="center" prop="id" width="80" />
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

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属部门">
          <el-input :value="deptDisplay" disabled />
        </el-form-item>
        <el-form-item label="车间编号">
          <el-input :value="selectedDept ? selectedDept.workshopCode : ''" disabled />
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

    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-alert
        :title="'当前部门：' + deptDisplay"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="uploadAction"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已存在的班组（按班组编码）
          </div>
          <span>仅允许 xls/xlsx。导入将挂到当前所选部门下。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createSubSystemTeam,
  deleteSubSystemTeam,
  deleteSubSystemTeamList,
  getSubSystemTeam,
  getSubSystemTeamPage,
  getSubSystemTeamUserSimpleList,
  importSubSystemTeamTemplate,
  updateSubSystemTeam
} from '@/api/system/subSystemTeam'
import { getSubSystemWorkshopPage } from '@/api/system/subSystemWorkshop'
import { getBaseHeader } from '@/utils/request'

export default {
  name: 'SubSystemTeam',
  data() {
    return {
      loading: false,
      deptsLoading: true,
      showSearch: true,
      total: 0,
      teamList: [],
      deptList: [],
      userOptions: [],
      deptKeyword: '',
      selectedDept: null,
      title: '',
      open: false,
      checkedIds: [],
      upload: {
        open: false,
        title: '',
        isUploading: false,
        updateSupport: false,
        headers: getBaseHeader()
      },
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
    deptDisplay() {
      if (!this.selectedDept) {
        return '未选择'
      }
      const name = this.selectedDept.deptName || '未命名部门'
      const code = this.selectedDept.workshopCode || ''
      return code ? (name + ' / ' + code) : name
    },
    uploadAction() {
      const subSystemId = this.selectedDept && this.selectedDept.subSystemId
      const deptId = this.selectedDept && this.selectedDept.deptId
      const update = this.upload.updateSupport ? 'true' : 'false'
      return process.env.VUE_APP_BASE_API + '/admin-api/system/sub-system-team/import'
        + '?subSystemId=' + (subSystemId || '')
        + '&deptId=' + (deptId || '')
        + '&updateSupport=' + update
    },
    filteredDeptList() {
      const keyword = (this.deptKeyword || '').trim().toLowerCase()
      const list = (this.deptList || []).filter(item => item.deptId != null)
      if (!keyword) {
        return list
      }
      return list.filter(item =>
        (item.deptName && item.deptName.toLowerCase().includes(keyword))
        || (item.workshopCode && String(item.workshopCode).toLowerCase().includes(keyword))
        || (item.workshopName && item.workshopName.toLowerCase().includes(keyword))
      )
    }
  },
  created() {
    this.loadDeptList()
  },
  methods: {
    loadDeptList() {
      this.deptsLoading = true
      return getSubSystemWorkshopPage({ pageNo: 1, pageSize: 200 }).then(res => {
        const page = res.data || {}
        this.deptList = (page.list || []).filter(item => item.deptId != null)
        const keepId = this.selectedDept && this.selectedDept.id
        const keep = this.deptList.find(d => d.id === keepId)
        if (keep) {
          this.selectedDept = keep
          this.getList()
          this.loadUserOptions()
          return
        }
        if (this.deptList.length > 0) {
          this.handleDeptClick(this.deptList[0])
        } else {
          this.selectedDept = null
          this.teamList = []
          this.total = 0
        }
      }).finally(() => {
        this.deptsLoading = false
      })
    },
    loadUserOptions() {
      if (!this.selectedDept || !this.selectedDept.subSystemId) {
        this.userOptions = []
        return
      }
      getSubSystemTeamUserSimpleList(this.selectedDept.subSystemId).then(res => {
        this.userOptions = res.data || []
      })
    },
    handleDeptClick(item) {
      this.selectedDept = item
      this.queryParams.teamCode = undefined
      this.queryParams.teamName = undefined
      this.queryParams.pageNo = 1
      this.loadUserOptions()
      this.getList()
    },
    ensureDeptSelected(actionLabel) {
      if (!this.selectedDept || !this.selectedDept.deptId || !this.selectedDept.subSystemId) {
        this.$modal.msgWarning('请先在左侧选择已做车间对照的部门后再' + actionLabel)
        return false
      }
      return true
    },
    getList() {
      if (!this.selectedDept || !this.selectedDept.deptId) {
        this.teamList = []
        this.total = 0
        return
      }
      this.loading = true
      getSubSystemTeamPage({
        ...this.queryParams,
        deptId: this.selectedDept.deptId,
        subSystemId: this.selectedDept.subSystemId
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
        subSystemId: this.selectedDept ? this.selectedDept.subSystemId : undefined,
        deptId: this.selectedDept ? this.selectedDept.deptId : undefined,
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
      if (!this.ensureDeptSelected('新增班组')) {
        return
      }
      this.loadUserOptions()
      this.resetFormData()
      this.open = true
      this.title = '添加班组'
    },
    handleImport() {
      if (!this.ensureDeptSelected('导入')) {
        return
      }
      this.upload.title = '导入班组 — ' + this.deptDisplay
      this.upload.open = true
      this.upload.headers = getBaseHeader()
    },
    importTemplate() {
      importSubSystemTeamTemplate().then(response => {
        this.$download.excel(response, '班组导入模板.xls')
      })
    },
    handleFileUploadProgress() {
      this.upload.isUploading = true
    },
    handleFileSuccess(response) {
      this.upload.open = false
      this.upload.isUploading = false
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
      if (response.code !== 0) {
        this.$modal.msgError(response.msg || '导入失败')
        return
      }
      const data = response.data || {}
      let text = '新建：' + ((data.createKeys && data.createKeys.length) || 0)
      ;(data.createKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + k })
      text += '<br />更新：' + ((data.updateKeys && data.updateKeys.length) || 0)
      ;(data.updateKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + k })
      const failMap = data.failureKeys || {}
      const failKeys = Object.keys(failMap)
      text += '<br />失败：' + failKeys.length
      failKeys.forEach(k => { text += '<br />&nbsp;&nbsp;' + k + '：' + failMap[k] })
      this.$alert(text, '导入结果', { dangerouslyUseHTMLString: true })
      this.getList()
    },
    submitFileForm() {
      this.$refs.upload.submit()
    },
    handleUpdate(row) {
      if (!this.ensureDeptSelected('修改')) {
        return
      }
      this.loadUserOptions()
      this.resetFormData()
      getSubSystemTeam(row.id).then(res => {
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId,
          deptId: res.data.deptId || (this.selectedDept && this.selectedDept.deptId),
          teamCode: res.data.teamCode,
          teamName: res.data.teamName,
          description: res.data.description,
          teamLeaderId: res.data.teamLeaderId
        }
        this.open = true
        this.title = '修改班组'
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
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除班组"' + row.teamName + '"？').then(() => {
        return deleteSubSystemTeam(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的班组？').then(() => {
        return deleteSubSystemTeamList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.checkedIds = []
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
.dept-list {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}
.dept-item {
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
