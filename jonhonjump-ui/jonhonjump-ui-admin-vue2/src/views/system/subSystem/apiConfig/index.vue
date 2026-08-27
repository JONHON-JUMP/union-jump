<template>
  <div class="app-container api-config-page">
    <div class="api-layout">
      <aside class="api-side">
        <el-button
          type="primary"
          plain
          size="mini"
          icon="el-icon-plus"
          class="api-side__add"
          @click="openAddDialog"
          v-hasPermi="['sub-system:apiconfig:create']"
        >接入业务系统</el-button>
        <el-input
          v-model="clientKeyword"
          placeholder="筛选"
          clearable
          size="small"
          prefix-icon="el-icon-search"
          class="api-side__search"
        />
        <div class="api-side__tree" v-loading="loading">
          <el-tree
            v-if="treeData.length"
            ref="tree"
            class="api-tree"
            :data="treeData"
            :props="{ children: 'children', label: 'label' }"
            node-key="nodeKey"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <span slot-scope="{ data }" class="tree-node">
              <span class="tree-node__main">
                <i :class="nodeIcon(data)" class="tree-node__icon" />
                <span class="tree-node__label">{{ data.label }}</span>
              </span>
              <el-tag v-if="data.type === 'api'" size="mini" :type="data.enabled === false ? 'info' : ''">
                {{ data.enabled === false ? '停用' : (data.method || 'API') }}
              </el-tag>
              <el-tag v-else-if="data.purposeHint" size="mini" type="warning">{{ data.purposeHint }}</el-tag>
            </span>
          </el-tree>
          <el-empty v-else description="点上方接入业务系统" :image-size="48" />
        </div>
      </aside>

      <main class="api-main">
        <div v-if="!currentNode" class="empty-hint">左侧选择目录或接口</div>

        <!-- 目录：像菜单一样可操作 -->
        <div v-else-if="currentNode.type === 'dir'" class="api-panel">
          <div class="panel-head">
            <div class="panel-head__title">
              <strong>{{ currentNode.label }}</strong>
              <span class="meta">目录</span>
            </div>
            <div class="panel-head__actions">
              <el-button size="mini" icon="el-icon-folder-add" @click="addChildDir"
                         v-hasPermi="['sub-system:apiconfig:update']">新增子目录</el-button>
              <el-button size="mini" type="primary" plain icon="el-icon-document-add" @click="addChildApi"
                         v-hasPermi="['sub-system:apiconfig:update']">新增接口</el-button>
              <el-button size="mini" @click="renameDir" v-if="currentNode.dirKind !== 'system'"
                         v-hasPermi="['sub-system:apiconfig:update']">重命名</el-button>
              <el-button size="mini" type="danger" plain @click="removeDir"
                         v-if="currentNode.dirKind !== 'system'"
                         v-hasPermi="['sub-system:apiconfig:update']">删除目录</el-button>
              <el-button size="mini" type="danger" plain @click="handleDeleteConfig"
                         v-if="currentNode.dirKind === 'system'"
                         v-hasPermi="['sub-system:apiconfig:delete']">取消接入</el-button>
              <el-button size="mini" type="primary" @click="persistCatalog"
                         v-hasPermi="['sub-system:apiconfig:update']">保存结构</el-button>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="目录只做分组。叶子接口可设置「用途」：用途=新增人员 时，用户管理勾选同步该业务系统会调用这条接口。"
          />
        </div>

        <!-- 叶子接口 -->
        <div v-else class="api-panel">
          <div class="panel-head">
            <div class="panel-head__title">
              <strong>{{ currentApiTitle }}</strong>
            </div>
            <div class="panel-head__actions">
              <el-button type="danger" plain size="mini" @click="removeApi"
                         v-hasPermi="['sub-system:apiconfig:update']">删除接口</el-button>
              <el-button type="primary" size="mini" @click="submitApi"
                         v-hasPermi="['sub-system:apiconfig:update']">保存</el-button>
              <el-button type="success" plain size="mini" :loading="testing" @click="handleTest"
                         v-hasPermi="['sub-system:apiconfig:list']">测试</el-button>
            </div>
          </div>
          <el-form label-width="100px" size="small">
            <el-form-item label="接口名称">
              <el-input v-model="editApi.name" placeholder="左侧树显示名称" maxlength="50" />
            </el-form-item>
            <el-form-item label="用途">
              <el-select v-model="editApi.purpose" style="width: 240px" placeholder="绑定业务动作">
                <el-option label="无（仅配置/测试）" value="" />
                <el-option label="鉴权登录" value="auth" />
                <el-option label="查询人员" value="query" />
                <el-option label="新增人员（用户同步用）" value="create" />
                <el-option label="修改人员" value="update" />
                <el-option label="删除人员" value="delete" />
              </el-select>
              <div class="form-tip" v-if="editApi.purpose === 'create'">用户管理 → 添加用户 → 同步业务系统，将调用本接口</div>
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="editApi.enabled" />
            </el-form-item>
            <el-form-item label="方法">
              <el-select v-model="editApi.method" style="width: 120px">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
                <el-option label="PUT" value="PUT" />
                <el-option label="DELETE" value="DELETE" />
              </el-select>
            </el-form-item>
            <el-form-item label="完整地址">
              <el-input v-model="editApi.url" placeholder="http://host/path" />
            </el-form-item>
            <template v-if="editApi.purpose === 'auth'">
              <el-form-item label="调用工号">
                <el-input v-model="editApi.userCode" placeholder="Camstar 调用账号" style="width: 240px" />
              </el-form-item>
              <el-form-item label="Cookie名">
                <el-input v-model="editApi.cookieName" style="width: 240px" />
              </el-form-item>
              <div class="form-tip">鉴权成功后生成 Cookie；下面「人员」增删改查调用/测试时会自动带上，无需在每条接口再配。</div>
            </template>
            <el-form-item v-else-if="isPersonPurpose" label="会话Cookie">
              <span class="sync-workshop-text" v-if="authLeafSummary">
                自动使用鉴权接口：{{ authLeafSummary }}
              </span>
              <span class="sync-workshop-text is-empty" v-else>未配置鉴权接口或未填调用工号，业务调用可能失败</span>
            </el-form-item>
            <el-form-item label="请求参数">
              <el-input v-model="testBody" type="textarea" :rows="10" />
            </el-form-item>
            <el-form-item label="响应">
              <el-input v-model="testResult" type="textarea" :rows="10" readonly placeholder="点测试后显示" />
            </el-form-item>
          </el-form>
        </div>
      </main>
    </div>

    <el-dialog title="接入业务系统" :visible.sync="addDialogVisible" width="480px" append-to-body>
      <el-form label-width="90px" size="small">
        <el-form-item label="业务系统" required>
          <el-select v-model="addSubSystemId" filterable style="width: 100%">
            <el-option
              v-for="item in availableClients"
              :key="item.id"
              :label="item.name + ' (' + item.clientId + ')'"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="适配器">
          <el-select v-model="addApiType" style="width: 100%">
            <el-option label="Camstar" value="camstar" />
            <el-option label="HTTP" value="http" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" required>
          <el-input v-model="addHost" placeholder="http://192.168.240.127:8090（生成默认接口完整地址）" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" :disabled="!canConfirmAdd" @click="confirmAddAccess">确定</el-button>
        <el-button @click="addDialogVisible = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createSubSystemApiConfig,
  deleteSubSystemApiConfig,
  getSubSystemApiConfigList,
  getSubSystemClientSimpleList,
  testSubSystemApiInvoke,
  updateSubSystemApiConfig
} from '@/api/system/subSystemApiConfig'

