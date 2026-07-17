<!-- frontend/src/views/admin/StudentEditView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <!-- Header -->
      <div class="page-header">
        <h1 class="page-title">Editar Cadastro do Aluno</h1>
        <div class="steps-indicator">
          <div v-for="(s, i) in steps" :key="i"
            :class="['step-dot', { active: currentStep === i, done: currentStep > i }]"
            @click="currentStep > i && (currentStep = i)">
            <span class="step-num">{{ i + 1 }}</span>
            <span class="step-label">{{ s }}</span>
          </div>
        </div>
      </div>

      <div v-if="loading" class="wizard-card">
        <div class="empty-state"><i class="pi pi-spin pi-spinner" /> Carregando dados do aluno...</div>
      </div>

      <div v-else class="wizard-card">

        <!-- PASSO 1: Dados da Conta -->
        <div v-if="currentStep === 0" class="step-content">
          <h2 class="step-title"><i class="pi pi-user" /> Dados da Conta</h2>
          <div class="field-grid">
            <div class="field col-full">
              <label>Nome completo *</label>
              <InputText v-model="account.name" placeholder="Nome completo" class="w-full" />
            </div>
            <div class="field col-full">
              <label>E-mail</label>
              <InputText :value="account.email" disabled class="w-full disabled-field" />
            </div>
          </div>
        </div>

        <!-- PASSO 2: Dados Pessoais -->
        <div v-if="currentStep === 1" class="step-content">
          <h2 class="step-title"><i class="pi pi-id-card" /> Dados Pessoais</h2>
          <div class="field-grid">
            <div class="field">
              <label>CPF *</label>
              <InputMask v-model="personal.cpf" mask="999.999.999-99" placeholder="000.000.000-00" class="w-full" />
            </div>
            <div class="field">
              <label>RG</label>
              <InputText v-model="personal.rg" placeholder="RG" class="w-full" />
            </div>
            <div class="field">
              <label>Data de Nascimento</label>
              <InputText v-model="personal.birthDate" type="date" class="w-full" />
            </div>
            <div class="field">
              <label>Gênero</label>
              <Select v-model="personal.gender" :options="genderOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field">
              <label>Estado Civil</label>
              <Select v-model="personal.maritalStatus" :options="maritalOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field">
              <label>Profissão</label>
              <InputText v-model="personal.profession" placeholder="Profissão" class="w-full" />
            </div>
            <div class="field">
              <label>Telefone / WhatsApp</label>
              <InputMask v-model="personal.phone" mask="(99) 99999-9999" placeholder="(11) 99999-9999" class="w-full" />
            </div>
          </div>
        </div>

        <!-- PASSO 3: Endereço -->
        <div v-if="currentStep === 2" class="step-content">
          <h2 class="step-title"><i class="pi pi-map-marker" /> Endereço</h2>
          <div class="field-grid">
            <div class="field">
              <label>CEP</label>
              <InputMask v-model="address.zip" mask="99999-999" placeholder="00000-000" class="w-full" @blur="fetchCep" />
            </div>
            <div class="field col-span-2">
              <label>Logradouro (Rua / Av.)</label>
              <InputText v-model="address.line" placeholder="Rua das Flores" class="w-full" />
            </div>
            <div class="field">
              <label>Número</label>
              <InputText v-model="address.number" placeholder="123" class="w-full" />
            </div>
            <div class="field">
              <label>Complemento</label>
              <InputText v-model="address.complement" placeholder="Apto 42" class="w-full" />
            </div>
            <div class="field">
              <label>Bairro</label>
              <InputText v-model="address.neighborhood" placeholder="Bairro" class="w-full" />
            </div>
            <div class="field">
              <label>Cidade</label>
              <InputText v-model="address.city" placeholder="Cidade" class="w-full" />
            </div>
            <div class="field">
              <label>Estado (UF)</label>
              <Select v-model="address.state" :options="stateOptions" placeholder="UF" class="w-full" />
            </div>
          </div>
        </div>

        <!-- PASSO 4: Contato de Emergência -->
        <div v-if="currentStep === 3" class="step-content">
          <h2 class="step-title"><i class="pi pi-phone" /> Contato de Emergência</h2>
          <div class="field-grid">
            <div class="field col-span-2">
              <label>Nome</label>
              <InputText v-model="emergency.name" placeholder="Nome do contato" class="w-full" />
            </div>
            <div class="field">
              <label>Parentesco</label>
              <Select v-model="emergency.relationship" :options="relationshipOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field">
              <label>Telefone</label>
              <InputMask v-model="emergency.phone" mask="(99) 99999-9999" placeholder="(11) 99999-9999" class="w-full" />
            </div>
          </div>
        </div>

        <!-- PASSO 5: Histórico de Saúde -->
        <div v-if="currentStep === 4" class="step-content">
          <h2 class="step-title"><i class="pi pi-heart" /> Histórico de Saúde</h2>

          <p class="section-label">Condições médicas pré-existentes</p>
          <div class="check-grid">
            <label class="check-item"><Checkbox v-model="health.hasHeartCondition" :binary="true" />Problema cardíaco</label>
            <label class="check-item"><Checkbox v-model="health.hasHypertension" :binary="true" />Hipertensão</label>
            <label class="check-item"><Checkbox v-model="health.hasDiabetes" :binary="true" />Diabetes</label>
            <label class="check-item"><Checkbox v-model="health.hasRespiratoryCondition" :binary="true" />Problema respiratório</label>
            <label class="check-item"><Checkbox v-model="health.hasOrthopedicCondition" :binary="true" />Problema ortopédico</label>
            <label class="check-item"><Checkbox v-model="health.hadSurgery" :binary="true" />Cirurgia prévia</label>
            <label class="check-item"><Checkbox v-model="health.hasChronicPain" :binary="true" />Dor crônica</label>
          </div>

          <div class="field-grid mt-3">
            <div v-if="health.hadSurgery" class="field col-span-2">
              <label>Descrição da cirurgia</label>
              <InputText v-model="health.surgeryDescription" placeholder="Tipo e data da cirurgia" class="w-full" />
            </div>
            <div v-if="health.hasChronicPain" class="field col-span-2">
              <label>Local da dor crônica</label>
              <InputText v-model="health.chronicPainLocation" placeholder="Ex: lombar, joelho direito" class="w-full" />
            </div>
            <div class="field col-span-2">
              <label>Medicamentos em uso</label>
              <Textarea v-model="health.medications" placeholder="Liste os medicamentos" rows="2" class="w-full" />
            </div>
            <div class="field col-span-2">
              <label>Restrições físicas / lesões</label>
              <Textarea v-model="health.physicalRestrictions" placeholder="Ex: hérnia de disco L4-L5, lesão no ombro..." rows="2" class="w-full" />
            </div>
          </div>

          <p class="section-label mt-4">Hábitos de vida</p>
          <div class="field-grid">
            <div class="field">
              <label>Fuma?</label>
              <Select v-model="health.smokes" :options="[{label:'Não',value:false},{label:'Sim',value:true}]"
                optionLabel="label" optionValue="value" class="w-full" />
            </div>
            <div class="field">
              <label>Consome álcool?</label>
              <Select v-model="health.drinksAlcohol" :options="[{label:'Não',value:false},{label:'Sim',value:true}]"
                optionLabel="label" optionValue="value" class="w-full" />
            </div>
            <div v-if="health.drinksAlcohol" class="field">
              <label>Frequência de consumo</label>
              <Select v-model="health.alcoholFrequency" :options="alcoholOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field">
              <label>Horas de sono por noite</label>
              <InputNumber v-model="health.sleepHours" :min="1" :max="14" placeholder="Ex: 7" class="w-full" />
            </div>
            <div class="field">
              <label>Nível de estresse (1–5)</label>
              <Select v-model="health.stressLevel" :options="[1,2,3,4,5]" placeholder="1 = baixo, 5 = alto" class="w-full" />
            </div>
            <div class="field">
              <label>Nível de atividade física atual</label>
              <Select v-model="health.activityLevel" :options="activityOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
          </div>

          <p class="section-label mt-4">Objetivos & Histórico de treino</p>
          <div class="field-grid">
            <div class="field">
              <label>Objetivo principal</label>
              <Select v-model="health.fitnessGoal" :options="goalOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field">
              <label>Nível de condicionamento</label>
              <Select v-model="health.fitnessLevel" :options="levelOptions" optionLabel="label" optionValue="value"
                placeholder="Selecione" class="w-full" />
            </div>
            <div class="field col-span-2">
              <label>Histórico de atividade física</label>
              <Textarea v-model="health.exerciseHistory" placeholder="Descreva experiências anteriores com exercícios..." rows="2" class="w-full" />
            </div>
          </div>
        </div>

        <!-- PASSO 6: PAR-Q -->
        <div v-if="currentStep === 5" class="step-content">
          <h2 class="step-title"><i class="pi pi-clipboard" /> PAR-Q — Questionário de Prontidão Física</h2>
          <p class="parq-intro">Responda SIM ou NÃO para cada pergunta. Em caso de dúvida, responda SIM.</p>

          <div class="parq-list">
            <div v-for="q in parqQuestions" :key="q.key" class="parq-item">
              <p class="parq-question">{{ q.text }}</p>
              <div class="parq-opts">
                <label class="parq-opt">
                  <RadioButton v-model="health[q.key as keyof typeof health]" :value="false" /> Não
                </label>
                <label class="parq-opt parq-yes">
                  <RadioButton v-model="health[q.key as keyof typeof health]" :value="true" /> Sim
                </label>
              </div>
            </div>
            <div v-if="health.parqOtherReason" class="field mt-2">
              <label>Descreva o motivo</label>
              <InputText v-model="health.parqOtherReasonDetail" placeholder="Descreva o motivo" class="w-full" />
            </div>
          </div>

          <div class="field mt-3">
            <label>Observações gerais</label>
            <Textarea v-model="health.notes" rows="3" placeholder="Informações adicionais relevantes" class="w-full" />
          </div>
        </div>

        <!-- Feedback -->
        <Message v-if="errorMsg" severity="error" :closable="false" class="mt-3">{{ errorMsg }}</Message>
        <Message v-if="successMsg" severity="success" :closable="false" class="mt-3">{{ successMsg }}</Message>

        <!-- Navegação -->
        <div class="step-nav">
          <Button v-if="currentStep > 0" label="Anterior" icon="pi pi-arrow-left" text @click="currentStep--" />
          <div class="spacer" />
          <Button v-if="currentStep < steps.length - 1"
            label="Próximo" icon="pi pi-arrow-right" iconPos="right"
            @click="nextStep" />
          <Button v-else
            label="Salvar Alterações" icon="pi pi-check" iconPos="right"
            :loading="saving" @click="submit" />
        </div>
      </div>
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
import InputMask from 'primevue/inputmask'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import RadioButton from 'primevue/radiobutton'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'

