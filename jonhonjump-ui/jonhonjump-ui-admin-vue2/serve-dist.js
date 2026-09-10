/**
 * 本地测试服务器：serve dist 生产产物 + /admin-api 反代到本地后端 48080。
 * 用法：node serve-dist.js [端口，默认 8080]
 * 用 Chrome 82 访问 http://<本机IP>:端口/ 即可复现生产环境表现。
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = Number(process.argv[2]) || 8080
const DIST = path.join(__dirname, 'dist')
const BACKEND = { host: '127.0.0.1', port: 48080 }

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.gif': 'image/gif',
  '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.eot': 'application/vnd.ms-fontobject',
  '.map': 'application/json'
}

function proxy(req, res) {
  const up = http.request({ host: BACKEND.host, port: BACKEND.port, method: req.method, path: req.url, headers: { ...req.headers, host: `127.0.0.1:${BACKEND.port}` } }, rp => {
    res.writeHead(rp.statusCode, rp.headers)
    rp.pipe(res)
  })
  up.on('error', err => {
    res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('后端不可达(48080)：' + err.message)
  })
  req.pipe(up)
}

http.createServer((req, res) => {
  if (req.url.startsWith('/admin-api') || req.url.startsWith('/x-admin-api') || req.url.startsWith('/actuator')) {
    return proxy(req, res)
  }
  let urlPath = decodeURIComponent(req.url.split('?')[0])
  let file = path.join(DIST, urlPath)
  if (!file.startsWith(DIST)) { res.writeHead(403); return res.end('forbidden') }
  if (urlPath === '/' || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    file = path.join(DIST, 'index.html') // history 路由 fallback
  }
  const ext = path.extname(file).toLowerCase()
  res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream', 'Cache-Control': 'no-cache' })
  fs.createReadStream(file).pipe(res)
}).listen(PORT, '0.0.0.0', () => {
  console.log(`[serve-dist] http://localhost:${PORT}  (dist 产物, API -> ${BACKEND.host}:${BACKEND.port})`)
})
