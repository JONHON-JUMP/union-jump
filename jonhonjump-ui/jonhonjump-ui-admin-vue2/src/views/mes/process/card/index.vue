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
          prop="processNo"
          class="query-form__input"
        >
          <el-input
            v-model.trim="queryParams.processNo"
            clearable
            prefix-icon="el-icon-search"
            placeholder="请输入工艺号，如 C12345"
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
          :key="item"
          type="button"
          class="recent-query"
          @click="handleRecentQuery(item)"
        >{{ item }}</button>
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
            <div><dt>工艺名称</dt><dd>{{ activeProcess.name }}</dd></div>
            <div><dt>工序数</dt><dd>{{ visibleOperationCount }}</dd></div>
            <div><dt>工艺节点</dt><dd>{{ visibleProcessCount }}</dd></div>
            <div><dt>更新时间</dt><dd>{{ activeProcess.updatedAt }}</dd></div>
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
            prop="processNo"
            label="工艺编码"
            min-width="150"
          />
          <el-table-column
            label="工序号"
            width="130"
            align="center"
          >
            <template slot-scope="scope"><span class="operation-no">{{ scope.row.operationNo }}</span></template>
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
const BASE_URL = 'https://example.com/process/view'
const link = processNo => `${BASE_URL}?processNo=${encodeURIComponent(processNo)}`
const leaf = (id, name, processNo, operationNo, parentName) => ({
  id, name, processNo, operationNo, parentName, externalUrl: link(processNo)
})
const branch = (id, name, processNo, operationNo, parentName, children) => ({
  id, name, processNo, operationNo, parentName, externalUrl: link(processNo), children
})
const MOCK_PROCESS_TREE = [{
  id: 1,
  name: '连接器总成制造工艺',
  processNo: 'C12345',
  operationNo: '0005',
  version: 'V3.2',
  updatedAt: '2026-08-18 16:42',
  externalUrl: link('C12345'),
  children: [
    leaf(2, '端子高速冲压工艺', 'DX889001', '0005', '连接器总成制造工艺'),
    branch(3, '精密模具冲压成型', 'DX889012', '0010', '连接器总成制造工艺', [
      leaf(4, '铜带上料与矫平', 'DX889011', '0005', '精密模具冲压成型'),
      leaf(5, '电镀表面处理（镀金 / 镀锡）', 'DX889013', '0015', '精密模具冲压成型')
    ]),
    branch(6, '胶壳精密注塑工艺', 'DX889002', '0010', '连接器总成制造工艺', [
      leaf(7, '工程塑料干燥与喂料', 'DX889021', '0005', '胶壳精密注塑工艺'),
      leaf(8, '高温熔融注射成型', 'DX889022', '0010', '胶壳精密注塑工艺')
    ]),
    branch(9, '自动化组装工艺', 'DX889003', '0015', '连接器总成制造工艺', [
      leaf(10, '端子自动插入胶壳', 'DX889031', '0005', '自动化组装工艺'),
      leaf(11, '外壳锁合与超声波焊接', 'DX889032', '0010', '自动化组装工艺')
    ]),
    branch(12, '电性能与外观检验', 'DX889004', '0020', '连接器总成制造工艺', [
      leaf(13, '导通电阻与绝缘耐压测试', 'DX889041', '0005', '电性能与外观检验'),
      leaf(14, 'CCD机器视觉外观缺陷检测', 'DX889042', '0010', '电性能与外观检验')
    ])
  ]
}]

export default {
  name: 'MesProcessViewer',
  data() {
    return {
      loading: false,
      queryParams: { processNo: 'C12345' },
      processTree: MOCK_PROCESS_TREE,
      displayProcessTree: MOCK_PROCESS_TREE,
      recentQueries: ['C12345', 'DX889001']
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
      return this.flatDisplayNodes.filter(item => !this.hasChildren(item)).length
    },
    visibleProcessCount() {
      return this.flatDisplayNodes.filter(item => this.hasChildren(item)).length
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
    filterProcessTree(nodes, keyword) {
      const value = String(keyword || '').trim().toLowerCase()
      if (!value) return nodes
      return (nodes || []).reduce((result, node) => {
        if (String(node.processNo || '').toLowerCase().includes(value)) {
          result.push(node)
          return result
        }
        const children = this.filterProcessTree(node.children || [], value)
        if (children.length) result.push({ ...node, children })
        return result
      }, [])
    },
    setAllExpanded(expanded) {
      const table = this.$refs && this.$refs.processTable
      if (!table || typeof table.toggleRowExpansion !== 'function') return
      this.flattenProcessTree(this.displayProcessTree)
        .filter(item => item.children && item.children.length)
        .forEach(item => table.toggleRowExpansion(item, expanded))
    },
    handleQuery() {
      const keyword = this.queryParams.processNo.trim()
      this.loading = true
      this.displayProcessTree = this.filterProcessTree(this.processTree, keyword)
      if (keyword && this.displayProcessTree.length) {
        this.recentQueries = [keyword, ...this.recentQueries.filter(item => item !== keyword)].slice(0, 3)
      }
      this.$nextTick(() => {
        this.loading = false
        if (keyword && this.displayProcessTree.length) this.setAllExpanded(true)
      })
    },
    handleRecentQuery(processNo) {
      this.queryParams.processNo = processNo
      this.handleQuery()
    },
    resetQuery() {
      if (this.$refs.queryForm) this.$refs.queryForm.resetFields()
      this.queryParams.processNo = ''
      this.displayProcessTree = this.processTree
      this.$nextTick(() => this.setAllExpanded(false))
    },
    getNodeIndex(row) {
      return this.flatDisplayNodes.findIndex(item => item.id === row.id) + 1
    },
    getNodeMeta(row) {
      return this.hasChildren(row) ? `${row.children.length} 个下级工序` : `所属：${row.parentName || '主工艺'}`
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