const PURPOSES = ['auth', 'query', 'create', 'update', 'delete']

const CAMSTAR_SAMPLES = {
  auth: { token: '' },
  query: { userCode: '', userName: '', workshopCode: '4200', page: 1, rows: 10 },
  create: [{
    userCode: '00078', userName: '张三', workshopCode: '4200',
    teamCode: '', domainName: '', erpNo: '', cardNo: ''
  }],
  update: [{
    userCode: '00078', userName: '张三', workshopCode: '4200',
    teamCode: '', domainName: '', erpNo: '', cardNo: ''
  }],
  delete: { userCode: '00078' }
}

function uid(prefix) {
  return prefix + '-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 7)
}

function joinUrl(host, path) {
  const base = String(host || '').replace(/\/+$/, '')
  const p = String(path || '')
  if (!base) return p
  if (!p) return base
  return base + (p.startsWith('/') ? p : '/' + p)
}

function parseJsonSafe(json, fallback) {
  if (!json) return fallback
  try {
    return typeof json === 'string' ? JSON.parse(json) : json
  } catch (e) {
    return fallback
  }
}

function endpointFromField(json, defaults) {
  const d = defaults || {}
  const obj = parseJsonSafe(json, {}) || {}
  return {
    url: String(obj.url || obj.path || obj.loginPath || d.url || '').trim(),
    method: String(obj.method || d.method || 'POST').toUpperCase(),
    name: obj.name || d.name || '',
    enabled: obj.enabled === false ? false : true,
    userCode: obj.userCode || '',
    cookieName: obj.cookieName || 'Nancal_Cam_SessionId'
  }
}

