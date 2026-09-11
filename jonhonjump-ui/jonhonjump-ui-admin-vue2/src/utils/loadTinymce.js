import ELEMENT from 'element-ui'

let tinymceObj

export default function loadTinymce(cb) {
  if (tinymceObj) {
    cb(tinymceObj)
    return
  }
  if (typeof window !== 'undefined' && window.tinymce) {
    tinymceObj = window.tinymce
    cb(tinymceObj)
    return
  }
  ELEMENT.Message.warning('富文本未安装本地资源，已禁止从外网加载')
}
