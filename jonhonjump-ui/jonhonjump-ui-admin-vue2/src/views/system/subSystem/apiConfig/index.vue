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
              <el-tag size="mini" :type="configMap[item.id] ? 'success' : 'info'">
                {{ configMap[item.id] ? configMap[item.id].apiType : '未配置' }}
              </el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>
      <!-- 配置表单 -->
      <el-col :span="20" :xs="24">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择外部系统；人员管理页只显示已配置且启用的系统"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form v-if="selectedClient" ref="form" :model="form" :rules="rules" label-width="130px" size="small">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="适配器类型" prop="apiType">
                <el-select v-model="form.apiType" placeholder="请选择适配器" style="width: 100%">
                  <el-option label="Camstar 专用（Cookie 会话）" value="camstar" />
                  <el-option label="通用 HTTP（配置驱动）" value="http" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="接口基地址" prop="baseUrl">
                <el-input v-model="form.baseUrl" placeholder="如 http://127.0.0.1:8090" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="鉴权方式" prop="authType">
                <el-select v-model="form.authType" style="width: 100%">
                  <el-option label="无鉴权" value="none" />
                  <el-option label="Cookie 会话（SSO 登录）" value="cookie_sso" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :label="0">启用</el-radio>
                  <el-radio :label="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="鉴权配置 JSON" prop="authConfig">
                <el-input v-model="form.authConfig" type="textarea" :rows="2"
                          placeholder='Camstar 示例：{"userCode":"00078","cookieName":"Nancal_Cam_SessionId","loginPath":"/Base/SSOLogin/SSOLoginIn"}' />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="查询接口 JSON" prop="apiQuery">
                <el-input v-model="form.apiQuery" placeholder='{"path":"/BasicData/Employee/getEmployeeInfo","method":"POST"}' />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="班组下拉接口 JSON" prop="apiTeamCombo">
                <el-input v-model="form.apiTeamCombo" placeholder='{"path":"/BasicData/Team/getTeamComboByFactory","method":"GET"}' />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新增接口 JSON" prop="apiCreate">
                <el-input v-model="form.apiCreate" placeholder='{"path":"/BasicData/Employee/addOrUpdateUser","method":"POST"}' />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="修改接口 JSON" prop="apiUpdate">
                <el-input v-model="form.apiUpdate" placeholder="可与新增相同（upsert）" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="删除接口 JSON" prop="apiDelete">
                <el-input v-model="form.apiDelete" placeholder='{"path":"/BasicData/Employee/deleteEmployeeInfo","method":"POST"}' />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="删除确认提示语" prop="deleteTip">
                <el-input v-model="form.deleteTip" placeholder="如：删除将同时删除该用户的域账号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="连接超时(毫秒)" prop="connectTimeoutMs">
                <el-input-number v-model="form.connectTimeoutMs" :min="1000" :max="120000" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="读取超时(毫秒)" prop="readTimeoutMs">
                <el-input-number v-model="form.readTimeoutMs" :min="1000" :max="300000" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="参数映射 JSON" prop="paramMapping">
                <el-input v-model="form.paramMapping" type="textarea" :rows="2"
                          placeholder='通用HTTP适配器用：JUMP标准参数名→对方参数名，如 {"userCode":"empNo","page":"pageNo","rows":"pageSize"}' />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="响应映射 JSON" prop="responseMapping">
                <el-input v-model="form.responseMapping" type="textarea" :rows="3"
                          placeholder='通用HTTP适配器用：{"successField":"code","successValue":200,"listPath":"data.list","totalPath":"data.total","fields":{"userCode":"empNo"}}' />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <el-row v-if="selectedClient" :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="el-icon-check" size="mini" @click="submitForm"
                       v-hasPermi="['sub-system:apiconfig:update']">保存配置</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="el-icon-connection" size="mini" @click="handleTestConnection"
                       v-hasPermi="['sub-system:apiconfig:list']" :disabled="!form.id">测试连接</el-button>
          </el-col>
          <el-col :span="1.5" v-if="form.id">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" @click="handleDeleteConfig"
                       v-hasPermi="['sub-system:apiconfig:delete']">删除配置</el-button>
          </el-col>
        </el-row>
        <div v-if="selectedClient && form.id" class="tip-line">
          提示：保存后适配器实例（含 Cookie 会话）会重建；测试连接会真实调用一次目标系统查询接口。
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script>
import {
  createSubSystemApiConfig,
  deleteSubSystemApiConfig,
  getSubSystemApiConfigList,
  getSubSystemClientSimpleList,
  testSubSystemApiConnection,
  updateSubSystemApiConfig
} from '@/api/system/subSystemApiConfig'
import subSystemImportGate from '@/utils/subSystemImportGate'
export default {
  name: 'SubSystemApiConfig',
  mixins: [subSystemImportGate],
  data() {
    return {
      loading: false,
      clientList: [],
      configMap: {},
      clientKeyword: '',
      selectedClient: null,
      form: {},
      rules: {
        apiType: [{ required: true, message: '适配器类型不能为空', trigger: 'change' }],
        baseUrl: [{ required: true, message: '接口基地址不能为空', trigger: 'blur' }]
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
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.withClientsLoading(() => {
        return Promise.all([getSubSystemClientSimpleList(), getSubSystemApiConfigList()]).then(([clients, configs]) => {
          this.clientList = clients.data || []
          this.configMap = {}
          ;(configs.data || []).forEach(c => {
            this.configMap[c.subSystemId] = c
          })
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
      const config = this.configMap[item.id]
      if (config) {
        this.form = {
          id: config.id,
          subSystemId: config.subSystemId,
          apiType: config.apiType,
          baseUrl: config.baseUrl,
          authType: config.authType,
          authConfig: config.authConfig,
          apiQuery: config.apiQuery,
          apiCreate: config.apiCreate,
          apiUpdate: config.apiUpdate,
          apiDelete: config.apiDelete,
          apiTeamCombo: config.apiTeamCombo,
          paramMapping: config.paramMapping,
          responseMapping: config.responseMapping,
          deleteTip: config.deleteTip,
          connectTimeoutMs: config.connectTimeoutMs || 10000,
          readTimeoutMs: config.readTimeoutMs || 30000,
          status: config.status == null ? 0 : config.status
        }
      } else {
        this.form = {
          id: undefined,
          subSystemId: item.id,
          apiType: 'http',
          baseUrl: '',
          authType: 'none',
          authConfig: '',
          apiQuery: '',
          apiCreate: '',
          apiUpdate: '',
          apiDelete: '',
          apiTeamCombo: '',
          paramMapping: '',
          responseMapping: '',
          deleteTip: '',
          connectTimeoutMs: 10000,
          readTimeoutMs: 30000,
          status: 1
        }
      }
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const promise = this.form.id
          ? updateSubSystemApiConfig(this.form)
          : createSubSystemApiConfig(this.form)
        promise.then(res => {
          this.$modal.msgSuccess('保存成功')
          this.form.id = this.form.id || res.data
          this.loadAll()
        })
      })
    },
    handleTestConnection() {
      if (!this.form.id) {
        this.$modal.msgWarning('请先保存配置再进行测试')
        return
      }
      this.loading = true
      testSubSystemApiConnection(this.form.id).then(res => {
        this.$modal.alert(res.data || '连接成功', '测试结果')
      }).finally(() => {
        this.loading = false
      })
    },
    handleDeleteConfig() {
      this.$modal.confirm('是否确认删除该系统的人员接口配置？删除后人员管理页将不再显示该系统。').then(() => {
        return deleteSubSystemApiConfig(this.form.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.configMap = {}
        this.handleClientClick(this.selectedClient)
        this.loadAll()
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.tip-line {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