/** 无 catalog 时，用旧字段拼默认菜单树 */
function buildDefaultCatalog(config, host) {
  const h = host || (config && config.baseUrl) || ''
  const auth = endpointFromField(config && config.authConfig, {
    name: 'SSO登录', method: 'GET', url: joinUrl(h, '/Base/SSOLogin/SSOLoginIn')
  })
  const query = endpointFromField(config && config.apiQuery, {
    name: '查询', method: 'POST', url: joinUrl(h, '/BasicData/Employee/getEmployeeInfo')
  })
  const create = endpointFromField(config && config.apiCreate, {
    name: '新增', method: 'POST', url: joinUrl(h, '/BasicData/Employee/addOrUpdateUser')
  })
  const update = endpointFromField(config && config.apiUpdate, {
    name: '修改', method: 'POST', url: joinUrl(h, '/BasicData/Employee/addOrUpdateUser')
  })
  const del = endpointFromField(config && config.apiDelete, {
    name: '删除', method: 'POST', url: joinUrl(h, '/BasicData/Employee/deleteEmployeeInfo')
  })
  return {
    nodes: [
      {
        id: 'dir-auth',
        type: 'dir',
        name: '鉴权',
        children: [{
          id: 'api-auth',
          type: 'api',
          name: auth.name || 'SSO登录',
          purpose: 'auth',
          method: auth.method,
          url: auth.url,
          enabled: auth.enabled,
          userCode: auth.userCode,
          cookieName: auth.cookieName || 'Nancal_Cam_SessionId'
        }]
      },
      {
        id: 'dir-person',
        type: 'dir',
        name: '人员',
        children: [
          { id: 'api-query', type: 'api', name: query.name || '查询', purpose: 'query', method: query.method, url: query.url, enabled: query.enabled },
          { id: 'api-create', type: 'api', name: create.name || '新增', purpose: 'create', method: create.method, url: create.url, enabled: create.enabled },
          { id: 'api-update', type: 'api', name: update.name || '修改', purpose: 'update', method: update.method, url: update.url, enabled: update.enabled },
          { id: 'api-delete', type: 'api', name: del.name || '删除', purpose: 'delete', method: del.method, url: del.url, enabled: del.enabled }
        ]
      }
    ]
  }
}

function normalizeCatalog(raw, config) {
  const parsed = parseJsonSafe(raw, null)
  if (parsed && Array.isArray(parsed.nodes)) {
    return parsed
  }
  return buildDefaultCatalog(config || {}, (config && config.baseUrl) || '')
}

