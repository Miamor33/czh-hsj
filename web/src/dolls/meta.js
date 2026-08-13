export const DOLL_META = {
  czh: {
    key: 'czh',
    name: 'czh',
    role: '一身黑的czh',
    src: '/models/czh.glb',
    theme: 'him',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 czh 了，我会替你告诉他的',
  },
  hsj: {
    key: 'hsj',
    name: 'hsj',
    role: '蓝色小裙hsj',
    src: '/models/hsj.glb',
    theme: 'her',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 hsj 了，我会替你告诉她的',
  },
}

export function otherPartnerKey(key) {
  return key === 'czh' ? 'hsj' : 'czh'
}
