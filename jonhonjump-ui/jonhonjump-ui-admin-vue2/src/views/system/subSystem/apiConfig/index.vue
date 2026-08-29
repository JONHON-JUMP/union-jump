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
        >接入系统</el-button>
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
          <el-empty v-else description="点上方接入系统" :image-size="48" />
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
              <el-button size="mini" @click="renameSystem" v-if="currentNode.dirKind === 'system'"
                         v-hasPermi="['sub-system:apiconfig:update']">重命名系统</el-button>
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

          <!-- 会话鉴权（系统级，一次配置全系统接口共用；树上不再有独立鉴权接口） -->
          <el-card v-if="currentNode.dirKind === 'system'" shadow="never" class="session-card">
            <div slot="header">
              <strong>会话鉴权（Cookie）</strong>
              <span class="form-tip" style="margin-left:8px">登录一次生成 Cookie，本系统接口调用/测试时自动携带；个别不需要的接口可在其表单里关闭「携带会话 Cookie」</span>
            </div>
            <el-form label-width="100px" size="small">
              <el-form-item label="启用会话">
                <el-switch v-model="sessionForm.enabled" />
              </el-form-item>
              <template v-if="sessionForm.enabled">
                <el-form-item label="登录地址">
                  <el-input
                    v-model="sessionForm.url"
                    placeholder="如 http://192.168.240.125:8888/Base/SSOLogin/SSOLoginIn"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="请求方法">
                  <el-select v-model="sessionForm.method" style="width: 120px">
                    <el-option label="GET" value="GET" />
                    <el-option label="POST" value="POST" />
                  </el-select>
                </el-form-item>
                <el-form-item label="调用工号">
                  <el-input v-model="sessionForm.userCode" placeholder="Camstar 调用账号" style="width: 240px" />
                </el-form-item>
                <el-form-item label="Cookie名">
                  <el-input v-model="sessionForm.cookieName" style="width: 240px" />
                </el-form-item>
              </template>
              <el-form-item>
                <el-button size="mini" type="primary" @click="saveSession"
                           v-hasPermi="['sub-system:apiconfig:update']">保存会话设置</el-button>
                <el-button size="mini" type="success" plain :loading="sessionTesting" :disabled="!canTestSession"
                           @click="testSession" v-hasPermi="['sub-system:apiconfig:list']">测试会话登录</el-button>
              </el-form-item>
              <el-form-item v-if="sessionTestResult" label="登录结果">
                <el-input type="textarea" :rows="4" readonly v-model="sessionTestResult" />
              </el-form-item>
            </el-form>
          </el-card>
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
            <el-form-item v-if="isPersonPurpose" label="会话Cookie">
              <el-switch v-model="editApi.withSession" :disabled="!currentSessionEnabled" active-text="携带会话 Cookie" />
              <div class="form-tip" v-if="sessionSummary">
                {{ sessionSummary }}；无需 Cookie 的接口关闭本开关即可裸调
              </div>
              <div class="form-tip" v-else style="color:#f56c6c">
                本系统未启用会话鉴权（接口直接裸调）；需要 Cookie 时请在左侧系统节点的「会话鉴权」中开启
              </div>
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

    <el-dialog title="接入系统" :visible.sync="addDialogVisible" width="520px" append-to-body>
      <el-form label-width="100px" size="small">
        <el-form-item label="接入方式" required>
          <el-radio-group v-model="addMode">
            <el-radio label="existing">选择 JUMP 业务系统</el-radio>
            <el-radio label="manual">添加其他系统</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="addMode === 'existing'" label="业务系统" required>
          <el-select v-model="addSubSystemId" filterable clearable placeholder="如 MES4200" style="width: 100%">
            <el-option
              v-for="item in availableClients"
              :key="item.id"
              :label="item.name + (item.clientId ? (' (' + item.clientId + ')') : '')"
              :value="item.id"
            />
          </el-select>
          <div v-if="!availableClients.length" class="form-tip">暂无可接入的 JUMP 业务系统（均已接入或尚未登记）</div>
          <div v-else class="form-tip">来自已登记且绑定门户的业务系统；同步用户时会写入外部用户管理</div>
        </el-form-item>
        <el-form-item v-else label="系统名称" required>
          <el-input v-model="addSystemName" maxlength="100" placeholder="如：Camstar人员管理" />
          <div class="form-tip">非 JUMP 业务系统，只做接口配置；不出现在外部用户管理，同步时也只调对方接口</div>
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
  renameSubSystemApiAccess,
  testSubSystemApiInvoke,
  updateSubSystemApiConfig
} from '@/api/system/subSystemApiConfig'

const PURPOSES = ['query', 'create', 'update', 'delete']

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
    withSession: obj.withSession !== false
  }
}

