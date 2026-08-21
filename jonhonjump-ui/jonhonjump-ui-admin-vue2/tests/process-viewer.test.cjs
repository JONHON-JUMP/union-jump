const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')

const vueFilePath = path.join(__dirname, '../src/views/mes/process/card/index.vue')

function readVueFile() {
  return fs.readFileSync(vueFilePath, 'utf8')
}

function loadComponent(openWindow = () => null) {
  const scriptMatch = readVueFile().match(/<script>([\s\S]*?)<\/script>/)
  assert.ok(scriptMatch, 'index.vue should contain a script block')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    window: { open: openWindow },
    encodeURIComponent
  }
  const executableScript = scriptMatch[1].replace('export default', 'module.exports =')
  vm.runInNewContext(executableScript, sandbox, { filename: 'index.vue' })
  return sandbox.module.exports
}

test('keeps ancestor nodes when a child process matches', () => {
  const component = loadComponent()
  const state = component.data()
  const context = { ...state, filterProcessTree: component.methods.filterProcessTree }

  const result = component.methods.filterProcessTree.call(context, state.processTree, ' dx889011 ')

  assert.equal(result.length, 1)
  assert.equal(result[0].processNo, 'C12345')
  assert.equal(result[0].children.length, 1)
  assert.equal(result[0].children[0].processNo, 'DX889012')
  assert.equal(result[0].children[0].children[0].processNo, 'DX889011')
})

test('keeps the complete subtree when its parent process matches', () => {
  const component = loadComponent()
  const state = component.data()
  const context = { ...state, filterProcessTree: component.methods.filterProcessTree }

  const result = component.methods.filterProcessTree.call(context, state.processTree, 'c12345')
  const flatNodes = component.methods.flattenProcessTree.call({
    flattenProcessTree: component.methods.flattenProcessTree
  }, result)

  assert.equal(flatNodes.length, 14)
})

test('toggles every expandable row through the Element table instance', () => {
  const component = loadComponent()
  const state = component.data()
  const toggled = []
  const context = {
    ...state,
    displayProcessTree: state.processTree,
    flattenProcessTree: component.methods.flattenProcessTree,
    $refs: {
      processTable: {
        toggleRowExpansion: (node, expanded) => toggled.push([node.processNo, expanded])
      }
    }
  }

  component.methods.setAllExpanded.call(context, false)

  assert.deepEqual(toggled, [
    ['C12345', false],
    ['DX889012', false],
    ['DX889002', false],
    ['DX889003', false],
    ['DX889004', false]
  ])
})

test('opens the configured process link in a protected new window', () => {
  const openedWindow = { opener: 'source-window' }
  const calls = []
  const component = loadComponent((...args) => {
    calls.push(args)
    return openedWindow
  })
  const messages = []
  const context = { $message: { warning: message => messages.push(message) } }

  component.methods.handleView.call(context, {
    externalUrl: 'https://example.com/process/C12345'
  })

  assert.deepEqual(calls, [[
    'https://example.com/process/C12345',
    '_blank',
    'noopener,noreferrer'
  ]])
  assert.equal(openedWindow.opener, null)
  assert.deepEqual(messages, [])
})

test('warns instead of opening when a process link is missing', () => {
  let opened = false
  const component = loadComponent(() => {
    opened = true
  })
  const messages = []
  const context = { $message: { warning: message => messages.push(message) } }

  component.methods.handleView.call(context, { externalUrl: '' })

  assert.equal(opened, false)
  assert.deepEqual(messages, ['暂未配置工艺查看地址'])
})

test('does not force viewport height that overflows the parent scroller', () => {
  assert.doesNotMatch(readVueFile(), /min-height:\s*calc\(100vh/)
})
