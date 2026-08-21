<template>
  <div class="app-container process-viewer-page">
    <section class="query-card">
      <el-form
        ref="queryForm"
        :model="queryParams"
        class="query-form"
        @submit.native.prevent
      >
        <el-form-item
          prop="prtno"
          class="query-form__input"
          :rules="[{ required: true, message: '请输入物料号', trigger: 'blur' }]"
        >
          <el-input
            v-model.trim="queryParams.prtno"
            clearable
            prefix-icon="el-icon-search"
            placeholder="请输入物料号"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item
          prop="accno"
          class="query-form__input"
          :rules="[{ required: true, message: '请输入工艺规程号', trigger: 'blur' }]"
        >
          <el-input
            v-model.trim="queryParams.accno"
            clearable
            prefix-icon="el-icon-document"
            placeholder="请输入工艺规程号"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item class="query-form__actions">
          <el-button
            type="primary"
            icon="el-icon-search"
            :loading="loading"
            @click="handleQuery"
          >查询</el-button>
          <el-button
            icon="el-icon-refresh-left"
            @click="resetQuery"
          >重置</el-button>
        </el-form-item>
      </el-form>
      <div class="query-hints">
        <span>最近查询：</span>
        <button
          v-for="item in recentQueries"
          :key="`${item.prtno}-${item.accno}`"
          type="button"
          class="recent-query"
          @click="handleRecentQuery(item)"
        >{{ item.label }}</button>
        <span class="enter-tip">支持按 <strong>Enter</strong> 快速查询</span>
      </div>
    </section>

    <section class="result-card">
      <div
        v-if="activeProcess"
        class="result-summary"
      >
        <div>
          <div class="summary-title">
            <h2>{{ activeProcess.processNo }}</h2>
            <span class="version-tag">{{ activeProcess.version }}</span>
          </div>
          <dl class="summary-meta">
            <div><dt>工艺类型</dt><dd>{{ activeProcess.name }}</dd></div>
            <div><dt>工序数</dt><dd>{{ visibleOperationCount }}</dd></div>
            <div><dt>工艺卡数</dt><dd>{{ visibleProcessCount }}</dd></div>
            <div><dt>订单类型</dt><dd>{{ activeProcess.isFix === 1 ? '返修' : '普通' }}</dd></div>
          </dl>
        </div>
        <div class="summary-actions">
          <el-button
            icon="el-icon-arrow-down"
            @click="setAllExpanded(true)"
          >全部展开</el-button>
          <el-button
            icon="el-icon-arrow-up"
            @click="setAllExpanded(false)"
          >全部收起</el-button>
        </div>
      </div>

      <div class="tree-table-scroll">
        <el-table
          ref="processTable"
          v-loading="loading"
          :data="displayProcessTree"
          row-key="id"
          :tree-props="{ children: 'children' }"
          :default-expand-all="true"
          class="process-table"
          empty-text="暂无匹配的工艺信息"
        >
          <el-table-column
            label="序号"
            width="92"
            align="center"
          >
            <template slot-scope="scope"><span class="row-index">{{ getNodeIndex(scope.row) }}</span></template>
          </el-table-column>
          <el-table-column
            label="工艺 / 工序名称"
            min-width="400"
          >
            <template slot-scope="scope">
              <div class="name-cell">
                <span :class="['node-icon', { leaf: !hasChildren(scope.row) }]">
                  <i :class="hasChildren(scope.row) ? 'el-icon-folder-opened' : 'el-icon-share'" />
                </span>
                <span class="name-content">
                  <strong>{{ scope.row.name }}</strong>
                  <small>{{ getNodeMeta(scope.row) }}</small>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            label="工艺编码"
            min-width="150"
          >
            <template slot-scope="scope">{{ scope.row.code || scope.row.processNo || '—' }}</template>
          </el-table-column>
          <el-table-column
            label="工序号"
            width="130"
            align="center"
          >
            <template slot-scope="scope"><span class="operation-no">{{ scope.row.operationNo || '—' }}</span></template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="130"
            align="center"
          >
            <template slot-scope="scope">
              <el-button
                type="text"
                class="view-button"
                :disabled="!scope.row.externalUrl"
                @click="handleView(scope.row)"
              >
                <i class="el-icon-view" /> 查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script>
import { queryProcessCard } from '@/api/mes/process/card'

