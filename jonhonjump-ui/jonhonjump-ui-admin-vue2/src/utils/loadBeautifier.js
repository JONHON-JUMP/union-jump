import beautifier from 'js-beautify'

let beautifierObj

export default function loadBeautifier(cb) {
  if (!beautifierObj) {
    beautifierObj = beautifier
  }
  cb(beautifierObj)
}
