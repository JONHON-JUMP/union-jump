<template>
  <div />
</template>

<script>
export default {
  name: 'Login',
  created() {
    const queryIndex = this.$route.fullPath.indexOf('?')
    const query = queryIndex >= 0 ? this.$route.fullPath.slice(queryIndex) : ''
    window.location.replace('/login/' + query)
  }
}
</script>
