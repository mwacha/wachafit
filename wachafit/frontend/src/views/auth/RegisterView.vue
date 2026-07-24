<template>
  <div class="auth-shell">
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="brand-logo-w">W</span>
      </div>
      <div class="brand-copy">
        <h1 class="brand-title">Comece agora</h1>
        <p class="brand-sub">Crie sua conta e gerencie treinos, agendamentos e alunos em um só lugar.</p>
      </div>
      <div class="brand-dots" aria-hidden="true" />
    </div>

    <div class="form-panel">
      <div class="form-box">
        <p class="form-eyebrow">Criar conta</p>
        <h2 class="form-title">Cadastre-se gratuitamente</h2>

        <div class="form-fields">
          <div class="field">
            <label class="field-label" for="tenantSlug">Slug da academia</label>
            <InputText id="tenantSlug" v-model="tenantSlug" placeholder="ex: minha-academia" autocomplete="organization" />
          </div>

          <div class="field">
            <label class="field-label" for="name">Nome completo</label>
            <InputText id="name" v-model="name" placeholder="Seu nome" autocomplete="name" />
          </div>

          <div class="field">
            <label class="field-label" for="email">E-mail</label>
            <InputText id="email" v-model="email" type="email" placeholder="seu@email.com" autocomplete="email" />
          </div>

          <div class="field">
            <label class="field-label" for="password">Senha <span class="field-hint">(mín. 8 caracteres)</span></label>
            <Password id="password" v-model="password" toggleMask :feedback="false" autocomplete="new-password" />
          </div>

          <div class="field">
            <label class="field-label" for="confirm">Confirmar senha</label>
            <Password
              id="confirm"
              v-model="confirmPassword"
              toggleMask
              :feedback="false"
              :invalid="passwordMismatch"
              autocomplete="new-password"
            />
            <span v-if="passwordMismatch" class="field-error">Senhas não coincidem.</span>
          </div>

          <div v-if="errorMessage" class="error-banner" role="alert">
            <i class="pi pi-exclamation-circle" />
            {{ errorMessage }}
          </div>

          <Button
            label="Criar conta"
            :loading="loading"
            class="submit-btn"
            @click="handleRegister"
          />
        </div>

        <div class="form-footer">
          <span>Já tem conta? <RouterLink to="/login" class="link">Entrar</RouterLink></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()
const tenantSlug = ref('')
const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')
const loading = ref(false)

const passwordMismatch = computed(() =>
  confirmPassword.value.length > 0 && password.value !== confirmPassword.value
)

async function handleRegister() {
  errorMessage.value = ''
  if (!tenantSlug.value || !name.value || !email.value || !password.value) {
    errorMessage.value = 'Preencha todos os campos.'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMessage.value = 'Senhas não coincidem.'
    return
  }
  if (password.value.length < 8) {
    errorMessage.value = 'Senha deve ter ao menos 8 caracteres.'
    return
  }
  loading.value = true
  try {
    const result = await auth.register(name.value, email.value, password.value, tenantSlug.value)
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao criar conta. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell {
  display: flex;
  min-height: 100dvh;
}

.brand-panel {
  display: none;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
  background: var(--dark-surface);
  position: relative;
  overflow: hidden;
}
@media (min-width: 768px) {
  .brand-panel { display: flex; width: 44%; }
}

.brand-logo {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.brand-logo-w {
  font-family: var(--font-display);
  font-weight: 800; font-size: 22px; color: #fff; line-height: 1;
}

.brand-copy {
  flex: 1; display: flex; flex-direction: column;
  justify-content: flex-end; padding-bottom: 40px;
}
.brand-title {
  font-family: var(--font-display); font-size: 32px; font-weight: 700;
  color: #fff; line-height: 1.15; margin-bottom: 12px;
}
.brand-sub {
  font-size: 15px; color: var(--neutral-500); line-height: 1.6; max-width: 280px;
}

.brand-dots {
  position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(ellipse at 60% 40%, black 30%, transparent 80%);
}

.form-panel {
  flex: 1; display: flex; align-items: center;
  justify-content: center; padding: 32px 24px; background: #fff;
}
.form-box { width: 100%; max-width: 400px; }

.form-eyebrow {
  font-family: var(--font-mono); font-size: 11px; font-weight: 500;
  color: var(--blue-500); letter-spacing: 0.08em;
  text-transform: uppercase; margin-bottom: 8px;
}
.form-title {
  font-family: var(--font-display); font-size: 26px; font-weight: 600;
  color: var(--neutral-900); line-height: 1.25; margin-bottom: 28px;
}

.form-fields { display: flex; flex-direction: column; gap: 16px; }

.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.field-hint { font-weight: 400; color: var(--neutral-500); }
.field-error { font-size: 12px; color: var(--error-text); font-weight: 500; }

.error-banner {
  display: flex; align-items: center; gap: 8px;
  background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text);
  font-size: 13px; font-weight: 500; padding: 10px 14px;
}
.error-banner .pi { font-size: 14px; }

.submit-btn { width: 100% !important; justify-content: center; margin-top: 4px; }

.form-footer {
  margin-top: 20px; font-size: 13px; color: var(--neutral-600);
}
.link {
  color: var(--blue-500); text-decoration: none; font-weight: 500;
}
.link:hover { color: var(--blue-700); }
</style>
