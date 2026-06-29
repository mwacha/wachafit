import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userService } from '@/services/user.service'
import { groupClassService } from '@/services/groupclass.service'
import type { AdminUser, GroupClass } from '@/types/api'

export const useAdminStore = defineStore('admin', () => {
  const users = ref<AdminUser[]>([])
  const classes = ref<GroupClass[]>([])
  const loading = ref(false)

  async function fetchUsers() {
    loading.value = true
    try { users.value = await userService.list() } finally { loading.value = false }
  }

  async function fetchClasses() {
    loading.value = true
    try { classes.value = await groupClassService.list() } finally { loading.value = false }
  }

  return { users, classes, loading, fetchUsers, fetchClasses }
})
