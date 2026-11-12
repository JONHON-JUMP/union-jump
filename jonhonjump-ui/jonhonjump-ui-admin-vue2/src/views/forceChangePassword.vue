<template>
  <div class="force-pwd">
    <el-card class="force-pwd-card">
      <div class="force-pwd-title">请先修改初始密码</div>
      <div class="force-pwd-desc">当前密码仍是初始密码，修改完成后才能进入 JUMP。</div>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" @submit.native.prevent>
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" @keyup.enter.native="submit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">确认修改</el-button>
          <el-button @click="logout">退出登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { updateUserPwd } from '@/api/system/user'

export default {
  name: 'ForceChangePassword',
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.form.newPassword !== value) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      submitting: false,
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '新密码不能为空', trigger: 'blur' },
          { min: 4, max: 16, message: '密码长度为 4-16 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '确认密码不能为空', trigger: 'blur' },
          { validator: equalToPassword, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid || this.submitting) {
          return
        }
        this.submitting = true
        updateUserPwd(this.form.oldPassword, this.form.newPassword).then(() => {
          this.$store.commit('SET_MUST_CHANGE_PASSWORD', false)
          this.$modal.msgSuccess('密码已修改')
          this.$router.replace('/index')
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => {
        window.location.href = '/login/'
      })
    }
  }
}
</script>

<style scoped>
.force-pwd {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.force-pwd-card {
  width: 460px;
}
.force-pwd-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}
.force-pwd-desc {
  color: #606266;
  margin-bottom: 24px;
}
</style>