const route = useRoute()
const router = useRouter()
const studentId = route.params.id as string
const adminStore = useAdminStore()

const steps = ['Dados da Conta', 'Dados Pessoais', 'Endereço', 'Emergência', 'Saúde', 'PAR-Q']
const currentStep = ref(0)
const loading = ref(true)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

// ---- Form state ----
const account = ref({ name: '', email: '' })

const personal = ref({ cpf: '', rg: '', birthDate: '', gender: '', maritalStatus: '', profession: '', phone: '' })

const address = ref({ zip: '', line: '', number: '', complement: '', neighborhood: '', city: '', state: '' })

const emergency = ref({ name: '', phone: '', relationship: '' })

const health = ref({
  hasHeartCondition: false, hasDiabetes: false, hasHypertension: false,
  hasRespiratoryCondition: false, hasOrthopedicCondition: false,
  hadSurgery: false, surgeryDescription: '',
  hasChronicPain: false, chronicPainLocation: '',
  medications: '', physicalRestrictions: '',
  smokes: false, drinksAlcohol: false, alcoholFrequency: '',
  sleepHours: null as number | null, stressLevel: null as number | null,
  activityLevel: '', fitnessGoal: '', fitnessLevel: '', exerciseHistory: '',
  parqHeartProblem: false, parqChestPainExercise: false, parqChestPainRest: false,
  parqDizziness: false, parqBoneJoint: false, parqBloodPressureMeds: false,
  parqOtherReason: false, parqOtherReasonDetail: '',
  notes: '',
})

