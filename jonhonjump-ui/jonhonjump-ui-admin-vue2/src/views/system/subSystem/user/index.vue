<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：业务系统列表（与业务菜单同款卡片） -->
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
              <el-tag size="mini" type="info">{{ item.userCount || 0 }} 人</el-tag>
            </div>
          </div>
          <el-empty v-if="!clientsLoading && filteredClientList.length === 0" description="暂无业务系统" :image-size="60" />
        </div>
      </el-col>

      <!-- 用户数据（子表 sub_system_users） -->
      <el-col :span="20" :xs="24" v-loading="clientsLoading">
        <el-alert
          v-if="showSubSystemBindHint"
          title="与「人员管理 → 用户详情」同源：可按系统维度在此创建/关联主系统用户，也可在人员详情挂系统；分配角色后门户才有入口"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
        />
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="业务系统" prop="subSystemId">
            <el-select
              v-model="queryParams.subSystemId"
              placeholder="请选择业务系统"
              clearable
              filterable
              style="width: 240px"
            >
              <el-option
                v-for="item in clientList"
                :key="item.id"
                :label="item.name + ' (' + item.clientId + ')'"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="workshopFilterOptions.length" label="车间" prop="workshopId">
            <el-select
              v-model="queryParams.workshopId"
              placeholder="车间筛选"
              clearable
              filterable
              style="width: 200px"
              @change="handleQuery"
            >
              <el-option
                v-for="item in workshopFilterOptions"
                :key="item.workshopCode"
                :label="(item.workshopName || '') + ' (' + item.workshopCode + ')'"
                :value="item.workshopCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="queryParams.nickname" placeholder="请输入用户姓名" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="班组名称" prop="teamName">
            <el-input v-model="queryParams.teamName" placeholder="请输入班组名称" clearable style="width: 240px"
                      @keyup.enter.native="handleQuery"/>
          </el-form-item>
          <el-form-item label="JUMP用户" prop="status">
            <el-select v-model="queryParams.status" placeholder="是否有同名 JUMP 用户" clearable style="width: 180px">
              <el-option label="无" value="unlinked" />
              <el-option label="有" value="0" />
              <el-option label="有（禁用）" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="外部关联" prop="employeeRegistered">
            <el-select v-model="queryParams.employeeRegistered" placeholder="请选择" clearable style="width: 130px">
              <el-option label="未关联" value="0" />
              <el-option label="已关联外部" value="1" />
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
                         v-hasPermi="['sub-system:user:create']">新增</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport"
                         v-hasPermi="['sub-system:user:create']">导入</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button
                type="danger"
                plain
                icon="el-icon-delete"
                size="mini"
                :disabled="checkedIds.length === 0"
                @click="handleDeleteBatch"
                v-hasPermi="['sub-system:user:delete']"
              >批量删除</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
          </el-row>

          <el-table v-loading="loading" :data="userList" @selection-change="handleRowCheckboxChange">
            <el-table-column type="selection" width="55" fixed="left" />
            <el-table-column label="用户名" prop="username" :show-overflow-tooltip="true" width="110" fixed="left" />
            <el-table-column label="用户姓名" prop="nickname" :show-overflow-tooltip="true" width="100" fixed="left" />
            <el-table-column label="车间编号" prop="workshopId" width="120" :show-overflow-tooltip="true">
              <template v-slot="scope">
                {{ scope.row.workshopId || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="关联车间编号" align="center" width="120">
              <template v-slot="scope">
                <el-tag
                  v-if="scope.row.usernameWithWorkshop === '1'"
                  type="success"
                  size="mini"
                >车间_工号</el-tag>
                <el-tag v-else type="info" size="mini">工号</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="班组编码" prop="teamId" width="110" />
            <el-table-column label="班组名称" prop="teamName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="岗位" prop="postNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="角色" prop="roleNames" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="JUMP用户" align="center" width="110">
              <template v-slot="scope">
                <el-tag :type="displayStatusType(scope.row)" size="mini">
                  {{ displayStatusLabel(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="外部关联" align="center" width="110">
              <template v-slot="scope">
                <el-tag :type="scope.row.employeeRegistered === '1' ? 'success' : 'info'" size="mini">
                  {{ scope.row.employeeRegistered === '1' ? '已关联外部' : '未关联' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
              <template v-slot="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="300" fixed="right" class-name="small-padding fixed-width">
              <template v-slot="scope">
                <el-button size="mini" type="text" icon="el-icon-connection" @click="handleBind(scope.row)"
                           v-hasPermi="['sub-system:user:update']">关联外部</el-button>
                <el-button size="mini" type="text" icon="el-icon-edit"
                           :loading="openingKey === 'update:' + scope.row.id"
                           @click="handleUpdate(scope.row)"
                           v-hasPermi="['sub-system:user:update']">修改</el-button>
                <el-button size="mini" type="text" icon="el-icon-circle-check"
                           :loading="openingKey === 'role:' + scope.row.id"
                           @click="handleRole(scope.row)"
                           v-hasPermi="['sub-system:user:update']">分配角色</el-button>
                <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['sub-system:user:delete']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNo" :limit.sync="queryParams.pageSize"
                      @pagination="getList"/>
      </el-col>
    </el-row>

    <!-- 新增/修改：关闭销毁表单，避免下次 resetFields 踩到上次残留字段 -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body destroy-on-close :before-close="handleDialogBeforeClose">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item v-if="!form.id" label="挂接目标" prop="hangLeafKey">
          <el-cascader
            v-model="hangPath"
            :options="hangableCascaderOptions"
            :props="hangCascaderProps"
            filterable
            clearable
            placeholder="请选择业务系统/车间叶子（与人员管理挂接同源）"
            style="width: 100%"
            @change="handleHangLeafChange"
          />
          <div class="form-tip">与用户管理「挂接业务系统」同源：选叶子后带出系统与车间编号，车间编号仍可改</div>
        </el-form-item>
        <el-form-item v-else label="业务系统">
          <el-input :value="editClientLabel" disabled />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入业务系统用户名" maxlength="64" />
        </el-form-item>
        <el-form-item label="用户姓名" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入用户姓名" maxlength="64" />
        </el-form-item>
        <el-divider content-position="left">组织与权限</el-divider>
        <el-form-item label="车间编号" prop="workshopId">
          <el-select
            v-if="formWorkshopOptions.length"
            v-model="form.workshopId"
            placeholder="请选择车间叶子，没有车间维度可留空"
            clearable
            filterable
            style="width: 100%"
            @change="handleWorkshopChange"
          >
            <el-option
              v-for="item in formWorkshopOptions"
              :key="item.workshopCode"
              :label="(item.workshopName || '') + ' (' + item.workshopCode + ')'"
              :value="item.workshopCode"
            />
          </el-select>
          <el-input
            v-else
            v-model="form.workshopId"
            :placeholder="syncWorkshopRequired ? 'Camstar 同步注册请填写车间编号' : '该系统没有车间叶子，可留空'"
            maxlength="50"
            clearable
            @change="handleWorkshopChange"
          />
          <div v-if="form.id" class="form-tip">
            修改用户名或车间后，已关联的外部账号会变为“未关联”，需要重新注册或关联
          </div>
        </el-form-item>
        <el-form-item v-if="form.id" label="班组" prop="teamId">
          <el-select v-model="form.teamId" placeholder="请选择班组" clearable filterable style="width: 100%">
            <el-option
              v-for="item in teamOptions"
              :key="item.teamCode"
              :label="item.teamName + ' (' + item.teamCode + ')'"
              :value="item.teamCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="岗位">
          <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width: 100%">
            <el-option
              v-for="item in postOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
          <div class="form-tip">这里是本行启用还是禁用。列表「JUMP用户」另看有没有同名的主系统用户</div>
        </el-form-item>
        <template v-if="!form.id">
          <el-form-item label="同步注册">
            <el-checkbox
              v-model="form.syncToExternal"
              :disabled="!registerApis.length"
              @change="applyDefaultAddRegisterApi"
            >同步注册到外部系统（调「新增人员」接口）</el-checkbox>
            <div class="form-tip">
              <span v-if="registerApis.length">新建后同时在对方系统建人，成功后标记为已关联外部</span>
              <span v-else style="color:#e6a23c">未找到已启用的「新增人员」接口。请到【接口管理】配置并启用</span>
            </div>
          </el-form-item>
          <el-form-item v-if="form.syncToExternal" label="接口目标" prop="apiSubSystemId">
            <el-select
              v-model="form.apiSubSystemId"
              placeholder="请选择要调用的新增人员接口"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in registerApis"
                :key="item.subSystemId"
                :label="item.systemName"
                :value="item.subSystemId"
              />
            </el-select>
            <div class="form-tip">接口来自【接口管理】中「新增」用途已启用的系统，可与左侧花名册系统不同</div>
          </el-form-item>
        </template>
        <el-form-item label="关联车间编号" prop="usernameWithWorkshop">
          <el-radio-group v-model="form.usernameWithWorkshop" size="mini">
            <el-radio label="0" border>工号</el-radio>
            <el-radio label="1" border>车间编号_工号</el-radio>
          </el-radio-group>
          <div class="form-tip">
            同步注册外部时选「车间编号_工号」即以该形态建账号；切换系统 Cookie 也认此字段（车间编号为空时仍用工号）
          </div>
        </el-form-item>
        <el-form-item v-if="form.id" label="外部关联">
          <el-radio-group v-model="form.employeeRegistered">
            <el-radio label="0">未关联</el-radio>
            <el-radio label="1">已关联外部</el-radio>
          </el-radio-group>
          <div class="form-tip">标记未关联后，可重新通过【关联外部】或【同步注册】对接对方系统{{ form.registeredApiType ? '（注册接口：' + form.registeredApiType + '）' : '' }}</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配角色：业务系统角色分配的唯一入口 -->
    <el-dialog title="分配角色" :visible.sync="openRole" width="560px" append-to-body>
      <el-form :model="roleForm" label-width="110px">
        <el-form-item label="用户姓名">
          <el-input v-model="roleForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width: 100%" filterable>
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="roleOptionLabel(item)"
              :value="item.id"
              :disabled="roleOptionDisabled(item)"
            />
          </el-select>
          <div v-if="roleForm.workshopId" class="form-tip">
            当前车间「{{ roleForm.workshopId }}」，只能选择该车间下的角色或未打车间的角色
          </div>
        </el-form-item>
        <el-form-item label="同步外部">
          <el-checkbox v-model="roleForm.syncToExternal">调用外部系统接口同步上挂/解除角色</el-checkbox>
          <div class="form-tip">
            勾选的角色必须已在「业务角色管理」中关联外部角色，否则本次同步失败；只增删 JUMP 管辖范围内的角色，对方系统手工挂的角色（含 Login）保持不变
          </div>
        </el-form-item>
        <el-form-item v-if="roleForm.syncToExternal" label="接口目标">
          <el-select v-model="roleForm.apiSubSystemId" placeholder="请选择调用哪个系统的人员接口" filterable style="width: 100%">
            <el-option
              v-for="item in registerApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
          <div v-if="!registerApis.length" class="form-tip" style="color:#f56c6c">
            没有已启用「新增人员」的接口目标，请先在【接口管理】接入并启用
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">确 定</el-button>
        <el-button @click="openRole = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 业务系统用户导入（须先选择并确认已关联的业务系统） -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="460px" append-to-body>
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
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已存在的用户名数据
          </div>
          <span>仅允许 xls/xlsx。按用户名写入业务系统花名册，可不关联主系统用户。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 关联外部人员：选对方已有人员，或当场调「新增人员」再建 -->
    <el-dialog title="关联外部人员" :visible.sync="bindOpen" width="560px" append-to-body :close-on-click-modal="false">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input :value="bindForm.username + (bindForm.nickname ? '（' + bindForm.nickname + '）' : '')" disabled />
        </el-form-item>
        <el-form-item label="接口目标" required>
          <el-select v-model="bindForm.apiSubSystemId" placeholder="请选择外部系统" filterable style="width: 100%"
                     :disabled="bindLoading" @change="onBindFilterChange">
            <el-option
              v-for="item in registerApis"
              :key="item.subSystemId"
              :label="item.systemName"
              :value="item.subSystemId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="车间编号" required>
          <el-input
            v-model="bindForm.workshopCode"
            placeholder="请填写车间编号"
            maxlength="50"
            clearable
            :disabled="bindLoading"
            @change="onBindFilterChange"
          />
          <div class="form-tip">优先用业务用户上的车间编号；新增外部人员时必填</div>
        </el-form-item>
        <el-form-item label="外部人员">
          <div style="margin-bottom: 6px">
            <el-button size="mini" type="primary" plain :loading="bindLoading"
                       :disabled="!bindForm.apiSubSystemId"
                       @click="loadExternalEmployees">查询外部人员</el-button>
            <el-button size="mini" type="primary" plain :loading="bindRegistering"
                       :disabled="!bindForm.apiSubSystemId || !bindForm.workshopCode"
                       @click="pushRegisterFromBind">新增外部人员</el-button>
          </div>
          <el-checkbox v-model="bindForm.usernameWithWorkshop" style="margin-bottom: 6px">关联车间编号：注册为 车间编号_工号</el-checkbox>
          <el-select
            v-model="bindForm.userCode"
            filterable
            clearable
            :loading="bindLoading"
            :placeholder="bindLoading ? '正在查询外部人员…' : '选择对应的人员（姓名 + 工号）'"
            style="width: 100%"
          >
            <el-option
              v-for="item in externalEmployees"
              :key="item.userCode"
              :label="(item.userName || item.userCode) + '（' + item.userCode + (item.workshopName || item.workshopCode ? '，' + (item.workshopName || item.workshopCode) : '') + '）'"
              :value="item.userCode"
            />
          </el-select>
          <div class="form-tip">{{ externalEmployeeTip }}</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="bindForm.employeeRegistered === '1'" type="text" :loading="bindSaving" @click="clearExternalEmployee">解除关联</el-button>
        <el-button type="primary" :loading="bindSaving" :disabled="!bindForm.userCode" @click="submitBind">保存关联</el-button>
        <el-button @click="bindOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignSubSystemUserRole,
  createSubSystemUser,
  deleteSubSystemUser,
  deleteSubSystemUserList,
  getSubSystemClientSimpleList,
  getSubSystemPostSimpleList,
  getSubSystemRoleSimpleList,
  getSubSystemTeamSimpleList,
  getSubSystemUser,
  getSubSystemUserPage,
  getSubSystemUserRoleIds,
  importSubSystemUserTemplate,
  updateSubSystemUser,
  bindExternalEmployee
} from '@/api/system/subSystemUsers'
import {
  getSubSystemRegisterableApis,
  listExternalEmployees,
  registerSubSystemEmployee
} from '@/api/system/subSystemEmployee'
import { getHangableExternalTree } from '@/api/system/subSystemWorkshop'
import { getUser } from '@/api/system/user'
import { getBaseHeader } from '@/utils/request'
import subSystemImportGate from '@/utils/subSystemImportGate'
import { escapeHtml } from '@/utils/escapeHtml'
import { subSystemRoleOptionLabel, isSubSystemRoleOptionDisabled } from '@/utils/subSystemRoleOption'
import dialogDirtyGuard from '@/mixins/dialogDirtyGuard'
import workshopLeftNav from '../mixins/workshopLeftNav'

export default {
  name: 'SubSystemUser',
  mixins: [subSystemImportGate, workshopLeftNav, dialogDirtyGuard],
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      userList: [],
      clientList: [],
      clientKeyword: '',
      /** mixin 左侧树叶子计数字段 */
      leftNavCountKey: 'userCount',
      selectedClient: null,
      listSubSystemId: null,
      roleOptions: [],
      postOptions: [],
      teamOptions: [],
      hangableTree: [],
      hangPath: [],
      hangCascaderProps: {
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: true,
        checkStrictly: false
      },
      title: '',
      open: false,
      submitting: false,
      openRole: false,
      roleSubmitting: false,
      form: {},
      roleForm: {},
      mainUserInfo: {},
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
        subSystemId: undefined,
        username: undefined,
        mainUserId: undefined,
        workshopId: undefined,
        nickname: undefined,
        teamName: undefined,
        status: undefined,
        employeeRegistered: undefined
      },
      registerApis: [],
      bindOpen: false,
      bindLoading: false,
      bindSaving: false,
      bindRegistering: false,
      externalEmployees: [],
      externalEmployeeTip: '',
      bindForm: {
        id: undefined,
        username: '',
        nickname: '',
        employeeRegistered: '0',
        apiSubSystemId: undefined,
        workshopCode: undefined,
        userCode: undefined,
        usernameWithWorkshop: false
      },
      // 按行锁定打开中状态；勿用全局 boolean，否则一挂全表「修改」点不动，关页重进才恢复
      openingKey: '',
      rules: {
        hangLeafKey: [{
          validator: (rule, value, callback) => {
            if (this.form.id) {
              callback()
              return
            }
            if (!value && !this.form.subSystemId) {
              callback(new Error('请选择挂接的业务系统/车间'))
              return
            }
            callback()
          },
          trigger: 'change'
        }],
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
        apiSubSystemId: [{
          validator: (rule, value, callback) => {
            if (this.form.id || !this.form.syncToExternal) {
              callback()
              return
            }
            if ((this.registerApis || []).length && !value) {
              callback(new Error('请选择接口目标'))
              return
            }
            callback()
          },
          trigger: 'change'
        }],
        workshopId: [{
          validator: (rule, value, callback) => {
            if (!this.syncWorkshopRequired) {
              callback()
              return
            }
            if (!value) {
              callback(new Error('请选择车间'))
              return
            }
            callback()
          },
          trigger: 'change'
        }]
      }
    }
  },
  computed: {
    filteredClientList() {
      const keyword = (this.clientKeyword || '').trim().toLowerCase()
      if (!keyword) {
        return this.clientList
      }
      return (this.clientList || []).filter(item =>
        (item.name && item.name.toLowerCase().includes(keyword)) ||
        (item.clientId && item.clientId.toLowerCase().includes(keyword))
      )
    },
    formTargetSystem() {
      const id = this.form && this.form.subSystemId
      if (id != null) {
        const hit = (this.clientList || []).find(item => item.id === id)
        if (hit) {
          return hit
        }
      }
      return this.selectedClient || null
    },
    formWorkshopOptions() {
      const system = this.formTargetSystem
      const id = system && system.id
      if (!id) {
        return []
      }
      return (this.workshopList || []).filter(item => item.subSystemId === id)
    },
    syncWorkshopRequired() {
      if (!this.form || this.form.id || !this.form.syncToExternal) {
        return false
      }
      const api = (this.registerApis || []).find(item => item.subSystemId === this.form.apiSubSystemId)
      const camstar = !!(api && api.apiType && String(api.apiType).toLowerCase().indexOf('camstar') >= 0)
      const system = this.formTargetSystem
      const multi = !!(system && system.workshopScopeEnabled) && this.formWorkshopOptions.length > 1
      return camstar || multi
    },
    hangableCascaderOptions() {
      return (this.hangableTree || []).map(node => {
        if (node.nodeType === 'group') {
          const children = (node.children || []).map(c => ({
            value: this.leafKey(c.subSystemId, c.workshopCode),
            label: c.label,
            leafMeta: c
          }))
          if (!children.length) {
            return null
          }
          return {
            value: 'g-' + node.subSystemId,
            label: node.label,
            children
          }
        }
        return {
          value: this.leafKey(node.subSystemId, node.workshopCode),
          label: node.label,
          leafMeta: node
        }
      }).filter(Boolean)
    },
    editClientLabel() {
      const hit = (this.clientList || []).find(c => c.id === this.form.subSystemId)
      if (hit) {
        return (hit.name || '') + ' (' + (hit.clientId || '') + ')'
      }
      return this.selectedClient
        ? (this.selectedClient.name + ' (' + this.selectedClient.clientId + ')')
        : ''
    },
    uploadAction() {
      const id = this.selectedClient && this.selectedClient.id
      const update = this.upload.updateSupport ? 'true' : 'false'
      return process.env.VUE_APP_BASE_API + '/admin-api/system/sub-system-users/import'
        + '?subSystemId=' + (id || '')
        + '&updateSupport=' + update
    },
  },
  created() {
    this.loadClientList()
    this.loadRegisterableApis()
  },
  activated() {
    this.openingKey = ''
  },
  deactivated() {
    this.openingKey = ''
  },
  methods: {
    formatErpNos(erpNos) {
      if (!erpNos) {
        return ''
      }
      if (Array.isArray(erpNos)) {
        return erpNos.join('、')
      }
      return String(erpNos)
    },
    resetMainUserInfo() {
      this.mainUserInfo = {
        nickname: undefined,
        employeeNo: undefined,
        cardNo: undefined,
        erpNos: undefined,
        domainNo: undefined
      }
    },
    fillMainUserInfo(data) {
      this.mainUserInfo = {
        nickname: data.nickname,
        employeeNo: data.employeeNo,
        cardNo: data.cardNo,
        erpNos: data.erpNos,
        domainNo: data.domainNo
      }
    },
    handleMainUserIdChange(mainUserId) {
      if (!mainUserId) {
        this.resetMainUserInfo()
        return
      }
      getUser(mainUserId).then(res => {
        this.fillMainUserInfo(res.data || {})
      }).catch(() => {
        this.resetMainUserInfo()
      })
    },
    loadClientList() {
      return this.withClientsLoading(() => {
        return Promise.all([
          getSubSystemClientSimpleList(true),
          this.loadWorkshopList()
        ]).then(([clientRes]) => {
          this.clientList = (clientRes && clientRes.data) || []
          if (this.syncSelectedClientFromList()) {
            return
          }
          if (!this.selectedClient && this.clientList.length > 0) {
            this.handleClientClick(this.clientList[0])
          }
        })
      })
    },
    onWorkshopNavSelected(item) {
      this.queryParams.subSystemId = item.subSystemId
      this.queryParams.workshopId = item.workshopCode
      this.queryParams.username = undefined
      this.queryParams.nickname = undefined
      this.queryParams.teamName = undefined
      this.queryParams.status = undefined
      this.queryParams.employeeRegistered = undefined
      this.queryParams.pageNo = 1
      this.listSubSystemId = item.subSystemId
      this.getList()
    },
    onSystemNavSelected(item) {
      this.handleClientClick(item)
    },
    loadSubOptions(subSystemId) {
      const id = subSystemId || (this.selectedClient ? this.selectedClient.id : null)
      if (!id) {
        this.roleOptions = []
        this.postOptions = []
        this.teamOptions = []
        return Promise.resolve()
      }
      // 单项失败不影响其它下拉；对齐主系统用户管理：选项后台加载，不堵弹窗
      return Promise.all([
        getSubSystemRoleSimpleList(id).then(res => { this.roleOptions = res.data || [] }).catch(() => { this.roleOptions = [] }),
        getSubSystemPostSimpleList(id).then(res => { this.postOptions = res.data || [] }).catch(() => { this.postOptions = [] })
      ]).then(() => this.reloadTeams(id).catch(() => { this.teamOptions = [] }))
    },
    leafKey(subSystemId, workshopId) {
      const w = workshopId == null || workshopId === '' ? '' : String(workshopId)
      return subSystemId + '|' + w
    },
    findHangLeafMeta(key) {
      for (const node of this.hangableTree || []) {
        if (node.nodeType === 'group') {
          const hit = (node.children || []).find(c => this.leafKey(c.subSystemId, c.workshopCode) === key)
          if (hit) {
            return hit
          }
        } else if (this.leafKey(node.subSystemId, node.workshopCode) === key) {
          return node
        }
      }
      return null
    },
    loadHangableTree() {
      return getHangableExternalTree().then(res => {
        this.hangableTree = res.data || []
      }).catch(() => {
        this.hangableTree = []
      })
    },
    /** 左侧已选系统时，新增默认挂到该系统下的叶子（一对一直接选中） */
    preselectHangFromLeftNav() {
      const ssId = this.selectedClient && this.selectedClient.id
      if (!ssId || !(this.hangableTree || []).length) {
        return
      }
      const matches = []
      for (const node of this.hangableTree) {
        if (node.nodeType === 'group' && node.subSystemId === ssId) {
          (node.children || []).forEach(c => matches.push(c))
        } else if (node.subSystemId === ssId) {
          matches.push(node)
        }
      }
      if (!matches.length) {
        return
      }
      const prefer = this.selectedWorkshop
        ? matches.find(m => m.workshopCode === this.selectedWorkshop.workshopCode)
        : null
      const leaf = prefer || (matches.length === 1 ? matches[0] : null)
      if (!leaf) {
        return
      }
      const key = this.leafKey(leaf.subSystemId, leaf.workshopCode)
      if (prefer || matches.length === 1) {
        // 组下叶子：cascader path = [group, leaf]
        const group = (this.hangableTree || []).find(n =>
          n.nodeType === 'group' && n.subSystemId === ssId
        )
        this.hangPath = group ? ['g-' + ssId, key] : [key]
        this.handleHangLeafChange(this.hangPath)
      }
    },
    handleHangLeafChange(path) {
      const key = path && path.length ? path[path.length - 1] : null
      this.form.hangLeafKey = key
      const meta = key ? this.findHangLeafMeta(key) : null
      this.form.subSystemId = meta ? meta.subSystemId : undefined
      this.form.workshopId = meta && meta.workshopCode ? meta.workshopCode : undefined
      this.form.teamId = undefined
      this.form.postIds = []
      if (this.form.subSystemId) {
        this.loadSubOptions(this.form.subSystemId)
        this.loadRegisterableApis().then(() => this.applyDefaultAddRegisterApi())
      }
    },
    handleWorkshopChange() {
      this.form.teamId = undefined
      const id = this.form.subSystemId || (this.selectedClient && this.selectedClient.id)
      this.reloadTeams(id)
    },
    reloadTeams(subSystemId) {
      if (!subSystemId) {
        this.teamOptions = []
        return Promise.resolve()
      }
      return getSubSystemTeamSimpleList(subSystemId).then(res => {
        this.teamOptions = res.data || []
      }).catch(() => {
        this.teamOptions = []
      })
    },
    handleClientClick(item, options = {}) {
      this.leftNavMode = 'system'
      this.selectedWorkshop = null
      this.selectedClient = item
      this.queryParams.subSystemId = item.id
      this.queryParams.workshopId = options.workshopId || undefined
      this.queryParams.username = undefined
      this.queryParams.nickname = undefined
      this.queryParams.teamName = undefined
      this.queryParams.status = undefined
      this.queryParams.employeeRegistered = undefined
      this.queryParams.pageNo = 1
      this.listSubSystemId = item.id
      if (options.skipGetList !== true) {
        this.getList()
      }
    },
    getList() {
      const seq = (this._listSeq = (this._listSeq || 0) + 1)
      const params = { ...this.queryParams }
      if (this.listSubSystemId != null) {
        params.subSystemId = this.listSubSystemId
      } else if (params.subSystemId == null || params.subSystemId === '') {
        delete params.subSystemId
      }
      this.loading = true
      getSubSystemUserPage(params).then(res => {
        // 快速切系统/筛选：只认最后一次请求
        if (seq !== this._listSeq) {
          return
        }
        this.userList = res.data.list || []
        this.total = res.data.total || 0
      }).catch(() => {
        if (seq === this._listSeq) {
          this.userList = []
          this.total = 0
        }
      }).finally(() => {
        if (seq === this._listSeq) {
          this.loading = false
        }
      })
    },
    handleQuery() {
      this.listSubSystemId = null
      if (this.queryParams.subSystemId == null || this.queryParams.subSystemId === '') {
        this.queryParams.subSystemId = undefined
      }
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.subSystemId = this.selectedClient ? this.selectedClient.id : undefined
      this.queryParams.workshopId = this.selectedWorkshop ? this.selectedWorkshop.workshopCode : undefined
      this.listSubSystemId = this.selectedClient ? this.selectedClient.id : null
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetFormData() {
      this.form = {
        id: undefined,
        hangLeafKey: undefined,
        subSystemId: undefined,
        mainUserId: undefined,
        username: undefined,
        nickname: undefined,
        workshopId: undefined,
        teamId: undefined,
        status: '0',
        employeeRegistered: '0',
        syncToExternal: false,
        apiSubSystemId: undefined,
        usernameWithWorkshop: '0',
        remark: undefined,
        postIds: []
      }
      this.hangPath = []
      // 弹窗未打开时不要 resetFields：ElementUI 会对 undefined 调 indexOf 直接炸
      // 整表单已赋默认值，只需清校验；打开后再 nextTick 清一次更稳
      this.$nextTick(() => {
        if (this.$refs.form && typeof this.$refs.form.clearValidate === 'function') {
          this.$refs.form.clearValidate()
        }
      })
    },
    cancel() {
      this.confirmCloseDialog(this.dialogFormValue(), () => {
        this.open = false
        this.resetFormData()
      })
    },
    /** 关闭弹窗（右上角/遮罩/ESC）前做一次脏检查 */
    handleDialogBeforeClose(done) {
      this.confirmCloseDialog(this.dialogFormValue(), () => {
        this.open = false
        this.resetFormData()
        done()
      })
    },
    dialogFormValue() {
      return { ...this.form }
    },
    handleAdd() {
      // 与人员管理一致：挂接树选系统，不强制锁死左侧当前系统
      this.resetFormData()
      this.open = true
      this.title = '添加业务系统用户'
      this.loadHangableTree().then(() => {
        this.preselectHangFromLeftNav()
        this.$nextTick(() => {
          if (this.$refs.form && typeof this.$refs.form.clearValidate === 'function') {
            this.$refs.form.clearValidate()
          }
          // 预选完成后再记快照，避免一打开就显示"有未保存修改"
          this.markDialogPristine(this.dialogFormValue())
        })
      })
    },
    handleImport() {
      this.ensureSubSystemBoundBeforeAction('导入').then(() => {
        this.upload.title = '导入业务系统用户 — ' + (this.selectedClient.name || '')
        this.upload.open = true
        this.upload.headers = getBaseHeader()
      }).catch(() => {})
    },
    importTemplate() {
      importSubSystemUserTemplate().then(response => {
        this.$download.excel(response, '业务系统用户导入模板.xls')
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
      let text = '新建绑定：' + ((data.createKeys && data.createKeys.length) || 0)
      ;(data.createKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + escapeHtml(k) })
      text += '<br />更新绑定：' + ((data.updateKeys && data.updateKeys.length) || 0)
      ;(data.updateKeys || []).forEach(k => { text += '<br />&nbsp;&nbsp;' + escapeHtml(k) })
      const failMap = data.failureKeys || {}
      const failKeys = Object.keys(failMap)
      text += '<br />失败：' + failKeys.length
      failKeys.forEach(k => { text += '<br />&nbsp;&nbsp;' + escapeHtml(k) + '：' + escapeHtml(failMap[k]) })
      this.$alert(text, '导入结果', { dangerouslyUseHTMLString: true })
      this.getList()
      this.loadClientList()
    },
    submitFileForm() {
      this.$refs.upload.submit()
    },
    handleUpdate(row) {
      const openKey = 'update:' + row.id
      this.openingKey = openKey
      // 只等详情 → 立刻开弹窗；下拉后台补。禁止在未打开时 resetFields（会 indexOf undefined）
      getSubSystemUser(row.id).then(res => {
        // 连点两行时丢弃先到但已过期的响应，避免 A 的详情盖掉 B
        if (this.openingKey !== openKey) {
          return
        }
        this.form = {
          id: res.data.id,
          subSystemId: res.data.subSystemId,
          mainUserId: res.data.mainUserId,
          username: res.data.username,
          nickname: res.data.nickname,
          workshopId: res.data.workshopId,
          teamId: res.data.teamId,
          status: res.data.status || '0',
          employeeRegistered: res.data.employeeRegistered || '0',
          usernameWithWorkshop: res.data.usernameWithWorkshop || '0',
          registeredApiType: res.data.registeredApiType,
          remark: res.data.remark,
          postIds: res.data.postIds || [],
          apiSubSystemId: undefined
        }
        this.open = true
        this.title = '修改业务系统用户'
        this.markDialogPristine(this.dialogFormValue())
        this.openingKey = ''
        this.$nextTick(() => {
          if (this.$refs.form && typeof this.$refs.form.clearValidate === 'function') {
            this.$refs.form.clearValidate()
          }
        })
        return this.loadSubOptions(row.subSystemId || res.data.subSystemId).then(() => {
          return this.reloadTeams(this.form.subSystemId)
        })
      }).catch(() => {
        if (this.openingKey === openKey) {
          this.$modal.msgError('加载用户信息失败，请重试')
          this.openingKey = ''
        }
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
        const isAdd = !this.form.id
        if (isAdd && !this.form.subSystemId) {
          this.$modal.msgWarning('请选择挂接的业务系统/车间')
          return
        }
        const payload = Object.assign({}, this.form)
        delete payload.hangLeafKey
        const apiSubSystemId = payload.apiSubSystemId
        const syncToExternal = !!payload.syncToExternal
        delete payload.apiSubSystemId
        delete payload.syncToExternal
        // 角色入库唯一入口是「分配角色」，本表单不携带任何角色数据
        delete payload.roleIds
        // 新增不再写班组/岗位，这些只在修改弹窗里维护
        if (isAdd) {
          delete payload.teamId
          delete payload.postIds
        }
        // 关联车间编号：统一落库为 0/1；同步注册时勾选「车间编号_工号」即 usernameWithWorkshop=1
        const usernameWithWorkshop = payload.usernameWithWorkshop === '1' || payload.usernameWithWorkshop === true
        payload.usernameWithWorkshop = usernameWithWorkshop ? '1' : '0'
        if (isAdd) {
          // 新增时外部关联状态由注册结果回写，不随表单提交
          delete payload.employeeRegistered
        }
        delete payload.registeredApiType
        const shouldRegister = isAdd && syncToExternal && !!apiSubSystemId
        this.submitting = true
        const request = isAdd ? createSubSystemUser : updateSubSystemUser
        const afterSaved = (msg) => {
          this.$modal.msgSuccess(msg)
          this.open = false
          const sid = payload.subSystemId
          const client = (this.clientList || []).find(c => c.id === sid)
          if (isAdd) {
            // 新增后按保存的车间定位列表，避免仍停在别的车间筛选里看不见新人
            if (client && (!this.selectedClient || this.selectedClient.id !== sid)) {
              // handleClientClick 内部已按「新系统 + 车间」刷新一次；不要再补一次 getList，
              // 否则两个请求并发返回，先到的旧筛选结果可能覆盖后到的新结果
              this.handleClientClick(client, { workshopId: payload.workshopId })
            } else {
              this.queryParams.workshopId = payload.workshopId || undefined
              this.selectedWorkshop = null
              this.getList()
              this.loadClientList()
            }
          } else if (client && (!this.selectedClient || this.selectedClient.id !== sid)) {
            this.handleClientClick(client)
          } else {
            this.getList()
            this.loadClientList()
          }
        }
        request(payload).then(res => {
          if (!shouldRegister) {
            afterSaved(isAdd ? '新增成功' : '修改成功')
            return
          }
          return registerSubSystemEmployee({
            apiSubSystemId,
            workshopCode: payload.workshopId,
            usernameWithWorkshop,
            ids: [res.data]
          }).then(regRes => {
            const results = regRes.data || []
            const fail = results.filter(r => !r.success)
            if (fail.length) {
              this.$modal.msgError('用户已保存，但接口注册失败：' + (fail[0].message || '未知错误'))
              this.open = false
              this.getList()
              this.loadClientList()
              return
            }
            afterSaved('新增成功，已同步注册并关联外部人员')
          })
        }).catch(() => {
          // 具体原因由请求拦截器提示（如「用户名已存在」「角色车间不一致」），不再叠加通用提示
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该业务系统用户？').then(() => {
        return deleteSubSystemUser(row.id)
      }).then(() => {
        if (this.userList && this.userList.length === 1 && this.queryParams.pageNo > 1) {
          this.queryParams.pageNo -= 1
        }
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    handleDeleteBatch() {
      this.$modal.confirm('是否确认批量删除选中的业务系统用户？').then(() => {
        return deleteSubSystemUserList(this.checkedIds)
      }).then(() => {
        const removing = (this.checkedIds || []).length
        if (this.userList && this.userList.length <= removing && this.queryParams.pageNo > 1) {
          this.queryParams.pageNo -= 1
        }
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.loadClientList()
      }).catch(() => {})
    },
    /** 有没有挂上仍存在的 JUMP 用户。0 和空都是无。有且本行被禁用时标出来 */
    hasJumpUser(row) {
      const id = row && row.mainUserId
      return id != null && id !== '' && Number(id) > 0
    },
    displayStatusLabel(row) {
      if (!this.hasJumpUser(row)) {
        return '无'
      }
      return row.status === '1' ? '有（禁用）' : '有'
    },
    displayStatusType(row) {
      if (!this.hasJumpUser(row)) {
        return 'info'
      }
      return row.status === '1' ? 'danger' : 'success'
    },
    handleRole(row) {
      // 对齐主系统：先开弹窗再拉角色，不堵在 loadSubOptions 上
      this.roleForm = {
        id: row.id,
        nickname: row.nickname,
        workshopId: row.workshopId || '',
        roleIds: [],
        // 默认不同步外部：多数角色未关联外部角色，默认勾选会让“分配成功”看起来像失败
        syncToExternal: false,
        apiSubSystemId: undefined
      }
      this.openRole = true
      const openKey = 'role:' + row.id
      this.openingKey = openKey
      const sid = row.subSystemId
      Promise.all([
        getSubSystemRoleSimpleList(sid).then(res => {
          if (this.openingKey === openKey) {
            this.roleOptions = res.data || []
          }
        }).catch(() => {
          if (this.openingKey === openKey) {
            this.roleOptions = []
          }
        }),
        getSubSystemUserRoleIds(row.id).then(res => {
          if (this.openingKey === openKey) {
            this.roleForm.roleIds = res.data || []
          }
        }).catch(() => {
          if (this.openingKey === openKey) {
            this.$modal.msgError('加载已分配角色失败，请重试')
          }
        }),
        this.loadRegisterableApis()
      ]).then(() => {
        if (this.openingKey === openKey) {
          this.applyDefaultAssignApi(sid)
        }
      }).finally(() => {
        if (this.openingKey === openKey) {
          this.openingKey = ''
        }
      })
    },
    roleOptionLabel(item) {
      return subSystemRoleOptionLabel(item, { syncToExternal: this.roleForm.syncToExternal })
    },
    /** 车间叶子下：禁选其它车间编号的角色（未打车间的角色如超管可选）；停用角色也不可选 */
    roleOptionDisabled(item) {
      return isSubSystemRoleOptionDisabled(item, this.roleForm.workshopId)
    },
    /** 接口目标默认带出本花名册系统已启用的人员接口；没有同系统配置且只有一个目标时用那一个 */
    applyDefaultAssignApi(rosterSubSystemId) {
      const apis = this.registerApis || []
      const hit = apis.find(item => item.subSystemId === rosterSubSystemId)
      const picked = hit ? hit.subSystemId : (apis.length === 1 ? apis[0].subSystemId : undefined)
      if (picked && !this.roleForm.apiSubSystemId) {
        this.roleForm.apiSubSystemId = picked
      }
    },
    submitRole() {
      // 车间叶子下：已选角色不能是其它车间的（历史勾选也拦一道，与后端校验一致）
      if (this.roleForm.workshopId) {
        const bad = (this.roleForm.roleIds || []).some(id => {
          const role = this.roleOptions.find(r => r.id === id)
          return role && role.workshopCode && String(role.workshopCode) !== String(this.roleForm.workshopId)
        })
        if (bad) {
          this.$modal.msgError('请只选择当前车间「' + this.roleForm.workshopId + '」下的角色')
          return
        }
      }
      if (this.roleForm.syncToExternal && !this.roleForm.apiSubSystemId) {
        this.$modal.msgError('勾选同步外部时请选择接口目标')
        return
      }
      this.roleSubmitting = true
      assignSubSystemUserRole({
        id: this.roleForm.id,
        roleIds: this.roleForm.roleIds || [],
        syncToExternal: !!this.roleForm.syncToExternal,
        apiSubSystemId: this.roleForm.syncToExternal ? this.roleForm.apiSubSystemId : undefined
      }).then(res => {
        const data = res.data || {}
        if (data.externalSynced && data.externalSuccess === false) {
          // 本地分配已生效不回滚；明示外部失败原因，重新分配一次即重试
          this.$modal.msgError('JUMP 分配成功；外部系统同步失败：' + (data.externalMessage || '未知错误'))
        } else if (data.externalSynced) {
          this.$modal.msgSuccess('分配成功，已同步外部系统')
        } else {
          this.$modal.msgSuccess('分配成功')
        }
        this.openRole = false
        this.getList()
      }).catch(() => {
        // 具体原因由请求拦截器提示（车间角色不一致、角色已停用等）
      }).finally(() => {
        this.roleSubmitting = false
      })
    },
    handleRowCheckboxChange(selection) {
      this.checkedIds = selection.map(item => item.id)
    },
    /** 可选「新增人员」接口目标（接口管理中 create 已启用；与花名册系统解耦） */
    loadRegisterableApis() {
      return getSubSystemRegisterableApis().then(res => {
        this.registerApis = res.data || []
        this.applyDefaultAddRegisterApi()
      }).catch(() => {
        this.registerApis = []
      })
    },
    applyDefaultAddRegisterApi() {
      if (this.form.id || !this.form.syncToExternal || this.form.apiSubSystemId) {
        return
      }
      if (this.registerApis.length === 1) {
        this.form.apiSubSystemId = this.registerApis[0].subSystemId
      }
    },
    handleBind(row) {
      this.externalEmployees = []
      this.externalEmployeeTip = ''
      this.bindForm = {
        id: row.id,
        username: row.username,
        nickname: row.nickname,
        employeeRegistered: row.employeeRegistered || '0',
        apiSubSystemId: undefined,
        workshopCode: row.workshopId || undefined,
        userCode: undefined,
        usernameWithWorkshop: row.usernameWithWorkshop === '1'
      }
      this.bindOpen = true
      const sid = row.subSystemId || (this.selectedClient && this.selectedClient.id)
      Promise.all([
        this.registerApis.length ? Promise.resolve() : this.loadRegisterableApis(),
        this.loadSubOptions(sid)
      ]).then(() => {
        if (!this.bindForm.workshopCode) {
          this.bindForm.workshopCode = row.workshopId || undefined
        }
        const hit = (this.registerApis || []).find(item => item.subSystemId === sid)
        this.bindForm.apiSubSystemId = hit
          ? hit.subSystemId
          : ((this.registerApis || []).length === 1 ? this.registerApis[0].subSystemId : undefined)
      })
    },
    onBindFilterChange() {
      this.bindForm.userCode = undefined
      this.externalEmployees = []
      this.externalEmployeeTip = ''
    },
    loadExternalEmployees(silent) {
      if (!this.bindForm.apiSubSystemId) {
        if (!silent) {
          this.$modal.msgWarning('请先选择接口目标')
        }
        return
      }
      if (!this.bindForm.username) {
        if (!silent) {
          this.$modal.msgWarning('当前记录没有工号，无法查询外部人员')
        }
        return
      }
      this.bindLoading = true
      // 默认全系统按工号查（与接口管理「查询人员」相同），不按车间拉固定条数。
      // 车间仍传给后端，仅用于再查一次「车间编号_工号」
      listExternalEmployees(this.bindForm.apiSubSystemId,
        this.bindForm.workshopCode,
        this.bindForm.username).then(res => {
        const list = res.data || []
        this.externalEmployees = list
        this.externalEmployeeTip = list.length
          ? ('共 ' + list.length + ' 人（按工号全系统查询）。请选择与本地用户名相同，或 车间编号_用户名 的人员')
          : '没有查到该工号的外部人员'
        const bare = (this.bindForm.username || '').trim()
        const workshop = (this.bindForm.workshopCode || '').trim()
        const prefixed = workshop ? (workshop + '_' + bare) : ''
        const sameCode = (a, b) => a && b && String(a).trim().toLowerCase() === String(b).trim().toLowerCase()
        const hit = (prefixed && list.find(item => sameCode(item.userCode, prefixed)))
          || list.find(item => sameCode(item.userCode, bare))
        if (hit) {
          this.bindForm.userCode = hit.userCode
          this.bindForm.usernameWithWorkshop = hit.userCode === prefixed
        }
      }).catch(() => {
        this.externalEmployees = []
        this.externalEmployeeTip = '查询外部人员失败'
      }).finally(() => {
        this.bindLoading = false
      })
    },
    submitBind() {
      if (!this.bindForm.userCode) {
        this.$modal.msgWarning('请选择外部人员')
        return
      }
      this.bindSaving = true
      bindExternalEmployee(this.bindForm.id, this.bindForm.userCode, this.bindForm.workshopCode, this.bindForm.apiSubSystemId)
        .then(() => {
          this.$modal.msgSuccess('已关联外部人员')
          this.bindOpen = false
          this.getList()
        }).finally(() => {
          this.bindSaving = false
        })
    },
    clearExternalEmployee() {
      this.bindSaving = true
      bindExternalEmployee(this.bindForm.id, '', this.bindForm.workshopCode, this.bindForm.apiSubSystemId)
        .then(() => {
          this.$modal.msgSuccess('已解除关联')
          this.bindOpen = false
          this.getList()
        }).finally(() => {
          this.bindSaving = false
        })
    },
    pushRegisterFromBind() {
      this.bindRegistering = true
      registerSubSystemEmployee({
        apiSubSystemId: this.bindForm.apiSubSystemId,
        workshopCode: this.bindForm.workshopCode,
        usernameWithWorkshop: !!this.bindForm.usernameWithWorkshop,
        ids: [this.bindForm.id]
      }).then(res => {
        const results = res.data || []
        // 接口没返回任何结果时不能当成注册成功
        if (!results.length) {
          this.$modal.msgError('外部系统未返回注册结果，请刷新后重试')
          return
        }
        const fail = results.filter(r => !r.success)
        if (fail.length) {
          this.$modal.msgError(fail[0].message || '注册失败')
          return
        }
        // 展示后端真实结果（如"已注册，跳过推送"），避免把跳过推送误当成真的调了接口
        this.$modal.msgSuccess(results[0].message || '已在外部系统建人并关联')
        this.bindForm.employeeRegistered = '1'
        this.getList()
        this.loadExternalEmployees(true)
      }).catch(() => {
        // 失败原因（后端连接异常/业务错误）由全局拦截器统一提示，这里保证不再误报成功
      }).finally(() => {
        this.bindRegistering = false
      })
    },
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

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}

.register-users {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 12px;
}

.register-user-row {
  display: flex;
  align-items: center;
  /* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
  > :not(:last-child) { margin-right: 8px; }
  min-height: 28px;
  font-size: 13px;
  color: #303133;
}

.register-result-msg {
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
</style>
