<!-- frontend/src/views/trainer/StudentsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Alunos</h1>
      <InputText v-model="search" placeholder="Buscar por nome..." class="mb-4 w-full" />
      <DataTable :value="filteredStudents" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="email" header="Email" />
        <Column header="Ações">
          <template #body="{ data }">
            <RouterLink :to="`/trainer/students/${data.id}/overview`">
              <Button icon="pi pi-eye" text label="Ver" />
            </RouterLink>
          </template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { RouterLink } from 'vue-router'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'

const adminStore = useAdminStore()
const search = ref('')

onMounted(() => adminStore.fetchUsers())

const filteredStudents = computed(() =>
  adminStore.users
    .filter(u => u.role === 'STUDENT' && u.active)
    .filter(u => u.name.toLowerCase().includes(search.value.toLowerCase()))
)
</script>