// ---- Options (idênticas ao cadastro) ----
const genderOptions = [
  { label: 'Masculino', value: 'M' }, { label: 'Feminino', value: 'F' }, { label: 'Outro', value: 'OUTRO' }
]
const maritalOptions = [
  { label: 'Solteiro(a)', value: 'SOLTEIRO' }, { label: 'Casado(a)', value: 'CASADO' },
  { label: 'Divorciado(a)', value: 'DIVORCIADO' }, { label: 'Viúvo(a)', value: 'VIUVO' },
  { label: 'União estável', value: 'UNIAO_ESTAVEL' },
]
const relationshipOptions = [
  { label: 'Cônjuge / Parceiro(a)', value: 'CONJUGE' }, { label: 'Pai / Mãe', value: 'PAI_MAE' },
  { label: 'Filho(a)', value: 'FILHO' }, { label: 'Irmão / Irmã', value: 'IRMAO' },
  { label: 'Amigo(a)', value: 'AMIGO' }, { label: 'Outro', value: 'OUTRO' },
]
const alcoholOptions = [
  { label: 'Raramente', value: 'RARAMENTE' }, { label: 'Até 2x por semana', value: 'ATE_2X_SEMANA' },
  { label: '3x ou mais por semana', value: '3X_OU_MAIS' }, { label: 'Diariamente', value: 'DIARIAMENTE' },
]
const activityOptions = [
  { label: 'Sedentário (sem exercício)', value: 'SEDENTARIO' },
  { label: 'Levemente ativo (1–2x/semana)', value: 'LEVEMENTE_ATIVO' },
  { label: 'Moderadamente ativo (3–4x/semana)', value: 'MODERADO' },
  { label: 'Muito ativo (5+ vezes/semana)', value: 'MUITO_ATIVO' },
  { label: 'Atleta (treino diário intenso)', value: 'ATLETA' },
]
const goalOptions = [
  { label: 'Emagrecimento', value: 'EMAGRECIMENTO' }, { label: 'Hipertrofia / Ganho de massa', value: 'HIPERTROFIA' },
  { label: 'Condicionamento físico', value: 'CONDICIONAMENTO' }, { label: 'Saúde e bem-estar', value: 'SAUDE' },
  { label: 'Reabilitação', value: 'REABILITACAO' }, { label: 'Outro', value: 'OUTRO' },
]
const levelOptions = [
  { label: 'Sedentário (nunca treinou)', value: 'SEDENTARIO' }, { label: 'Iniciante (< 6 meses)', value: 'INICIANTE' },
  { label: 'Intermediário (6 meses – 2 anos)', value: 'INTERMEDIARIO' }, { label: 'Avançado (> 2 anos)', value: 'AVANCADO' },
]
const stateOptions = [
  'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'
]
const parqQuestions = [
  { key: 'parqHeartProblem', text: '1. Algum médico já disse que você possui algum problema cardíaco e que só deve realizar atividade física sob supervisão médica?' },
  { key: 'parqChestPainExercise', text: '2. Você sente dores no peito quando pratica atividade física?' },
  { key: 'parqChestPainRest', text: '3. No último mês, você sentiu dores no peito sem realizar atividade física?' },
  { key: 'parqDizziness', text: '4. Você perde o equilíbrio por tontura ou já perdeu os sentidos?' },
  { key: 'parqBoneJoint', text: '5. Você possui algum problema ósseo ou articular (ex: coluna, joelho, quadril) que poderia se agravar com atividade física?' },
  { key: 'parqBloodPressureMeds', text: '6. Algum médico está prescrevendo medicamentos para pressão arterial ou condição cardíaca?' },
  { key: 'parqOtherReason', text: '7. Existe outro motivo que poderia impedir sua prática de atividade física?' },
]