function walkCatalog(nodes, fn) {
  ;(nodes || []).forEach(n => {
    fn(n)
    if (n.children) walkCatalog(n.children, fn)
  })
}

function findInCatalog(nodes, id) {
  let found = null
  walkCatalog(nodes, n => {
    if (n.id === id) found = n
  })
  return found
}

function findParent(nodes, id, parent) {
  for (let i = 0; i < (nodes || []).length; i++) {
    const n = nodes[i]
    if (n.id === id) return parent
    const p = findParent(n.children, id, n)
    if (p) return p
  }
  return null
}

function findByPurpose(nodes, purpose) {
  let found = null
  walkCatalog(nodes, n => {
    if (n.type === 'api' && n.purpose === purpose) found = n
  })
  return found
}

function stringifyEndpoint(api) {
  if (!api || !String(api.url || '').trim()) return ''
  const payload = {
    url: String(api.url).trim(),
    method: (api.method || 'POST').toUpperCase(),
    enabled: api.enabled !== false,
    name: api.name || ''
  }
  return JSON.stringify(payload)
}

function stringifyAuth(api) {
  if (!api) return ''
  return JSON.stringify({
    name: api.name || 'SSO登录',
    url: String(api.url || '').trim(),
    method: (api.method || 'GET').toUpperCase(),
    enabled: api.enabled !== false,
    userCode: api.userCode || '',
    cookieName: api.cookieName || 'Nancal_Cam_SessionId'
  })
}