export default {
  name: 'MesProcessViewer',
  data() {
    return {
      loading: false,
      queryParams: { prtno: '', accno: '' },
      processTree: [],
      displayProcessTree: [],
      recentQueries: []
    }
  },
  computed: {
    flatDisplayNodes() {
      return this.flattenProcessTree(this.displayProcessTree)
    },
    activeProcess() {
      return this.displayProcessTree.length ? this.displayProcessTree[0] : null
    },
    visibleOperationCount() {
      return this.flatDisplayNodes.filter(item => item.nodeType === 'operation').length
    },
    visibleProcessCount() {
      return this.flatDisplayNodes.filter(item => item.nodeType === 'card').length
    }
  },
  methods: {
    hasChildren(row) {
      return Boolean(row && row.children && row.children.length)
    },
    flattenProcessTree(nodes) {
      return (nodes || []).reduce((result, node) => {
        result.push(node)
        if (node.children && node.children.length) result.push(...this.flattenProcessTree(node.children))
        return result
      }, [])
    },
    normalizeCards(cards) {
      const mapDetails = (details, parentName, cardIndex) => (details || []).map((detail, index) => ({
        id: `card-${cardIndex}-${detail.no || 'invalid'}-${detail.idx || index}`,
        idx: detail.idx,
        name: detail.name || '未命名工序',
        processNo: '',
        operationNo: detail.no,
        code: detail.code,
        externalUrl: detail.url,
        parentName,
        nodeType: 'operation',
        children: mapDetails(detail.children, detail.name || parentName, cardIndex)
      }))
      return (cards || []).map((card, cardIndex) => ({
        id: `card-${cardIndex}-${card.accno}`,
        idx: null,
        name: card.isFormal === 1 ? '正式工艺' : '临时工艺',
        processNo: card.accno,
        operationNo: '',
        code: card.accno,
        version: card.version || '—',
        isFormal: card.isFormal,
        isFix: card.isFix,
        externalUrl: '',
        parentName: '',
        nodeType: 'card',
        children: mapDetails(card.details, card.accno, cardIndex)
      }))
    },
    validateQueryForm() {
      const form = this.$refs && this.$refs.queryForm
      if (!form || typeof form.validate !== 'function') return Promise.resolve(true)
      return new Promise(resolve => form.validate(valid => resolve(Boolean(valid))))
    },
    setAllExpanded(expanded) {
      const table = this.$refs && this.$refs.processTable
      if (!table || typeof table.toggleRowExpansion !== 'function') return
      this.flattenProcessTree(this.displayProcessTree)
        .filter(item => item.children && item.children.length)
        .forEach(item => table.toggleRowExpansion(item, expanded))
    },
    async handleQuery() {
      const valid = await this.validateQueryForm()
      if (!valid) return
      this.loading = true
      try {
        const requestData = { ...this.queryParams }
        const response = await queryProcessCard(requestData)
        const cards = Array.isArray(response) ? response : response && response.data
        this.processTree = this.normalizeCards(cards)
        this.displayProcessTree = this.processTree
        if (this.processTree.length) {
          const recent = { ...requestData, label: `${requestData.prtno} / ${requestData.accno}` }
          this.recentQueries = [recent, ...this.recentQueries.filter(item => (
            item.prtno !== recent.prtno || item.accno !== recent.accno
          ))].slice(0, 3)
        }
        await this.$nextTick()
        this.setAllExpanded(true)
      } catch (error) {
        this.processTree = []
        this.displayProcessTree = []
      } finally {
        this.loading = false
      }
    },
    handleRecentQuery(item) {
      this.queryParams = { prtno: item.prtno, accno: item.accno }
      this.handleQuery()
    },
    resetQuery() {
      if (this.$refs.queryForm) this.$refs.queryForm.resetFields()
      this.queryParams = { prtno: '', accno: '' }
      this.processTree = []
      this.displayProcessTree = []
    },
    getNodeIndex(row) {
      return row.idx || '—'
    },
    getNodeMeta(row) {
      if (row.nodeType === 'card') return `${row.children.length} 个一级工序`
      return this.hasChildren(row) ? `${row.children.length} 个子工序` : `所属：${row.parentName || '主工艺'}`
    },
    handleView(row) {
      if (!row.externalUrl) {
        this.$message.warning('暂未配置工艺查看地址')
        return
      }
      const openedWindow = window.open(row.externalUrl, '_blank', 'noopener,noreferrer')
      if (openedWindow) openedWindow.opener = null
    }
  }
}
</script>

