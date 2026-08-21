const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')

const vueFilePath = path.join(__dirname, '../src/views/mes/process/card/index.vue')
const apiFilePath = path.join(__dirname, '../src/api/mes/process/card.js')

function readVueFile() {
  return fs.readFileSync(vueFilePath, 'utf8')
}

function loadComponent(options = {}) {
  const scriptMatch = readVueFile().match(/<script>([\s\S]*?)<\/script>/)
  assert.ok(scriptMatch, 'index.vue should contain a script block')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    window: { open: options.openWindow || (() => null) },
    queryProcessCard: options.queryProcessCard || (() => Promise.resolve({ data: [] })),
    encodeURIComponent
  }
  const executableScript = scriptMatch[1]
    .replace(/import \{ queryProcessCard \} from '@\/api\/mes\/process\/card'\s*/, '')
    .replace('export default', 'module.exports =')
  vm.runInNewContext(executableScript, sandbox, { filename: 'index.vue' })
  return sandbox.module.exports
}

function responseCards() {
  return [{
    accno: '43091', version: null, isFormal: 0, isFix: 1,
    details: [{
      idx: 1, name: '铆接', code: null, no: '10', url: 'http://pdm.example/export?oid=1',
      children: [{
        idx: 2, name: '子工序', code: null, no: '10.1',
        url: 'http://pdm.example/export?oid=1', children: []
      }]
    }]
  }]
}

function createContext(component, overrides = {}) {
  return {
    ...component.data(),
    ...component.methods,
    $refs: {
      queryForm: { validate: callback => callback(true), resetFields: () => {} },
      processTable: { toggleRowExpansion: () => {} }
    },
    $nextTick: callback => {
      if (callback) callback()
      return Promise.resolve()
    },
    ...overrides
  }
}

