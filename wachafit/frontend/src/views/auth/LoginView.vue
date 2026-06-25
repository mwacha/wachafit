<template>
  <div class="auth-shell">
    <!-- Painel esquerdo — branding -->
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="brand-logo-w">W</span>
      </div>
      <div class="brand-copy">
        <h1 class="brand-title">WachaFit</h1>
        <p class="brand-sub">Gestão completa para academias e personal trainers.</p>
      </div>
      <div class="brand-dots" aria-hidden="true" />
    </div>

    <!-- Painel direito — formulário -->
    <div class="form-panel">
      <div class="form-box">
        <p class="form-eyebrow">Bem-vindo de volta</p>
        <h2 class="form-title">Entrar na sua conta</h2>

        <div class="form-fields">
          <div class="field">
            <label class="field-label" for="email">E-mail</label>
            <InputText
              id="email"
              v-model="email"
              type="email"
              placeholder="seu@email.com"
              :invalid="!!errorMessage"
              autocomplete="email"
            />
          </div>

          <div class="field">
            <label class="field-label" for="password">Senha</label>
            <Password
              id="password"
              v-model="password"
              :feedback="false"
              toggleMask
              :invalid="!!errorMessage"
              autocomplete="current-password"
            />
          </div>

          <div v-if="errorMessage" class="error-banner" role="alert">
            <i class="pi pi-exclamation-circle" />
            {{ errorMessage }}
          </div>

          <Button
            label="Entrar"
            :loading="loading"
            class="submit-btn"
            @click="handleLogin"
          />
        </div>

        <div class="form-footer">
          <RouterLink to="/forgot-password" class="link">Esqueci minha senha</RouterLink>
          <span class="sep">·</span>
          <span>Não tem conta? <RouterLink to="/register" class="link">Cadastre-se</RouterLink></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()
const email = ref('')
const password = ref('')
const errorMessage = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = 'Preencha todos os campos.'
    return
  }
  loading.value = true
  try {
    const result = await auth.login(email.value, password.value)
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao fazer login. Tente novamente.'
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

/* ── Brand panel ── */
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
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-logo-w {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 22px;
  color: #fff;
  line-height: 1;
}

.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 40px;
}
.brand-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  line-height: 1.15;
  margin-bottom: 12px;
}
.brand-sub {
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--neutral-500);
  line-height: 1.6;
  max-width: 280px;
}

/* dot grid decoration */
.brand-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(ellipse at 60% 40%, black 30%, transparent 80%);
}

/* ── Form panel ── */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background: #fff;
}

.form-box {
  width: 100%;
  max-width: 400px;
}

.form-eyebrow {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  color: var(--blue-500);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 8px;
}
.form-title {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--neutral-900);
  line-height: 1.25;
  margin-bottom: 28px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--neutral-800);
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--error-bg);
  border: 1px solid #FECACA;
  border-radius: var(--radius-md);
  color: var(--error-text);
  font-size: 13px;
  font-weight: 500;
  padding: 10px 14px;
}
.error-banner .pi { font-size: 14px; }

.submit-btn {
  width: 100% !important;
  justify-content: center;
  margin-top: 4px;
}

.form-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 20px;
  font-size: 13px;
  color: var(--neutral-600);
}
.sep { color: var(--neutral-300); }
.link {
  color: var(--blue-500);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}
.link:hover { color: var(--blue-700); }
</style>
