<template>
  <div class="task-panel">
    <div class="task-panel__header">
      <div class="task-panel__tabs">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="task-panel__tab"
          :class="{ 'is-active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }} {{ tab.count }}
        </div>
      </div>
      <div class="task-panel__search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题、流程名称、发起人"
          prefix-icon="el-icon-search"
          clearable
          size="small"
          @keyup.enter.native="handleSearch"
          @clear="handleSearch"
        />
      </div>
      <div class="task-panel__actions">
        <i class="el-icon-refresh action-icon" title="刷新" @click="loadData" />
        <span class="more-link" @click="handleMore">
          <i class="el-icon-menu" /> 更多
        </span>
      </div>
    </div>

    <div v-loading="loading" class="task-panel__body">
      <div
        v-for="(item, index) in displayList"
        :key="index"
        class="task-panel__row"
        @click="handleRowClick(item)"
      >
        <span class="task-panel__category">{{ item.category }}</span>
        <span class="task-panel__title" :title="item.title">{{ item.title }}</span>
        <span class="task-panel__author">{{ item.author }}</span>
        <span class="task-panel__date">{{ item.date }}</span>
      </div>
      <el-empty v-if="!loading && displayList.length === 0" description="暂无数据" :image-size="80" />
    </div>
  </div>
</template>

<script>
import { getTodoTaskPage } from '@/api/bpm/task'
import { getMyNotifyMessagePage } from '@/api/system/notify/message'
import { listFaqWorkbench } from '@/api/system/faq'

export default {
  name: 'HomeTaskPanel',
  data() {
    return {
      activeTab: 'todo',
      searchKeyword: '',
      loading: false,
      todoList: [],
      notifyList: [],
      faqList: [],
      todoCount: 0,
      notifyCount: 0,
      faqCount: 0
    }
  },
  computed: {
    tabs() {
      return [
        { key: 'todo', label: '待办', count: this.todoCount },
        { key: 'notify', label: '通知', count: this.notifyCount },
        { key: 'faq', label: '常见问题', count: this.faqCount }
      ]
    },
    displayList() {
      let list = []
      if (this.activeTab === 'todo') {
        list = this.todoList
      } else if (this.activeTab === 'notify') {
        list = this.notifyList
      } else {
        list = this.faqList
      }
      if (!this.searchKeyword) {
        return list
      }
      const keyword = this.searchKeyword.trim().toLowerCase()
      return list.filter(item =>
        item.title.toLowerCase().includes(keyword) ||
        item.category.toLowerCase().includes(keyword) ||
        item.author.toLowerCase().includes(keyword)
      )
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    switchTab(key) {
      this.activeTab = key
      this.searchKeyword = ''
    },
    handleSearch() {
      // 本地过滤，无需额外请求
    },
    loadData() {
      this.loading = true
      Promise.all([this.loadTodoList(), this.loadNotifyList(), this.loadFaqList()]).finally(() => {
        this.loading = false
      })
    },
    loadTodoList() {
      return getTodoTaskPage({ pageNo: 1, pageSize: 20 }).then(response => {
        const list = response.data.list || []
        this.todoCount = response.data.total || 0
        this.todoList = list.map(row => ({
          category: `【${row.processInstance && row.processInstance.name ? row.processInstance.name : '流程审批'}】`,
          title: this.buildTodoTitle(row),
          author: row.processInstance && row.processInstance.startUserNickname ? row.processInstance.startUserNickname : '-',
          date: this.formatDate(row.createTime),
          raw: row
        }))
      }).catch(() => {
        this.todoList = []
        this.todoCount = 0
      })
    },
    loadNotifyList() {
      return getMyNotifyMessagePage({ pageNo: 1, pageSize: 20 }).then(response => {
        const list = response.data.list || []
        this.notifyCount = response.data.total || 0
        this.notifyList = list.map(row => ({
          category: '【系统通知】',
          title: row.templateContent || '-',
          author: row.templateNickname || '-',
          date: this.formatDate(row.createTime),
          raw: row
        }))
      }).catch(() => {
        this.notifyList = []
        this.notifyCount = 0
      })
    },
    loadFaqList() {
      return listFaqWorkbench({ pageNo: 1, pageSize: 20 }).then(response => {
        const list = response.data.list || []
        this.faqCount = response.data.total || 0
        this.faqList = list.map(row => ({
          category: `【${this.getDictDataLabel(this.DICT_TYPE.SYSTEM_FAQ_CATEGORY, row.category) || '常见QA'}】`,
          title: row.title || '-',
          author: row.publisherName || '-',
          date: this.formatDate(row.createTime),
          raw: row
        }))
      }).catch(() => {
        this.faqList = []
        this.faqCount = 0
      })
    },
    buildTodoTitle(row) {
      const user = row.processInstance && row.processInstance.startUserNickname
      const taskName = row.name || ''
      if (user && taskName) {
        return `${user} - ${taskName}`
      }
      return taskName || user || '-'
    },
    formatDate(time) {
      if (!time) {
        return '-'
      }
      return this.parseTime(time, '{y}/{m}/{d}')
    },
    handleRowClick(item) {
      if (this.activeTab === 'todo' && item.raw && item.raw.processInstance) {
        this.$router.push({ name: 'BpmProcessInstanceDetail', query: { id: item.raw.processInstance.id } }).catch(() => {})
      } else if (this.activeTab === 'faq' && item.raw && item.raw.id) {
        this.$router.push({ name: 'MyFaqDetail', query: { faqId: item.raw.id } }).catch(() => {})
      }
    },
    handleMore() {
      if (this.activeTab === 'todo') {
        this.$router.push({ path: '/bpm/task/todo' }).catch(() => {
          this.$message.info('请在左侧菜单进入「待办任务」')
        })
      } else if (this.activeTab === 'notify') {
        this.$router.push({ path: '/user/notify-message' })
      } else if (this.activeTab === 'faq') {
        this.$router.push({ name: 'MyFaq' }).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.task-panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 560px;
}

.task-panel__header {
  display: flex;
  align-items: center;
  padding: 16px 20px 0;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
  margin: -6px 0 0 -6px;
}
.task-panel__header > * {
  margin: 6px 0 0 6px;
}

.task-panel__tabs {
  display: flex;
  flex-shrink: 0;
}

.task-panel__tab {
  padding: 0 4px 14px;
  margin-right: 28px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  white-space: nowrap;

  &.is-active {
    color: #1890ff;
    font-weight: 500;
    border-bottom-color: #1890ff;
  }

  &:hover {
    color: #1890ff;
  }
}

.task-panel__search {
  flex: 1;
  min-width: 200px;
  max-width: 360px;
  margin: 0 auto;
}

.task-panel__actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
  padding-bottom: 10px;

  .action-icon {
    font-size: 18px;
    color: #999;
    cursor: pointer;
    margin-right: 16px;

    &:hover {
      color: #1890ff;
    }
  }

  .more-link {
    font-size: 14px;
    color: #666;
    cursor: pointer;

    i {
      margin-right: 4px;
    }

    &:hover {
      color: #1890ff;
    }
  }
}

.task-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.task-panel__row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #fafafa;
  }
}

.task-panel__category {
  flex-shrink: 0;
  width: 120px;
  font-size: 13px;
  color: #d48806;
}

.task-panel__title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 16px;
}

.task-panel__author {
  flex-shrink: 0;
  width: 80px;
  text-align: center;
  font-size: 13px;
  color: #666;
}

.task-panel__date {
  flex-shrink: 0;
  width: 90px;
  text-align: right;
  font-size: 13px;
  color: #999;
}
</style>
