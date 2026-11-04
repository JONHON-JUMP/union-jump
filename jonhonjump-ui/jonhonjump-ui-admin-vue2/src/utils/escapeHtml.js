/**
 * 把文本转义后放进 innerHTML（导入结果提示用了 dangerouslyUseHTMLString）。
 * 只用于拼 HTML 字符串，不要用于普通文本渲染。
 */
const HTML_ESCAPE_MAP = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

export function escapeHtml(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).replace(/[&<>"']/g, ch => HTML_ESCAPE_MAP[ch])
}