export default {
  name: 'SubSystemApiConfig',
  data() {
    return {
      loading: false,
      testing: false,
      clientList: [],
      configMap: {},
      /** subSystemId → catalog { nodes: [] }，内存编辑 */
      catalogMap: {},
      clientKeyword: '',
      currentNode: null,
      editApi: {},
      testBody: '',
      testResult: '',
      addDialogVisible: false,
      addSubSystemId: undefined,
      addApiType: 'camstar',
      addHost: '',
      formMeta: {}
    }
  },
  computed: {
    accessList() {
      const keyword = (this.clientKeyword || '').trim().toLowerCase()
      return (this.clientList || []).filter(item => {
        if (!this.configMap[item.id]) return false
        if (!keyword) return true
        return (item.name && item.name.toLowerCase().includes(keyword))
          || (item.clientId && item.clientId.toLowerCase().includes(keyword))
      })
    },
    treeData() {
      return this.accessList.map(item => {
        const catalog = this.catalogMap[item.id] || { nodes: [] }
        const mapNode = (n) => {
          if (n.type === 'dir') {
            return {
              nodeKey: 'sys-' + item.id + '-' + n.id,
              type: 'dir',
              dirKind: 'category',
              catalogId: n.id,
              label: n.name,
              subSystemId: item.id,
              systemName: item.name,
              children: (n.children || []).map(mapNode)
            }
          }
          return {
            nodeKey: 'sys-' + item.id + '-' + n.id,
            type: 'api',
            catalogId: n.id,
            label: n.name,
            method: n.method,
            enabled: n.enabled !== false,
            purpose: n.purpose || '',
            purposeHint: n.purpose === 'create' ? '同步' : '',
            subSystemId: item.id,
            systemName: item.name
          }
        }
        return {
          nodeKey: 'sys-' + item.id,
          type: 'dir',
          dirKind: 'system',
          catalogId: null,
          label: item.name,
          id: item.id,
          subSystemId: item.id,
          systemName: item.name,
          children: (catalog.nodes || []).map(mapNode)
        }
      })
    },
    availableClients() {
      return (this.clientList || []).filter(item => !this.configMap[item.id])
    },
    canConfirmAdd() {
      return !!this.addSubSystemId && !!(this.addHost || '').trim()
    },
    currentApiTitle() {
      if (!this.currentNode || this.currentNode.type !== 'api') return ''
      const name = (this.editApi && this.editApi.name) || this.currentNode.label
      return (this.currentNode.systemName || '') + ' / ' + name
    },
    /** 人员增删改查：Cookie 从鉴权叶子自动带，不在本页重复配置 */
    isPersonPurpose() {
      return ['query', 'create', 'update', 'delete'].indexOf(this.editApi && this.editApi.purpose) >= 0
    },
    authLeafSummary() {
      if (!this.currentNode || !this.currentNode.subSystemId) return ''
      const catalog = this.catalogMap[this.currentNode.subSystemId]
      if (!catalog) return ''
      const auth = findByPurpose(catalog.nodes, 'auth')
      if (!auth || !auth.userCode) return ''
      return (auth.name || 'SSO登录') + ' / 工号 ' + auth.userCode + ' / ' + (auth.cookieName || 'Nancal_Cam_SessionId')
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    nodeIcon(data) {
      return data.type === 'api' ? 'el-icon-document' : 'el-icon-folder-opened'
    },
    loadAll() {
      this.loading = true
      const keepKey = this.currentNode && this.currentNode.nodeKey
      return Promise.all([getSubSystemClientSimpleList(), getSubSystemApiConfigList()]).then(([clients, configs]) => {
        this.clientList = clients.data || []
        this.configMap = {}
        this.catalogMap = {}
        ;(configs.data || []).forEach(c => {
          this.configMap[c.subSystemId] = c
          this.$set(this.catalogMap, c.subSystemId, normalizeCatalog(c.apiCatalog, c))
        })
        this.$nextTick(() => {
          const keep = this.findTreeNode(keepKey)
          const firstApi = this.firstApiNode()
          const first = keep || firstApi
          if (first) {
            this.handleNodeClick(first)
            if (this.$refs.tree) this.$refs.tree.setCurrentKey(first.nodeKey)
          } else {
            this.currentNode = null
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    findTreeNode(key) {
      if (!key) return null
      let found = null
      const walk = (nodes) => {
        ;(nodes || []).forEach(n => {
          if (n.nodeKey === key) found = n
          walk(n.children)
        })
      }
      walk(this.treeData)
      return found
    },
    firstApiNode() {
      let found = null
      const walk = (nodes) => {
        ;(nodes || []).forEach(n => {
          if (!found && n.type === 'api') found = n
          walk(n.children)
        })
      }
      walk(this.treeData)
      return found
    },
    applyMeta(subSystemId) {
      const config = this.configMap[subSystemId]
      if (!config) return
      this.formMeta = {
        id: config.id,
        subSystemId: config.subSystemId,
        apiType: config.apiType || 'camstar',
        baseUrl: config.baseUrl || '',
        authType: config.authType || 'none',
        paramMapping: config.paramMapping || '',
        responseMapping: config.responseMapping || '',
        deleteTip: config.deleteTip || '',
        connectTimeoutMs: config.connectTimeoutMs || 10000,
        readTimeoutMs: config.readTimeoutMs || 30000,
        apiTeamCombo: config.apiTeamCombo || '',
        status: 0
      }
    },
    handleNodeClick(data) {
      if (!data) return
      this.applyMeta(data.subSystemId)
      this.currentNode = data
      this.testResult = ''
      if (data.type === 'api') {
        const catalog = this.catalogMap[data.subSystemId]
        const node = findInCatalog(catalog.nodes, data.catalogId) || {}
        this.editApi = {
          id: node.id,
          name: node.name || '',
          purpose: node.purpose || '',
          method: (node.method || 'POST').toUpperCase(),
          url: node.url || '',
          enabled: node.enabled !== false,
          userCode: node.userCode || '',
          cookieName: node.cookieName || 'Nancal_Cam_SessionId'
        }
        const sample = CAMSTAR_SAMPLES[node.purpose] || {}
        this.testBody = JSON.stringify(sample, null, 2)
      }
    },
    ensureUniquePurpose(nodes, purpose, exceptId) {
      if (!purpose) return
      walkCatalog(nodes, n => {
        if (n.type === 'api' && n.purpose === purpose && n.id !== exceptId) {
          n.purpose = ''
        }
      })
    },
    addChildDir() {
      if (!this.currentNode || this.currentNode.type !== 'dir') return
      this.$prompt('目录名称', '新增子目录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入名称'
      }).then(({ value }) => {
        const sid = this.currentNode.subSystemId
        const catalog = this.catalogMap[sid]
        const node = { id: uid('dir'), type: 'dir', name: value.trim(), children: [] }
        if (this.currentNode.dirKind === 'system') {
          catalog.nodes.push(node)
        } else {
          const parent = findInCatalog(catalog.nodes, this.currentNode.catalogId)
          if (!parent) return
          if (!parent.children) parent.children = []
          parent.children.push(node)
        }
        this.$forceUpdate()
        this.persistCatalog()
      }).catch(() => {})
    },
    addChildApi() {
      if (!this.currentNode || this.currentNode.type !== 'dir') return
      if (this.currentNode.dirKind === 'system') {
        this.$modal.msgWarning('请先在业务系统下新增目录，再在目录下新增接口')
        return
      }
      this.$prompt('接口名称', '新增接口', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入名称'
      }).then(({ value }) => {
        const sid = this.currentNode.subSystemId
        const catalog = this.catalogMap[sid]
        const parent = findInCatalog(catalog.nodes, this.currentNode.catalogId)
        if (!parent) return
        if (!parent.children) parent.children = []
        const node = {
          id: uid('api'),
          type: 'api',
          name: value.trim(),
          purpose: '',
          method: 'POST',
          url: '',
          enabled: true
        }
        parent.children.push(node)
        this.persistCatalog().then(() => {
          this.$nextTick(() => {
            const treeNode = this.findTreeNode('sys-' + sid + '-' + node.id)
            if (treeNode) {
              this.handleNodeClick(treeNode)
              if (this.$refs.tree) this.$refs.tree.setCurrentKey(treeNode.nodeKey)
            }
          })
        })
      }).catch(() => {})
    },
    renameDir() {
      if (!this.currentNode || this.currentNode.dirKind === 'system') return
      this.$prompt('目录名称', '重命名', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: this.currentNode.label,
        inputPattern: /\S+/,
        inputErrorMessage: '请输入名称'
      }).then(({ value }) => {
        const catalog = this.catalogMap[this.currentNode.subSystemId]
        const node = findInCatalog(catalog.nodes, this.currentNode.catalogId)
        if (node) {
          node.name = value.trim()
          this.currentNode.label = node.name
          this.persistCatalog()
        }
      }).catch(() => {})
    },
    removeDir() {
      if (!this.currentNode || this.currentNode.dirKind === 'system') return
      const catalog = this.catalogMap[this.currentNode.subSystemId]
      const node = findInCatalog(catalog.nodes, this.currentNode.catalogId)
      if (node && node.children && node.children.length) {
        this.$modal.msgWarning('请先删除目录下的子项')
        return
      }
      this.$modal.confirm('确认删除该目录？').then(() => {
        const parent = findParent(catalog.nodes, this.currentNode.catalogId, null)
        const list = parent ? parent.children : catalog.nodes
        const idx = list.findIndex(n => n.id === this.currentNode.catalogId)
        if (idx >= 0) list.splice(idx, 1)
        this.currentNode = null
        this.persistCatalog()
      }).catch(() => {})
    },
    removeApi() {
      if (!this.currentNode || this.currentNode.type !== 'api') return
      this.$modal.confirm('确认删除该接口？').then(() => {
        const sid = this.currentNode.subSystemId
        const catalog = this.catalogMap[sid]
        const parent = findParent(catalog.nodes, this.currentNode.catalogId, null)
        if (!parent || !parent.children) return
        const idx = parent.children.findIndex(n => n.id === this.currentNode.catalogId)
        if (idx >= 0) parent.children.splice(idx, 1)
        this.currentNode = null
        this.persistCatalog()
      }).catch(() => {})
    },
    applyEditToCatalog() {
      if (!this.currentNode || this.currentNode.type !== 'api') return
      const catalog = this.catalogMap[this.currentNode.subSystemId]
      const node = findInCatalog(catalog.nodes, this.currentNode.catalogId)
      if (!node) return
      const purpose = this.editApi.purpose || ''
      this.ensureUniquePurpose(catalog.nodes, purpose, node.id)
      node.name = this.editApi.name || node.name
      node.purpose = purpose
      node.method = (this.editApi.method || 'POST').toUpperCase()
      node.url = String(this.editApi.url || '').trim()
      node.enabled = this.editApi.enabled !== false
      if (purpose === 'auth') {
        node.userCode = this.editApi.userCode || ''
        node.cookieName = this.editApi.cookieName || 'Nancal_Cam_SessionId'
      }
    },
    buildPayload(subSystemId) {
      const config = this.configMap[subSystemId]
      const catalog = this.catalogMap[subSystemId] || { nodes: [] }
      const auth = findByPurpose(catalog.nodes, 'auth')
      const query = findByPurpose(catalog.nodes, 'query')
      const create = findByPurpose(catalog.nodes, 'create')
      const update = findByPurpose(catalog.nodes, 'update')
      const del = findByPurpose(catalog.nodes, 'delete')
      let baseUrl = (this.formMeta && this.formMeta.baseUrl) || (config && config.baseUrl) || ''
      const urls = [auth, query, create].filter(Boolean).map(a => a.url)
      for (let i = 0; i < urls.length; i++) {
        const m = String(urls[i] || '').match(/^(https?:\/\/[^/]+)/i)
        if (m) {
          baseUrl = m[1]
          break
        }
      }
      return {
        id: config.id,
        subSystemId,
        apiType: (config.apiType || 'camstar'),
        baseUrl: baseUrl || 'http://127.0.0.1',
        authType: auth ? 'cookie_sso' : (config.authType || 'none'),
        authConfig: auth ? stringifyAuth(auth) : (config.authConfig || ''),
        apiQuery: stringifyEndpoint(query),
        apiCreate: stringifyEndpoint(create),
        apiUpdate: stringifyEndpoint(update),
        apiDelete: stringifyEndpoint(del),
        apiTeamCombo: config.apiTeamCombo || '',
        apiCatalog: JSON.stringify(catalog),
        paramMapping: config.paramMapping || '',
        responseMapping: config.responseMapping || '',
        deleteTip: config.deleteTip || '',
        connectTimeoutMs: config.connectTimeoutMs || 10000,
        readTimeoutMs: config.readTimeoutMs || 30000,
        status: 0
      }
    },
    persistCatalog() {
      const sid = (this.currentNode && this.currentNode.subSystemId)
        || (this.formMeta && this.formMeta.subSystemId)
      if (!sid || !this.configMap[sid]) {
        return Promise.resolve()
      }
      const payload = this.buildPayload(sid)
      return updateSubSystemApiConfig(payload).then(() => {
        this.$modal.msgSuccess('已保存')
        return this.loadAll()
      })
    },
    submitApi() {
      if (!this.currentNode || this.currentNode.type !== 'api') return
      if (!String(this.editApi.url || '').trim()) {
        this.$modal.msgWarning('请填写完整地址')
        return
      }
      this.applyEditToCatalog()
      this.persistCatalog()
    },
    handleTest() {
      if (!this.formMeta.id) {
        this.$modal.msgWarning('请先保存')
        return
      }
      if (!this.currentNode || this.currentNode.type !== 'api') return
      try {
        JSON.parse(this.testBody || '{}')
      } catch (e) {
        this.$modal.msgWarning('请求参数不是合法 JSON')
        return
      }
      this.applyEditToCatalog()
      const purpose = this.editApi.purpose
      if (!purpose || PURPOSES.indexOf(purpose) < 0) {
        this.$modal.msgWarning('测试需先设置用途（鉴权/查询/新增/修改/删除）')
        return
      }
      this.testing = true
      this.testResult = ''
      const payload = this.buildPayload(this.currentNode.subSystemId)
      updateSubSystemApiConfig(payload).then(() => {
        return testSubSystemApiInvoke({
          id: this.formMeta.id,
          apiKey: purpose,
          requestBody: this.testBody
        })
      }).then(res => {
        const data = res.data || {}
        this.testResult = [
          (data.method || '') + ' ' + (data.url || ''),
          data.success === false ? '失败' : '成功',
          data.responseBody || ''
        ].join('\n')
        this.loadAll()
      }).catch(err => {
        this.testResult = (err && (err.msg || err.message)) || String(err)
      }).finally(() => {
        this.testing = false
      })
    },
    openAddDialog() {
      this.addSubSystemId = undefined
      this.addApiType = 'camstar'
      this.addHost = ''
      this.addDialogVisible = true
    },
    confirmAddAccess() {
      if (!this.canConfirmAdd) return
      const host = this.addHost.trim().replace(/\/+$/, '')
      const catalog = buildDefaultCatalog({ baseUrl: host }, host)
      createSubSystemApiConfig({
        subSystemId: this.addSubSystemId,
        baseUrl: host,
        connectTimeoutMs: 10000,
        readTimeoutMs: 30000,
        apiType: this.addApiType,
        authType: this.addApiType === 'camstar' ? 'cookie_sso' : 'none',
        authConfig: stringifyAuth(findByPurpose(catalog.nodes, 'auth')),
        apiQuery: stringifyEndpoint(findByPurpose(catalog.nodes, 'query')),
        apiCreate: stringifyEndpoint(findByPurpose(catalog.nodes, 'create')),
        apiUpdate: stringifyEndpoint(findByPurpose(catalog.nodes, 'update')),
        apiDelete: stringifyEndpoint(findByPurpose(catalog.nodes, 'delete')),
        apiTeamCombo: '',
        apiCatalog: JSON.stringify(catalog),
        deleteTip: this.addApiType === 'camstar' ? '删除将同时删除该用户在 Camstar 的域账号，不可恢复！' : '',
        status: 0
      }).then(() => {
        this.$modal.msgSuccess('已接入')
        this.addDialogVisible = false
        this.loadAll()
      })
    },
    handleDeleteConfig() {
      if (!this.formMeta.id) return
      this.$modal.confirm('取消接入？将移除该业务系统下全部分类与接口').then(() => {
        return deleteSubSystemApiConfig(this.formMeta.id)
      }).then(() => {
        this.$modal.msgSuccess('已取消')
        this.currentNode = null
        this.loadAll()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.api-config-page {
  height: calc(100vh - 120px);
  min-height: 480px;
  box-sizing: border-box;
}
.api-layout {
  display: flex;
  height: 100%;
  gap: 12px;
}
.api-side {
  width: 280px;
  flex: 0 0 280px;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
  background: #fff;
  box-sizing: border-box;
}
.api-side__add {
  width: 100%;
  margin-bottom: 8px;
}
.api-side__search {
  margin-bottom: 8px;
}
.api-side__tree {
  flex: 1;
  overflow: auto;
  min-height: 200px;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
  font-size: 13px;
}
.tree-node__main {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
}
.tree-node__icon {
  margin-right: 6px;
  color: #909399;
  flex-shrink: 0;
}
.tree-node__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 6px;
}
.api-tree ::v-deep .el-tree-node__content {
  height: 34px;
}
.api-main {
  flex: 1;
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
  background: #fff;
  overflow: auto;
  box-sizing: border-box;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 8px;
}
.panel-head__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.panel-head__title .meta {
  color: #909399;
  font-weight: normal;
  font-size: 13px;
}
.panel-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.empty-hint {
  color: #909399;
  padding: 80px 0;
  text-align: center;
}
.form-tip {
  color: #e6a23c;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
.auth-tip {
  margin: 0 0 16px 0;
}
.sync-workshop-text {
  color: #303133;
  line-height: 32px;
}
.sync-workshop-text.is-empty {
  color: #f56c6c;
}
</style>
