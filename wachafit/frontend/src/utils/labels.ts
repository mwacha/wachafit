export const roleLabel: Record<string, string> = {
  ADMIN:        'Administrador',
  MANAGER:      'Gerente',
  TRAINER:      'Personal Trainer',
  RECEPTIONIST: 'Recepcionista',
  CASHIER:      'Caixa',
  STUDENT:      'Aluno',
}

export const roleOptions = Object.entries(roleLabel).map(([value, label]) => ({ value, label }))

export const scheduleTypeLabel: Record<string, string> = {
  CLASS:    'Aula em grupo',
  PERSONAL: 'Personal',
}

export const scheduleTypeOptions = Object.entries(scheduleTypeLabel).map(([value, label]) => ({ value, label }))

export const scheduleStatusLabel: Record<string, string> = {
  OPEN:      'Aberto',
  FULL:      'Lotado',
  CANCELLED: 'Cancelado',
}

export const bookingStatusLabel: Record<string, string> = {
  PENDING:   'Agendado',
  CONFIRMED: 'Agendado',
  CANCELLED: 'Cancelado',
}

export const chargeStatusLabel: Record<string, string> = {
  PENDING:   'Pendente',
  PAID:      'Pago',
  OVERDUE:   'Vencido',
  CANCELLED: 'Cancelado',
}

export const chargeStatusSeverity: Record<string, string> = {
  PENDING:   'warn',
  PAID:      'success',
  OVERDUE:   'danger',
  CANCELLED: 'secondary',
}

export const payMethodLabel: Record<string, string> = {
  CASH:        'Dinheiro',
  PIX:         'PIX',
  CREDIT_CARD: 'Cartão de crédito',
  DEBIT_CARD:  'Cartão de débito',
  TRANSFER:    'Transferência',
}

export const payMethodOptions = Object.entries(payMethodLabel).map(([value, label]) => ({ value, label }))
