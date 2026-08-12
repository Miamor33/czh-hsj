export const DOLL_META = {
  czh: {
    key: 'czh',
    name: 'czh',
    role: '黑衣位',
    src: '/models/czh.glb',
    theme: 'him',
    greet: '嗨，又见面了',
    miss: '想 czh 了',
  },
  hsj: {
    key: 'hsj',
    name: 'hsj',
    role: '蓝裙位',
    src: '/models/hsj.glb',
    theme: 'her',
    greet: '嗨，欢迎回来',
    miss: '想 hsj 了',
  },
}

export function otherPartnerKey(key) {
  return key === 'czh' ? 'hsj' : 'czh'
}
