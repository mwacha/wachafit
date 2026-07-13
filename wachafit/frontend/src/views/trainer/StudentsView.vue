<!-- frontend/src/views/trainer/StudentsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Alunos</h1>
        <Button v-if="authStore.role === 'ADMIN'" label="Cadastrar Aluno" icon="pi pi-plus" @click="router.push('/admin/enroll')" />
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button :class="['tab', { active: tab === 'ativos' }]" @click="tab = 'ativos'">Ativos</button>
        <button v-if="authStore.role === 'ADMIN'" :class="['tab', { active: tab === 'inadimplentes' }]" @click="switchToOverdue">Inadimplentes</button>
      </div>

      <!-- Tab: Ativos -->
      <template v-if="tab === 'ativos'">
        <InputText v-model="search" placeholder="Buscar por nome..." />
        <div class="table-scroll">
          <DataTable :value="filteredStudents" :loading="adminStore.loading" stripedRows emptyMessage="Nenhum aluno encontrado.">
            <Column field="name" header="Nome" style="min-width:140px" />
            <Column field="email" header="Email" style="min-width:180px" />
            <Column header="Ações" style="min-width:90px">
              <template #body="{ data }">
                <RouterLink :to="`/trainer/students/${data.id}/overview`">
                  <Button icon="pi pi-eye" text label="Ver" />
                </RouterLink>
              </template>
            </Column>
          </DataTable>
        </div>
      </template>

      <!-- Tab: Inadimplentes (admin only) -->
      <template v-else-if="tab === 'inadimplentes'">
        <div v-if="loadingOverdue" class="empty-state">Carregando...</div>
        <div v-else-if="overdue.length === 0" class="empty-state">Nenhum aluno inadimplente.</div>
        <div v-else class="table-scroll">
          <DataTable :value="overdue" stripedRows>
            <Column field="name" header="Aluno" style="min-width:140px" />
            <Column header="Total Devido" style="min-width:130px">
              <template #body="{ data }">R$ {{ data.totalDue.toFixed(2) }}</template>
            </Column>
            <Column field="daysOverdue" header="Dias em Atraso" style="min-width:130px" />
          </DataTable>
        </div>
      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { useAuthStore } from '@/stores/auth.store'
import { RouterLink, useRouter } from 'vue-router'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import reportService from '@/services/report.service'
import type { OverdueStudent } from '@/types/api'

const adminStore = useAdminStore()
const authStore = useAuthStore()
const router = useRouter()
const search = ref('')
const tab = ref<'ativos' | 'inadimplentes'>('ativos')
const overdue = ref<OverdueStudent[]>([])
const loadingOverdue = ref(false)

onMounted(() => adminStore.fetchUsers())

const filteredStudents = computed(() =>
  adminStore.users
    .filter(u => u.role === 'STUDENT' && u.active)
    .filter(u => u.name.toLowerCase().includes(search.value.toLowerCase()))
)

async function switchToOverdue() {
  tab.value = 'inadimplentes'
  if (overdue.value.length > 0) return
  loadingOverdue.value = true
  try { overdue.value = await reportService.getOverdue() }
  finally { loadingOverdue.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.tabs { display: flex; gap: 4px; border-bottom: 2px solid var(--neutral-200); }
.tab {
  padding: 8px 18px; background: none; border: none; cursor: pointer;
  font-size: 14px; font-weight: 500; color: var(--neutral-500);
  border-bottom: 2px solid transparent; margin-bottom: -2px;
  transition: color .15s, border-color .15s;
}
.tab:hover { color: var(--neutral-800); }
.tab.active { color: var(--blue-600, #2563eb); border-bottom-color: var(--blue-600, #2563eb); font-weight: 600; }

.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
</style>
