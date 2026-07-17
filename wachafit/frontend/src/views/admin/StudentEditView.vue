<!-- frontend/src/views/admin/StudentEditView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <Button icon="pi pi-arrow-left" text label="Voltar" @click="$router.back()" class="mb-2" />
      <h1 class="page-title">Editar Cadastro do Aluno</h1>

      <div v-if="loading" class="empty-state"><i class="pi pi-spin pi-spinner" /> Carregando...</div>

      <form v-else @submit.prevent="submit" class="form-sections">

        <!-- ── Dados da Conta ── -->
        <section class="form-section">
          <h2 class="section-title">Dados da Conta</h2>
          <div class="form-grid">
            <div class="field col-span-2">
              <label class="field-label">Nome completo *</label>
              <InputText v-model="form.name" required />
            </div>
            <div class="field col-span-2">
              <label class="field-label">E-mail</label>
              <InputText :value="form.email" disabled class="disabled-field" />
            </div>
          </div>
        </section>

        <!-- ── Dados Pessoais ── -->
        <section class="form-section">
          <h2 class="section-title">Dados Pessoais</h2>
          <div class="form-grid">
            <div class="field">
              <label class="field-label">CPF</label>
              <InputText v-model="form.cpf" placeholder="000.000.000-00" />
            </div>
            <div class="field">
              <label class="field-label">RG</label>
              <InputText v-model="form.rg" />
            </div>
            <div class="field">
              <label class="field-label">Data de Nascimento</label>
              <InputText v-model="form.birthDate" type="date" />
            </div>
            <div class="field">
              <label class="field-label">Gênero</label>
              <Select v-model="form.gender" :options="genderOptions" showClear placeholder="Selecione" />
            </div>
            <div class="field">
              <label class="field-label">Estado Civil</label>
              <Select v-model="form.maritalStatus" :options="maritalOptions" showClear placeholder="Selecione" />
            </div>
            <div class="field">
              <label class="field-label">Profissão</label>
              <InputText v-model="form.profession" />
            </div>
            <div class="field">
              <label class="field-label">Telefone</label>
              <InputText v-model="form.phone" placeholder="(00) 00000-0000" />
            </div>
          </div>
        </section>

        <!-- ── Endereço ── -->
        <section class="form-section">
          <h2 class="section-title">Endereço</h2>
          <div class="form-grid">
            <div class="field">
              <label class="field-label">CEP</label>
              <InputText v-model="form.addressZip" placeholder="00000-000" />
            </div>
            <div class="field col-span-2">
              <label class="field-label">Logradouro</label>
              <InputText v-model="form.addressLine" />
            </div>
            <div class="field">
              <label class="field-label">Número</label>
              <InputText v-model="form.addressNumber" />
            </div>
            <div class="field">
              <label class="field-label">Complemento</label>
              <InputText v-model="form.addressComplement" />
            </div>
            <div class="field">
              <label class="field-label">Bairro</label>
              <InputText v-model="form.addressNeighborhood" />
            </div>
            <div class="field">
              <label class="field-label">Cidade</label>
              <InputText v-model="form.addressCity" />
            </div>
            <div class="field">
              <label class="field-label">Estado</label>
              <InputText v-model="form.addressState" maxlength="2" placeholder="SP" />
            </div>
          </div>
        </section>

        <!-- ── Contato de Emergência ── -->
        <section class="form-section">
          <h2 class="section-title">Contato de Emergência</h2>
          <div class="form-grid">
            <div class="field col-span-2">
              <label class="field-label">Nome</label>
              <InputText v-model="form.emergencyContactName" />
            </div>
            <div class="field">
              <label class="field-label">Telefone</label>
              <InputText v-model="form.emergencyContactPhone" placeholder="(00) 00000-0000" />
            </div>
            <div class="field">
              <label class="field-label">Parentesco / Relação</label>
              <InputText v-model="form.emergencyContactRelationship" placeholder="Ex: mãe, cônjuge" />
            </div>
          </div>
        </section>

        <!-- ── Saúde ── -->
        <section class="form-section">
          <h2 class="section-title">Saúde</h2>
          <div class="form-grid">
            <div class="field">
              <label class="field-label">Nível de atividade física</label>
              <Select v-model="form.activityLevel" :options="activityOptions" showClear placeholder="Selecione" />
            </div>
            <div class="field">
              <label class="field-label">Objetivo principal</label>
              <InputText v-model="form.fitnessGoal" placeholder="Ex: Perda de peso, hipertrofia" />
            </div>
            <div class="field">
              <label class="field-label">Nível de condicionamento</label>
              <InputText v-model="form.fitnessLevel" placeholder="Ex: Iniciante, intermediário" />
            </div>
            <div class="field">
              <label class="field-label">Horas de sono / noite</label>
              <InputNumber v-model="form.sleepHours" :min="0" :max="24" />
            </div>
            <div class="field">
              <label class="field-label">Nível de estresse (1-10)</label>
              <InputNumber v-model="form.stressLevel" :min="1" :max="10" />
            </div>
          </div>

          <div class="conditions-grid">
            <label class="checkbox-item">
              <Checkbox v-model="form.hasHeartCondition" :binary="true" />
              <span>Problema cardíaco</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hasDiabetes" :binary="true" />
              <span>Diabetes</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hasHypertension" :binary="true" />
              <span>Hipertensão</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hasRespiratoryCondition" :binary="true" />
              <span>Condição respiratória</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hasOrthopedicCondition" :binary="true" />
              <span>Condição ortopédica</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hasChronicPain" :binary="true" />
              <span>Dor crônica</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.hadSurgery" :binary="true" />
              <span>Cirurgia prévia</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.smokes" :binary="true" />
              <span>Fumante</span>
            </label>
            <label class="checkbox-item">
              <Checkbox v-model="form.drinksAlcohol" :binary="true" />
              <span>Consome álcool</span>
            </label>
          </div>

          <div class="form-grid mt-3">
            <div class="field col-span-2" v-if="form.hadSurgery">
              <label class="field-label">Descrição da cirurgia</label>
              <InputText v-model="form.surgeryDescription" />
            </div>
            <div class="field col-span-2" v-if="form.hasChronicPain">
              <label class="field-label">Local da dor crônica</label>
              <InputText v-model="form.chronicPainLocation" />
            </div>
            <div class="field col-span-2" v-if="form.drinksAlcohol">
              <label class="field-label">Frequência de consumo de álcool</label>
              <InputText v-model="form.alcoholFrequency" placeholder="Ex: Fins de semana, ocasionalmente" />
            </div>
            <div class="field col-span-2">
              <label class="field-label">Medicamentos em uso</label>
              <Textarea v-model="form.medications" rows="2" placeholder="Liste os medicamentos" />
            </div>
            <div class="field col-span-2">
              <label class="field-label">Restrições físicas</label>
              <Textarea v-model="form.physicalRestrictions" rows="2" placeholder="Descreva restrições para exercícios" />
            </div>
            <div class="field col-span-2">
              <label class="field-label">Histórico de exercícios</label>
              <Textarea v-model="form.exerciseHistory" rows="2" placeholder="Atividades físicas praticadas anteriormente" />
            </div>
            <div class="field col-span-2">
              <label class="field-label">Observações gerais</label>
              <Textarea v-model="form.notes" rows="2" />
            </div>
          </div>
        </section>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

        <div class="form-actions">
          <Button type="button" label="Cancelar" outlined @click="$router.back()" />
          <Button type="submit" label="Salvar alterações" :loading="saving" />
        </div>
      </form>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import profileService from '@/services/profile.service'