// ---- Load data ----
onMounted(async () => {
  try {
    await adminStore.fetchUsers()
    const user = adminStore.users.find(u => u.id === studentId)
    if (user) {
      account.value.name = user.name
      account.value.email = user.email
    }

    const [profile, healthData] = await Promise.all([
      profileService.getStudentProfile(studentId),
      profileService.getStudentHealth(studentId),
    ])

    if (profile) {
      personal.value.cpf = profile.cpf ?? ''
      personal.value.rg = profile.rg ?? ''
      personal.value.birthDate = profile.birthDate ?? ''
      personal.value.gender = profile.gender ?? ''
      personal.value.maritalStatus = profile.maritalStatus ?? ''
      personal.value.profession = profile.profession ?? ''
      personal.value.phone = profile.phone ?? ''
      address.value.zip = profile.addressZip ?? ''
      address.value.line = profile.addressLine ?? ''
      address.value.number = profile.addressNumber ?? ''
      address.value.complement = profile.addressComplement ?? ''
      address.value.neighborhood = profile.addressNeighborhood ?? ''
      address.value.city = profile.addressCity ?? ''
      address.value.state = profile.addressState ?? ''
      emergency.value.name = profile.emergencyContactName ?? ''
      emergency.value.phone = profile.emergencyContactPhone ?? ''
      emergency.value.relationship = profile.emergencyContactRelationship ?? ''
    }

    if (healthData) {
      health.value.hasHeartCondition = healthData.hasHeartCondition
      health.value.hasDiabetes = healthData.hasDiabetes
      health.value.hasHypertension = healthData.hasHypertension
      health.value.hasRespiratoryCondition = healthData.hasRespiratoryCondition
      health.value.hasOrthopedicCondition = healthData.hasOrthopedicCondition
      health.value.hadSurgery = healthData.hadSurgery
      health.value.surgeryDescription = healthData.surgeryDescription ?? ''
      health.value.hasChronicPain = healthData.hasChronicPain
      health.value.chronicPainLocation = healthData.chronicPainLocation ?? ''
      health.value.medications = healthData.medications ?? ''
      health.value.physicalRestrictions = healthData.physicalRestrictions ?? ''
      health.value.smokes = healthData.smokes
      health.value.drinksAlcohol = healthData.drinksAlcohol
      health.value.alcoholFrequency = healthData.alcoholFrequency ?? ''
      health.value.sleepHours = healthData.sleepHours
      health.value.stressLevel = healthData.stressLevel
      health.value.activityLevel = healthData.activityLevel ?? ''
      health.value.fitnessGoal = healthData.fitnessGoal ?? ''
      health.value.fitnessLevel = healthData.fitnessLevel ?? ''
      health.value.exerciseHistory = healthData.exerciseHistory ?? ''
      health.value.parqHeartProblem = healthData.parqHeartProblem
      health.value.parqChestPainExercise = healthData.parqChestPainExercise
      health.value.parqChestPainRest = healthData.parqChestPainRest
      health.value.parqDizziness = healthData.parqDizziness
      health.value.parqBoneJoint = healthData.parqBoneJoint
      health.value.parqBloodPressureMeds = healthData.parqBloodPressureMeds
      health.value.parqOtherReason = healthData.parqOtherReason
      health.value.parqOtherReasonDetail = healthData.parqOtherReasonDetail ?? ''
      health.value.notes = healthData.notes ?? ''
    }
  } finally {
    loading.value = false
  }
})

