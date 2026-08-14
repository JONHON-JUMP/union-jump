<template>
  <div v-loading="loading" class="faq-detail-page">
    <article v-if="detail.id" class="faq-detail-card">
      <h1 class="faq-detail-title">{{ detail.title }}</h1>

      <div class="faq-detail-meta">
        <div class="faq-detail-meta__item">
          <span class="label">分类</span>
          <span class="value">{{ getDictDataLabel(DICT_TYPE.SYSTEM_FAQ_CATEGORY, detail.category) || '-' }}</span>
        </div>
        <div class="faq-detail-meta__item">
          <span class="label">发布时间</span>
          <span class="value">{{ parseTime(detail.createTime) || '-' }}</span>
        </div>
        <div class="faq-detail-meta__item">
          <span class="label">发布人</span>
          <span class="value">{{ detail.publisherName || '-' }}</span>
        </div>
        <div class="faq-detail-meta__item">
          <span class="label">发布部门</span>
          <span class="value">{{ detail.deptName || '-' }}</span>
        </div>
      </div>

      <div class="faq-detail-divider"/>

      <div class="faq-detail-body" v-html="detail.content"/>
    </article>

    <el-empty v-else-if="!loading" description="常见 QA 不存在或已关闭"/>
  </div>
</template>

<script>
import { getAppFaq, getFaq } from '@/api/system/faq'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'MyFaqDetail',
  data() {
    return {
      loading: false,
      detail: {}
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.loadDetail()
      },
      deep: true
    }
  },
  created() {
    this.loadDetail()
  },
  activated() {
    this.loadDetail()
  },
  methods: {
    loadDetail() {
      if (this.$route.name !== 'MyFaqDetail') {
        return
      }
      const faqId = this.$route.query.faqId
      if (!faqId) {
        this.detail = {}
        this.$message.error('缺少常见 QA 编号')
        return
      }
      this.loading = true
      const fetchFaq = checkPermi(['system:faq:query']) ? getFaq : getAppFaq
      fetchFaq(faqId).then(response => {
        this.detail = response.data || {}
      }).catch(() => {
        this.detail = {}
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.faq-detail-page {
  width: 100%;
  max-width: 1320px;
  min-height: calc(100vh - 120px);
  margin: 0 auto;
  padding: 0 40px 24px;
  box-sizing: border-box;
}

.faq-detail-card {
  width: 100%;
  min-height: calc(100vh - 144px);
  padding: 40px 48px 48px;
  box-sizing: border-box;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 18px rgba(16, 35, 62, .05);
}

.faq-detail-title {
  margin: 0 0 32px;
  color: #087ce5;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.45;
  text-align: center;
}

.faq-detail-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px 24px;
}

.faq-detail-meta__item {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.faq-detail-meta__item > * + * {
  margin-top: 6px;
}

.faq-detail-meta__item .label {
  color: #7a8ea8;
  font-size: 13px;
}

.faq-detail-meta__item .value {
  color: #10233e;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.faq-detail-divider {
  height: 1px;
  margin: 24px 0;
  background: #e7eef6;
}

.faq-detail-body {
  min-height: 200px;
  color: #10233e;
  font-size: 16px;
  line-height: 2;
  word-break: break-word;
}

@media (max-width: 768px) {
  .faq-detail-page {
    padding: 0 16px 20px;
  }

  .faq-detail-page,
  .faq-detail-card {
    min-height: calc(100vh - 100px);
  }

  .faq-detail-card {
    padding: 24px 16px 32px;
  }

  .faq-detail-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .faq-detail-title {
    font-size: 22px;
  }
}
</style>
