<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="80px">
    <el-form-item label="用户昵称" prop="nickname">
      <el-input v-model="form.nickname" />
    </el-form-item>
    <el-form-item label="域账号">
      <el-input :value="user.domainNo || '-'" disabled />
    </el-form-item>
    <el-form-item label="工号">
      <el-input :value="user.employeeNo || '-'" disabled />
    </el-form-item>
    <el-form-item label="刷卡卡号">
      <el-input :value="user.cardNo || '-'" disabled />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="form.sex">
        <el-radio :label="1">男</el-radio>
        <el-radio :label="2">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">保存</el-button>
      <el-button type="danger" size="mini" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from '@/api/system/user'

export default {
  props: {
    user: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      form: {
        nickname: '',
        sex: undefined
      },
      rules: {
        nickname: [
          { required: true, message: '用户昵称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  watch: {
    user: {
      immediate: true,
      deep: true,
      handler(value) {
        this.form.nickname = value.nickname || ''
        this.form.sex = value.sex
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        updateUserProfile({
          nickname: this.form.nickname,
          sex: this.form.sex
        }).then(() => {
          this.$modal.msgSuccess('修改成功')
          this.user.nickname = this.form.nickname
          this.user.sex = this.form.sex
        })
      })
    },
    close() {
      this.$tab.closePage()
    }
  }
}
</script>