async function fetchCep() {
  const cep = address.value.zip.replace(/\D/g, '')
  if (cep.length !== 8) return
  try {
    const res = await fetch(`https://viacep.com.br/ws/${cep}/json/`)
    const data = await res.json()
    if (!data.erro) {
      address.value.line = data.logradouro || address.value.line
      address.value.neighborhood = data.bairro || address.value.neighborhood
      address.value.city = data.localidade || address.value.city
      address.value.state = data.uf || address.value.state
    }
  } catch { /* ignore */ }
}

function nextStep() {
  errorMsg.value = ''
  if (currentStep.value === 0 && !account.value.name) {
    errorMsg.value = 'Informe o nome do aluno.'; return
  }
  if (currentStep.value === 1 && !personal.value.cpf) {
    errorMsg.value = 'O CPF é obrigatório.'; return
  }
  currentStep.value++
}

async function submit() {
  errorMsg.value = ''; saving.value = true
  try {
    await userService.update(studentId, { name: account.value.name, role: 'STUDENT' })

    const profileData = {
      cpf: personal.value.cpf.replace(/\D/g, ''),
      rg: personal.value.rg || null,
      birthDate: personal.value.birthDate || null,
      gender: personal.value.gender || null,
      maritalStatus: personal.value.maritalStatus || null,
      profession: personal.value.profession || null,
      phone: personal.value.phone || null,
      addressZip: address.value.zip || null,
      addressLine: address.value.line || null,
      addressNumber: address.value.number || null,
      addressComplement: address.value.complement || null,
      addressNeighborhood: address.value.neighborhood || null,
      addressCity: address.value.city || null,
      addressState: address.value.state || null,
      emergencyContactName: emergency.value.name || null,
      emergencyContactPhone: emergency.value.phone || null,
      emergencyContactRelationship: emergency.value.relationship || null,
    }
    await profileService.updateStudentProfile(studentId, profileData)
    await profileService.upsertStudentHealth(studentId, health.value)

    successMsg.value = 'Cadastro atualizado com sucesso!'
    setTimeout(() => router.push('/trainer/students'), 1500)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message ?? e.response?.data?.error ?? 'Erro ao salvar. Tente novamente.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 860px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); margin-bottom: 12px; }
.page-header { display: flex; flex-direction: column; gap: 12px; }

/* Step indicator */
.steps-indicator {
  display: flex; gap: 0; overflow-x: auto;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 4px;
}
.step-dot {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; border-radius: var(--radius-md);
  cursor: default; flex: 1; min-width: 100px;
  transition: background .15s;
}
.step-dot.done { cursor: pointer; }
.step-dot.done:hover { background: var(--neutral-50); }
.step-dot.active { background: var(--blue-50); }
.step-num {
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; flex-shrink: 0;
  background: var(--neutral-200); color: var(--neutral-600);
}
.step-dot.active .step-num { background: var(--blue-500); color: #fff; }
.step-dot.done .step-num { background: #10b981; color: #fff; }
.step-label { font-size: 12px; font-weight: 500; color: var(--neutral-600); white-space: nowrap; }
.step-dot.active .step-label { color: var(--blue-600); font-weight: 600; }

/* Wizard card */
.wizard-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 28px;
  box-shadow: var(--shadow-card);
}
.step-content { display: flex; flex-direction: column; gap: 16px; }
.step-title {
  font-family: var(--font-display); font-size: 17px; font-weight: 700;
  color: var(--neutral-900); display: flex; align-items: center; gap: 8px;
  padding-bottom: 12px; border-bottom: 1px solid var(--neutral-100);
}

/* Field grid */
.field-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px;
}
.field { display: flex; flex-direction: column; gap: 5px; }
.field label { font-size: 12px; font-weight: 600; color: var(--neutral-700); }
.col-full { grid-column: 1 / -1; }
.col-span-2 { grid-column: span 2; }
.mt-3 { margin-top: 12px; }
.mt-4 { margin-top: 20px; }

/* Check grid */
.check-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px;
}
.check-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--neutral-800); cursor: pointer;
  padding: 6px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--neutral-100);
}
.check-item:hover { background: var(--neutral-50); }

