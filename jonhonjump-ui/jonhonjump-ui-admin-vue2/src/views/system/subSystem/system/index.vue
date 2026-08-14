<template>

  <div class="app-container">

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">

      <el-form-item label="系统名称" prop="systemName">

        <el-input v-model="queryParams.systemName" placeholder="请输入系统名称" clearable style="width: 240px"

                  @keyup.enter.native="handleQuery"/>

      </el-form-item>

      <el-form-item label="客户端编号" prop="clientId">

        <el-input v-model="queryParams.clientId" placeholder="请输入 OAuth2 客户端编号" clearable style="width: 240px"

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

      <el-table-column label="系统编号" align="center" prop="id" width="90" />
      <el-table-column label="系统图标" align="center" width="80">
        <template v-slot="scope">
          <img v-if="scope.row.systemIcon" width="40" height="40" :src="scope.row.systemIcon" class="system-icon">
          <el-avatar v-else :size="40" icon="el-icon-picture-outline" />
        </template>
      </el-table-column>
      <el-table-column label="OAuth2 客户端" align="center" min-width="160">
        <template v-slot="scope">
          <div>{{ scope.row.clientName || '-' }}</div>
          <div class="client-cell__id">{{ scope.row.clientId }}</div>
        </template>
      </el-table-column>

      <el-table-column label="系统名称" align="center" prop="systemName" :show-overflow-tooltip="true" />

      <el-table-column label="访问地址" align="center" prop="systemUrl" :show-overflow-tooltip="true" min-width="160" />

      <el-table-column label="系统状态" align="center" prop="status" width="100">

        <template v-slot="scope">

          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>

        </template>

      </el-table-column>

      <el-table-column label="客户端状态" align="center" prop="clientStatus" width="100">

        <template v-slot="scope">

          <dict-tag v-if="scope.row.clientStatus != null" :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.clientStatus"/>

          <span v-else>-</span>

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

        <el-form-item label="OAuth2 客户端" prop="oauth2ClientId">

          <el-select

            v-model="form.oauth2ClientId"

            placeholder="请选择 OAuth2 客户端"

            filterable

            style="width: 100%"

            @change="handleClientChange"

          >

            <el-option

              v-for="item in availableClientOptions"

              :key="item.id"

              :label="item.name + ' (' + item.clientId + ')'"

              :value="item.id"

              :disabled="item.bound && item.id !== form.oauth2ClientId"

            />

          </el-select>

        </el-form-item>

        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="form.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="系统图标" prop="systemIcon">
          <imageUpload v-model="form.systemIcon" :limit="1"/>
        </el-form-item>
        <el-form-item label="系统描述" prop="description">

          <el-input v-model="form.description" type="textarea" placeholder="请输入系统描述" :rows="3" />

        </el-form-item>

        <el-form-item label="访问地址" prop="systemUrl">

          <el-input v-model="form.systemUrl" placeholder="MES 入口，如 http://192.168.240.127:4221" />
          <div style="line-height: 18px; margin-top: 4px; color: #909399; font-size: 12px;">
            填老 MES（4221）入口即可，可与门户跨域。不要填 Camstar（4200）；Camstar 由 MES 菜单内链打开。
          </div>

        </el-form-item>

        <el-form-item label="系统状态" prop="status">

          <el-radio-group v-model="form.status">

            <el-radio v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">

              {{ dict.label }}

            </el-radio>

          </el-radio-group>

        </el-form-item>

        <el-form-item v-if="selectedClientInfo" label="客户端信息">

          <div class="client-preview">

            <el-avatar v-if="selectedClientInfo.logo" :size="36" :src="selectedClientInfo.logo" />

            <el-avatar v-else :size="36" icon="el-icon-connection" />

            <div class="client-preview__info">

              <div>{{ selectedClientInfo.name }}</div>

              <div class="client-preview__desc">{{ selectedClientInfo.description || '暂无描述' }}</div>

            </div>

          </div>

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

  getSubSystemOAuth2ClientSimpleList,

  getSubSystemPage,

  updateSubSystem

} from '@/api/system/subSystem'

import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'
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

      clientOptions: [],

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

        oauth2ClientId: [{ required: true, message: 'OAuth2 客户端不能为空', trigger: 'change' }],

        systemName: [{ required: true, message: '系统名称不能为空', trigger: 'blur' }],

        status: [{ required: true, message: '状态不能为空', trigger: 'change' }]

      },

      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS)

    }

  },

  computed: {

    availableClientOptions() {

      return this.clientOptions

    },

    selectedClientInfo() {

      if (!this.form.oauth2ClientId) {

        return null

      }

      return this.clientOptions.find(item => item.id === this.form.oauth2ClientId) || null

    }

  },

  created() {

    this.getList()

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

    loadClientOptions(excludeSubSystemId) {

      return getSubSystemOAuth2ClientSimpleList(excludeSubSystemId).then(res => {

        this.clientOptions = res.data || []

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

        oauth2ClientId: undefined,

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

      this.loadClientOptions().then(() => {

        this.open = true

        this.title = '添加外部系统'

      })

    },

    handleUpdate(row) {

      this.resetFormData()

      getSubSystem(row.id).then(res => {

        this.form = {

          id: res.data.id,

          oauth2ClientId: res.data.oauth2ClientId,

          systemName: res.data.systemName,
          description: res.data.description,
          systemUrl: res.data.systemUrl,
          systemIcon: res.data.systemIcon,
          status: res.data.status

        }

        return this.loadClientOptions(res.data.id)

      }).then(() => {

        this.open = true

        this.title = '修改外部系统'

      })

    },

    handleClientChange(oauth2ClientId) {

      const client = this.clientOptions.find(item => item.id === oauth2ClientId)

      if (client && !this.form.systemName) {

        this.form.systemName = client.name

      }

      if (client && !this.form.description && client.description) {

        this.form.description = client.description

      }

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

      this.$modal.confirm('是否确认批量删除选中的外部系统？').then(() => {

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
</style>

