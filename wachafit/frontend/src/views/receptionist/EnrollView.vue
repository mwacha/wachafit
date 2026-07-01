<template>
  <AppLayout>
    <div class="p-6 max-w-lg">
      <h1 class="text-2xl font-bold mb-6">Nova Matrícula</h1>

      <div class="card p-6">
        <form @submit.prevent="submit" class="flex flex-col gap-4">
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Email do Aluno</label>
            <InputText v-model="form.studentEmail" placeholder="aluno@email.com" class="w-full" />
          </div>
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Plano</label>
            <Select v-model="form.planId" :options="plans" optionLabel="name" optionValue="id"
                    placeholder="Selecione um plano" class="w-full" />
          </div>
          <div>
            <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Data de Início</label>
            <InputText v-model="form.startedAt" type="date" class="w-full" />
          </div>
          <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
          <Message v-if="successMsg" severity="success" :closable="false">{{ successMsg }}</Message>
          <Button type="submit" label="Matricular" :loading="loading" class="w-full" />
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
import Select from 'primevue/select'
import Message from 'primevue/message'
import membershipService from '@/services/membership.service'
import { userService } from '@/services/user.service'
import type { MembershipPlan } from '@/types/api'

const plans = ref<MembershipPlan[]>([])
const form = ref({ studentEmail: '', planId: '', startedAt: new Date().toISOString().slice(0, 10) })
const loading = ref(false)
const errorMsg = ref(''); const successMsg = ref('')

onMounted(async () => {
  const all = await membershipService.listPlans()
  plans.value = all.filter(p => p.active)
})

async function submit() {
  errorMsg.value = ''; successMsg.value = ''
  if (!form.value.studentEmail || !form.value.planId) { errorMsg.value = 'Preencha todos os campos.'; return }
  loading.value = true
  try {
    const users = await userService.list({ role: 'STUDENT' })
    const student = users.find((u) => u.email === form.value.studentEmail)
    if (!student) { errorMsg.value = 'Aluno não encontrado.'; return }
    await membershipService.createSubscription(student.id, { planId: form.value.planId, startedAt: form.value.startedAt })
    successMsg.value = `Aluno ${student.name} matriculado!`
    form.value = { studentEmail: '', planId: '', startedAt: new Date().toISOString().slice(0, 10) }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.error ?? 'Erro ao matricular.'
  } finally { loading.value = false }
}
</script>
