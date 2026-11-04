import Vue from 'vue'
import { getDicts, listSimpleDictDatas } from '@/api/system/dict/data'

const loadingPromises = {}

function mapDictDataList(dictDataList) {
  return (dictDataList || []).map(dictData => ({
    value: dictData.value,
    label: dictData.label,
    colorType: dictData.colorType,
    cssClass: dictData.cssClass
  }))
}

const state = {
  /**
   * 数据字典 MAP
   * key：数据字典大类枚举值 dictType
   * dictValue：数据字典小类数值 {dictValue: '', dictLabel: ''} 的数组
   */
  dictDatas: {}
}

/** 该类型已有响应式占位数组时原地填充（保引用），否则 Vue.set 新建 */
function fillDictTypeInPlace(state, dictType, list) {
  const target = state.dictDatas[dictType]
  if (Array.isArray(target)) {
    target.splice(0, target.length, ...(list || []))
  } else {
    Vue.set(state.dictDatas, dictType, list || [])
  }
}

const mutations = {
  SET_DICT_DATAS: (state, dictDatas) => {
    // 按 key 合并并原地填充，不整体替换 map：组件 data() 持有的是各类型数组的引用，
    // 整体替换会让引用失效，已渲染的表单选项（如部门状态）会突然变回空白
    const next = dictDatas || {}
    Object.keys(next).forEach(dictType => {
      fillDictTypeInPlace(state, dictType, next[dictType])
    })
  },
  MERGE_DICT_TYPE: (state, { dictType, list }) => {
    fillDictTypeInPlace(state, dictType, list)
  }
}

const actions = {
  loadDictType({ state, commit }, dictType) {
    if (!dictType) {
      return Promise.resolve([])
    }
    if (state.dictDatas[dictType] && state.dictDatas[dictType].length) {
      return Promise.resolve(state.dictDatas[dictType])
    }
    if (loadingPromises[dictType]) {
      return loadingPromises[dictType]
    }
    loadingPromises[dictType] = getDicts(dictType).then(response => {
      const list = mapDictDataList(response && response.data)
      commit('MERGE_DICT_TYPE', { dictType, list })
      delete loadingPromises[dictType]
      return list
    }).catch(() => {
      delete loadingPromises[dictType]
      return []
    })
    return loadingPromises[dictType]
  },
  loadDictTypes({ dispatch }, dictTypes) {
    const uniqueTypes = Array.from(new Set((dictTypes || []).filter(Boolean)))
    if (!uniqueTypes.length) {
      return Promise.resolve([])
    }
    return Promise.all(uniqueTypes.map(dictType => dispatch('loadDictType', dictType)))
  },
  loadDictDatas({ commit }) {
    return listSimpleDictDatas().then(response => {
      if (!response || !response.data) {
        return
      }
      const dictDataMap = {}
      response.data.forEach(dictData => {
        const enumValueObj = dictDataMap[dictData.dictType]
        if (!enumValueObj) {
          dictDataMap[dictData.dictType] = []
        }
        dictDataMap[dictData.dictType].push({
          value: dictData.value,
          label: dictData.label,
          colorType: dictData.colorType,
          cssClass: dictData.cssClass
        })
      })
      commit('SET_DICT_DATAS', dictDataMap)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
