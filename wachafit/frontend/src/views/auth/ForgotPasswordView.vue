<template>
  <div class="auth-centered">
    <div class="form-box">
      <div class="logo-mark">
        <span class="logo-w">W</span>
      </div>

      <p class="form-eyebrow">Recuperar acesso</p>
      <h2 class="form-title">Redefinir senha</h2>
      <p class="form-desc">
        Informe seu e-mail e enviaremos um link de redefinição.
      </p>

      <div class="form-fields">
        <div class="field">
          <label class="field-label" for="email">E-mail</label>
          <InputText
            id="email"
            v-model="email"
            type="email"
            placeholder="seu@email.com"
            autocomplete="email"
          />
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
          label="Enviar link"
          :loading="loading"
          class="submit-btn"
          @click="handleSubmit"
          :disabled="!!successMessage"
        />
      </div>

      <div class="form-footer">
        <RouterLink to="/login" class="link">
          <i class="pi pi-arrow-left" style="font-size:11px" />
          Voltar ao login
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from '@/services/api'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const email = ref('')
const loading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function handleSubmit() {
  successMessage.value = ''
  errorMessage.value = ''
  loading.value = true
  try {
    await api.post('/api/auth/forgot-password', { email: email.value })
    successMessage.value = 'Se o e-mail estiver cadastrado, você receberá o link em breve.'
  } catch {
    errorMessage.value = 'Erro ao processar solicitação. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-centered {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--neutral-50);
  padding: 32px 24px;
}

.form-box {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border: 1px solid var(--neutral-200);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-card);
}

.logo-mark {
  width: 44px; height: 44px;
  border-radius: 12px;
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
  font-size: 14px; color: var(--neutral-600); line-height: 1.55; margin-bottom: 24px;
}

.form-fields { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }

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

.form-footer {
  margin-top: 20px; display: flex; justify-content: center;
}
.link {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 13px; color: var(--neutral-600); text-decoration: none; font-weight: 500;
  transition: color 0.15s;
}
.link:hover { color: var(--blue-500); }
</style>
