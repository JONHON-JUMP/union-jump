<template>

  <div class="quick-nav">

    <div class="quick-nav__header">

      <i class="el-icon-discover header-icon" />

      <span class="quick-nav__title">快捷导航</span>

      <i class="el-icon-setting header-setting" title="配置本系统快捷导航" @click="settingsVisible = true" />

    </div>



    <div class="quick-nav__body">

      <!-- 本系统快捷导航 -->

      <div class="quick-nav__section">

        <div class="quick-nav__section-title">

          <span class="title-bar" />

          本系统

        </div>

        <el-empty

          v-if="!loading && navItems.length === 0"

          description="暂未添加快捷导航，点击右上角设置"

          :image-size="64"

        />

        <div v-else class="quick-nav__grid">

          <div

            v-for="item in filteredInternalItems"

            :key="'internal-' + item.menuId + item.path"

            class="quick-nav__item"

            @click="handleNavClick(item)"

          >

            <div v-if="item.hasIcon" class="quick-nav__icon quick-nav__icon--plain">
              <svg-icon v-if="item.svgIcon" :icon-class="item.svgIcon" class="quick-nav__menu-icon" />
              <i v-else :class="item.icon" />
            </div>
            <div v-else class="quick-nav__icon quick-nav__icon--plain quick-nav__icon--empty" />
            <span class="quick-nav__label">{{ item.name }}</span>

          </div>

        </div>

      </div>



      <!-- 外部系统快捷导航 -->

      <div class="quick-nav__section">

        <div class="quick-nav__section-title">

          <span class="title-bar title-bar--external" />

          外部系统

        </div>

        <el-empty

          v-if="!externalLoading && externalItems.length === 0"

          description="暂无可访问的外部系统"

          :image-size="64"

        />

        <div v-else class="quick-nav__grid">

          <div

            v-for="item in filteredExternalItems"

            :key="'external-' + item.id + item.clientId"

            class="quick-nav__item"

            @click="handleNavClick(item)"

          >

            <div class="quick-nav__icon" :class="{ 'quick-nav__icon--logo': item.logo }" :style="item.logo ? {} : { backgroundColor: item.color }">

              <img v-if="item.logo" :src="item.logo" class="quick-nav__logo" alt="" @error="onExternalLogoError(item)" />

              <i v-else class="el-icon-link" />

            </div>

            <span class="quick-nav__label">{{ item.name }}</span>

          </div>

        </div>

      </div>

    </div>



    <div class="quick-nav__footer">

      <span class="more-link" @click="settingsVisible = true">设置</span>

      <i class="el-icon-search footer-icon" title="搜索" @click="toggleSearch" />

    </div>



    <el-dialog title="搜索快捷导航" :visible.sync="searchVisible" width="400px" append-to-body>

      <el-input v-model="searchKeyword" placeholder="输入模块名称" prefix-icon="el-icon-search" clearable />

      <div v-if="searchKeyword" class="search-results">

        <div

          v-for="item in searchResults"

          :key="item.key"

          class="search-result-item"

          @click="handleNavClick(item); searchVisible = false"

        >

          <span class="search-result-item__tag">{{ item.section }}</span>

          {{ item.name }}

        </div>

        <el-empty v-if="searchResults.length === 0" description="未找到匹配模块" :image-size="60" />

      </div>

    </el-dialog>



    <quick-nav-settings v-model="settingsVisible" @saved="handleSettingsSaved" />

  </div>

</template>



<script>

import { mapGetters } from 'vuex'

import SvgIcon from '@/components/SvgIcon'

import { getUserQuickNavList } from '@/api/system/user/quickNav'

import { getMyExternalSystemList } from '@/api/system/subSystemUsers'

import { buildExternalNavItems, buildQuickNavItems } from './quickNavFromRoutes'

import QuickNavSettings from './QuickNavSettings'



export default {

  name: 'HomeQuickNav',

  components: { SvgIcon, QuickNavSettings },

  data() {

    return {

      loading: false,

      externalLoading: false,

      menuIds: [],

      externalList: [],

      settingsVisible: false,

      searchVisible: false,

      searchKeyword: ''

    }

  },

  computed: {

    ...mapGetters(['sidebarRouters']),

    navItems() {

      return buildQuickNavItems(this.sidebarRouters, this.menuIds)

    },

    externalItems() {

      return buildExternalNavItems(this.externalList)

    },

    filteredInternalItems() {

      return this.filterItems(this.navItems)

    },

    filteredExternalItems() {

      return this.filterItems(this.externalItems)

    },

    searchResults() {

      const keyword = this.searchKeyword.trim().toLowerCase()

      if (!keyword) {

        return []

      }

      const internal = this.navItems

        .filter(item => item.name.toLowerCase().includes(keyword))

        .map(item => ({ ...item, section: '本系统', key: 'internal-' + item.menuId + item.path }))

      const external = this.externalItems

        .filter(item => item.name.toLowerCase().includes(keyword))

        .map(item => ({ ...item, section: '外部系统', key: 'external-' + item.id + item.clientId }))

      return [...internal, ...external]

    }

  },

  created() {

    this.loadQuickNav()

    this.loadExternalSystems()

  },

  methods: {

    filterItems(items) {

      if (!this.searchKeyword) {

        return items

      }

      const keyword = this.searchKeyword.trim().toLowerCase()

      return items.filter(item => item.name.toLowerCase().includes(keyword))

    },

    onExternalLogoError(item) {
      item.logo = ''
    },

    loadQuickNav() {

      this.loading = true

      getUserQuickNavList()

        .then(res => {

          const config = res.data || {}

          this.menuIds = config.menuIds || []

        })

        .catch(() => {

          this.menuIds = []

        })

        .finally(() => {

          this.loading = false

        })

    },

    loadExternalSystems() {

      this.externalLoading = true

      getMyExternalSystemList()

        .then(res => {

          this.externalList = res.data || []

        })

        .catch(() => {

          this.externalList = []

        })

        .finally(() => {

          this.externalLoading = false

        })

    },

    handleSettingsSaved(menuIds) {

      this.menuIds = menuIds

    },

    toggleSearch() {

      this.searchVisible = true

      this.searchKeyword = ''

    },

    handleNavClick(item) {

      if (!item.path) {

        this.$message.warning('暂无可用地址')

        return

      }

      if (item.sso || item.external) {
        if (item.clientId || item.subSystemId) {
          this.$store.dispatch('portal/switchSystem', item.clientId || item.subSystemId).catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入外部系统失败'))
          })
          return
        }
        window.open(item.path, '_blank', 'noopener,noreferrer')
        return
      }

      this.$router.push(item.path).catch(() => {})

    }

  }

}

