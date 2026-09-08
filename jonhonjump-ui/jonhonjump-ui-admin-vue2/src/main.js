import Vue from 'vue'

import Element from 'element-ui'
import './assets/styles/element-variables.scss'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import plugins from './plugins' // plugins

import './assets/icons' // icon
import './permission' // permission control
import './tongji' // 百度统计
import { installChunkLoadGuard } from '@/utils/appVersion'
import { installPortalLogoutBroadcast } from '@/utils/portalLogoutBroadcast'
import { getDicts } from "@/api/system/dict/data";
import { getConfigKey } from "@/api/infra/config";
import { parseTime, resetForm, handleTree, addBeginAndEndTime, divide } from "@/utils/ruoyi";
import { isEmpty } from "@/utils";
import Pagination from "@/components/Pagination";
// 自定义表格工具扩展
import RightToolbar from "@/components/RightToolbar"
// 代码高亮插件
// import hljs from 'highlight.js'
// import 'highlight.js/styles/github-gist.css'
import { DICT_TYPE, getDictDataLabel, getDictDatas, getDictDatas2 } from "@/utils/dict";

// 低配机标记（≤4 核 或 ≤4GB 内存）：用于降级大面积毛玻璃等合成开销大的效果，
// 现场 Chrome 82 老机上 backdrop-filter 会拖垮抽屉/菜单开合动画帧率
;(function markLowPerfDevice() {
  const cores = navigator.hardwareConcurrency || 8
  const memory = navigator.deviceMemory || 8
  if (cores <= 4 || memory <= 4) {
    document.documentElement.classList.add('low-perf')
  }
})()

// 旧 Chromium 标记（<90，现场 82 内核）：82 的渲染管线对非合成层过渡动画
// （尤其 width/height/padding 布局动画和全屏 transform 过渡）每帧主线程重绘，
// 抽屉/dock/菜单开合严重掉帧（90 同机流畅）。legacy-anim 类下开合动画全部瞬开瞬关。
;(function markLegacyChromium() {
  const m = navigator.userAgent.match(/Chrom(?:e|ium)\/(\d+)/)
  if (m && Number(m[1]) < 90) {
    document.documentElement.classList.add('legacy-anim')
  }
})()

// 全局方法挂载
Vue.prototype.getDicts = getDicts
Vue.prototype.getConfigKey = getConfigKey
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.getDictDatas = getDictDatas
Vue.prototype.getDictDatas2 = getDictDatas2
Vue.prototype.getDictDataLabel = getDictDataLabel
Vue.prototype.DICT_TYPE = DICT_TYPE
Vue.prototype.handleTree = handleTree
Vue.prototype.addBeginAndEndTime = addBeginAndEndTime
Vue.prototype.divide = divide
Vue.prototype.isEmpty = isEmpty

// 全局组件挂载
Vue.component('DictTag', DictTag)
Vue.component('DocAlert', DocAlert)
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)
// 字典标签组件
import DictTag from '@/components/DictTag'
import DocAlert from '@/components/DocAlert'
// 头部标签插件
import VueMeta from 'vue-meta'

Vue.use(directive)
Vue.use(plugins)
Vue.use(VueMeta)
// Vue.use(hljs.vuePlugin);

// bpmnProcessDesigner 需要引入
import MyPD from "@/components/bpmnProcessDesigner/package/index.js";

Vue.use(MyPD);
import "@/components/bpmnProcessDesigner/package/theme/index.scss";
import "bpmn-js/dist/assets/diagram-js.css";
import "bpmn-js/dist/assets/bpmn-font/css/bpmn.css";
import "bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css";
import "bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css";

// Form Generator 组件需要使用到 tinymce
import Tinymce from '@/components/tinymce/index.vue'

Vue.component('tinymce', Tinymce)
import '@/assets/icons'
import request from "@/utils/request" // 实现 form generator 使用自己定义的 axios request 对象
console.log(request)
Vue.prototype.$axios = request
import '@/styles/index.scss'

// 默认点击背景不关闭弹窗
import ElementUI from 'element-ui'

ElementUI.Dialog.props.closeOnClickModal.default = false

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
  size: localStorage.getItem("size") || "medium", // set element-ui default size
});

Vue.config.productionTip = false

installChunkLoadGuard(router)
// 版本轮询停用（现场反馈：自动弹"系统已更新"打断使用且低配机有感知）。
// 保留 chunk 加载失败守卫：发版后旧页面点新路由 chunk 404 时仍会提示刷新，闭环不受影响。
// startAppVersionPoll()
installPortalLogoutBroadcast()

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