import { userService } from '@/services/user.service'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'

const route = useRoute()
const router = useRouter()
const studentId = route.params.id as string
const adminStore = useAdminStore()

const loading = ref(true)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const genderOptions = ['Masculino', 'Feminino', 'Outro', 'Prefiro não informar']
const maritalOptions = ['Solteiro(a)', 'Casado(a)', 'Divorciado(a)', 'Viúvo(a)', 'União estável']
const activityOptions = ['Sedentário', 'Levemente ativo', 'Moderadamente ativo', 'Muito ativo', 'Extremamente ativo']

const form = ref({
  // conta
  name: '',
  email: '',
  // pessoal
  cpf: '',
  rg: '',
  birthDate: '',
  gender: '',
  maritalStatus: '',
  profession: '',
  phone: '',
  // endereço
  addressZip: '',
  addressLine: '',
  addressNumber: '',
  addressComplement: '',
  addressNeighborhood: '',
  addressCity: '',
  addressState: '',
  // emergência
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',
  // saúde — condições booleanas
  hasHeartCondition: false,
  hasDiabetes: false,
  hasHypertension: false,
  hasRespiratoryCondition: false,
  hasOrthopedicCondition: false,
  hadSurgery: false,
  surgeryDescription: '',
  hasChronicPain: false,
  chronicPainLocation: '',
  medications: '',
  physicalRestrictions: '',
  smokes: false,
  drinksAlcohol: false,
  alcoholFrequency: '',
  sleepHours: null as number | null,
  stressLevel: null as number | null,
  activityLevel: '',
  fitnessGoal: '',
  fitnessLevel: '',
  exerciseHistory: '',
  notes: '',
})

