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
              <el-tag size="mini" type="info">{{ item.menuCount || 0 }} 菜单</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无外部系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 菜单数据 -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="请先在左侧选择已登记的外部系统；关联系统信息后，才可新增/维护该系统下的菜单"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="菜单名称" prop="name">
            <el-input v-model="queryParams.name" placeholder="请输入菜单名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="菜单状态" clearable style="width: 240px">
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
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd()"
                       v-hasPermi="['sub-system:menu:create']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport"
                       v-hasPermi="['sub-system:menu:create']">导入</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="checkedIds.length === 0"
              @click="handleDeleteBatch"
              v-hasPermi="['sub-system:menu:delete']"
            >批量删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="el-icon-refresh"
              size="mini"
              :disabled="!selectedClient"
              @click="handleClearPortalCache"
              v-hasPermi="['sub-system:menu:update']"
            >清门户缓存</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-share" size="mini" @click="$refs.commonMenuDialog.open()"
                       v-hasPermi="['sub-system:menu:list']">通用菜单</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
        </el-row>

        <common-menu-dialog ref="commonMenuDialog" @changed="getList" />

        <el-table v-if="refreshTable" v-loading="loading" :data="menuList" row-key="id"
                  :default-expand-all="isExpandAll"
                  :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
                  @selection-change="handleRowCheckboxChange">
          <el-table-column type="selection" width="55"/>
          <el-table-column prop="name" label="菜单名称" :show-overflow-tooltip="true" width="200"/>
          <el-table-column prop="icon" label="图标" align="center" width="80">
            <template v-slot="scope">
              <svg-icon :icon-class="scope.row.icon" />
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="60"/>
          <el-table-column prop="path" label="路由地址" :show-overflow-tooltip="true" min-width="160"/>
          <el-table-column prop="permission" label="权限标识" :show-overflow-tooltip="true" />
          <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true" min-width="120"/>
          <el-table-column prop="status" label="状态" width="80">
            <template v-slot="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['sub-system:menu:update']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)"
                         v-hasPermi="['sub-system:menu:create']">新增</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['sub-system:menu:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 新增/修改 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="外部系统">
              <el-input :value="formSubSystemLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <treeselect v-model="form.parentId" :options="menuOptions" :normalizer="normalizer" :show-count="true"
                          placeholder="选择上级菜单"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio v-for="dict in menuTypeDictDatas" :key="parseInt(dict.value)" :label="parseInt(dict.value)">
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="菜单图标">
              <el-popover placement="bottom-start" width="460" trigger="click" @show="$refs['iconSelect'].reset()">
                <IconSelect ref="iconSelect" @selected="selected" />
                <el-input slot="reference" v-model="form.icon" placeholder="点击选择图标" readonly>
                  <svg-icon v-if="form.icon" slot="prefix" :icon-class="form.icon" class="el-input__icon"
                            style="height: 32px;width: 16px;"/>
                  <i v-else slot="prefix" class="el-icon-search el-input__icon" />
                </el-input>
              </el-popover>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON && isFirstLevelMenu" label="菜单颜色">
              <menu-style-select v-model="form.styleId" />
            </el-form-item>
            <el-form-item v-else-if="form.type !== MenuTypeEnum.BUTTON" label="菜单颜色">
              <menu-style-select :value="inheritedStyleId" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="与4200 SYS_MENU.PATH 一致：Camstar 填完整 http 地址" />
              <div v-if="form.type === MenuTypeEnum.MENU" style="line-height: 18px; margin-top: 4px; color: #909399; font-size: 12px;">
                对齐 4200：PATH 填 Camstar 业务完整 http（iframe 直开，不经 4221）。
                推荐：http://192.168.240.127:4200/Process/...（门户壳会编成 192.168.240.12794200/...）；
                若只填壳 path、不要冒号：写成 192.168.240.12794200/Process/...。
                若门户机已用 nginx 监听 4200 反代到 Camstar：可填 http://192.168.240.129:4200/Process/...（勿填 /camstar- 路径）。
                组件路径留空。上方「访问地址」填 MES，只给若依页用。
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.type !== MenuTypeEnum.BUTTON">
            <el-form-item label="菜单说明书">
              <file-upload v-model="form.manualUrl" :limit="1" :file-size="20" :is-show-tip="true" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.DIR" label="权限标识">
              <el-input v-model="form.permission" placeholder="请输入权限标识" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.type === MenuTypeEnum.MENU || (form.type === MenuTypeEnum.DIR && form.component)">
            <el-form-item label="组件路径" prop="component">
              <el-input
                v-model="form.component"
                :placeholder="form.type === MenuTypeEnum.DIR ? '目录一般为空；若误填可清空后保存' : '例如说：system/user/index'"
                clearable
              />
              <div v-if="form.type === MenuTypeEnum.DIR" style="line-height: 18px; margin-top: 4px; color: #909399; font-size: 12px;">
                目录不需要组件路径。「15」应填在上方「路由地址」（对齐 4200 工艺管理 path），不要填在组件路径。
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.type === MenuTypeEnum.MENU">
            <el-form-item label="组件名称" prop="componentName">
              <el-input v-model="form.componentName" placeholder="例如说：SystemUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in statusDictDatas" :key="dict.value" :label="parseInt(dict.value)">
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio :label="true">显示</el-radio>
                <el-radio :label="false">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type === MenuTypeEnum.MENU" label="是否缓存">
              <el-radio-group v-model="form.keepAlive">
                <el-radio :label="true">缓存</el-radio>
                <el-radio :label="false">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== MenuTypeEnum.BUTTON" label="总是显示">
              <el-radio-group v-model="form.alwaysShow">
                <el-radio :label="true">总是</el-radio>
                <el-radio :label="false">不是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-alert
        :title="'当前系统：' + (selectedClient ? (selectedClient.name + ' (' + selectedClient.clientId + ')') : '未选择')"
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
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已存在的菜单（同级同名）
          </div>
          <span>仅允许 xls/xlsx。父菜单填名称（根填「根」）；请先导入父级再导入子级。</span>
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
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import IconSelect from '@/components/IconSelect'
import MenuStyleSelect from '@/components/MenuStyleSelect'
import FileUpload from '@/components/FileUpload'
import {
  clearPortalMenuCache,
  createSubSystemMenu,
  deleteSubSystemMenu,
  deleteSubSystemMenuList,
  getSubSystemClientSimpleList,
  getSubSystemMenu,
  getSubSystemMenuList,
  importSubSystemMenuTemplate,
  updateSubSystemMenu
} from '@/api/system/subSystemMenu'
import { SystemMenuTypeEnum, CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictDatas } from '@/utils/dict'
import { isExternal } from '@/utils/validate'
import { flattenMenuTree, inheritedStyleId as resolveInheritedStyleId, isFirstLevelMenu as checkFirstLevelMenu } from '@/utils/menuStyleInherit'
import { getBaseHeader } from '@/utils/request'
import subSystemImportGate from '@/utils/subSystemImportGate'
import CommonMenuDialog from './CommonMenuDialog.vue'

export default {
  name: 'SubSystemMenu',
  components: { Treeselect, IconSelect, MenuStyleSelect, FileUpload, CommonMenuDialog },
  mixins: [subSystemImportGate],
  data() {
    return {
      loading: false,
      showSearch: true,
      menuList: [],
      menuOptions: [],
      clientList: [],
      clientKeyword: '',
      selectedClient: null,
      title: '',
      open: false,
      isExpandAll: false,
      refreshTable: true,
      checkedIds: [],
      upload: {
        open: false,
        title: '',
        isUploading: false,
        updateSupport: false,
        headers: getBaseHeader()
      },
      queryParams: {
        name: undefined,
        status: undefined
      },
      form: {},
      rules: {
        name: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
        sort: [{ required: true, message: '菜单顺序不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'blur' }]
      },
      MenuTypeEnum: SystemMenuTypeEnum,
      menuTypeDictDatas: getDictDatas(DICT_TYPE.SYSTEM_MENU_TYPE),
      statusDictDatas: getDictDatas(DICT_TYPE.COMMON_STATUS)
    }
  },
  computed: {
    uploadAction() {
      const id = this.selectedClient && this.selectedClient.id
      const update = this.upload.updateSupport ? 'true' : 'false'
      return process.env.VUE_APP_BASE_API + '/admin-api/system/sub-system-menu/import'
        + '?subSystemId=' + (id || '')
        + '&updateSupport=' + update
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
    },
    formSubSystemLabel() {
      const id = this.form.subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      const item = this.clientList.find(client => client.id === id)
      return item ? item.name + ' (' + item.clientId + ')' : ''
    },
    isFirstLevelMenu() {
      return checkFirstLevelMenu(this.form.parentId)
    },
    inheritedStyleId() {
      const flatMenus = flattenMenuTree(this.menuList)
      return resolveInheritedStyleId(flatMenus, this.form.parentId)
    }
  },
  created() {
    this.loadClientList()
  },
  methods: {
    selected(name) {
      this.form.icon = name
    },
    loadClientList() {
      return this.withClientsLoading(() => {
        return getSubSystemClientSimpleList().then(res => {
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
      this.queryParams.name = undefined
      this.queryParams.status = undefined
      this.getList()
    },
    getList() {
      if (!this.selectedClient) {
        this.menuList = []
        return
      }
      this.loading = true
      getSubSystemMenuList({
        subSystemId: this.selectedClient.id,
        name: this.queryParams.name,
        status: this.queryParams.status
      }).then(res => {
        this.menuList = this.handleTree(res.data || [], 'id')
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.getList()
    },
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.id,
        label: node.name,
        children: node.children
      }
    },
    getTreeselect(subSystemId) {
      const id = subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      if (!id) {
        this.menuOptions = []
        return Promise.resolve()
      }
      return getSubSystemMenuList({ subSystemId: id }).then(res => {
        this.menuOptions = []
        const menu = { id: 0, name: '主类目', children: [] }
        menu.children = this.handleTree(res.data || [], 'id')
        this.menuOptions.push(menu)
      })
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    resetFormData() {
      this.form = {
        id: undefined,
        subSystemId: this.selectedClient ? this.selectedClient.id : undefined,
        parentId: 0,
        name: undefined,
        icon: undefined,
        styleId: undefined,
        type: SystemMenuTypeEnum.DIR,
        sort: 0,
        path: undefined,
        permission: undefined,
        component: undefined,
        componentName: undefined,
        status: CommonStatusEnum.ENABLE,
        visible: true,
        keepAlive: true,
        alwaysShow: true,
        manualUrl: undefined
      }
      this.resetForm('form')
    },
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    handleAdd(row) {
      this.ensureSubSystemBoundBeforeAction('新增菜单', { requireConfirm: false }).then(() => {
        this.resetFormData()
        this.getTreeselect(this.selectedClient.id).then(() => {
          if (row != null && row.id) {
            this.form.parentId = row.id
          } else {
            this.form.parentId = 0
          }
          this.open = true
          this.title = '添加外部系统菜单'
        })
      }).catch(() => {})
    },
    handleImport() {
      this.ensureSubSystemBoundBeforeAction('导入').then(() => {
        this.upload.title = '导入外部系统菜单 — ' + (this.selectedClient.name || '')
        this.upload.open = true
        this.upload.headers = getBaseHeader()
      }).catch(() => {})
    },
    importTemplate() {
      importSubSystemMenuTemplate().then(response => {
        this.$download.excel(response, '外部系统菜单导入模板.xls')
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
      this.loadClientList()
    },
    submitFileForm() {
      this.$refs.upload.submit()
    },
    handleUpdate(row) {
      this.resetFormData()
      const subSystemId = row.subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      this.getTreeselect(subSystemId).then(() => {
        getSubSystemMenu(row.id).then(res => {
          this.form = {
            id: res.data.id,
            subSystemId: res.data.subSystemId,
            parentId: res.data.parentId,
            name: res.data.name,
            icon: res.data.icon,
            type: res.data.type,
            sort: res.data.sort,
            path: res.data.path,
            permission: res.data.permission,
            component: res.data.component,
            componentName: res.data.componentName,
            status: res.data.status,
            visible: res.data.visible,
            keepAlive: res.data.keepAlive,
            alwaysShow: res.data.alwaysShow,
            styleId: res.data.styleId
          }
          this.open = true
          this.title = '修改外部系统菜单'
        })
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.type === SystemMenuTypeEnum.DIR || this.form.type === SystemMenuTypeEnum.MENU) {
          let path = this.form.path
          // 子系统菜单路由由前端拼接为 /portal/{clientId}/...，path 应为相对段，不能以 / 开头
          if (path && !isExternal(path) && path.charAt(0) === '/') {
            this.$modal.msgError('子系统菜单路由地址不能以 / 开头')
            return
          }
          // Camstar 内链：点分 IP:端口 → IP9端口（如 192.168.240.12794200）；http(s) 完整 URL 不要改
          if (path && !isExternal(path) && /(\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})/.test(path)) {
            const fixed = path.replace(/(\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})/g, '$19$2')
            this.form.path = fixed
            path = fixed
            this.$modal.msgWarning('端口冒号已自动改成 9（如 192.168.240.12794200）。Camstar 更推荐直接填 http://192.168.240.127:4200/WorkOrder/...')
            return
          }
          if (path && !isExternal(path) && /:\d+/.test(path)) {
            const fixed = path.replace(/:/g, '/')
            this.form.path = fixed
            path = fixed
            this.$modal.msgWarning('路由里的端口冒号已自动改成 /。Camstar 更推荐直接填 http://192.168.240.127:4200/WorkOrder/...')
            return
          }
          if (this.form.type === SystemMenuTypeEnum.MENU && path && !isExternal(path) && /WorkOrder\/?$/.test(path)) {
            this.$modal.msgError('路由地址不完整，请填到页面名，或直接填 Camstar 的 http 完整地址')
            return
          }
        }
        this.warnDuplicateMenuName()
        const payload = { ...this.form }
        // 目录不需要组件；列表曾把误填的「15」显示在组件路径且无法编辑，保存时清空
        if (payload.type === SystemMenuTypeEnum.DIR) {
          payload.component = ''
          payload.componentName = ''
        }
        if (!checkFirstLevelMenu(payload.parentId)) {
          payload.styleId = null
        }
        const request = payload.id ? updateSubSystemMenu : createSubSystemMenu
        request(payload).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
          this.loadClientList()
        })
      })
    },
    /** 菜单重名提醒（不拦截保存） */
    warnDuplicateMenuName() {
      const { name, parentId, id } = this.form
      if (!name) {
        return
      }
      const flatMenus = flattenMenuTree(this.menuList)
      const duplicate = flatMenus.some(menu =>
        menu.id !== id && menu.name === name && menu.parentId === parentId
      )
      if (duplicate) {
        this.$modal.msgWarning('已经存在该名字的菜单，请注意区分')
      }
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.name + '"的菜单？').then(() => {
        return deleteSubSystemMenu(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的外部系统菜单？').then(() => {
        return deleteSubSystemMenuList(this.checkedIds)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.checkedIds = []
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleClearPortalCache() {
      if (!this.selectedClient || !this.selectedClient.id) {
        this.$modal.msgWarning('请先选择外部系统')
        return
      }
      this.$modal.confirm('将清除该系统门户菜单 Redis 缓存，用户下次进入会重新从数据库加载。是否继续？').then(() => {
        return clearPortalMenuCache(this.selectedClient.id)
      }).then(() => {
        this.$modal.msgSuccess('门户缓存已清除')
      }).catch(() => {})
    },
    handleRowCheckboxChange(records) {
      this.checkedIds = records.map(item => item.id)
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
