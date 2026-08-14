<template>
  <div v-loading="loading" class="notify-detail-page">
    <article v-if="detail.id" class="notify-detail-card">
      <h1 class="notify-detail-title">{{ detail.title }}</h1>

      <div class="notify-detail-meta">
        <div class="notify-detail-meta__item">
          <span class="label">通知类型</span>
          <span class="value">{{ getDictDataLabel(DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE, detail.templateType) || '-' }}</span>
        </div>
        <div class="notify-detail-meta__item">
          <span class="label">发布时间</span>
          <span class="value">{{ parseTime(detail.createTime) || '-' }}</span>
        </div>
        <div class="notify-detail-meta__item">
          <span class="label">发布人</span>
          <span class="value">{{ detail.publisherName || '-' }}</span>
        </div>
        <div class="notify-detail-meta__item">
          <span class="label">发布部门</span>
          <span class="value">{{ detail.deptName || '-' }}</span>
        </div>
      </div>

      <div class="notify-detail-divider" />

      <div class="notify-detail-body" v-html="detail.content" />

      <section v-if="detail.attachments && detail.attachments.length" class="notify-detail-attachments">
        <h3>附件</h3>
        <div
          v-for="(file, index) in detail.attachments"
          :key="file.url + index"
          class="notify-attachment-item"
        >
          <div class="notify-attachment-item__icon">
            <i class="el-icon-document" />
          </div>
          <div class="notify-attachment-item__info">
            <button type="button" class="notify-attachment-item__name" @click="downloadAttachment(file)">
              {{ file.name || getFileName(file.url) }}
            </button>
            <span v-if="file.size" class="notify-attachment-item__size">{{ formatFileSize(file.size) }}</span>
          </div>
          <el-button type="text" @click="downloadAttachment(file)">下载</el-button>
        </div>
      </section>
    </article>

    <el-empty v-else-if="!loading" description="通知不存在或无权查看" />
  </div>
</template>

<script>
import axios from 'axios'
import { getAccessToken } from '@/utils/auth'
import { getAppNotice, getNotice } from '@/api/system/notice'
import { getMyNotifyMessageDetail } from '@/api/system/notify/message'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'MyNotifyMessageDetail',
  data() {
    return {
      loading: false,
      detail: {}
    }
  },
  watch: {
    '$route'(route) {
      if (route.name !== 'MyNotifyMessageDetail') {
        return
      }
      this.loadDetail()
    }
  },
  created() {
    if (this.$route.name === 'MyNotifyMessageDetail') {
      this.loadDetail()
    }
  },
  methods: {
    loadDetail() {
      if (this.$route.name !== 'MyNotifyMessageDetail') {
        return
      }
      const noticeId = this.$route.query.noticeId
      const id = this.$route.query.id
      if (!noticeId && !id) {
        this.$message.error('缺少通知编号，请从列表点击某条通知进入')
        return
      }
      this.loading = true
      const request = noticeId
        ? this.loadNoticeById(noticeId)
        : getMyNotifyMessageDetail(id).then(response => response.data || {})
      request.then(detail => {
        this.detail = detail
      }).catch(() => {
        this.detail = {}
      }).finally(() => {
        this.loading = false
      })
    },
    loadNoticeById(noticeId) {
      const fetchNotice = checkPermi(['system:notice:query']) ? getNotice : getAppNotice
      return fetchNotice(noticeId).then(response => {
        const data = response.data || {}
        return { ...data, templateType: data.type }
      })
    },
    getFileName(url) {
      if (!url) return '附件'
      const cleanUrl = url.split('?')[0]
      return cleanUrl.slice(cleanUrl.lastIndexOf('/') + 1) || '附件'
    },
    formatFileSize(size) {
      if (!size) return ''
      if (size < 1024) return `${size}B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)}K`
      return `${(size / 1024 / 1024).toFixed(2)}M`
    },
    downloadAttachment(file) {
      const rawUrl = file.url
      if (!rawUrl) {
        this.$message.warning('附件地址无效')
        return
      }
      const url = /^https?:\/\//i.test(rawUrl) ? rawUrl : `${process.env.VUE_APP_BASE_API}${rawUrl}`
      axios({
        method: 'get',
        url,
        responseType: 'blob',
        headers: { Authorization: 'Bearer ' + getAccessToken() }
      }).then(response => {
        const blob = new Blob([response.data])
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = file.name || this.getFileName(rawUrl)
        link.click()
        window.URL.revokeObjectURL(link.href)
      }).catch(() => {
        this.$message.error('附件下载失败')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.notify-detail-page {
  width: 100%;
  max-width: 1320px;
  min-height: calc(100vh - 120px);
  margin: 0 auto;
  padding: 0 40px 24px;
  box-sizing: border-box;
}

.notify-detail-card {
  width: 100%;
  min-height: calc(100vh - 144px);
  padding: 40px 48px 48px;
  box-sizing: border-box;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 18px rgba(16, 35, 62, .05);
}

.notify-detail-title {
  margin: 0 0 32px;
  color: #087ce5;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.45;
  text-align: center;
}

.notify-detail-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px 24px;
}

.notify-detail-meta__item {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.notify-detail-meta__item > * + * {
  margin-top: 6px;
}

.notify-detail-meta__item .label {
  color: #7a8ea8;
  font-size: 13px;
}

.notify-detail-meta__item .value {
  color: #10233e;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.notify-detail-divider {
  height: 1px;
  margin: 24px 0;
  background: #e7eef6;
}

.notify-detail-body {
  min-height: 200px;
  color: #10233e;
  font-size: 16px;
  line-height: 2;
  word-break: break-word;
}

.notify-detail-attachments {
  margin-top: 28px;
  padding: 18px 20px;
  border-radius: 10px;
  background: #f5f8fc;
}

.notify-detail-attachments h3 {
  margin: 0 0 14px;
  color: #10233e;
  font-size: 15px;
}

.notify-attachment-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid #e5edf5;
}
.notify-attachment-item > * + * {
  margin-left: 14px;
}

.notify-attachment-item:first-of-type {
  border-top: 0;
}

.notify-attachment-item__icon {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  color: #087ce5;
  background: #e8f3fd;
  font-size: 22px;
}

.notify-attachment-item__info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}
.notify-attachment-item__info > * + * {
  margin-top: 4px;
}

.notify-attachment-item__name {
  padding: 0;
  border: 0;
  color: #10233e;
  background: transparent;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.notify-attachment-item__name:hover {
  color: #087ce5;
}

.notify-attachment-item__size {
  color: #7a8ea8;
  font-size: 12px;
}

@media (max-width: 768px) {
  .notify-detail-page {
    padding: 0 16px 20px;
  }

  .notify-detail-page,
  .notify-detail-card {
    min-height: calc(100vh - 100px);
  }

  .notify-detail-card {
    padding: 24px 16px 32px;
  }

  .notify-detail-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notify-detail-title {
    font-size: 22px;
  }
}
</style>
