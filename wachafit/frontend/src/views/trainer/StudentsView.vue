<!-- frontend/src/views/trainer/StudentsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Alunos</h1>
        <Button v-if="authStore.role === 'ADMIN'" label="Cadastrar Aluno" icon="pi pi-plus" @click="router.push('/admin/enroll')" />
      </div>
      <InputText v-model="search" placeholder="Buscar por nome..." />
      <div class="table-scroll">
        <DataTable :value="filteredStudents" :loading="adminStore.loading" stripedRows>
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

const adminStore = useAdminStore()
const authStore = useAuthStore()
const router = useRouter()
const search = ref('')

onMounted(() => adminStore.fetchUsers())

const filteredStudents = computed(() =>
  adminStore.users
    .filter(u => u.role === 'STUDENT' && u.active)
    .filter(u => u.name.toLowerCase().includes(search.value.toLowerCase()))
)
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
</style>
