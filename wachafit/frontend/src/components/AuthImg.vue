<template>
  <img :src="blobUrl ?? undefined" v-bind="$attrs" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import api from '@/services/api'

const props = defineProps<{ src: string }>()
defineOptions({ inheritAttrs: false })

const blobUrl = ref<string | null>(null)

async function load(url: string) {
  if (!url) return
  try {
    const res = await api.get(url, { responseType: 'blob' })
    const next = URL.createObjectURL(res.data)
    if (blobUrl.value) URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = next
  } catch {
    blobUrl.value = null
  }
}

onMounted(() => load(props.src))
watch(() => props.src, load)
onUnmounted(() => { if (blobUrl.value) URL.revokeObjectURL(blobUrl.value) })
</script>
