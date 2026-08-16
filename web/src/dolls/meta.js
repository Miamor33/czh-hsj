export const DOLL_META = {
  czh: {
    key: 'czh',
    baseKey: 'czh',
    name: 'czh',
    role: '一身黑的czh',
    src: '/models/czh.glb',
    theme: 'him',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 czh 了，我会替你告诉他的',
  },
  hsj: {
    key: 'hsj',
    baseKey: 'hsj',
    name: 'hsj',
    role: '蓝色小裙hsj',
    src: '/models/hsj.glb',
    theme: 'her',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 hsj 了，我会替你告诉她的',
  },
  czhr: {
    key: 'czhr',
    baseKey: 'czh',
    name: 'czh',
    role: '一身黑的czh',
    src: '/models/czhr.glb',
    theme: 'him',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 czh 了，我会替你告诉他的',
  },
  hsjr: {
    key: 'hsjr',
    baseKey: 'hsj',
    name: 'hsj',
    role: '蓝色小裙hsj',
    src: '/models/hsjr.glb',
    theme: 'her',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 hsj 了，我会替你告诉她的',
  },
}

/** 变体 key → 基础身份（登录 / 气泡用） */
export function toBaseKey(key) {
  return DOLL_META[key]?.baseKey || key
}

/** 基础 ↔ r 后缀来回切换 */
export function toggleVariantKey(key) {
  const base = toBaseKey(key)
  if (base === 'czh') return key === 'czhr' ? 'czh' : 'czhr'
  if (base === 'hsj') return key === 'hsjr' ? 'hsj' : 'hsjr'
  return key
}

export function otherPartnerKey(key) {
  return toBaseKey(key) === 'czh' ? 'hsj' : 'czh'
}
