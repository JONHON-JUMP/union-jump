<template>
  <div class="app-container">
    <doc-alert title="功能权限" url="https://doc.iocoder.cn/resource-permission" />
    <doc-alert title="菜单路由" url="https://doc.iocoder.cn/vue2/route/" />
    <!-- 搜索工作栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="菜单名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入菜单名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="菜单状态" clearable>
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
                   v-hasPermi="['system:menu:create']">新增</el-button>
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
          :disabled="isEmpty(checkedIds)"
          @click="handleDeleteBatch"
          v-hasPermi="['system:menu:delete']"
        >
          批量删除
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-if="refreshTable" v-loading="loading" :data="menuList" row-key="id" :default-expand-all="isExpandAll"
              :tree-props="{children: 'children', hasChildren: 'hasChildren'}" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55"/>
      <el-table-column prop="name" label="菜单名称" :show-overflow-tooltip="true" width="250"></el-table-column>
      <el-table-column prop="icon" label="图标" align="center" width="100">
        <template v-slot="scope">
          <svg-icon v-if="scope.row.icon" :icon-class="scope.row.icon" />
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="60"></el-table-column>
      <el-table-column prop="permission" label="权限标识" :show-overflow-tooltip="true" />
      <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true" />
      <el-table-column prop="componentName" label="组件名称" :show-overflow-tooltip="true" />
      <el-table-column prop="status" label="状态" width="80">
        <template v-slot="scope">
          <span>{{ statusLabelMap[scope.row.status] || scope.row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template v-slot="scope">
          <el-button v-if="canUpdateMenu" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-if="canCreateMenu" size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)">新增</el-button>
          <el-button v-if="canDeleteMenu" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改菜单对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
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
                  {{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item v-if="form.type !== 3" label="菜单图标">
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
          <el-col :span="24" v-if="form.type !== 3 && isFirstLevelMenu">
            <el-form-item label="菜单颜色">
              <menu-style-select v-model="form.styleId" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-else-if="form.type !== 3">
            <el-form-item label="菜单颜色">
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
            <el-form-item v-if="form.type !== 3" label="路由地址" prop="path">
              <span slot="label">
                <el-tooltip content="访问的路由地址，如：`user`。如需外网地址时，则以 `http(s)://` 开头" placement="top">
                <i class="el-icon-question" />
                </el-tooltip>
                路由地址
              </span>
              <el-input v-model="form.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.type !== 3">
            <el-form-item label="菜单说明书">
              <file-upload v-model="form.manualUrl" :limit="1" :file-size="20" :is-show-tip="true" />
            </el-form-item>
          </el-col>
					<el-col :span="12">
						<el-form-item v-if="form.type !== 1" label="权限标识">
              <span slot="label">
                <el-tooltip content="Controller 方法上的权限字符，如：@PreAuthorize(`@ss.hasPermission('system:user:list')`)" placement="top">
                  <i class="el-icon-question" />
                </el-tooltip>
                权限字符
              </span>
							<el-input v-model="form.permission" placeholder="请权限标识" maxlength="50" />
						</el-form-item>
					</el-col>
          <el-col :span="12" v-if="form.type === 2">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="例如说：system/user/index" />
            </el-form-item>
          </el-col>
					<el-col :span="12" v-if="form.type === 2">
						<el-form-item label="组件名称" prop="componentName">
							<el-input v-model="form.componentName" placeholder="例如说：SystemUser" />
						</el-form-item>
					</el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态" prop="status">
              <span slot="label">
                <el-tooltip content="选择停用时，路由将不会出现在侧边栏，也不能被访问" placement="top">
                  <i class="el-icon-question" />
                </el-tooltip>
                菜单状态
              </span>
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in statusDictDatas"
                          :key="dict.value" :label="parseInt(dict.value)">{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== 3" label="显示状态">
              <span slot="label">
                <el-tooltip content="选择隐藏时，路由将不会出现在侧边栏，但仍然可以访问" placement="top">
                  <i class="el-icon-question" />
                </el-tooltip>
                是否显示
              </span>
              <el-radio-group v-model="form.visible">
                <el-radio :key="true" :label="true">显示</el-radio>
                <el-radio :key="false" :label="false">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== 3" label="总是显示">
              <span slot="label">
                <el-tooltip content="选择不是时，当该菜单只有一个子菜单时，不展示自己，直接展示子菜单" placement="top">
                  <i class="el-icon-question" />
                </el-tooltip>
                 总是显示
              </span>
              <el-radio-group v-model="form.alwaysShow">
                <el-radio :key="true" :label="true">总是</el-radio>
                <el-radio :key="false" :label="false">不是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
					<el-col :span="12">
						<el-form-item v-if="form.type === 2" label="是否缓存">
              <span slot="label">
                <el-tooltip content="选择缓存时，则会被 `keep-alive` 缓存，必须填写「组件名称」字段" placement="top">
                  <i class="el-icon-question" />
                </el-tooltip>
                 是否缓存
              </span>
							<el-radio-group v-model="form.keepAlive">
								<el-radio :key="true" :label="true">缓存</el-radio>
								<el-radio :key="false" :label="false">不缓存</el-radio>
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
  </div>
</template>

<script>
import { listMenu, getMenu, delMenu, addMenu, updateMenu, delMenuList } from "@/api/system/menu";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import IconSelect from "@/components/IconSelect";
import MenuStyleSelect from "@/components/MenuStyleSelect";
import FileUpload from '@/components/FileUpload';

import { SystemMenuTypeEnum, CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import {isExternal} from "@/utils/validate";
import { flattenMenuTree, inheritedStyleId as resolveInheritedStyleId, isFirstLevelMenu as checkFirstLevelMenu } from '@/utils/menuStyleInherit'

export default {
  name: "SystemMenu",
  components: { Treeselect, IconSelect, MenuStyleSelect, FileUpload },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 菜单表格树数据
      menuList: [],
      // 菜单树选项
      menuOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部折叠
      isExpandAll: false,
      // 重新渲染表格状态
      refreshTable: true,
      // 选中行
      checkedIds: [],
      // 查询参数
      queryParams: {
        name: undefined,
        visible: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "菜单名称不能为空", trigger: "blur" }
        ],
        sort: [
          { required: true, message: "菜单顺序不能为空", trigger: "blur" }
        ],
        path: [
          { required: true, message: "路由地址不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "blur" }
        ]
      },

      // 枚举
      MenuTypeEnum: SystemMenuTypeEnum,
      CommonStatusEnum: CommonStatusEnum
    };
  },
  computed: {
    menuTypeDictDatas() {
      return this.$store.getters.dict_datas[DICT_TYPE.SYSTEM_MENU_TYPE] || []
    },
    statusDictDatas() {
      return this.$store.getters.dict_datas[DICT_TYPE.COMMON_STATUS] || []
    },
    statusLabelMap() {
      const map = {}
      this.statusDictDatas.forEach(dict => {
        map[parseInt(dict.value)] = dict.label
      })
      return map
    },
    canCreateMenu() {
      return checkPermi(['system:menu:create'])
    },
    canUpdateMenu() {
      return checkPermi(['system:menu:update'])
    },
    canDeleteMenu() {
      return checkPermi(['system:menu:delete'])
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
    this.$store.dispatch('dict/loadDictTypes', [
      DICT_TYPE.SYSTEM_MENU_TYPE,
      DICT_TYPE.COMMON_STATUS
    ])
    this.getList();
  },
  methods: {
    // 选择图标
    selected(name) {
      this.form.icon = name;
    },
    /** 查询菜单列表 */
    getList() {
      this.loading = true;
      listMenu(this.queryParams).then(response => {
        const list = (response && response.data) || []
        this.menuList = Array.isArray(list) && list.length ? this.handleTree(list, 'id') : []
      }).catch(() => {
        this.menuList = []
        this.$message.error('加载菜单列表失败，请检查权限或稍后重试')
      }).finally(() => {
        this.loading = false
      })
    },
    /** 转换菜单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.id,
        label: node.name,
        children: node.children
      };
    },
    /** 查询菜单下拉树结构 */
    getTreeselect() {
      listMenu().then(response => {
        this.menuOptions = [];
        const menu = { id: 0, name: '主类目', children: [] };
        menu.children = this.handleTree(response.data, "id");
        this.menuOptions.push(menu);
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        parentId: 0,
        name: undefined,
        icon: undefined,
        styleId: undefined,
        type: SystemMenuTypeEnum.DIR,
        sort: undefined,
        status: CommonStatusEnum.ENABLE,
        visible: true,
        keepAlive: true,
        alwaysShow: true,
        manualUrl: undefined,
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.id) {
        this.form.parentId = row.id;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = "添加菜单";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      getMenu(row.id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改菜单";
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 若权限类型为目录或者菜单时，进行 path 的校验，避免后续拼接出来的路由无法跳转
          if (this.form.type === SystemMenuTypeEnum.DIR
            || this.form.type === SystemMenuTypeEnum.MENU) {
            // 如果是外链，则不进行校验
            const path = this.form.path
            if (!isExternal(path)) {
              // 父权限为根节点，path 必须以 / 开头
              if (this.form.parentId === 0 && path.charAt(0) !== '/') {
                this.$modal.msgSuccess('前端必须以 / 开头')
                return
              } else if (this.form.parentId !== 0 && path.charAt(0) === '/') {
                this.$modal.msgSuccess('前端不能以 / 开头')
                return
              }
            }
          }

          this.warnDuplicateMenuName()

          // 提交
          const payload = { ...this.form }
          if (!checkFirstLevelMenu(payload.parentId)) {
            payload.styleId = null
          }
          if (payload.id !== undefined) {
            updateMenu(payload).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addMenu(payload).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 菜单重名提醒（不拦截保存） */
    warnDuplicateMenuName() {
      const { name, type, parentId, id } = this.form
      if (!name) {
        return
      }
      const flatMenus = flattenMenuTree(this.menuList)
      const duplicate = flatMenus.some(menu => {
        if (menu.id === id || menu.name !== name) {
          return false
        }
        if (type === SystemMenuTypeEnum.BUTTON) {
          return menu.type === type && menu.parentId === parentId
        }
        return menu.type === SystemMenuTypeEnum.DIR || menu.type === SystemMenuTypeEnum.MENU
      })
      if (duplicate) {
        this.$modal.msgWarning('主系统已存在同名菜单，请注意区分')
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.name + '"的数据项?').then(function() {
          return delMenu(row.id);
        }).then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 批量删除操作 */
    async handleDeleteBatch() {
      await this.$modal.confirm('是否确认批量删除选中的菜单数据?')
      try {
        await delMenuList(this.checkedIds);
        this.checkedIds = [];
        await this.getList();
        this.$modal.msgSuccess("删除成功");
      } catch {}
    },
    /** 选择行数据 */
    handleRowCheckboxChange(records) {
      this.checkedIds = records.map((item) => item.id);
    }
  }
};
</script>