/** authConfig 列 → 系统级会话设置（树上不再有鉴权叶子，会话在系统节点维护） */
function sessionFromConfig(config) {
  const obj = parseJsonSafe(config && config.authConfig, {}) || {}
  return {
    enabled: obj.enabled !== false && !!(obj.userCode || '').trim(),
    url: String(obj.url || obj.path || obj.loginPath || '').trim(),
    method: String(obj.method || 'GET').toUpperCase(),
    userCode: obj.userCode || '',
    cookieName: obj.cookieName || 'Nancal_Cam_SessionId'
  }
}

/** 会话设置 → authConfig 列 JSON（关闭时保留字段、enabled=false：接口不带 Cookie、不重登） */
function stringifySession(session) {
  return JSON.stringify({
    name: 'SSO登录',
    url: String(session.url || '').trim(),
    method: (session.method || 'GET').toUpperCase(),
    enabled: session.enabled !== false,
    userCode: session.userCode || '',
    cookieName: session.cookieName || 'Nancal_Cam_SessionId'
  })
}

/** 剔除旧版鉴权叶子（会话设置已上移到系统级）；「鉴权」目录剔除后为空则一并移除 */
function stripAuthNodes(nodes) {
  const result = []
  ;(nodes || []).forEach(n => {
    if (n.type === 'api' && n.purpose === 'auth') {
      return
    }
    if (n.children) {
      const children = stripAuthNodes(n.children)
      if (n.type === 'dir' && children.length === 0 && (n.id === 'dir-auth' || n.name === '鉴权')) {
        return
      }
      result.push(Object.assign({}, n, { children }))
    } else {
      result.push(n)
    }
  })
  return result
}

