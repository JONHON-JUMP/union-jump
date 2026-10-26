/**
 * 弹窗脏检查：打开时记录快照，关闭前比较，有改动才提示。
 * 用法：
 *   - <el-dialog :before-close="handleDialogBeforeClose">
 *   - 打开/回填表单后调用 markDialogPristine(value)
 *   - 取消按钮调用 confirmCloseDialog(value, done)
 */
export default {
  methods: {
    serializeDialogValue(value) {
      try {
        const text = JSON.stringify(value === undefined ? {} : value)
        return text === undefined ? '' : text
      } catch (e) {
        return ''
      }
    },
    markDialogPristine(value) {
      this._dialogSnapshot = this.serializeDialogValue(value)
    },
    isDialogDirty(value) {
      return !!this._dialogSnapshot
        && this.serializeDialogValue(value) !== this._dialogSnapshot
    },
    confirmCloseDialog(value, done) {
      if (typeof done !== 'function') {
        return
      }
      if (!this.isDialogDirty(value)) {
        done()
        return
      }
      this.$confirm('当前有未保存的修改，确定关闭吗？', '提示', {
        type: 'warning',
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑'
      }).then(done).catch(() => {})
    }
  }
}
