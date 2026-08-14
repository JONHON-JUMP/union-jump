<template>
  <div />
</template>

<script>
import { getTopWindow } from '@/utils/switchUser'

export default {
  name: 'Login',
  created() {
    const queryIndex = this.$route.fullPath.indexOf('?')
    const query = queryIndex >= 0 ? this.$route.fullPath.slice(queryIndex) : ''
    // 顶层跳转静态登录页，避免门户壳内再嵌一层
    getTopWindow().location.replace('/login/' + query)
  }
}
</script>
