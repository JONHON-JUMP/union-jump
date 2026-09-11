<template>

  <div class="app-container">

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">

      <el-form-item label="系统名称" prop="systemName">

        <el-input v-model="queryParams.systemName" placeholder="请输入系统名称" clearable style="width: 240px"

                  @keyup.enter.native="handleQuery"/>

      </el-form-item>

      <el-form-item label="系统编号" prop="clientId">

        <el-input v-model="queryParams.clientId" placeholder="请输入系统编号" clearable style="width: 240px"

                  @keyup.enter.native="handleQuery"/>

      </el-form-item>

      <el-form-item label="状态" prop="status">

        <el-select v-model="queryParams.status" placeholder="系统状态" clearable style="width: 240px">

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

                   v-hasPermi="['sub-system:system:create']">新增</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="checkedIds.length === 0"

                   @click="handleDeleteBatch" v-hasPermi="['sub-system:system:delete']">批量删除</el-button>

      </el-col>

      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />

    </el-row>



    <el-table v-loading="loading" :data="systemList" @selection-change="handleRowCheckboxChange">

      <el-table-column type="selection" width="55"/>

      <el-table-column label="编号" align="center" prop="id" width="90" />
      <el-table-column label="系统图标" align="center" width="80">
        <template v-slot="scope">
          <img v-if="scope.row.systemIcon" width="40" height="40" :src="scope.row.systemIcon" class="system-icon">
          <el-avatar v-else :size="40" icon="el-icon-picture-outline" />
        </template>
      </el-table-column>
      <el-table-column label="系统编号" align="center" min-width="160">
        <template v-slot="scope">
          <div>{{ scope.row.clientId || '-' }}</div>
        </template>
      </el-table-column>

      <el-table-column label="系统名称" align="center" prop="systemName" :show-overflow-tooltip="true" />

      <el-table-column label="访问地址" align="center" prop="systemUrl" :show-overflow-tooltip="true" min-width="160" />

      <el-table-column label="系统状态" align="center" prop="status" width="100">

        <template v-slot="scope">

          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>

        </template>

      </el-table-column>

      <el-table-column label="创建时间" align="center" prop="createTime" width="180">

        <template v-slot="scope">

          <span>{{ parseTime(scope.row.createTime) }}</span>

        </template>

      </el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">

        <template v-slot="scope">

          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"

                     v-hasPermi="['sub-system:system:update']">修改</el-button>

          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"

                     v-hasPermi="['sub-system:system:delete']">删除</el-button>

        </template>

      </el-table-column>

    </el-table>



    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"

                @pagination="getList"/>



    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>

      <el-form ref="form" :model="form" :rules="rules" label-width="110px">

        <el-form-item label="系统编号" prop="clientId">
          <el-input v-model="form.clientId" placeholder="如 mes4200" maxlength="64" />
        </el-form-item>

        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="form.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="系统图标" prop="systemIcon">
          <imageUpload v-model="form.systemIcon" :limit="1"/>
          <div class="form-item-tip">选填，不上传也可保存</div>
        </el-form-item>
        <el-form-item label="系统描述" prop="description">

          <el-input v-model="form.description" type="textarea" placeholder="请输入系统描述" :rows="3" />

        </el-form-item>

        <el-form-item label="访问地址" prop="systemUrl">

          <el-input v-model="form.systemUrl" placeholder="请输入访问地址" />

        </el-form-item>

        <el-form-item label="系统状态" prop="status">

          <el-radio-group v-model="form.status">

            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">

              {{ dict.label }}

            </el-radio>

          </el-radio-group>

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

  createSubSystem,

  deleteSubSystem,

  deleteSubSystemList,

  getSubSystem,

  getSubSystemPage,

  updateSubSystem

} from '@/api/system/subSystem'

import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas, ensureDictDatas } from '@/utils/dict'
import ImageUpload from '@/components/ImageUpload'

export default {
  name: 'SubSystemManage',
  components: {
    ImageUpload
  },

  data() {

    return {

      loading: false,

      showSearch: true,

      total: 0,

      systemList: [],

      title: '',

      open: false,

      checkedIds: [],

      queryParams: {

        pageNo: 1,

        pageSize: 10,

        systemName: undefined,

        clientId: undefined,

        status: undefined

      },

      form: {},

      rules: {
        clientId: [
          { required: true, message: '系统编号不能为空', trigger: 'blur' },
          { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '须以字母开头，仅含字母、数字、下划线或中划线', trigger: 'blur' }
        ],
        systemName: [{ required: true, message: '系统名称不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
      }

    }

  },

  computed: {
    statusDictDatas() {
      const list = getDictDatas(DICT_TYPE.COMMON_STATUS)
      if (list && list.length) {
        return list
      }
      return [
        { label: '开启', value: CommonStatusEnum.ENABLE },
        { label: '关闭', value: CommonStatusEnum.DISABLE }
      ]
    }
  },

  created() {
    ensureDictDatas(DICT_TYPE.COMMON_STATUS).finally(() => {
      this.getList()
    })
  },

  methods: {

    getList() {

      this.loading = true

      getSubSystemPage(this.queryParams).then(res => {

        this.systemList = res.data.list || []

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

        clientId: undefined,

        systemName: undefined,
        description: undefined,
        systemUrl: undefined,
        systemIcon: undefined,
        status: CommonStatusEnum.ENABLE

      }

      this.resetForm('form')

    },

    cancel() {

      this.open = false

      this.resetFormData()

    },

    handleAdd() {
      this.resetFormData()
      this.open = true
      this.title = '添加业务系统'
    },

    handleUpdate(row) {

      this.resetFormData()

      getSubSystem(row.id).then(res => {

        this.form = {

          id: res.data.id,
          clientId: res.data.clientId,
          systemName: res.data.systemName,
          description: res.data.description,
          systemUrl: res.data.systemUrl,
          systemIcon: res.data.systemIcon,
          status: res.data.status
        }
        this.open = true
        this.title = '修改业务系统'
      })

    },

    submitForm() {

      this.$refs.form.validate(valid => {

        if (!valid) {

          return

        }

        const request = this.form.id ? updateSubSystem : createSubSystem

        request(this.form).then(() => {

          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')

          this.open = false

          this.getList()

        })

      })

    },

    handleDelete(row) {

      this.$modal.confirm('是否确认删除系统"' + row.systemName + '"？').then(() => {

        return deleteSubSystem(row.id)

      }).then(() => {

        this.$modal.msgSuccess('删除成功')

        this.getList()

      }).catch(() => {})

    },

    handleDeleteBatch() {

      this.$modal.confirm('是否确认批量删除选中的业务系统？').then(() => {

        return deleteSubSystemList(this.checkedIds)

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

.client-cell__id {
  font-size: 12px;
  color: #909399;
}

.client-preview {

  display: flex;

  align-items: flex-start;

  padding: 10px 12px;

  background: #f5f7fa;

  border-radius: 6px;



  & > * + * {

    margin-left: 10px;

  }

  &__info {

    flex: 1;

    font-size: 14px;

    color: #303133;

  }



  &__desc {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }
}

.system-icon {
  border-radius: 4px;
  object-fit: cover;
}

.form-item-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>

