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

        <!-- 目录：系统节点 / 固定分组节点 -->
        <div v-else-if="currentNode.type === 'dir'" class="api-panel">
          <div class="panel-head">
            <div class="panel-head__title">
              <strong>{{ currentNode.label }}</strong>
              <span class="meta">{{ currentNode.dirKind === 'system' ? '系统' : '分组' }}</span>
            </div>
            <div class="panel-head__actions">
              <el-button size="mini" type="primary" plain icon="el-icon-document-add" @click="addChildApi"
                         v-hasPermi="['sub-system:apiconfig:update']">新增接口</el-button>
              <el-button size="mini" @click="renameSystem" v-if="currentNode.dirKind === 'system'"
                         v-hasPermi="['sub-system:apiconfig:update']">重命名系统</el-button>
              <el-button size="mini" type="danger" plain @click="handleDeleteConfig"
                         v-if="currentNode.dirKind === 'system'"
                         v-hasPermi="['sub-system:apiconfig:delete']">取消接入</el-button>
              <el-button size="mini" type="primary" @click="persistConfig"
                         v-hasPermi="['sub-system:apiconfig:update']">保存</el-button>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="分组按接口「用途」自动归组（人员接口 / 角色接口 / 其他接口）。用途=新增人员 时，用户管理勾选同步该业务系统会调用这条接口。"
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
                <el-option label="人员分配角色（分配角色同步用）" value="assign_role" />
                <el-option label="删除人员" value="delete" />
                <el-option label="班组下拉" value="team_combo" />
                <el-option label="角色查询" value="role_query" />
                <el-option label="角色新增（裸角色，不挂页面）" value="role_create" />
                <el-option label="角色删除" value="role_delete" />
              </el-select>
              <div class="form-tip">同一用途只能配置一个接口；设置用途后自动归入对应分组</div>
              <div class="form-tip" v-if="editApi.purpose === 'create'">用户管理 → 添加用户 → 同步业务系统，将调用本接口</div>
              <div class="form-tip" v-else-if="editApi.purpose === 'assign_role'">花名册 → 分配角色 → 勾选「调用接口更新 Camstar 人员角色」时调用本接口（请求体自动带 roleOnly=true，只改角色）</div>
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
            <el-form-item v-if="isWithSessionPurpose" label="会话Cookie">
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
          <div v-else class="form-tip">来自已登记且绑定门户的业务系统；同步用户时会写入业务系统用户管理</div>
        </el-form-item>
        <el-form-item v-else label="系统名称" required>
          <el-input v-model="addSystemName" maxlength="100" placeholder="如：Camstar人员管理" />
          <div class="form-tip">非 JUMP 业务系统，只做接口配置；不出现在业务系统用户管理，同步时也只调对方接口</div>
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

const PURPOSES = ['query', 'create', 'update', 'delete', 'team_combo', 'assign_role', 'role_query', 'role_create', 'role_delete']

/** 用途 → 显示名 */
const PURPOSE_LABELS = {
  '': '无',
  query: '查询人员',
  create: '新增人员',
  update: '修改人员',
  delete: '删除人员',
  team_combo: '班组下拉',
  assign_role: '人员分配角色',
  role_query: '角色查询',
  role_create: '角色新增',
  role_delete: '角色删除'
}

/** 固定分组：按用途自动归组（与后端 ApiEndpointSpecs.groupOf 规则一致） */
const GROUPS = [
  { code: 'person', label: '人员接口' },
  { code: 'role', label: '角色接口' },
  { code: 'other', label: '其他接口' }
]

function groupOfPurpose(purpose) {
  if (!purpose) return 'other'
  return String(purpose).indexOf('role_') === 0 ? 'role' : 'person'
}

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
  delete: { userCode: '00078' },
  team_combo: { workshopCode: '4200' },
  // 人员分配角色：请求体 roleOnly=true，只增删角色不改菜单/主页/工位/班组/密码
  assign_role: [{
    userCode: '00078', userName: '张三', workshopCode: '4200',
    roleOnly: true, userRoleIdStr: '001bda8000000001,角色查询返回的roleId'
  }],
  // Camstar 角色（裸角色，不挂页面）：全部 POST；新增/删除要求会话工号挂「管理员/Administrator」角色
  role_query: { workshopCode: '4200' },
  role_create: [{ roleName: 'JUMP测试角色', workshopCode: '4200' }],
  role_delete: { roleId: '从角色查询结果中复制的 roleId' }
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

