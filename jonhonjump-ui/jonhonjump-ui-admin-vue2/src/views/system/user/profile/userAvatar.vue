<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <img :src="displayAvatar" title="点击修改头像" class="img-circle img-lg" />
    </div>

    <!-- 自定义头像裁剪 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body @opened="modalOpened" @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <vue-cropper
            ref="cropper"
            :img="options.img"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixedBox="options.fixedBox"
            @realTime="realTime"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <img :src="previews.url" :style="previews.img" />
          </div>
        </el-col>
      </el-row>
      <div class="avatar-mode-tip">
        <span v-if="avatarMode === 'custom'">当前为自定义头像，裁剪后点击提交保存</span>
        <span v-else>当前为系统头像；上传图片可切换为自定义头像</span>
      </div>
      <el-row class="avatar-toolbar">
        <el-col :span="14">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择图片
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
          <el-button size="small" icon="el-icon-picture-outline" @click="openSystemDialog">系统头像</el-button>
          <el-button icon="el-icon-plus" size="small" @click="changeScale(1)"></el-button>
          <el-button icon="el-icon-minus" size="small" @click="changeScale(-1)"></el-button>
          <el-button icon="el-icon-refresh-left" size="small" @click="rotateLeft()"></el-button>
          <el-button icon="el-icon-refresh-right" size="small" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :span="10" class="avatar-submit-col">
          <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 系统头像选择 -->
    <el-dialog title="系统头像" :visible.sync="systemOpen" width="560px" append-to-body @open="initSystemSelection">
      <p class="system-avatar-desc">
        按角色优先级排列，排序最靠前的为默认头像（当前默认：<strong>{{ defaultRoleLabel }}</strong>）。
      </p>
      <div class="system-avatar-list">
        <!-- 多角色时才显示「跟随角色默认」，避免与首个角色头像重复 -->
        <div
          v-if="showAutoFollowOption"
          class="system-avatar-item"
          :class="{ active: selectedSystemValue === '' }"
          @click="selectAutoFollow()"
        >
          <div class="system-avatar-auto-icon">
            <i class="el-icon-refresh"></i>
          </div>
          <div class="system-avatar-name">跟随角色默认</div>
          <div class="system-avatar-sub">{{ defaultRoleLabel }}</div>
          <i v-if="selectedSystemValue === ''" class="el-icon-check system-avatar-check"></i>
        </div>
        <div
          v-for="(item, index) in systemAvatarOptions"
          :key="item.code"
          class="system-avatar-item"
          :class="{ active: isRoleAvatarActive(item) }"
          @click="selectRoleAvatar(item)"
        >
          <div class="system-avatar-img-wrap">
            <img :src="item.url" :alt="item.label" />
          </div>
          <div class="system-avatar-name">{{ item.label }}</div>
          <div v-if="index === 0" class="system-avatar-sub">默认</div>
          <i v-if="isRoleAvatarActive(item)" class="el-icon-check system-avatar-check"></i>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="systemOpen = false">取 消</el-button>
        <el-button type="primary" @click="confirmSystemAvatar">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import store from "@/store";
import { VueCropper } from "vue-cropper";
import { updateUserProfile } from "@/api/system/user";
import { uploadFile } from "@/api/infra/file";
import {
  resolveUserAvatar,
  resolvePrimaryRoleCode,
  getSystemAvatarOptions,
  parseAvatarSource,
  buildSystemAvatarValue,
  getRoleAvatarLabel
} from "@/utils/defaultAvatar";