test('declares the real queryCard API contract', () => {
  const apiSource = fs.readFileSync(apiFilePath, 'utf8')
  assert.match(apiSource, /url:\s*['"]\/mes\/process\/query\/card['"]/)
  assert.match(apiSource, /method:\s*['"]post['"]/)
  assert.match(apiSource, /\bdata\b/)
})

test('normalizes backend cards and nested operation details', () => {
  const component = loadComponent()
  const result = component.methods.normalizeCards.call(createContext(component), responseCards())

  assert.equal(result.length, 1)
  assert.equal(result[0].processNo, '43091')
  assert.equal(result[0].version, '—')
  assert.equal(result[0].name, '临时工艺')
  assert.equal(result[0].children[0].operationNo, '10')
  assert.equal(result[0].children[0].code, null)
  assert.equal(result[0].children[0].children[0].operationNo, '10.1')
})

test('uses the full tree path to keep fallback row keys unique', () => {
  const component = loadComponent()
  const cards = responseCards()
  cards[0].details = [
    { name: '父工序A', no: '10', children: [{ name: '同号子工序', no: '1', children: [] }] },
    { name: '父工序B', no: '20', children: [{ name: '同号子工序', no: '1', children: [] }] }
  ]

  const result = component.methods.normalizeCards.call(createContext(component), cards)

  assert.notEqual(result[0].children[0].children[0].id, result[0].children[1].children[0].id)
})

test('queries with material and process numbers then expands the result tree', async () => {
  const calls = []
  const component = loadComponent({
    queryProcessCard: data => {
      calls.push(data)
      return Promise.resolve({ code: 0, data: responseCards() })
    }
  })
  const toggled = []
  const context = createContext(component, {
    queryParams: { prtno: 'MAT-1', accno: '43091' },
    $refs: {
      queryForm: { validate: callback => callback(true), resetFields: () => {} },
      processTable: { toggleRowExpansion: (node, expanded) => toggled.push([node.operationNo, expanded]) }
    }
  })

  await component.methods.handleQuery.call(context)

  assert.equal(calls.length, 1)
  assert.equal(calls[0].prtno, 'MAT-1')
  assert.equal(calls[0].accno, '43091')
  assert.equal(context.loading, false)
  assert.equal(context.processTree[0].children[0].operationNo, '10')
  assert.equal(context.displayProcessTree, context.processTree)
  assert.equal(context.recentQueries.length, 1)
  assert.equal(context.recentQueries[0].label, 'MAT-1 / 43091')
  assert.deepEqual(toggled, [['', true], ['10', true]])
})

test('clears stale results when queryCard fails', async () => {
  const component = loadComponent({ queryProcessCard: () => Promise.reject(new Error('query failed')) })
  const staleTree = [{ id: 'stale' }]
  const context = createContext(component, {
    queryParams: { prtno: 'MAT-1', accno: '43091' }, processTree: staleTree, displayProcessTree: staleTree
  })

  await component.methods.handleQuery.call(context)

  assert.equal(context.loading, false)
  assert.equal(context.processTree.length, 0)
  assert.equal(context.displayProcessTree.length, 0)
})

test('keeps the newest result when concurrent queries finish out of order', async () => {
  const pending = []
  const component = loadComponent({
    queryProcessCard: data => new Promise(resolve => pending.push({ data, resolve }))
  })
  const context = createContext(component, {
    queryParams: { prtno: 'MAT-A', accno: '43091' }
  })

  const first = component.methods.handleQuery.call(context)
  await Promise.resolve()
  context.queryParams = { prtno: 'MAT-B', accno: '43092' }
  const second = component.methods.handleQuery.call(context)
  await Promise.resolve()

  const newestCards = responseCards()
  newestCards[0].accno = '43092'
  pending[1].resolve({ data: newestCards })
  await second
  pending[0].resolve({ data: responseCards() })
  await first

  assert.equal(context.processTree[0].processNo, '43092')
  assert.equal(context.loading, false)
})

test('replays both fields from a recent query', () => {
  const component = loadComponent()
  let queried = false
  const context = createContext(component, { handleQuery: () => { queried = true } })

  component.methods.handleRecentQuery.call(context, { prtno: 'MAT-2', accno: '4309', label: 'MAT-2 / 4309' })

  assert.equal(context.queryParams.prtno, 'MAT-2')
  assert.equal(context.queryParams.accno, '4309')
  assert.equal(queried, true)
})

test('toggles every expandable row through the Element table instance', () => {
  const component = loadComponent()
  const tree = component.methods.normalizeCards.call(createContext(component), responseCards())
  const toggled = []
  const context = createContext(component, {
    displayProcessTree: tree,
    $refs: { processTable: { toggleRowExpansion: (node, expanded) => toggled.push([node.operationNo, expanded]) } }
  })

  component.methods.setAllExpanded.call(context, false)

  assert.deepEqual(toggled, [['', false], ['10', false]])
})

test('opens the configured process link in a protected new window', () => {
  const openedWindow = { opener: 'source-window' }
  const calls = []
  const component = loadComponent({
    openWindow: (...args) => { calls.push(args); return openedWindow }
  })
  const messages = []
  const context = { $message: { warning: message => messages.push(message) } }

  component.methods.handleView.call(context, { externalUrl: 'http://pdm.example/export?oid=1' })

  assert.deepEqual(calls, [['http://pdm.example/export?oid=1', '_blank', 'noopener,noreferrer']])
  assert.equal(openedWindow.opener, null)
  assert.deepEqual(messages, [])
})

test('warns instead of opening when a process link is missing', () => {
  let opened = false
  const component = loadComponent({ openWindow: () => { opened = true } })
  const messages = []
  const context = { $message: { warning: message => messages.push(message) } }

  component.methods.handleView.call(context, { externalUrl: '' })

  assert.equal(opened, false)
  assert.deepEqual(messages, ['暂未配置工艺查看地址'])
})

test('does not force viewport height that overflows the parent scroller', () => {
  assert.doesNotMatch(readVueFile(), /min-height:\s*calc\(100vh/)
})
