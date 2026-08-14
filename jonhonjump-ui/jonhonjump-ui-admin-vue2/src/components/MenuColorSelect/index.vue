<template>
  <div class="menu-color-select">
    <div class="menu-color-select__preview" v-if="selected">
      <span class="menu-color-select__swatch" :style="swatchStyle(selected)" />
      <span class="menu-color-select__label">{{ selected.name }}</span>
      <el-button v-if="clearable && value" type="text" size="mini" @click="clear">清除</el-button>
    </div>
    <el-popover v-model="visible" placement="bottom-start" width="520" trigger="click">
      <div class="menu-color-select__panel">
        <el-input v-model="keyword" size="small" placeholder="搜索颜色名称 / MES 大类" clearable prefix-icon="el-icon-search" />
        <div class="menu-color-select__grid">
          <el-tooltip
            v-for="item in filteredList"
            :key="item.id"
            :content="tooltipContent(item)"
            placement="top"
            :open-delay="300"
          >
            <div
              class="menu-color-select__item"
              :class="{ 'is-active': item.id === value }"
              @click="choose(item)"
            >
              <span class="menu-color-select__item-swatch" :style="swatchStyle(item)" />
              <div class="menu-color-select__item-text">
                <div class="menu-color-select__item-name">{{ item.name }}</div>
                <div class="menu-color-select__item-meta">{{ item.mesCategory || '通用' }}</div>
              </div>
            </div>
          </el-tooltip>
        </div>
        <el-empty v-if="!filteredList.length" description="暂无匹配颜色" :image-size="48" />
      </div>
      <el-button slot="reference" size="small" icon="el-icon-brush">{{ value ? '更换颜色' : '选择菜单颜色' }}</el-button>
    </el-popover>
  </div>
</template>

<script>
import { getMenuStyleSimpleList } from '@/api/system/menuStyle'
import { buildMenuStylePreview } from '@/utils/menuIconStyle'

export default {
  name: 'MenuColorSelect',
  props: {
    value: { type: Number, default: null },
    clearable: { type: Boolean, default: true }
  },
  data() {
    return {
      visible: false,
      keyword: '',
      list: []
    }
  },
  computed: {
    selected() {
      return this.list.find(item => item.id === this.value) || null
    },
    filteredList() {
      const kw = (this.keyword || '').trim().toLowerCase()
      if (!kw) return this.list
      return this.list.filter(item => {
        const text = `${item.name || ''} ${item.mesCategory || ''} ${item.remark || ''}`.toLowerCase()
        return text.includes(kw)
      })
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      getMenuStyleSimpleList().then(res => {
        this.list = res.data || []
      })
    },
    swatchStyle(item) {
      if (!item) return {}
      return buildMenuStylePreview(item)
    },
    tooltipContent(item) {
      const parts = []
      if (item.mesCategory) parts.push(item.mesCategory)
      if (item.remark) parts.push(item.remark)
      parts.push(item.color)
      return parts.join(' · ')
    },
    choose(item) {
      this.$emit('input', item.id)
      this.visible = false
    },
    clear() {
      this.$emit('input', null)
    }
  }
}
</script>

<style scoped>
.menu-color-select__preview {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.menu-color-select__preview > * + * {
  margin-left: 8px;
}
.menu-color-select__swatch,
.menu-color-select__item-swatch {
  display: inline-block;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex-shrink: 0;
}
.menu-color-select__label {
  color: #303133;
  font-size: 13px;
}
.menu-color-select__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  max-height: 320px;
  overflow: auto;
}
.menu-color-select__item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.menu-color-select__item > * + * {
  margin-left: 10px;
}
.menu-color-select__item:hover,
.menu-color-select__item.is-active {
  border-color: #087ce5;
  box-shadow: 0 0 0 1px rgba(8, 124, 229, 0.15);
}
.menu-color-select__item-name {
  font-size: 13px;
  color: #303133;
  line-height: 1.3;
}
.menu-color-select__item-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