onMounted(async () => {
  try {
    await adminStore.fetchUsers()
    const user = adminStore.users.find(u => u.id === studentId)
    if (user) {
      form.value.name = user.name
      form.value.email = user.email
    }

    const [profile, health] = await Promise.all([
      profileService.getStudentProfile(studentId),
      profileService.getStudentHealth(studentId),
    ])

    if (profile) {
      form.value.cpf = profile.cpf ?? ''
      form.value.rg = profile.rg ?? ''
      form.value.birthDate = profile.birthDate ?? ''
      form.value.gender = profile.gender ?? ''
      form.value.maritalStatus = profile.maritalStatus ?? ''
      form.value.profession = profile.profession ?? ''
      form.value.phone = profile.phone ?? ''
      form.value.addressZip = profile.addressZip ?? ''
      form.value.addressLine = profile.addressLine ?? ''
      form.value.addressNumber = profile.addressNumber ?? ''
      form.value.addressComplement = profile.addressComplement ?? ''
      form.value.addressNeighborhood = profile.addressNeighborhood ?? ''
      form.value.addressCity = profile.addressCity ?? ''
      form.value.addressState = profile.addressState ?? ''
      form.value.emergencyContactName = profile.emergencyContactName ?? ''
      form.value.emergencyContactPhone = profile.emergencyContactPhone ?? ''
      form.value.emergencyContactRelationship = profile.emergencyContactRelationship ?? ''
    }

    if (health) {
      form.value.hasHeartCondition = health.hasHeartCondition
      form.value.hasDiabetes = health.hasDiabetes
      form.value.hasHypertension = health.hasHypertension
      form.value.hasRespiratoryCondition = health.hasRespiratoryCondition
      form.value.hasOrthopedicCondition = health.hasOrthopedicCondition
      form.value.hadSurgery = health.hadSurgery
      form.value.surgeryDescription = health.surgeryDescription ?? ''
      form.value.hasChronicPain = health.hasChronicPain
      form.value.chronicPainLocation = health.chronicPainLocation ?? ''
      form.value.medications = health.medications ?? ''
      form.value.physicalRestrictions = health.physicalRestrictions ?? ''
      form.value.smokes = health.smokes
      form.value.drinksAlcohol = health.drinksAlcohol
      form.value.alcoholFrequency = health.alcoholFrequency ?? ''
      form.value.sleepHours = health.sleepHours
      form.value.stressLevel = health.stressLevel
      form.value.activityLevel = health.activityLevel ?? ''
      form.value.fitnessGoal = health.fitnessGoal ?? ''
      form.value.fitnessLevel = health.fitnessLevel ?? ''
      form.value.exerciseHistory = health.exerciseHistory ?? ''
      form.value.notes = health.notes ?? ''
    }
  } finally {
    loading.value = false
  }
})