</script>



<style lang="scss" scoped>

.quick-nav {

  background: #fff;

  border-radius: 8px;

  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  height: 100%;

  display: flex;

  flex-direction: column;

  min-height: 560px;

}



.quick-nav__header {

  display: flex;

  align-items: center;

  padding: 18px 20px;

  border-bottom: 1px solid #f0f0f0;



  .header-icon {

    font-size: 20px;

    color: #1890ff;

    margin-right: 8px;

  }



  .header-setting {

    margin-left: auto;

    font-size: 18px;

    color: #999;

    cursor: pointer;



    &:hover {

      color: #1890ff;

    }

  }

}



.quick-nav__title {

  font-size: 16px;

  font-weight: 600;

  color: #333;

}



.quick-nav__body {

  flex: 1;

  overflow-y: auto;

  padding: 16px;

}



.quick-nav__section {

  & + & {

    margin-top: 20px;

    padding-top: 16px;

    border-top: 1px dashed #ebeef5;

  }

}



.quick-nav__section-title {

  display: flex;

  align-items: center;

  margin-bottom: 12px;

  font-size: 13px;

  font-weight: 600;

  color: #666;



  .title-bar {

    width: 3px;

    height: 14px;

    background: #1890ff;

    margin-right: 8px;

    border-radius: 2px;



    &--external {

      background: #52c41a;

    }

  }

}



.quick-nav__grid {

  display: flex;

  flex-wrap: wrap;

}



.quick-nav__item {

  width: 16.666%;

  min-width: 72px;

  display: flex;

  flex-direction: column;

  align-items: center;

  padding: 8px 4px;

  cursor: pointer;

  transition: transform 0.15s;



  &:hover {

    transform: translateY(-2px);



    .quick-nav__label {

      color: #1890ff;

    }

  }

}



.quick-nav__icon {

  width: 44px;

  height: 44px;

  border-radius: 50%;

  display: flex;

  align-items: center;

  justify-content: center;

  margin-bottom: 8px;



  i {

    font-size: 20px;

    color: #fff;

  }



  .quick-nav__svg {

    width: 22px;

    height: 22px;

    color: #fff;

    fill: #fff;

  }



  &--logo {

    background: #f5f7fa;

    overflow: hidden;

  }

  &--plain {
    width: 44px;
    height: 44px;
    border-radius: 0;
    background: transparent;

    .quick-nav__menu-icon,
    .svg-icon {
      width: 28px;
      height: 28px;
      color: #606266;
      fill: #606266;
    }

    i {
      font-size: 28px;
      color: #606266;
    }

    &--empty {
      visibility: hidden;
    }
  }

}



.quick-nav__logo {

  width: 100%;

  height: 100%;

  object-fit: cover;

}



.quick-nav__label {

  font-size: 12px;

  color: #666;

  text-align: center;

  line-height: 1.3;

  word-break: break-all;

}



.quick-nav__footer {

  display: flex;

  align-items: center;

  justify-content: flex-end;

  padding: 10px 20px 16px;

  border-top: 1px solid #f0f0f0;



  .more-link {

    font-size: 13px;

    color: #666;

    cursor: pointer;

    margin-right: 12px;



    &:hover {

      color: #1890ff;

    }

  }



  .footer-icon {

    font-size: 16px;

    color: #999;

    cursor: pointer;



    &:hover {

      color: #1890ff;

    }

  }

}



.search-results {

  margin-top: 16px;

  max-height: 240px;

  overflow-y: auto;

}



.search-result-item {

  padding: 10px 12px;

  border-radius: 4px;

  cursor: pointer;

  font-size: 14px;

  color: #333;



  &:hover {

    background: #f5f7fa;

    color: #1890ff;

  }

}



.search-result-item__tag {

  display: inline-block;

  margin-right: 8px;

  padding: 0 6px;

  font-size: 12px;

  line-height: 20px;

  color: #909399;

  background: #f4f4f5;

  border-radius: 2px;

}



@media (max-width: 1400px) {

  .quick-nav__item {

    width: 25%;

  }

}

</style>