/* Section labels */
.section-label {
  font-size: 12px; font-weight: 700; text-transform: uppercase;
  letter-spacing: .07em; color: var(--neutral-500);
}

/* PAR-Q */
.parq-intro {
  font-size: 13px; color: var(--neutral-600);
  background: #fef3c7; border: 1px solid #fde68a;
  border-radius: var(--radius-md); padding: 10px 14px;
}
.parq-list { display: flex; flex-direction: column; gap: 12px; margin-top: 4px; }
.parq-item {
  background: var(--neutral-50); border: 1px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 12px 14px;
}
.parq-question { font-size: 13px; color: var(--neutral-800); margin-bottom: 10px; line-height: 1.5; }
.parq-opts { display: flex; gap: 20px; }
.parq-opt {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; cursor: pointer; padding: 4px 10px;
  border-radius: 20px; border: 1.5px solid var(--neutral-200);
}
.parq-yes { border-color: #fca5a5; color: #b91c1c; }

/* Navigation */
.step-nav {
  display: flex; align-items: center; gap: 10px;
  margin-top: 24px; padding-top: 16px;
  border-top: 1px solid var(--neutral-100);
}
.spacer { flex: 1; }

.disabled-field { background: var(--neutral-50) !important; color: var(--neutral-400) !important; }
.empty-state { text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px; }

@media (max-width: 600px) {
  .field-grid { grid-template-columns: 1fr; }
  .check-grid { grid-template-columns: 1fr; }
  .steps-indicator { flex-wrap: wrap; }
  .step-dot { min-width: 80px; }
}
</style>