<style lang="scss" scoped>
.process-viewer-page {
  padding: 24px 30px 36px;
  color: #263653;
  background: #f4f7fb;
}
.query-card, .result-card {
  overflow: hidden;
  border: 1px solid #e2e9f2;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 4px 8px rgba(37, 57, 87, .04);
}
.query-card { margin-bottom: 20px; padding: 24px 28px 20px; }
.query-form {
  display: flex; align-items: center; width: min(100%, 1040px);
  ::v-deep .el-form-item { margin-bottom: 0; }
  &__input {
    flex: 1;
    ::v-deep .el-input__inner {
      height: 52px; padding-left: 46px; border-color: #d7e1ee; border-radius: 9px;
      color: #20304a; font-size: 16px; line-height: 52px;
    }
    ::v-deep .el-input__prefix { left: 13px; color: #8494aa; font-size: 21px; line-height: 52px; }
    ::v-deep .el-input__inner:focus { border-color: #3385f5; box-shadow: 0 0 0 3px rgba(51, 133, 245, .1); }
  }
  &__input + &__input { margin-left: 12px; }
  &__actions {
    margin-left: 14px; white-space: nowrap;
    ::v-deep .el-button { height: 52px; padding: 0 25px; border-radius: 9px; font-size: 15px; }
    ::v-deep .el-button--primary {
      border-color: #2f80ed; background: #2f80ed; box-shadow: 0 5px 10px rgba(47, 128, 237, .18);
    }
  }
}
.query-hints {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  margin-top: 16px; color: #64748b; font-size: 14px;
  .enter-tip { margin-left: 8px; }
  strong { color: #46566f; }
}
.recent-query {
  padding: 5px 13px; border: 1px solid #e4eaf2; border-radius: 16px; outline: none;
  color: #60708a; background: #f7f9fc; cursor: pointer;
  &:hover, &:focus-visible { border-color: #b9d4fb; color: #2f80ed; background: #eef5ff; }
}
.result-summary {
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 24px 28px 22px; border-bottom: 1px solid #e7edf4;
}
.summary-title {
  display: flex; align-items: center; flex-wrap: wrap; gap: 10px;
  h2 { margin: 0 6px 0 0; color: #152843; font-size: 24px; line-height: 32px; }
}
.summary-meta {
  display: flex; align-items: center; flex-wrap: wrap; gap: 16px 28px; margin: 15px 0 0;
  div { display: flex; align-items: center; gap: 8px; }
  dt { color: #718096; }
  dd { margin: 0; color: #31445f; font-weight: 600; }
}
.summary-actions {
  display: flex; flex-shrink: 0;
  ::v-deep .el-button {
    height: 40px; padding: 0 16px; border-color: #d8e2ef; border-radius: 8px; color: #53647c;
  }
}
.version-tag {
  display: inline-flex; align-items: center; height: 30px; padding: 0 12px;
  border-radius: 15px; font-size: 14px; font-weight: 600;
}
.version-tag { color: #316cdb; background: #edf4ff; }
.tree-table-scroll { width: 100%; }
.process-table {
  width: 100%;
  &::before { display: none; }
  ::v-deep th.el-table__cell {
    height: 56px; padding: 0; border-bottom-color: #e4eaf2; color: #53647c;
    background: #f8fafc; font-size: 14px; font-weight: 600;
  }
  ::v-deep td.el-table__cell {
    height: 76px; padding: 0; border-bottom-color: #e8edf3; color: #60708a; font-size: 14px;
  }
  ::v-deep .el-table__row:hover > td.el-table__cell { background: #f7faff; }
  ::v-deep .el-table__expand-icon { margin-right: 8px; color: #7b8ca4; }
}
.row-index { color: #75859b; font-variant-numeric: tabular-nums; }
.name-cell { display: flex; align-items: center; min-width: 0; padding: 10px 0; }
.name-content {
  display: flex; min-width: 0; flex-direction: column; gap: 5px;
  strong, small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: #263852; font-size: 15px; line-height: 21px; }
  small { color: #96a3b5; font-size: 13px; line-height: 18px; }
}
.node-icon {
  display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px;
  margin-right: 12px; flex: 0 0 38px; border-radius: 9px; color: #2f80ed;
  background: #edf5ff; font-size: 17px;
  &.leaf { color: #8392a8; background: #f3f5f8; }
}
.operation-no {
  display: inline-flex; align-items: center; justify-content: center; min-width: 66px; height: 30px;
  border-radius: 15px; color: #53647c; background: #f3f5f8; font-weight: 600;
}
.view-button { font-size: 14px; font-weight: 600; i { margin-right: 4px; } }
@media (max-width: 900px) {
  .process-viewer-page { padding: 18px 16px 28px; }
  .query-form {
    align-items: stretch; flex-direction: column;
    &__input + &__input { margin: 12px 0 0; }
    &__actions {
      display: flex; margin: 12px 0 0;
      ::v-deep .el-button { flex: 1; }
    }
  }
  .result-summary { align-items: flex-start; flex-direction: column; }
  .summary-actions { width: 100%; ::v-deep .el-button { flex: 1; } }
  .tree-table-scroll { overflow-x: auto; }
  .process-table { min-width: 900px; }
}
@media (max-width: 520px) {
  .query-card, .result-summary { padding-right: 18px; padding-left: 18px; }
  .query-hints .enter-tip { width: 100%; margin: 2px 0 0; }
}
</style>
