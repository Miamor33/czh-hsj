<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'

const loading = ref(true)
const error = ref('')
const photos = ref([])
const uploading = ref(false)
const caption = ref('')

async function loadPhotos() {
  loading.value = true
  error.value = ''
  try {
    photos.value = await http.get('/photos')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function handleUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  error.value = ''
  try {
    const form = new FormData()
    form.append('file', file)
    if (caption.value.trim()) {
      form.append('caption', caption.value.trim())
    }
    await http.post('/photos', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    caption.value = ''
    event.target.value = ''
    await loadPhotos()
  } catch (e) {
    error.value = e.message
  } finally {
    uploading.value = false
  }
}

async function toggleFeatured(photo) {
  try {
    await http.patch(`/photos/${photo.id}/featured`, {
      featured: !photo.featured,
    })
    await loadPhotos()
  } catch (e) {
    error.value = e.message
  }
}

async function deletePhoto(photo) {
  if (!confirm('确定删除这张照片？')) return
  try {
    await http.delete(`/photos/${photo.id}`)
    await loadPhotos()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(loadPhotos)
</script>

<template>
  <div class="album-page">
    <section class="card upload-card">
      <p class="section-title">上传照片</p>
      <div class="field">
        <label>说明（可选）</label>
        <input v-model="caption" placeholder="写一句描述…" />
      </div>
      <label class="btn btn--primary upload-btn">
        {{ uploading ? '上传中…' : '选择图片' }}
        <input
          type="file"
          accept="image/*"
          hidden
          :disabled="uploading"
          @change="handleUpload"
        />
      </label>
    </section>

    <p v-if="error" class="error-banner">{{ error }}</p>

    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="photos.length" class="photo-grid">
      <div v-for="photo in photos" :key="photo.id" class="photo-item">
        <img :src="photo.url" :alt="photo.caption || '照片'" loading="lazy" />
        <span v-if="photo.featured" class="photo-item__badge">精选</span>
        <div class="photo-actions">
          <button
            class="photo-action"
            :title="photo.featured ? '取消精选' : '设为精选'"
            @click="toggleFeatured(photo)"
          >
            {{ photo.featured ? '★' : '☆' }}
          </button>
          <button class="photo-action photo-action--del" @click="deletePhoto(photo)">×</button>
        </div>
        <p v-if="photo.caption" class="photo-caption">{{ photo.caption }}</p>
      </div>
    </div>
    <p v-else class="empty">还没有照片，上传第一张吧</p>
  </div>
</template>

<style scoped>
.upload-card {
  margin-bottom: 16px;
}

.upload-btn {
  width: 100%;
  cursor: pointer;
}

.photo-item {
  position: relative;
}

.photo-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 6px;
  background: linear-gradient(transparent, rgba(20, 20, 24, 0.58));
}

.photo-action {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: var(--coral-deep);
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-action--del {
  color: var(--ink-muted);
  font-size: 1.25rem;
}

.photo-caption {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 6px 8px;
  font-size: 0.7rem;
  color: #fff;
  background: linear-gradient(rgba(42, 37, 32, 0.5), transparent);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
