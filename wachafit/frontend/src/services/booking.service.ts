import api from './api'
import type { Booking } from '@/types/api'

export const bookingService = {
  myBookings: () => api.get<Booking[]>('/api/bookings/my').then(r => r.data),
  create: (scheduleId: string) => api.post<Booking>('/api/bookings', { scheduleId }).then(r => r.data),
  cancel: (id: string) => api.delete(`/api/bookings/${id}`),
}
