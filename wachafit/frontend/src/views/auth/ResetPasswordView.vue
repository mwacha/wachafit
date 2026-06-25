<template>
  <div class="auth-centered">
    <div class="form-box">
      <div class="logo-mark">
        <span class="logo-w">W</span>
      </div>

      <p class="form-eyebrow">Nova senha</p>
      <h2 class="form-title">Criar nova senha</h2>
      <p class="form-desc">Escolha uma senha com ao menos 8 caracteres.</p>

      <div class="form-fields">
        <div class="field">
          <label class="field-label" for="new-pass">Nova senha</label>
          <Password
            id="new-pass"
            v-model="newPassword"
            toggleMask
            :feedback="false"
            placeholder="Mín. 8 caracteres"
            autocomplete="new-password"
          />
        </div>

        <div class="field">
          <label class="field-label" for="confirm-pass">Confirmar nova senha</label>
          <Password
            id="confirm-pass"
            v-model="confirmPassword"
            toggleMask
            :feedback="false"
            :invalid="passwordMismatch"
            autocomplete="new-password"
          />
          <span v-if="passwordMismatch" class="field-error">Senhas não coincidem.</span>
        </div>

        <div v-if="successMessage" class="success-banner" role="status">
          <i class="pi pi-check-circle" />
          {{ successMessage }}
        </div>
        <div v-if="errorMessage" class="error-banner" role="alert">
          <i class="pi pi-exclamation-circle" />
          {{ errorMessage }}
        </div>

        <Button
          label="Redefinir senha"
          :loading="loading"
          class="submit-btn"
          @click="handleReset"
          :disabled="!!successMessage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'
import Password from 'primevue/password'
import Button from 'primevue/button'

const route = useRoute()
const router = useRouter()
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const passwordMismatch = computed(() =>
  confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value
)

async function handleReset() {
  successMessage.value = ''
  errorMessage.value = ''
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Senhas não coincidem.'
    return
  }
  loading.value = true
  try {
    await api.post('/api/auth/reset-password', {
      token: route.query.token as string,
      newPassword: newPassword.value,
    })
    successMessage.value = 'Senha redefinida com sucesso! Redirecionando...'
    setTimeout(() => router.push('/login'), 2000)
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao redefinir senha.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-centered {
  min-height: 100dvh;
  display: flex; align-items: center; justify-content: center;
  background: var(--neutral-50); padding: 32px 24px;
}

.form-box {
  width: 100%; max-width: 400px;
  background: #fff;
  border: 1px solid var(--neutral-200);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-card);
}

.logo-mark {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 24px;
}
.logo-w {
  font-family: var(--font-display);
  font-weight: 800; font-size: 20px; color: #fff; line-height: 1;
}

.form-eyebrow {
  font-family: var(--font-mono); font-size: 11px; font-weight: 500;
  color: var(--blue-500); letter-spacing: 0.08em;
  text-transform: uppercase; margin-bottom: 8px;
}
.form-title {
  font-family: var(--font-display); font-size: 22px; font-weight: 600;
  color: var(--neutral-900); line-height: 1.25; margin-bottom: 8px;
}
.form-desc {
  font-size: 14px; color: var(--neutral-600); margin-bottom: 24px;
}

.form-fields { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.field-error { font-size: 12px; color: var(--error-text); font-weight: 500; }

.success-banner {
  display: flex; align-items: flex-start; gap: 8px;
  background: var(--success-bg); border: 1px solid #BBF7D0;
  border-radius: var(--radius-md); color: var(--success-text);
  font-size: 13px; font-weight: 500; padding: 10px 14px;
}
.success-banner .pi { font-size: 14px; margin-top: 1px; flex-shrink: 0; }

.error-banner {
  display: flex; align-items: center; gap: 8px;
  background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text);
  font-size: 13px; font-weight: 500; padding: 10px 14px;
}
.error-banner .pi { font-size: 14px; }

.submit-btn { width: 100% !important; justify-content: center; }
</style>
