/**

 * 子系统导入/新增门禁：必须先选择已登记外部系统，操作前确认关联。

 * 同时提供左侧系统列表的加载态，避免首屏「未选择」提示闪一下。

 */

export default {

  data() {

    return {

      /** 外部系统列表首次加载中（避免告警条/空态闪烁） */

      clientsLoading: true

    }

  },

  computed: {

    /** 仅在列表加载结束且仍未选中系统时展示绑定提示 */

    showSubSystemBindHint() {

      return !this.clientsLoading && !this.selectedClient

    }

  },

  methods: {

    /**

     * @param {string} actionLabel 操作名，如「导入」「新增」

     * @param {object} [options]

     * @param {boolean} [options.requireConfirm=true] 是否弹出关联确认框

     * @returns {Promise<void>}

     */

    ensureSubSystemBoundBeforeAction(actionLabel = '操作', options = {}) {

      const requireConfirm = options.requireConfirm !== false

      return new Promise((resolve, reject) => {

        if (!this.selectedClient || !this.selectedClient.id) {

          this.$modal.msgWarning(

            `请先在左侧选择已登记的外部系统，关联系统信息后再${actionLabel}`

          )

          reject(new Error('SUB_SYSTEM_NOT_SELECTED'))

          return

        }

        if (!requireConfirm) {

          resolve()

          return

        }

        const name = this.selectedClient.name || this.selectedClient.clientId || this.selectedClient.id

        this.$confirm(

          `即将对外部系统「${name}」执行${actionLabel}。\n\n` +

            '请确认：该系统已在「外部系统」中登记，并完成与主系统的关联配置。\n' +

            '未关联完成前请勿导入，以免数据写入错误系统。\n\n是否继续？',

          '关联确认',

          {

            type: 'warning',

            confirmButtonText: '已确认关联，继续',

            cancelButtonText: '取消',

            dangerouslyUseHTMLString: false

          }

        ).then(() => resolve()).catch(() => reject(new Error('CANCELLED')))

      })

    },

    /**

     * 包装外部系统列表请求：首次进入显示 loading，后续静默刷新不闪屏。

     * @param {() => Promise} fetcher

     * @returns {Promise}

     */

    withClientsLoading(fetcher) {

      const isFirstLoad = this.clientsLoading

      return Promise.resolve()

        .then(() => fetcher())

        .finally(() => {

          if (isFirstLoad) {

            this.clientsLoading = false

          }

        })

    },

    /**

     * 列表返回后同步当前选中项（刷新人数等计数），避免整页重选造成闪动。

     */

    syncSelectedClientFromList() {

      if (!this.selectedClient || !this.selectedClient.id || !this.clientList) {

        return false

      }

      const fresh = this.clientList.find(item => item.id === this.selectedClient.id)

      if (fresh) {

        this.selectedClient = fresh

        return true

      }

      return false

    }

  }

}

