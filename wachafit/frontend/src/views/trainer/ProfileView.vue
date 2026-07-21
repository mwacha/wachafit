<template>
  <AppLayout>
    <div class="p-6 max-w-lg">
      <h1 class="text-2xl font-bold mb-6">Meu Perfil Profissional</h1>

      <div v-if="loading" class="text-center py-8">Carregando...</div>

      <div v-else class="card p-6">
        <form @submit.prevent="save" class="flex flex-col gap-4">
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">CREF</label>
            <InputText v-model="form.cref" placeholder="000000-G/XX" class="w-full" />
          </div>
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Especialidades</label>
            <InputText v-model="form.specialties" placeholder="Musculação, Funcional..." class="w-full" />
          </div>
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Bio</label>
            <Textarea v-model="form.bio" rows="3" class="w-full" />
          </div>
          <Message v-if="saved" severity="success" :closable="false">Perfil atualizado com sucesso!</Message>
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { useAuthStore } from '@/stores/auth.store'
import profileService from '@/services/profile.service'

const auth = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const saved = ref(false)
const form = ref({ cref: '', specialties: '', bio: '' })

onMounted(async () => {
  if (auth.userId) {
    const profile = await profileService.getTrainerProfile(auth.userId)
    if (profile) {
      form.value.cref = profile.cref ?? ''
      form.value.specialties = profile.specialties ?? ''
      form.value.bio = profile.bio ?? ''
    }
  }
  loading.value = false
})

async function save() {
  if (!auth.userId) return
  saving.value = true; saved.value = false
  try {
    await profileService.updateTrainerProfile(auth.userId, form.value)
    saved.value = true
  } finally { saving.value = false }
}
</script>