export default {
  components: { VueCropper },
  props: {
    user: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      open: false,
      systemOpen: false,
      visible: false,
      title: "修改头像",
      avatarMode: "system",
      selectedSystemValue: "",
      options: {
        img: "",
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        fixedBox: true
      },
      previews: {}
    };
  },
  computed: {
    roles() {
      return store.getters.roles || [];
    },
    displayAvatar() {
      return resolveUserAvatar(this.user.avatar, this.roles);
    },
    defaultRoleLabel() {
      return getRoleAvatarLabel(resolvePrimaryRoleCode(this.roles));
    },
    systemAvatarOptions() {
      return getSystemAvatarOptions(this.roles);
    },
    showAutoFollowOption() {
      return this.systemAvatarOptions.length > 1;
    }
  },
  methods: {
    buildSystemValue(roleCode) {
      return buildSystemAvatarValue(roleCode);
    },
    selectAutoFollow() {
      this.selectedSystemValue = "";
    },
    selectRoleAvatar(item) {
      if (this.showAutoFollowOption) {
        this.selectedSystemValue = buildSystemAvatarValue(item.code);
      } else {
        this.selectedSystemValue = "";
      }
    },
    isRoleAvatarActive(item) {
      const roleValue = buildSystemAvatarValue(item.code);
      if (this.showAutoFollowOption) {
        return this.selectedSystemValue === roleValue;
      }
      return this.selectedSystemValue === "" || this.selectedSystemValue === roleValue;
    },
    editCropper() {
      const source = parseAvatarSource(this.user.avatar);
      this.avatarMode = source.type === "custom" ? "custom" : "system";
      this.options.img = this.displayAvatar;
      this.open = true;
    },
    openSystemDialog() {
      this.systemOpen = true;
    },
    initSystemSelection() {
      const source = parseAvatarSource(this.user.avatar);
      if (source.type === "auto") {
        this.selectedSystemValue = "";
      } else if (source.type === "system") {
        this.selectedSystemValue = source.value;
      } else {
        this.selectedSystemValue = "";
      }
    },
    modalOpened() {
      this.visible = true;
    },
    requestUpload() {},
    rotateLeft() {
      this.$refs.cropper.rotateLeft();
    },
    rotateRight() {
      this.$refs.cropper.rotateRight();
    },
    changeScale(num) {
      num = num || 1;
      this.$refs.cropper.changeScale(num);
    },
    beforeUpload(file) {
      if (file.type.indexOf("image/") === -1) {
        this.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。");
      } else {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          this.options.img = reader.result;
          this.avatarMode = "custom";
        };
      }
    },
    async confirmSystemAvatar() {
      try {
        await updateUserProfile({ avatar: this.selectedSystemValue });
        store.commit("SET_RAW_AVATAR", this.selectedSystemValue);
        store.commit("SET_AVATAR", resolveUserAvatar(this.selectedSystemValue, this.roles));
        this.options.img = store.getters.avatar;
        this.avatarMode = "system";
        this.systemOpen = false;
        this.open = false;
        this.visible = false;
        this.$emit("refresh");
        this.$modal.msgSuccess("修改成功");
      } catch (error) {
        console.error("系统头像设置失败:", error);
        this.$modal.msgError("系统头像设置失败，请重试");
      }
    },
    async uploadImg() {
      if (this.avatarMode !== "custom") {
        this.$modal.msgWarning("请先选择图片，或使用「系统头像」设置系统默认头像");
        return;
      }
      try {
        this.$refs.cropper.getCropBlob(async (data) => {
          const response = await uploadFile(data, "user/avatar");
          const avatar = response.data;
          await updateUserProfile({ avatar });
          store.commit("SET_RAW_AVATAR", avatar);
          store.commit("SET_AVATAR", avatar);
          this.open = false;
          this.visible = false;
          this.$emit("refresh");
          this.$modal.msgSuccess("修改成功");
        });
      } catch (error) {
        console.error("头像上传失败:", error);
        this.$modal.msgError("头像上传失败，请重试");
      }
    },
    realTime(data) {
      this.previews = data;
    },
    closeDialog() {
      this.options.img = this.displayAvatar;
      this.visible = false;
    }
  }
};
</script>

<style scoped lang="scss">
.user-info-head {
  position: relative;
  display: inline-block;
  width: 120px;
  height: 120px;
  overflow: hidden;
  border-radius: 50%;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.user-info-head:hover:after {
  content: '+';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  color: #eee;
  background: rgba(0, 0, 0, 0.5);
  font-size: 24px;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  cursor: pointer;
  line-height: 110px;
  border-radius: 50%;
}

.avatar-mode-tip {
  margin: 8px 0 12px;
  color: #909399;
  font-size: 13px;
}

.avatar-toolbar {
  .el-button {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

.avatar-submit-col {
  text-align: right;
}

.system-avatar-desc {
  margin: 0 0 16px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.system-avatar-list {
  display: flex;
  flex-wrap: wrap;
  margin: -8px;
}
.system-avatar-list > * {
  margin: 8px;
}

.system-avatar-item {
  position: relative;
  width: 120px;
  padding: 12px 8px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: #c0c4cc;
  }

  &.active {
    border-color: #409eff;
    box-shadow: 0 0 0 1px #409eff;
  }
}

.system-avatar-img-wrap {
  width: 72px;
  height: 72px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f7fa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.system-avatar-auto-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.system-avatar-name {
  margin-top: 8px;
  font-size: 13px;
  color: #303133;
}

.system-avatar-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.system-avatar-check {
  position: absolute;
  top: 6px;
  right: 6px;
  color: #409eff;
  font-size: 16px;
  font-weight: bold;
}
</style>
