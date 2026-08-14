/**
 * Camstar / 门户打开链路诊断日志（对照 4200：cookie → 立刻挂 meta.link）。
 * 控制台过滤：camstar-diag
 *
 * 分段含义：
 * - click: 用户点击快捷导航/菜单
 * - shell: 切子系统壳（应 <5ms，禁止 await 菜单）
 * - navigate: router.push 完成
 * - iframe-mount: InnerLink 开始挂 src
 * - iframe-load: iframe 文档 onload（之后白屏多半是 Camstar 自己拉数）
 * - total: click → onload
 */
const TAG = '[camstar-diag]'

let seq = 0
const openers = {}

function now() {
  return typeof performance !== 'undefined' && performance.now
    ? performance.now()
    : Date.now()
}

function style(kind) {
  if (kind === 'warn') return 'color:#e6a23c;font-weight:600'
  if (kind === 'err') return 'color:#f56c6c;font-weight:600'
  if (kind === 'ok') return 'color:#67c23a;font-weight:600'
  return 'color:#409eff'
}

export function startCamstarOpenTrace(meta = {}) {
  const id = ++seq
  openers[id] = {
    id,
    t0: now(),
    marks: {},
    meta: { ...meta }
  }
  log(id, 'click', 'ok', meta)
  return id
}

export function markCamstarOpen(id, name, detail) {
  const tr = openers[id]
  if (!tr) {
    return
  }
  const t = now()
  const prev = tr.lastMarkAt != null ? tr.lastMarkAt : tr.t0
  tr.marks[name] = t
  tr.lastMarkAt = t
  const stepMs = Math.round(t - prev)
  const totalMs = Math.round(t - tr.t0)
  const kind = stepMs > 1000 ? 'warn' : (stepMs > 3000 ? 'err' : 'ok')
  log(id, name, kind, {
    stepMs,
    totalMs,
    ...(detail || {})
  })
  if (name === 'iframe-load' || name === 'fail') {
    summarize(id)
  }
}

export function getActiveCamstarOpenId() {
  const ids = Object.keys(openers)
  return ids.length ? Number(ids[ids.length - 1]) : 0
}

function log(id, name, kind, detail) {
  if (typeof console === 'undefined' || !console.log) {
    return
  }
  console.log(
    `%c${TAG} #${id} ${name}`,
    style(kind),
    detail || {}
  )
}

function markAt(tr, name) {
  return tr.marks[name] != null ? Math.round(tr.marks[name] - tr.t0) : null
}

function summarize(id) {
  const tr = openers[id]
  if (!tr) {
    return
  }
  const tCookie = markAt(tr, 'cookie')
  const tShell = markAt(tr, 'shell')
  const tNav = markAt(tr, 'navigate')
  const tMount = markAt(tr, 'iframe-mount')
  const tLoad = markAt(tr, 'iframe-load')
  const jumpMs = tMount != null ? tMount : (tNav != null ? tNav : 0)
  const camstarMs = (tLoad != null && tMount != null) ? (tLoad - tMount) : null
  const total = tr.lastMarkAt != null ? Math.round(tr.lastMarkAt - tr.t0) : -1

  let verdict = '未知'
  let kind = 'ok'
  if (tLoad == null && tMount == null) {
    verdict = 'JUMP问题：还没挂上 iframe（看 syncPortalIframeView / placeholder）'
    kind = 'err'
  } else if (tLoad == null && tMount != null) {
    verdict = 'Camstar/网络问题：src 已挂上但文档一直 onload 不来（或被挡）'
    kind = 'err'
  } else if (jumpMs != null && jumpMs > 1000) {
    verdict = `JUMP偏慢：点击→挂src 用了 ${jumpMs}ms（4200 应接近 0）`
    kind = 'warn'
  } else if (camstarMs != null && camstarMs > 2000) {
    verdict = `Camstar偏慢：iframe 文档加载 ${camstarMs}ms（与 JUMP 无关；4200 直开也会接近这个数）`
    kind = camstarMs > 5000 ? 'err' : 'warn'
  } else {
    verdict = '正常：JUMP 开销小，Camstar 文档也还行'
    kind = 'ok'
  }

  console.log(
    `%c${TAG} #${id} 结论：${verdict}`,
    style(kind),
    {
      totalMs: total,
      jumpUntilMountMs: jumpMs,
      camstarDocMs: camstarMs,
      timelineMs: {
        cookie: tCookie,
        shell: tShell,
        navigate: tNav,
        'iframe-mount': tMount,
        'iframe-load': tLoad
      },
      meta: tr.meta,
      tip: '对照：同页用 4200 打开看 Network 文档耗时；若两边都慢=Camstar；仅 JUMP 慢=门户'
    }
  )
  setTimeout(() => {
    delete openers[id]
  }, 60000)
}

/** 单点事件（非完整 open 链路） */
export function camstarDiagEvent(name, detail) {
  if (typeof console === 'undefined' || !console.log) {
    return
  }
  console.log(`%c${TAG} ${name}`, style('ok'), detail || {})
}
