import assert from 'node:assert/strict'
import { DOLL_META, toBaseKey, toggleVariantKey, otherPartnerKey } from './meta.js'

assert.equal(toBaseKey('czh'), 'czh')
assert.equal(toBaseKey('czhr'), 'czh')
assert.equal(toBaseKey('hsj'), 'hsj')
assert.equal(toBaseKey('hsjr'), 'hsj')
assert.equal(toggleVariantKey('czh'), 'czhr')
assert.equal(toggleVariantKey('czhr'), 'czh')
assert.equal(toggleVariantKey('hsj'), 'hsjr')
assert.equal(toggleVariantKey('hsjr'), 'hsj')
assert.equal(DOLL_META.czhr.src, '/models/czhr.glb')
assert.equal(DOLL_META.hsjr.src, '/models/hsjr.glb')
assert.equal(DOLL_META.czhr.baseKey, 'czh')
assert.equal(DOLL_META.hsjr.baseKey, 'hsj')
assert.equal(otherPartnerKey('czh'), 'hsj')
assert.equal(otherPartnerKey('hsj'), 'czh')
console.log('meta.selftest OK')
