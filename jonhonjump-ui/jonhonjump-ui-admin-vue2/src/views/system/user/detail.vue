<template>
  <div v-loading="loading" class="app-container user-detail-page">
    <el-card shadow="never" class="user-detail-card">
      <div slot="header">
        <span>主系统用户信息</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户编号">{{ user.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名称">{{ user.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ user.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ user.employeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="域账号">{{ user.domainNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="刷卡卡号">{{ user.cardNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ERP账号">{{ formatErpNos(user.erpNos) }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ user.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ user.deptName || (user.dept && user.dept.name) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="user.status === 0 ? 'success' : 'danger'" size="mini">
            {{ user.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(user.createTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ user.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="user-detail-card">
      <div slot="header" class="user-detail-card__header">
        <span>外部系统信息</span>
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAddSubSystemUser"
          v-hasPermi="['sub-system:user:create']"
        >添加外部系统</el-button>
      </div>
      <el-table :data="subSystemList" border empty-text="暂无关联外部系统">
        <el-table-column label="外部系统名称" prop="clientName" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="用户名" prop="username" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="用户姓名" prop="nickname" width="100" :show-overflow-tooltip="true" />
        <el-table-column label="车间编号" prop="workshopId" width="100" :show-overflow-tooltip="true" />
        <el-table-column label="班组编码" prop="teamId" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="班组名称" prop="teamName" width="120" :show-overflow-tooltip="true" />
        <el-table-column label="岗位" prop="postNames" width="120" :show-overflow-tooltip="true" />
        <el-table-column label="角色" prop="roleNames" width="120" :show-overflow-tooltip="true" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template v-slot="scope">
            <el-tag
              :type="!scope.row.mainUserId ? 'info' : (scope.row.status === '1' ? 'danger' : 'success')"
              size="mini"
            >
              {{ !scope.row.mainUserId ? '未关联' : (scope.row.status === '1' ? '禁用' : '正常') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template v-slot="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="130" fixed="right">
          <template v-slot="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditSubSystemUser(scope.row)"
              v-hasPermi="['sub-system:user:update']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDeleteSubSystemUser(scope.row)"
              v-hasPermi="['sub-system:user:delete']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <sub-system-user-edit-dialog
      :visible.sync="subSystemEditOpen"
      :record-id="subSystemEditId"
      :main-user-id="subSystemEditMainUserId"
      :portal-username="user.username"
      :exclude-sub-system-ids="linkedSubSystemIds"
      :client-name="subSystemEditClientName"
      @success="loadSubSystemList"
    />
  </div>
</template>

<script>
import { getUser } from '@/api/system/user'
import { deleteSubSystemUser, getSubSystemUsersByMainUserId } from '@/api/system/subSystemUsers'
import SubSystemUserEditDialog from '@/views/system/components/SubSystemUserEditDialog'
import {
  formatErpNos
} from '@/utils/userFieldDisplay'

export default {
  name: 'SystemUserDetail',
  components: { SubSystemUserEditDialog },
  data() {
    return {
      loading: false,
      user: {},
      subSystemList: [],
      subSystemEditOpen: false,
      subSystemEditId: undefined,
      subSystemEditMainUserId: undefined,
      subSystemEditClientName: ''
    }
  },
  computed: {
    linkedSubSystemIds() {
      return (this.subSystemList || []).map(item => item.subSystemId).filter(id => id != null)
    }
  },
  watch: {
    '$route'(route) {
      if (route.name === 'SystemUserDetail') {
        this.loadDetail()
      }
    }
  },
  created() {
    if (this.$route.name === 'SystemUserDetail') {
      this.loadDetail()
    }
  },
  methods: {
    formatErpNos,
    loadDetail() {
      const userId = Number(this.$route.query.userId)
      if (!userId) {
        this.$message.error('缺少用户编号，请从用户列表进入')
        return
      }
      this.loading = true
      this.user = {}
      this.subSystemList = []
      getUser(userId).then(userResponse => {
        this.user = userResponse.data || {}
        return this.loadSubSystemList(userId)
      }).catch(() => {
        this.user = {}
        this.subSystemList = []
      }).finally(() => {
        this.loading = false
      })
    },
    loadSubSystemList(userId) {
      const mainUserId = userId || Number(this.$route.query.userId)
      if (!mainUserId) {
        this.subSystemList = []
        return Promise.resolve()
      }
      return getSubSystemUsersByMainUserId(mainUserId).then(subSystemResponse => {
        this.subSystemList = subSystemResponse.data || []
      }).catch(() => {
        this.subSystemList = []
      })
    },
    handleEditSubSystemUser(row) {
      if (!row || !row.id) {
        return
      }
      this.subSystemEditId = row.id
      this.subSystemEditMainUserId = undefined
      this.subSystemEditClientName = row.clientName || ''
      this.subSystemEditOpen = true
    },
    handleAddSubSystemUser() {
      const mainUserId = Number(this.$route.query.userId)
      if (!mainUserId) {
        this.$message.error('缺少用户编号')
        return
      }
      this.subSystemEditId = undefined
      this.subSystemEditMainUserId = mainUserId
      this.subSystemEditClientName = ''
      this.subSystemEditOpen = true
    },
    handleDeleteSubSystemUser(row) {
      if (!row || !row.id) {
        return
      }
      this.$modal.confirm('是否确认删除该外部系统关联？').then(() => {
        return deleteSubSystemUser(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        return this.loadSubSystemList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.user-detail-page {
  padding-bottom: 20px;
}

.user-detail-card {
  margin-bottom: 16px;
}

.user-detail-card ::v-deep .el-card__header {
  font-weight: 600;
}

.user-detail-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