function isAbsoluteUrl(url) {
  return /^https?:\/\//i.test(String(url || '').trim())
}

function extractHost(url) {
  const m = String(url || '').trim().match(/^(https?:\/\/[^/?#]+)/i)
  return m ? m[1] : ''
}

/** 相对 path 拼上主机；已是 http(s) 则原样返回 */
function resolveApiUrl(urlOrPath, host) {
  const raw = String(urlOrPath || '').trim()
  if (!raw) return ''
  if (isAbsoluteUrl(raw)) return raw
  return joinUrl(host, raw)
}

function parseJsonSafe(json, fallback) {
  if (!json) return fallback
  try {
    return typeof json === 'string' ? JSON.parse(json) : json
  } catch (e) {
    return fallback
  }
}

/** authConfig 列 → 系统级会话设置（会话在系统节点维护） */
function sessionFromConfig(config) {
  const obj = parseJsonSafe(config && config.authConfig, {}) || {}
  const raw = String(obj.url || obj.path || obj.loginPath || '').trim()
  return {
    enabled: obj.enabled !== false && !!(obj.userCode || '').trim(),
    url: resolveApiUrl(raw, extractHost(config && config.baseUrl)),
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

/** 新接入系统的默认接口行（Camstar 风格路径；班组下拉默认不建，需要时手动加） */
function defaultEndpoints(host) {
  const mk = (purpose, name, path) => ({
    uid: uid('ep'),
    id: null,
    purpose,
    name,
    url: joinUrl(host, path),
    method: 'POST',
    enabled: true,
    withSession: true,
    sort: null
  })
  return [
    mk('query', '查询人员', '/BasicData/Employee/getEmployeeInfo'),
    mk('create', '新增人员', '/BasicData/Employee/addOrUpdateUser'),
    mk('update', '修改人员', '/BasicData/Employee/addOrUpdateUser'),
    mk('assign_role', '人员分配角色', '/BasicData/Employee/addOrUpdateUser'),
    mk('delete', '删除人员', '/BasicData/Employee/deleteEmployeeInfo'),
    mk('role_query', '角色查询', '/BasicData/Role/getRoleInfo'),
    mk('role_create', '角色新增', '/BasicData/Role/updateRoleInfo'),
    mk('role_delete', '角色删除', '/BasicData/Role/deleteRoleInfo')
  ]
}

export default {
  name: 'SubSystemApiConfig',
  data() {
    return {
      loading: false,
      testing: false,
      clientList: [],
      configMap: {},
      /** subSystemId → endpoints 数组（一行一接口，内存编辑；uid 为前端行标识） */
      endpointMap: {},
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
        const endpoints = this.endpointMap[item.id] || []
        const sorted = endpoints.slice().sort((a, b) => (a.sort || 0) - (b.sort || 0))
        const groups = GROUPS.map(g => ({
          nodeKey: 'sys-' + item.id + '-g-' + g.code,
          type: 'dir',
          dirKind: 'group',
          groupCode: g.code,
          label: g.label,
          subSystemId: item.id,
          systemName: item.name,
          children: sorted
            .filter(e => (e.groupCode || groupOfPurpose(e.purpose)) === g.code)
            .map(e => ({
              nodeKey: 'sys-' + item.id + '-ep-' + e.uid,
              type: 'api',
              epUid: e.uid,
              label: e.name || PURPOSE_LABELS[e.purpose] || e.purpose,
              method: e.method,
              enabled: e.enabled !== false,
              purpose: e.purpose || '',
              purposeHint: e.purpose === 'create' ? '同步' : '',
              subSystemId: item.id,
              systemName: item.name
            }))
        })).filter(g => g.groupCode !== 'other' || g.children.length)
        return {
          nodeKey: 'sys-' + item.id,
          type: 'dir',
          dirKind: 'system',
          label: item.name,
          id: item.id,
          subSystemId: item.id,
          systemName: item.name,
          children: groups
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
    /** 人员/角色接口：Cookie 从系统级会话设置自动带，不在本页重复配置 */
    isWithSessionPurpose() {
      const purposes = ['query', 'create', 'update', 'delete', 'team_combo', 'assign_role', 'role_query', 'role_create', 'role_delete']
      return purposes.indexOf(this.editApi && this.editApi.purpose) >= 0
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
        this.endpointMap = {}
        this.sessionMap = {}
        ;(configs.data || []).forEach(c => {
          this.configMap[c.subSystemId] = c
          const host = extractHost(c.baseUrl)
          const endpoints = (c.endpoints || []).map(e => ({
            uid: 'db-' + e.id,
            id: e.id,
            purpose: e.purpose || '',
            groupCode: e.groupCode || groupOfPurpose(e.purpose || ''),
            name: e.name || '',
            // 存量相对 path 补上主机展示
            url: resolveApiUrl(e.url || '', host),
            method: (e.method || 'POST').toUpperCase(),
            enabled: e.enabled !== false,
            withSession: e.withSession !== false,
            sort: e.sort
          }))
          this.$set(this.endpointMap, c.subSystemId, endpoints)
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
        const ep = this.findEndpoint(data.subSystemId, data.epUid) || {}
        this.editApi = {
          uid: ep.uid,
          id: ep.id,
          name: ep.name || '',
          purpose: ep.purpose || '',
          method: (ep.method || 'POST').toUpperCase(),
          url: ep.url || '',
          enabled: ep.enabled !== false,
          withSession: ep.withSession !== false
        }
        if (!sameNode) {
          const sample = CAMSTAR_SAMPLES[ep.purpose] || {}
          this.testBody = JSON.stringify(sample, null, 2)
        }
      } else {
        // 系统/分组节点：装载该系统的会话设置表单
        this.applySessionForm(data.subSystemId)
      }
    },
    findEndpoint(subSystemId, epUid) {
      const endpoints = this.endpointMap[subSystemId] || []
      return endpoints.find(e => e.uid === epUid) || null
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
    /** 同一用途只能一个接口：后选用途者保留，其他行用途清空 */
    ensureUniquePurpose(endpoints, purpose, exceptUid) {
      if (!purpose) return
      endpoints.forEach(e => {
        if (e.purpose === purpose && e.uid !== exceptUid) {
          e.purpose = ''
        }
      })
    },
    addChildApi() {
      if (!this.currentNode || this.currentNode.type !== 'dir') return
      const sid = this.currentNode.subSystemId
      const endpoints = this.endpointMap[sid]
      if (!endpoints) return
      this.$prompt('接口名称', '新增接口', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入名称'
      }).then(({ value }) => {
        const row = {
          uid: uid('ep'),
          id: null,
          purpose: '',
          name: value.trim(),
          method: 'POST',
          url: '',
          enabled: true,
          withSession: true,
          sort: null
        }
        endpoints.push(row)
        this.persistConfig().then(() => {
          this.$nextTick(() => {
            // 保存重载后 uid 会变（后端生成新 id），按名称定位新行
            let target = null
            const walk = (nodes) => {
              ;(nodes || []).forEach(n => {
                if (!target && n.type === 'api' && n.label === row.name) target = n
                walk(n.children)
              })
            }
            walk(this.treeData)
            if (target) {
              this.handleNodeClick(target)
              if (this.$refs.tree) this.$refs.tree.setCurrentKey(target.nodeKey)
            }
          })
        })
      }).catch(() => {})
    },
    removeApi() {
      if (!this.currentNode || this.currentNode.type !== 'api') return
      this.$modal.confirm('确认删除该接口？').then(() => {
        const sid = this.currentNode.subSystemId
        const endpoints = this.endpointMap[sid] || []
        const idx = endpoints.findIndex(e => e.uid === this.currentNode.epUid)
        if (idx >= 0) endpoints.splice(idx, 1)
        this.currentNode = null
        this.persistConfig()
      }).catch(() => {})
    },
    /** 表单值写回接口行（内存），保存时随 endpoints 一起提交 */
    applyEditToEndpoints() {
      if (!this.currentNode || this.currentNode.type !== 'api') return
      const endpoints = this.endpointMap[this.currentNode.subSystemId] || []
      const ep = endpoints.find(e => e.uid === (this.editApi.uid || this.currentNode.epUid))
      if (!ep) return
      const purpose = this.editApi.purpose || ''
      this.ensureUniquePurpose(endpoints, purpose, ep.uid)
      ep.name = this.editApi.name || ep.name
      ep.purpose = purpose
      ep.groupCode = groupOfPurpose(purpose)
      ep.method = (this.editApi.method || 'POST').toUpperCase()
      ep.url = String(this.editApi.url || '').trim()
      ep.enabled = this.editApi.enabled !== false
      // 是否携带系统会话 Cookie（false=该接口裸调）
      ep.withSession = this.editApi.withSession !== false
    },
    buildPayload(subSystemId) {
      const config = this.configMap[subSystemId]
      const endpoints = this.endpointMap[subSystemId] || []
      // 基地址优先取接口行 URL 的主机，避免接口改了主机而 base_url 还是旧值
      let baseUrl = (config && config.baseUrl) || ''
      const primary = endpoints.find(e => e.purpose === 'query') || endpoints.find(e => e.purpose === 'create')
      const m = primary && String(primary.url || '').match(/^(https?:\/\/[^/]+)/i)
      if (m) baseUrl = m[1]
      // 会话设置来自系统级 sessionMap；关闭时保留字段、enabled=false
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
        // 一行一接口：整系统接口行一起提交（后端级联全删全插）
        endpoints: endpoints.map((e, i) => ({
          id: e.id || undefined,
          purpose: e.purpose || '',
          groupCode: e.groupCode || groupOfPurpose(e.purpose || ''),
          name: e.name || '',
          url: String(e.url || '').trim(),
          method: (e.method || 'POST').toUpperCase(),
          enabled: e.enabled !== false,
          withSession: e.withSession !== false,
          sort: e.sort == null ? (i + 1) * 10 : e.sort
        })),
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
      this.persistConfig()
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
    persistConfig() {
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
      this.applyEditToEndpoints()
      this.persistConfig()
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
      this.applyEditToEndpoints()
      const purpose = this.editApi.purpose
      if (!purpose || PURPOSES.indexOf(purpose) < 0) {
        this.$modal.msgWarning('测试需先设置用途（人员或角色的查询/新增/删除）')
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
      const payload = {
        baseUrl: host,
        connectTimeoutMs: 10000,
        readTimeoutMs: 30000,
        apiType: this.addApiType,
        // 会话鉴权为系统级设置，接入后再在系统节点开启（不预置 authConfig）
        authType: 'none',
        authConfig: '',
        // 默认接口行（Camstar 风格路径占位，接入后按现场改）
        endpoints: defaultEndpoints(host).map((e, i) => ({
          purpose: e.purpose,
          name: e.name,
          url: e.url,
          method: e.method,
          enabled: e.enabled,
          withSession: e.withSession,
          sort: (i + 1) * 10
        })),
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
  /* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
  > :not(:last-child) { margin-right: 12px; }
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
  /* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
  > :not(:last-child) { margin-right: 8px; }
}
.panel-head__title {
  display: flex;
  align-items: baseline;
  /* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
  > :not(:last-child) { margin-right: 8px; }
}
.panel-head__title .meta {
  color: #909399;
  font-weight: normal;
  font-size: 13px;
}
.panel-head__actions {
  display: flex;
  flex-wrap: wrap;
  /* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
  > :not(:last-child) { margin-right: 8px; }
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
