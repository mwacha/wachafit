import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bookingService } from '@/services/booking.service'
import type { Booking } from '@/types/api'

export const useBookingStore = defineStore('booking', () => {
  const bookings = ref<Booking[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchMyBookings() {
    loading.value = true; error.value = null
    try { bookings.value = await bookingService.myBookings() }
    catch (e: any) { error.value = e.response?.data?.message ?? 'Erro ao carregar reservas' }
    finally { loading.value = false }
  }

  async function createBooking(scheduleId: string) {
    const b = await bookingService.create(scheduleId)
    bookings.value.unshift(b)
    return b
  }

  async function cancelBooking(id: string) {
    await bookingService.cancel(id)
    const idx = bookings.value.findIndex(b => b.id === id)
    if (idx !== -1) {
      bookings.value.splice(idx, 1, { ...bookings.value[idx], status: 'CANCELLED' })
    }
  }

  return { bookings, loading, error, fetchMyBookings, createBooking, cancelBooking }
})