/** 无 catalog 时，用旧字段拼默认菜单树（不含鉴权叶子，会话在系统节点配置） */
function buildDefaultCatalog(config, host) {
  const h = host || (config && config.baseUrl) || ''
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
        id: 'dir-person',
        type: 'dir',
        name: '人员',
        children: [
          { id: 'api-query', type: 'api', name: query.name || '查询', purpose: 'query', method: query.method, url: query.url, enabled: query.enabled, withSession: query.withSession },
          { id: 'api-create', type: 'api', name: create.name || '新增', purpose: 'create', method: create.method, url: create.url, enabled: create.enabled, withSession: create.withSession },
          { id: 'api-update', type: 'api', name: update.name || '修改', purpose: 'update', method: update.method, url: update.url, enabled: update.enabled, withSession: update.withSession },
          { id: 'api-delete', type: 'api', name: del.name || '删除', purpose: 'delete', method: del.method, url: del.url, enabled: del.enabled, withSession: del.withSession }
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
    name: api.name || '',
    // 是否携带系统会话 Cookie（后端 EndpointSpec.withSession；false=该接口裸调）
    withSession: api.withSession !== false
  }
  return JSON.stringify(payload)
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
      /** subSystemId → 系统级会话设置（登录地址/工号/Cookie 名；树上不再有鉴权叶子） */
      sessionMap: {},
      /** 当前编辑的会话设置（系统节点面板表单） */
      sessionForm: { enabled: false, url: '', method: 'GET', userCode: '', cookieName: 'Nancal_Cam_SessionId' },
      sessionTesting: false,
      sessionTestResult: '',
      clientKeyword: '',
      currentNode: null,
      editApi: {},
      testBody: '',
      testResult: '',
      addDialogVisible: false,
      addMode: 'existing',
      addSubSystemId: undefined,
      addSystemName: '',
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
          // 树上不展示旧版鉴权叶子（会话设置在系统节点面板维护）
          children: stripAuthNodes(catalog.nodes).map(mapNode)
        }
      })
    },
    availableClients() {
      // 「选择已有业务系统」只列门户业务系统；仅接口目标（如 Camstar人员管理）走手动新建
      return (this.clientList || []).filter(item => {
        if (this.configMap[item.id]) return false
        if (item.portalBound === true) return true
        if (item.portalBound === false) return false
        return !!item.clientId
      })
    },
    canConfirmAdd() {
      const hostOk = !!(this.addHost || '').trim()
      if (!hostOk) return false
      if (this.addMode === 'manual') {
        return !!(this.addSystemName || '').trim()
      }
      return !!this.addSubSystemId
    },
    currentApiTitle() {
      if (!this.currentNode || this.currentNode.type !== 'api') return ''
      const name = (this.editApi && this.editApi.name) || this.currentNode.label
      return (this.currentNode.systemName || '') + ' / ' + name
    },
    /** 人员增删改查：Cookie 从系统级会话设置自动带，不在本页重复配置 */
    isPersonPurpose() {
      return ['query', 'create', 'update', 'delete'].indexOf(this.editApi && this.editApi.purpose) >= 0
    },
    currentSession() {
      if (!this.currentNode || !this.currentNode.subSystemId) return null
      return this.sessionMap[this.currentNode.subSystemId] || null
    },
    currentSessionEnabled() {
      return !!(this.currentSession && this.currentSession.enabled)
    },
    sessionSummary() {
      const s = this.currentSession
      if (!s || !s.enabled) {
        return ''
      }
      return '本系统接口默认携带会话 Cookie：' + (s.url || '') + '（工号 ' + s.userCode + '）'
    },
    canTestSession() {
      return !!(this.sessionForm.enabled && String(this.sessionForm.url || '').trim())
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
        this.sessionMap = {}
        ;(configs.data || []).forEach(c => {
          this.configMap[c.subSystemId] = c
          this.$set(this.catalogMap, c.subSystemId, normalizeCatalog(c.apiCatalog, c))
          this.$set(this.sessionMap, c.subSystemId, sessionFromConfig(c))
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
      // loadAll 刷新后会重选当前节点：同一节点不重复清空响应/参数（否则测试结果一闪而过）
      const sameNode = !!(this.currentNode && this.currentNode.nodeKey === data.nodeKey)
      this.applyMeta(data.subSystemId)
      this.currentNode = data
      if (!sameNode) {
        this.testResult = ''
        this.sessionTestResult = ''
      }
      if (data.type === 'api') {
        const catalog = this.catalogMap[data.subSystemId]
        const node = findInCatalog(catalog.nodes, data.catalogId) || {}
        this.editApi = {
          id: node.id,
          name: node.name || '',
          // 旧版鉴权叶子已在树上隐藏；防御性不再允许编辑 auth 用途
          purpose: node.purpose === 'auth' ? '' : (node.purpose || ''),
          method: (node.method || 'POST').toUpperCase(),
          url: node.url || '',
          enabled: node.enabled !== false,
          withSession: node.withSession !== false
        }
        if (!sameNode) {
          const sample = CAMSTAR_SAMPLES[node.purpose] || {}
          this.testBody = JSON.stringify(sample, null, 2)
        }
      } else {
        // 目录/系统节点：装载该系统的会话设置表单
        this.applySessionForm(data.subSystemId)
      }
    },
    applySessionForm(subSystemId) {
      const s = this.sessionMap[subSystemId] || {}
      this.sessionForm = {
        enabled: !!s.enabled,
        url: s.url || '',
        method: s.method || 'GET',
        userCode: s.userCode || '',
        cookieName: s.cookieName || 'Nancal_Cam_SessionId'
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
      // 是否携带系统会话 Cookie（false=该接口裸调）
      node.withSession = this.editApi.withSession !== false
    },
    buildPayload(subSystemId) {
      const config = this.configMap[subSystemId]
      const catalog = this.catalogMap[subSystemId] || { nodes: [] }
      const query = findByPurpose(catalog.nodes, 'query')
      const create = findByPurpose(catalog.nodes, 'create')
      const update = findByPurpose(catalog.nodes, 'update')
      const del = findByPurpose(catalog.nodes, 'delete')
      let baseUrl = (this.formMeta && this.formMeta.baseUrl) || (config && config.baseUrl) || ''
      const urls = [query, create].filter(Boolean).map(a => a.url)
      for (let i = 0; i < urls.length; i++) {
        const m = String(urls[i] || '').match(/^(https?:\/\/[^/]+)/i)
        if (m) {
          baseUrl = m[1]
          break
        }
      }
      // 会话设置来自系统级 sessionMap（树上已无鉴权叶子）；关闭时保留字段、enabled=false
      const session = this.sessionMap[subSystemId] || {}
      const sessionOn = !!(session.enabled && String(session.url || '').trim())
      const sessionHasUrl = !!String(session.url || '').trim()
      return {
        id: config.id,
        subSystemId,
        apiType: (config.apiType || 'camstar'),
        baseUrl: baseUrl || 'http://127.0.0.1',
        authType: sessionOn ? 'cookie_sso' : 'none',
        authConfig: sessionHasUrl ? stringifySession(Object.assign({}, session, { enabled: sessionOn })) : '',
        apiQuery: stringifyEndpoint(query),
        apiCreate: stringifyEndpoint(create),
        apiUpdate: stringifyEndpoint(update),
        apiDelete: stringifyEndpoint(del),
        apiTeamCombo: config.apiTeamCombo || '',
        // 持久化时剔除旧版鉴权叶子（后端不解析目录树；会话信息在 authConfig 列）
        apiCatalog: JSON.stringify({ nodes: stripAuthNodes(catalog.nodes) }),
        paramMapping: config.paramMapping || '',
        responseMapping: config.responseMapping || '',
        deleteTip: config.deleteTip || '',
        connectTimeoutMs: config.connectTimeoutMs || 10000,
        readTimeoutMs: config.readTimeoutMs || 30000,
        status: 0
      }
    },
    /** 系统节点面板：保存会话设置 */
    saveSession() {
      const sid = this.currentNode && this.currentNode.subSystemId
      if (!sid || !this.configMap[sid]) {
        return
      }
      if (!this.validSessionForm()) {
        return
      }
      this.$set(this.sessionMap, sid, Object.assign({}, this.sessionForm))
      this.persistCatalog()
    },
    /** 启用会话时地址与调用工号必填（否则保存后按 authConfig 解析会自动回到未启用） */
    validSessionForm() {
      if (!this.sessionForm.enabled) {
        return true
      }
      if (!String(this.sessionForm.url || '').trim()) {
        this.$modal.msgWarning('启用会话需填写登录地址')
        return false
      }
      if (!String(this.sessionForm.userCode || '').trim()) {
        this.$modal.msgWarning('启用会话需填写调用工号')
        return false
      }
      return true
    },
    /** 系统节点面板：先保存配置再调鉴权接口验证会话 */
    testSession() {
      const sid = this.currentNode && this.currentNode.subSystemId
      if (!sid || !this.configMap[sid]) {
        return
      }
      if (!this.formMeta.id) {
        this.$modal.msgWarning('请先保存')
        return
      }
      if (!this.validSessionForm()) {
        return
      }
      this.$set(this.sessionMap, sid, Object.assign({}, this.sessionForm))
      this.sessionTesting = true
      this.sessionTestResult = ''
      updateSubSystemApiConfig(this.buildPayload(sid)).then(() => {
        return testSubSystemApiInvoke({ id: this.formMeta.id, apiKey: 'auth', requestBody: '' })
      }).then(res => {
        const data = res.data || {}
        this.sessionTestResult = [
          (data.method || '') + ' ' + (data.url || ''),
          data.success === false ? '失败' : '成功',
          data.responseBody || ''
        ].join('\n')
      }).catch(err => {
        this.sessionTestResult = (err && (err.msg || err.message)) || String(err)
      }).finally(() => {
        this.sessionTesting = false
      })
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
        this.$modal.msgWarning('测试需先设置用途（查询/新增/修改/删除）')
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
      this.addMode = this.availableClients.length ? 'existing' : 'manual'
      this.addSubSystemId = undefined
      this.addSystemName = ''
      this.addApiType = 'camstar'
      this.addHost = ''
      this.addDialogVisible = true
    },
    confirmAddAccess() {
      if (!this.canConfirmAdd) return
      const host = this.addHost.trim().replace(/\/+$/, '')
      const catalog = buildDefaultCatalog({ baseUrl: host }, host)
      const payload = {
        baseUrl: host,
        connectTimeoutMs: 10000,
        readTimeoutMs: 30000,
        apiType: this.addApiType,
        // 会话鉴权改为系统级设置，接入后再在系统节点开启（不预置 authConfig）
        authType: 'none',
        authConfig: '',
        apiQuery: stringifyEndpoint(findByPurpose(catalog.nodes, 'query')),
        apiCreate: stringifyEndpoint(findByPurpose(catalog.nodes, 'create')),
        apiUpdate: stringifyEndpoint(findByPurpose(catalog.nodes, 'update')),
        apiDelete: stringifyEndpoint(findByPurpose(catalog.nodes, 'delete')),
        apiTeamCombo: '',
        apiCatalog: JSON.stringify(catalog),
        deleteTip: this.addApiType === 'camstar' ? '删除将同时删除该用户在 Camstar 的域账号，不可恢复！' : '',
        status: 0
      }
      if (this.addMode === 'manual') {
        payload.systemName = this.addSystemName.trim()
      } else {
        payload.subSystemId = this.addSubSystemId
      }
      createSubSystemApiConfig(payload).then(() => {
        this.$modal.msgSuccess('已接入')
        this.addDialogVisible = false
        this.loadAll()
      })
    },
    renameSystem() {
      if (!this.currentNode || this.currentNode.dirKind !== 'system') return
      this.$prompt('系统显示名称（如：Camstar人员管理）', '重命名系统', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: this.currentNode.label || '',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入名称'
      }).then(({ value }) => {
        return renameSubSystemApiAccess({
          id: this.currentNode.subSystemId,
          systemName: String(value).trim()
        }).then(() => {
          this.$modal.msgSuccess('已重命名')
          return this.loadAll()
        })
      }).catch(() => {})
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
.session-card {
  margin-top: 12px;
}
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