async function submit() {
  errorMsg.value = ''
  saving.value = true
  try {
    await userService.update(studentId, { name: form.value.name, role: 'STUDENT' })

    await profileService.updateStudentProfile(studentId, {
      cpf: form.value.cpf || undefined,
      rg: form.value.rg || undefined,
      birthDate: form.value.birthDate || undefined,
      gender: form.value.gender || undefined,
      maritalStatus: form.value.maritalStatus || undefined,
      profession: form.value.profession || undefined,
      phone: form.value.phone || undefined,
      addressZip: form.value.addressZip || undefined,
      addressLine: form.value.addressLine || undefined,
      addressNumber: form.value.addressNumber || undefined,
      addressComplement: form.value.addressComplement || undefined,
      addressNeighborhood: form.value.addressNeighborhood || undefined,
      addressCity: form.value.addressCity || undefined,
      addressState: form.value.addressState || undefined,
      emergencyContactName: form.value.emergencyContactName || undefined,
      emergencyContactPhone: form.value.emergencyContactPhone || undefined,
      emergencyContactRelationship: form.value.emergencyContactRelationship || undefined,
    })

    await profileService.upsertStudentHealth(studentId, {
      hasHeartCondition: form.value.hasHeartCondition,
      hasDiabetes: form.value.hasDiabetes,
      hasHypertension: form.value.hasHypertension,
      hasRespiratoryCondition: form.value.hasRespiratoryCondition,
      hasOrthopedicCondition: form.value.hasOrthopedicCondition,
      hadSurgery: form.value.hadSurgery,
      surgeryDescription: form.value.surgeryDescription || undefined,
      hasChronicPain: form.value.hasChronicPain,
      chronicPainLocation: form.value.chronicPainLocation || undefined,
      medications: form.value.medications || undefined,
      physicalRestrictions: form.value.physicalRestrictions || undefined,
      smokes: form.value.smokes,
      drinksAlcohol: form.value.drinksAlcohol,
      alcoholFrequency: form.value.alcoholFrequency || undefined,
      sleepHours: form.value.sleepHours ?? undefined,
      stressLevel: form.value.stressLevel ?? undefined,
      activityLevel: form.value.activityLevel || undefined,
      fitnessGoal: form.value.fitnessGoal || undefined,
      fitnessLevel: form.value.fitnessLevel || undefined,
      exerciseHistory: form.value.exerciseHistory || undefined,
      notes: form.value.notes || undefined,
    })

    successMsg.value = 'Cadastro atualizado com sucesso!'
    setTimeout(() => router.push('/trainer/students'), 1500)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message ?? 'Erro ao salvar. Tente novamente.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 800px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.empty-state { text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px; }

.form-sections { display: flex; flex-direction: column; gap: 20px; }

.form-section {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px 24px;
  display: flex; flex-direction: column; gap: 16px;
}

.section-title {
  font-size: 15px; font-weight: 700; color: var(--neutral-800);
  border-bottom: 1px solid var(--neutral-100); padding-bottom: 10px; margin: 0;
}

.form-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px;
}
.field { display: flex; flex-direction: column; gap: 4px; }
.col-span-2 { grid-column: span 2; }

.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }
.disabled-field { background: var(--neutral-50); color: var(--neutral-400); }

.conditions-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px;
}
.checkbox-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; color: var(--neutral-700); cursor: pointer;
}

.mt-3 { margin-top: 4px; }

.error-msg { color: #ef4444; font-size: 0.875rem; }
.success-msg { color: #22c55e; font-size: 0.875rem; }

.form-actions { display: flex; justify-content: flex-end; gap: 12px; }

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
  .col-span-2 { grid-column: span 1; }
}
</style>
