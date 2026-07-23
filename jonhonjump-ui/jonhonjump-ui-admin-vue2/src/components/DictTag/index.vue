<template>
  <span>
    <template v-for="(dict, index) in dictItems">
      <span v-if="dict.colorType === 'default' || dict.colorType === '' || dict.colorType === undefined" :key="dict.value" :index="index"
            :class="dict.cssClass">{{ dict.label }}</span>
      <el-tag v-else :disable-transitions="true" :key="dict.value" :index="index" :type="dict.colorType" :class="dict.cssClass">
        {{ dict.label }}
      </el-tag>
    </template>
  </span>
</template>

<script>
import { getDictDatas2 } from '@/utils/dict'

export default {
  name: "DictTag",
  props: {
    type: String,
    value: [Number, String, Boolean, Array],
  },
  computed: {
    dictItems() {
      return getDictDatas2(this.type, this.value)
    }
  }
};
</script>
<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
