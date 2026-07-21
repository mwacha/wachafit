<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Meu Perfil</h1>

      <div v-if="loading" class="empty-state"><i class="pi pi-spin pi-spinner empty-icon" /></div>
      <template v-else>

        <!-- Dados pessoais -->
        <div class="section-card">
          <h2 class="section-title"><i class="pi pi-user mr-2" />Dados Pessoais</h2>
          <div v-if="!profile" class="empty-state">Perfil não preenchido.</div>
          <div v-else class="fields-grid">
            <div class="field"><span class="field-label">CPF</span><span>{{ profile.cpf }}</span></div>
            <div class="field"><span class="field-label">RG</span><span>{{ profile.rg ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Data de nascimento</span><span>{{ profile.birthDate ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Gênero</span><span>{{ profile.gender ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Estado civil</span><span>{{ profile.maritalStatus ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Profissão</span><span>{{ profile.profession ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Telefone</span><span>{{ profile.phone ?? '—' }}</span></div>
            <div class="field col-span-2"><span class="field-label">Endereço</span>
              <span>{{ addressLine }}</span>
            </div>
            <div class="field"><span class="field-label">Contato de emergência</span>
              <span>{{ profile.emergencyContactName ?? '—' }} ({{ profile.emergencyContactRelationship ?? '—' }}) {{ profile.emergencyContactPhone ?? '' }}</span>
            </div>
          </div>
        </div>

        <!-- Saúde & PAR-Q -->
        <div class="section-card">
          <h2 class="section-title"><i class="pi pi-heart mr-2" />Saúde & PAR-Q</h2>
          <div v-if="!health" class="empty-state">Ficha de saúde não preenchida.</div>
          <div v-else>
            <div class="fields-grid">
              <div class="field"><span class="field-label">Nível de atividade</span><span>{{ health.activityLevel ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Objetivo</span><span>{{ health.fitnessGoal ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Nível de condicionamento</span><span>{{ health.fitnessLevel ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Horas de sono</span><span>{{ health.sleepHours ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Nível de estresse</span><span>{{ health.stressLevel ?? '—' }}/10</span></div>
              <div class="field"><span class="field-label">Medicamentos</span><span>{{ health.medications ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Restrições físicas</span><span>{{ health.physicalRestrictions ?? '—' }}</span></div>
            </div>

            <h3 class="subsection-title">Condições de saúde</h3>
            <div class="conditions-grid">
              <div class="condition" :class="{ active: health.hasHeartCondition }">Cardíaco</div>
              <div class="condition" :class="{ active: health.hasDiabetes }">Diabetes</div>
              <div class="condition" :class="{ active: health.hasHypertension }">Hipertensão</div>
              <div class="condition" :class="{ active: health.hasRespiratoryCondition }">Respiratório</div>
              <div class="condition" :class="{ active: health.hasOrthopedicCondition }">Ortopédico</div>
              <div class="condition" :class="{ active: health.hasChronicPain }">Dor crônica</div>
              <div class="condition" :class="{ active: health.hadSurgery }">Cirurgia prévia</div>
              <div class="condition" :class="{ active: health.smokes }">Fumante</div>
              <div class="condition" :class="{ active: health.drinksAlcohol }">Álcool</div>
            </div>

            <h3 class="subsection-title">PAR-Q</h3>
            <div class="parq-grid">
              <div class="parq-item" :class="{ flag: health.parqHeartProblem }">Problema cardíaco</div>
              <div class="parq-item" :class="{ flag: health.parqChestPainExercise }">Dor no peito ao exercitar</div>
              <div class="parq-item" :class="{ flag: health.parqChestPainRest }">Dor no peito em repouso</div>
              <div class="parq-item" :class="{ flag: health.parqDizziness }">Tontura/desmaio</div>
              <div class="parq-item" :class="{ flag: health.parqBoneJoint }">Osso/articulação</div>
              <div class="parq-item" :class="{ flag: health.parqBloodPressureMeds }">Medicação PA</div>
              <div class="parq-item" :class="{ flag: health.parqOtherReason }">Outro</div>
            </div>
            <div class="field mt-3">
              <span class="field-label">Assinatura PAR-Q</span>
              <span>{{ health.parqSignedAt ?? 'Não assinado' }}</span>
            </div>
          </div>
        </div>

      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import profileService from '@/services/profile.service'
import { useAuthStore } from '@/stores/auth.store'
import type { StudentProfile, StudentHealth } from '@/types/api'

const authStore = useAuthStore()
const profile = ref<StudentProfile | null>(null)
const health = ref<StudentHealth | null>(null)
const loading = ref(false)

const addressLine = computed(() => {
  if (!profile.value) return '—'
  const p = profile.value
  if (!p.addressLine) return '—'
  return [p.addressLine, p.addressNumber, p.addressComplement, p.addressNeighborhood, p.addressCity, p.addressState, p.addressZip]
    .filter(Boolean).join(', ')
})

onMounted(async () => {
  loading.value = true
  try {
    [profile.value, health.value] = await Promise.all([
      profileService.getStudentProfile(authStore.userId!),
      profileService.getStudentHealth(authStore.userId!),
    ])
  } finally { loading.value = false }
})
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 780px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.section-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px;
  box-shadow: var(--shadow-card);
}
.section-title { font-size: 16px; font-weight: 700; color: var(--neutral-800); margin-bottom: 16px; display: flex; align-items: center; }
.subsection-title { font-size: 13px; font-weight: 700; color: var(--neutral-600); margin: 16px 0 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 2px; }
.field-label { font-size: 11px; font-weight: 700; color: var(--neutral-500); text-transform: uppercase; letter-spacing: 0.4px; }
.col-span-2 { grid-column: span 2; }
.conditions-grid, .parq-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.condition, .parq-item {
  padding: 4px 10px; border-radius: 20px; font-size: 12px;
  background: var(--neutral-100); color: var(--neutral-500); border: 1px solid var(--neutral-200);
}
.condition.active { background: #dcfce7; color: #166534; border-color: #86efac; }
.parq-item.flag { background: #fef2f2; color: #991b1b; border-color: #fca5a5; }
.empty-state { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 14px; }
.empty-icon { font-size: 2rem; }

@media (max-width: 520px) {
  .fields-grid { grid-template-columns: 1fr; }
  .col-span-2 { grid-column: span 1; }
}
</style>
