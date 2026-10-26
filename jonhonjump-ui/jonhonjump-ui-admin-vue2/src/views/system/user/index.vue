<template>
  <div class="app-container">
    <doc-alert title="用户体系" url="https://doc.iocoder.cn/user-center/" />
    <doc-alert title="三方登陆" url="https://doc.iocoder.cn/social-user/" />
    <doc-alert title="Excel 导入导出" url="https://doc.iocoder.cn/excel-import-and-export/" />
    <!-- 搜索工作栏 -->
    <el-row :gutter="20">
      <!--部门数据-->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input v-model="deptName" placeholder="请输入部门名称" clearable size="small" prefix-icon="el-icon-search" style="margin-bottom: 20px"/>
        </div>
        <div class="head-container">
          <el-tree :data="deptOptions" :props="defaultProps" :expand-on-click-node="false" :filter-node-method="filterNode"
                   ref="tree" default-expand-all highlight-current @node-click="handleNodeClick"/>
        </div>
      </el-col>
      <!--用户数据-->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="用户名称" prop="username">
            <el-input v-model="queryParams.username" placeholder="请输入用户名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="queryParams.nickname" placeholder="请输入用户姓名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="工号" prop="employeeNo">
            <el-input v-model="queryParams.employeeNo" placeholder="请输入工号" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="域账号" prop="domainNo">
            <el-input v-model="queryParams.domainNo" placeholder="请输入域账号" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
              <el-option v-for="dict in statusDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)"/>
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间" prop="createTime">
            <el-date-picker v-model="queryParams.createTime" style="width: 240px" value-format="yyyy-MM-dd HH:mm:ss" type="daterange"
              range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :default-time="['00:00:00', '23:59:59']" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                       v-hasPermi="['system:user:create']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" icon="el-icon-upload2" size="mini" @click="handleImport"
                       v-hasPermi="['system:user:import']">导入</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" icon="el-icon-download" size="mini" @click="handleExport" :loading="exportLoading"
                       v-hasPermi="['system:user:export']">导出</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="isEmpty(checkedIds)"
              @click="handleDeleteBatch"
              v-hasPermi="['system:user:delete']"
            >
              批量删除
            </el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <el-table
          v-loading="loading"
          :data="userList"
          class="user-list-table"
          @row-click="handleRowClick"
          @selection-change="handleRowCheckboxChange"
        >
          <el-table-column type="selection" width="55"/>
          <el-table-column label="用户名称" align="center" key="username" prop="username" v-if="columns[0].visible" :show-overflow-tooltip="true" />
          <el-table-column label="用户姓名" align="center" key="nickname" prop="nickname" v-if="columns[1].visible" :show-overflow-tooltip="true" />
          <el-table-column label="工号" align="center" key="employeeNo" prop="employeeNo" v-if="columns[2].visible" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="域账号" align="center" key="domainNo" prop="domainNo" v-if="columns[3].visible" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="刷卡卡号" align="center" key="cardNo" prop="cardNo" v-if="columns[4].visible" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="ERP账号" align="center" key="erpNos" v-if="columns[5].visible" width="120" :show-overflow-tooltip="true">
            <template v-slot="scope">
              <span>{{ formatErpNos(scope.row.erpNos) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="部门" align="center" key="deptName" v-if="columns[6].visible" :show-overflow-tooltip="true">
            <template v-slot="scope">
              <span>{{ scope.row.deptName || (scope.row.dept && scope.row.dept.name) || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" key="status" v-if="columns[7].visible" align="center" width="80">
            <template v-slot="scope">
              <el-switch v-model="scope.row.status" :active-value="0" :inactive-value="1" @click.native.stop @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[8].visible" width="160">
            <template v-slot="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
            <template v-slot="scope">
              <el-button size="mini" type="text" icon="el-icon-view" @click.stop="handleDetail(scope.row)">详情</el-button>
              <el-button size="mini" type="text" icon="el-icon-edit" @click.stop="handleUpdate(scope.row)"
                         v-hasPermi="['system:user:update']">修改</el-button>
              <el-dropdown  @command="(command) => handleCommand(command, scope.$index, scope.row)"
                            v-hasPermi="['system:user:delete', 'system:user:update-password', 'system:permission:assign-user-role']">
                <el-button size="mini" type="text" icon="el-icon-d-arrow-right" @click.stop>更多</el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="handleDelete" v-if="scope.row.id !== 1" size="mini" type="text" icon="el-icon-delete"
                                    v-hasPermi="['system:user:delete']">删除</el-dropdown-item>
                  <el-dropdown-item command="handleResetPwd" size="mini" type="text" icon="el-icon-key"
                                    v-hasPermi="['system:user:update-password']">重置密码</el-dropdown-item>
                  <el-dropdown-item command="handleRole" size="mini" type="text" icon="el-icon-circle-check"
                                    v-hasPermi="['system:permission:assign-user-role']">分配角色</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                    @pagination="getList"/>
      </el-col>
    </el-row>

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body :before-close="handleDialogBeforeClose">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户姓名" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入用户姓名" />
              <div v-if="form.id" style="color:#909399;font-size:12px;line-height:1.4;margin-top:4px">
                仍显示原姓名的业务系统用户会一起改。登录名不会改业务系统里的账号。
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect v-model="form.deptId" :options="deptOptions" :show-count="true" :clearable="false"
                          placeholder="请选择归属部门" :normalizer="normalizer"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.id === undefined" label="用户名称" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.id === undefined" label="用户密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeNo">
              <el-input v-model="form.employeeNo" placeholder="请输入工号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="域账号" prop="domainNo">
              <el-input v-model="form.domainNo" placeholder="请输入域账号" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="刷卡卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入刷卡卡号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ERP账号" prop="erpNos">
              <el-select v-model="form.erpNos" multiple filterable allow-create default-first-option
                         placeholder="输入后回车添加，可多个" style="width: 100%">
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择" style="width: 100%">
                <el-option v-for="dict in sexDictDatas" :key="parseInt(dict.value)" :label="dict.label" :value="parseInt(dict.value)"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择" style="width: 100%">
                <el-option
                    v-for="item in postOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 登记业务系统用户（仅新增时显示；纯本地花名册登记，不调外部接口，可多选） -->
        <el-row v-if="form.id === undefined">
          <el-col :span="24">
            <el-form-item label="登记业务系统">
              <el-checkbox v-model="syncSubSystem" @change="handleSyncChange">
                同时登记业务系统用户
              </el-checkbox>
            </el-form-item>
          </el-col>
          <template v-if="syncSubSystem">
            <el-col :span="24">
              <el-form-item label="业务系统/车间">
                <el-cascader
                  v-model="syncHangPath"
                  :options="syncCascaderOptions"
                  :props="syncCascaderProps"
                  filterable
                  clearable
                  collapse-tags
                  placeholder="可多选：无车间系统直接选系统；多车间系统请选到车间叶子"
                  style="width: 100%"
                  @change="handleSyncTargetChange"
                />
                <div class="sync-hint">
                  仅在 JUMP「业务系统用户管理」登记并关联该用户，不调用业务系统接口。多车间系统必须选到具体车间，登记后该用户从对应车间入口进入；角色可在登记后于【业务系统 → 用户管理】调整。
                </div>
                <el-alert
                  v-if="syncHangableTree.length === 0"
                  title="暂无可登记的业务系统（需在【业务系统管理】绑定门户）"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin-top: 8px"
                />
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的用户数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog title="分配角色" :visible.sync="openRole" width="500px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名称">
          <el-input v-model="form.username" :disabled="true" />
        </el-form-item>
        <el-form-item label="用户姓名">
          <el-input v-model="form.nickname" :disabled="true" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择">
            <el-option
                v-for="item in roleOptions"
                :key="parseInt(item.id)"
                :label="item.name"
                :value="parseInt(item.id)"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRole">确 定</el-button>
        <el-button @click="cancelRole">取 消</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import {
  addUser,
  changeUserStatus,
  delUser,
  exportUser,
  getUser,
  importTemplate,
  listUser,
  resetUserPwd,
  updateUser,
  delUserList
} from "@/api/system/user";

import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

import {listSimpleDepts} from "@/api/system/dept";
import {listSimplePosts} from "@/api/system/post";
import { getHangableExternalTree } from "@/api/system/subSystemWorkshop";

import {CommonStatusEnum} from "@/utils/constants";
import {DICT_TYPE, getDictDatas} from "@/utils/dict";
import {assignUserRole, listUserRoles} from "@/api/system/permission";
import {listSimpleRoles} from "@/api/system/role";
import {getBaseHeader} from "@/utils/request";
import { formatErpNos } from "@/utils/userFieldDisplay";
import dialogDirtyGuard from '@/mixins/dialogDirtyGuard'

export default {
  name: "SystemUser",
  components: { Treeselect },
  mixins: [dialogDirtyGuard],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 导出遮罩层
      exportLoading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户表格数据
      userList: null,
      // 弹出层标题
      title: "",
      // 部门树选项
      deptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 提交中：防双击重复创建
      submitting: false,
      // 部门名称
      deptName: undefined,
      // 默认密码
      initPassword: undefined,
      // 性别状态字典
      sexOptions: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      // 登记业务系统用户（新增时联动，可多选；纯本地花名册，不调外部接口）
      syncSubSystem: false,
      // 选中的叶子（emitPath=false，值为 `${subSystemId}|${workshopCode}`）
      syncHangPath: [],
      // 提交给后端的登记目标：[{ subSystemId, workshopId }]
      syncTargets: [],
      // 人员可挂外部树（仅叶子可选：系统本身或车间）
      syncHangableTree: [],
      syncCascaderProps: {
        value: 'value',
        label: 'label',
        children: 'children',
        multiple: true,
        emitPath: false,
        checkStrictly: false
      },
      defaultProps: {
        children: "children",
        label: "name"
      },
      // 用户导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: getBaseHeader(),
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + '/admin-api/system/user/import'
      },
      // 查询参数
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        username: undefined,
        nickname: undefined,
        employeeNo: undefined,
        domainNo: undefined,
        status: undefined,
        deptId: undefined,
        createTime: []
      },
      // 列信息
      columns: [
        { key: 0, label: `用户名称`, visible: true },
        { key: 1, label: `用户姓名`, visible: true },
        { key: 2, label: `工号`, visible: true },
        { key: 3, label: `域账号`, visible: true },
        { key: 4, label: `刷卡卡号`, visible: true },
        { key: 5, label: `ERP账号`, visible: true },
        { key: 6, label: `部门`, visible: true },
        { key: 7, label: `状态`, visible: true },
        { key: 8, label: `创建时间`, visible: true }
      ],
      // 表单校验
      rules: {
        username: [
          { required: true, message: "用户名称不能为空", trigger: "blur" }
        ],
        nickname: [
          { required: true, message: "用户姓名不能为空", trigger: "blur" }
        ],
        password: [
          { required: true, message: "用户密码不能为空", trigger: "blur" }
        ],
        deptId: [
          { required: true, message: "归属部门不能为空", trigger: "change" }
        ],
        mobile: [
          {
            pattern: /^(?:(?:\+|00)86)?1(?:3[\d]|4[5-79]|5[0-35-9]|6[5-7]|7[0-8]|8[\d]|9[189])\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      },
      // 是否显示弹出层（角色权限）
      openRole: false,

      // 枚举
      SysCommonStatusEnum: CommonStatusEnum,
      // 选中行
      checkedIds: [],
    };
  },
  computed: {
    // 数据字典走 computed：字典是异步加载的，data 快照会拿到空列表导致下拉空白
    statusDictDatas() {
      return getDictDatas(DICT_TYPE.COMMON_STATUS)
    },
    sexDictDatas() {
      return getDictDatas(DICT_TYPE.SYSTEM_USER_SEX)
    },
    /** 业务系统/车间叶子级联选项：分组节点只作父级，叶子才可选 */
    syncCascaderOptions() {
      return (this.syncHangableTree || []).map(node => {
        if (node.nodeType === 'group') {
          return {
            value: 'g-' + node.subSystemId,
            label: node.label,
            children: (node.children || []).map(child => ({
              value: this.syncLeafKey(child.subSystemId, child.workshopCode),
              label: child.label
            }))
          }
        }
        return {
          value: this.syncLeafKey(node.subSystemId, node.workshopCode),
          label: node.label
        }
      })
    }
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    }
  },
  created() {
    this.getList();
    this.getTreeselect();
    // 新建用户默认密码：key 与后端 AdminUserServiceImpl.USER_INIT_PASSWORD_KEY 一致；
    // 没配置就保持空，由管理员手填（不要再读旧的 sys.user.init-password）
    this.getConfigKey("system.user.init-password").then(response => {
      const value = response && response.data
      if (value) {
        this.initPassword = String(value)
      }
    }).catch(() => {});
  },
  methods: {
    formatErpNos,
    handleDetail(row) {
      if (!row || !row.id) {
        return
      }
      this.$router.push({
        name: 'SystemUserDetail',
        query: { userId: row.id }
      }).catch(() => {})
    },
    handleRowClick(row, column, event) {
      if (!row || !row.id) {
        return
      }
      const target = event && event.target
      if (target && target.closest('.el-switch, .el-button, .el-link, .el-checkbox, .el-dropdown')) {
        return
      }
      this.handleDetail(row)
    },
    // 更多操作
    handleCommand(command, index, row) {
      switch (command) {
        case 'handleUpdate':
          this.handleUpdate(row);//修改客户信息
          break;
        case 'handleDelete':
          this.handleDelete(row);//红号变更
          break;
        case 'handleResetPwd':
          this.handleResetPwd(row);
          break;
        case 'handleRole':
          this.handleRole(row);
          break;
        default:
          break;
      }
    },
    /** 查询用户列表 */
    getList() {
      const seq = (this._listSeq = (this._listSeq || 0) + 1)
      this.loading = true;
      listUser(this.queryParams).then(response => {
          // 快速切筛选/部门：只认最后一次请求，避免旧结果覆盖新结果
          if (seq !== this._listSeq) {
            return
          }
          this.userList = response.data.list;
          this.total = response.data.total;
        }
      ).catch(() => {
        if (seq === this._listSeq) {
          this.userList = []
          this.total = 0
        }
      }).finally(() => {
        if (seq === this._listSeq) {
          this.loading = false
        }
      });
    },
    /** 查询部门下拉树结构 + 岗位下拉。只保留当前数据权限内的部门及其下级，上级部门不出现、也不能选。 */
    getTreeselect() {
      listSimpleDepts().then(response => {
        this.deptOptions = this.buildAssignableDeptTree(response.data)
      });
      listSimplePosts().then(response => {
        // 处理 postOptions 参数
        this.postOptions = [];
        this.postOptions.push(...response.data);
      });
    },
    buildAssignableDeptTree(list) {
      const selectable = (list || []).filter(item => !item.disabled)
      const map = new Map()
      selectable.forEach(item => {
        map.set(item.id, { ...item, isDisabled: false })
      })
      const roots = []
      map.forEach(node => {
        const parent = map.get(node.parentId)
        if (parent) {
          if (!parent.children) {
            parent.children = []
          }
          parent.children.push(node)
        } else {
          roots.push(node)
        }
      })
      return roots
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.name.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.getList();
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === CommonStatusEnum.ENABLE ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.username + '"用户吗?').then(function() {
          return changeUserStatus(row.id, row.status);
        }).then(() => {
          this.$modal.msgSuccess(text + "成功");
        }).catch(function() {
          row.status = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE
              : CommonStatusEnum.ENABLE;
        });
    },
    // 取消按钮
    cancel() {
      this.confirmCloseDialog(this.dialogFormValue(), () => {
        this._userLoadSeq = (this._userLoadSeq || 0) + 1
        this.open = false;
        this.reset();
      })
    },
    /** 关闭弹窗（右上角/遮罩/ESC）前做一次脏检查 */
    handleDialogBeforeClose(done) {
      this.confirmCloseDialog(this.dialogFormValue(), () => {
        this._userLoadSeq = (this._userLoadSeq || 0) + 1
        this.open = false
        this.reset()
        done()
      })
    },
    /** 参与脏检查的表单值（含登记业务系统的选择） */
    dialogFormValue() {
      return {
        ...this.form,
        _syncSubSystem: !!this.syncSubSystem,
        _syncTargets: this.syncSubSystem ? (this.syncTargets || []) : []
      }
    },
    // 取消按钮（角色权限）
    cancelRole() {
      this.openRole = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        deptId: undefined,
        username: undefined,
        nickname: undefined,
        password: undefined,
        mobile: undefined,
        sex: undefined,
        status: "0",
        remark: undefined,
        employeeNo: undefined,
        domainNo: undefined,
        cardNo: undefined,
        erpNos: [],
        postIds: [],
        roleIds: []
      };
      this.syncSubSystem = false;
      this.syncHangPath = [];
      this.syncTargets = [];
      this.syncHangableTree = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNo = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this._userLoadSeq = (this._userLoadSeq || 0) + 1
      this.reset();
      // 获得下拉数据
      this.getTreeselect();
      // 业务系统/车间树在勾选"登记业务系统"时再拉，避免每次打开新增都请求
      // 打开表单，并设置初始化
      this.open = true;
      this.title = "添加用户";
      this.form.password = this.initPassword;
      this.markDialogPristine(this.dialogFormValue());
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      const id = row.id;
      const seq = (this._userLoadSeq = (this._userLoadSeq || 0) + 1)
      getUser(id).then(response => {
        // 连点两行/加载中切去新增：丢弃过期响应，避免旧详情盖掉当前弹窗
        if (seq !== this._userLoadSeq) {
          return
        }
        this.form = {
          ...response.data,
          erpNos: Array.isArray(response.data.erpNos) ? response.data.erpNos : []
        };
        this.open = true;
        this.title = "修改用户";
        this.markDialogPristine(this.dialogFormValue());
      });
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.username + '"的新密码', "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消"
      }).then(({ value }) => {
          resetUserPwd(row.id, value).then(response => {
            this.$modal.msgSuccess("修改成功，新密码是：" + value);
          });
        }).catch(() => {});
    },
    /** 分配用户角色操作 */
    handleRole(row) {
      this.reset();
      const id = row.id
      // 处理了 form 的用户 username 和 nickname 的展示
      this.form.id = id;
      this.form.username = row.username;
      this.form.nickname = row.nickname;
      // 打开弹窗
      this.openRole = true;
      // 获得角色列表
      listSimpleRoles().then(response => {
        // 处理 roleOptions 参数
        this.roleOptions = [];
        this.roleOptions.push(...response.data);
      });
      // 获得角色拥有的菜单集合
      listUserRoles(id).then(response => {
        // 设置选中
        this.form.roleIds = response.data;
      })
    },
    /** 提交按钮 */
    submitForm: function() {
      if (this.submitting) {
        return
      }
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.id === undefined
            && this.syncSubSystem && (!this.syncTargets || !this.syncTargets.length)) {
          this.$modal.msgWarning("请选择要登记的业务系统（多车间系统需选到车间叶子）");
          return;
        }
        this.submitting = true
        if (this.form.id !== undefined) {
          updateUser(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.open = false;
            this.getList();
          }).finally(() => {
            this.submitting = false
          });
        } else {
          // 勾选登记时随用户创建一并提交（后端同事务写入外部用户管理，不调外部接口）
          const payload = Object.assign({}, this.form, {
            subSystemTargets: this.syncSubSystem ? this.syncTargets : []
          });
          // 旧字段只给旧调用用，这里不再发送，避免多车间系统被当成"未选叶子"
          delete payload.subSystemIds;
          addUser(payload).then(() => {
            this.$modal.msgSuccess("新增成功");
            this.open = false;
            this.getList();
          }).finally(() => {
            this.submitting = false
          });
        }
      });
    },
    /** 勾选登记业务系统：拉取"系统/车间叶子"树（仅叶子可选） */
    handleSyncChange(val) {
      if (val) {
        this.loadSyncHangableTree();
      } else {
        this.syncHangPath = [];
        this.syncTargets = [];
      }
    },
    loadSyncHangableTree() {
      return getHangableExternalTree().then(res => {
        this.syncHangableTree = res.data || [];
        if (this.syncHangableTree.length === 0) {
          this.$modal.msgWarning("没有已绑定门户的业务系统。请先在【业务系统管理】绑定门户");
          return;
        }
        // 只有一个可选叶子时自动选中
        const leaves = this.flattenHangableLeaves(this.syncHangableTree);
        if (leaves.length === 1 && (!this.syncHangPath || !this.syncHangPath.length)) {
          this.syncHangPath = [this.syncLeafKey(leaves[0].subSystemId, leaves[0].workshopCode)];
          this.handleSyncTargetChange(this.syncHangPath);
        }
      }).catch(() => {
        this.syncHangableTree = [];
        // 加载失败（含切页主动取消）由请求拦截器统一处理；空树会显示提示
        console.warn('[user] 登记业务系统树加载失败')
      });
    },
    syncLeafKey(subSystemId, workshopCode) {
      const workshop = workshopCode == null ? '' : String(workshopCode);
      return subSystemId + '|' + workshop;
    },
    flattenHangableLeaves(nodes) {
      const leaves = [];
      const walk = list => {
        (list || []).forEach(node => {
          if (!node) {
            return;
          }
          if (node.nodeType === 'group') {
            walk(node.children);
          } else {
            leaves.push(node);
          }
        });
      };
      walk(nodes);
      return leaves;
    },
    findSyncLeaf(key) {
      return this.flattenHangableLeaves(this.syncHangableTree)
        .find(leaf => this.syncLeafKey(leaf.subSystemId, leaf.workshopCode) === key) || null;
    },
    handleSyncTargetChange(keys) {
      const list = Array.isArray(keys) ? keys : (keys ? [keys] : []);
      const targets = [];
      list.forEach(key => {
        const leaf = this.findSyncLeaf(key);
        if (leaf && leaf.subSystemId) {
          targets.push({
            subSystemId: leaf.subSystemId,
            workshopId: leaf.workshopCode ? String(leaf.workshopCode) : undefined
          });
        }
      });
      this.syncTargets = targets;
    },
    /** 提交按钮（角色权限） */
    submitRole: function() {
      if (this.form.id !== undefined) {
        assignUserRole({
          userId: this.form.id,
          roleIds: this.form.roleIds,
        }).then(response => {
          this.$modal.msgSuccess("分配角色成功");
          this.openRole = false;
          this.getList();
        });
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除用户编号为"' + row.id + '"的数据项?').then(() => {//红号变更
        return delUser(row.id);
      }).then(() => {
        // 删除的是当前页最后一条 → 回退一页，避免停在空列表
        if (this.userList && this.userList.length === 1 && this.queryParams.pageNo > 1) {
          this.queryParams.pageNo -= 1
        }
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.confirm('是否确认导出所有用户数据项?').then(() => {
        // 处理查询参数
        let params = {...this.queryParams};
        params.pageNo = undefined;
        params.pageSize = undefined;
        this.exportLoading = true;
        return exportUser(params);
      }).then(response => {
        this.$download.excel(response, '用户数据.xls');
      }).finally(() => {
        this.exportLoading = false;
      });
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "用户导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      importTemplate().then(response => {
        this.$download.excel(response, '用户导入模板.xls');
      });
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      if (response.code !== 0) {
        this.$modal.msgError(response.msg)
        return;
      }
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      // 拼接提示语
      let data = response.data;
      let text = '创建成功数量：' + data.createUsernames.length;
      for (const username of data.createUsernames) {
        text += '<br />&nbsp;&nbsp;&nbsp;&nbsp;' + username;
      }
      text += '<br />更新成功数量：' + data.updateUsernames.length;
      for (const username of data.updateUsernames) {
        text += '<br />&nbsp;&nbsp;&nbsp;&nbsp;' + username;
      }
      text += '<br />更新失败数量：' + Object.keys(data.failureUsernames).length;
      for (const username in data.failureUsernames) {
        text += '<br />&nbsp;&nbsp;&nbsp;&nbsp;' + username + '：' + data.failureUsernames[username];
      }
      this.$alert(text, "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },
    // 格式化部门的下拉框
    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
        children: node.children,
        // 数据权限之外的父级只用于展示层级，不能选择
        isDisabled: !!node.disabled
      }
    },
    /** 批量删除操作 */
    async handleDeleteBatch() {
      await this.$modal.confirm('是否确认批量删除选中的用户数据?')
      try {
        await delUserList(this.checkedIds);
        this.checkedIds = [];
        this.checkedIds = [];
        await this.getList();
        this.$modal.msgSuccess("批量删除成功");
      } catch {}
    },
    /** 选择行数据 */
    handleRowCheckboxChange(records) {
      // 过滤掉 id 为 1 的用户（系统管理员）
      this.checkedIds = records.filter(item => item.id !== 1).map((item) => item.id);
    },
  }
};
</script>

<style scoped>
.user-list-table ::v-deep .el-table__row {
  cursor: pointer;
}
.sync-hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
