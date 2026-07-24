<!-- frontend/src/views/auth/SignupView.vue -->
<template>
  <div class="auth-shell">
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="brand-logo-w">W</span>
      </div>
      <div class="brand-copy">
        <h1 class="brand-title">Cadastre sua academia</h1>
        <p class="brand-sub">Leve o WachaFit para sua academia em poucos minutos.</p>
      </div>
      <div class="brand-dots" aria-hidden="true" />
    </div>

    <div class="form-panel">
      <div class="form-box">
        <p class="form-eyebrow">Passo {{ step }} de 3</p>
        <h2 class="form-title">{{ stepTitles[step - 1] }}</h2>

        <div v-if="step === 1" class="form-fields">
          <div class="field">
            <label class="field-label" for="adminName">Nome completo</label>
            <InputText id="adminName" v-model="admin.name" placeholder="Seu nome" autocomplete="name" />
          </div>
          <div class="field">
            <label class="field-label" for="adminEmail">E-mail</label>
            <InputText id="adminEmail" v-model="admin.email" type="email" placeholder="seu@email.com" autocomplete="email" />
          </div>
          <div class="field">
            <label class="field-label" for="adminPassword">Senha <span class="field-hint">(mín. 8 caracteres)</span></label>
            <Password id="adminPassword" v-model="admin.password" toggleMask :feedback="false" autocomplete="new-password" />
          </div>
          <div class="field">
            <label class="field-label" for="adminConfirm">Confirmar senha</label>
            <Password id="adminConfirm" v-model="adminConfirm" toggleMask :feedback="false"
                      :invalid="passwordMismatch" autocomplete="new-password" />
            <span v-if="passwordMismatch" class="field-error">Senhas não coincidem.</span>
          </div>
        </div>

        <div v-else-if="step === 2" class="form-fields">
          <div class="field">
            <label class="field-label" for="companyName">Razão social</label>
            <InputText id="companyName" v-model="company.name" placeholder="Academia Fitness Ltda" @input="suggestSlug" />
          </div>
          <div class="field">
            <label class="field-label" for="cnpj">CNPJ</label>
            <InputMask id="cnpj" v-model="company.cnpj" mask="99.999.999/9999-99" placeholder="00.000.000/0000-00" />
          </div>
          <div class="field">
            <label class="field-label" for="phone">Telefone</label>
            <InputMask id="phone" v-model="company.phone" mask="(99) 99999-9999" placeholder="(11) 99999-9999" />
          </div>
          <div class="field">
            <label class="field-label" for="slug">Slug da academia <span class="field-hint">(usado no login)</span></label>
            <InputText id="slug" v-model="company.slug" placeholder="minha-academia" @input="checkSlugAvailability" />
            <span v-if="slugStatus === 'taken'" class="field-error">Slug já em uso.</span>
            <span v-if="slugStatus === 'available'" class="field-success">Disponível!</span>
          </div>
        </div>

        <div v-else class="form-fields">
          <div class="plan-cards">
            <button
              v-for="p in plans" :key="p.id" type="button"
              class="plan-card" :class="{ selected: selectedPlanId === p.id }"
              @click="selectedPlanId = p.id"
            >
              <span class="plan-name">{{ p.name }}</span>
              <span class="plan-price">R$ {{ p.price.toFixed(2) }}/mês</span>
              <span class="plan-desc">{{ p.description }}</span>
            </button>
          </div>

          <div class="field">
            <label class="field-label">Forma de pagamento</label>
            <div class="payment-options">
              <label v-for="m in paymentMethods" :key="m.value" class="payment-option">
                <input type="radio" v-model="paymentMethod" :value="m.value" />
                {{ m.label }}
              </label>
            </div>
          </div>

          <div v-if="paymentMethod === 'CREDIT_CARD'" class="field">
            <label class="field-label" for="cardName">Nome no cartão</label>
            <InputText id="cardName" v-model="cardHolderName" placeholder="Como está impresso no cartão" />
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner" role="alert">
          <i class="pi pi-exclamation-circle" />
          {{ errorMessage }}
        </div>

        <div class="wizard-actions">
          <Button v-if="step > 1" label="Voltar" severity="secondary" outlined @click="step--" />
          <Button v-if="step < 3" label="Próximo" class="submit-btn" @click="goNext" />
          <Button v-else label="Criar conta" :loading="loading" class="submit-btn" @click="handleSignup" />
        </div>

        <div class="form-footer">
          <span>Já tem conta? <RouterLink to="/login" class="link">Entrar</RouterLink></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import api from '@/services/api'
import type { SaasPlan, PaymentMethod } from '@/types/api'
import InputText from 'primevue/inputtext'
import InputMask from 'primevue/inputmask'
import Password from 'primevue/password'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()

const step = ref(1)
const stepTitles = ['Crie sua conta', 'Dados da sua academia', 'Escolha o plano']

const admin = reactive({ name: '', email: '', password: '' })
const adminConfirm = ref('')
const company = reactive({ name: '', cnpj: '', phone: '', slug: '' })
const selectedPlanId = ref('')
const paymentMethod = ref<PaymentMethod>('PIX')
const cardHolderName = ref('')

const plans = ref<SaasPlan[]>([])
const slugStatus = ref<'idle' | 'checking' | 'available' | 'taken'>('idle')
const errorMessage = ref('')
const loading = ref(false)

const paymentMethods: { value: PaymentMethod; label: string }[] = [
  { value: 'CREDIT_CARD', label: 'Cartão de crédito' },
  { value: 'PIX', label: 'Pix' },
  { value: 'BOLETO', label: 'Boleto' },
]

const passwordMismatch = computed(() =>
  adminConfirm.value.length > 0 && admin.password !== adminConfirm.value
)

onMounted(async () => {
  const { data } = await api.get<SaasPlan[]>('/api/public/saas-plans')
  plans.value = data
  if (data.length > 0) selectedPlanId.value = data[0].id
})

function suggestSlug() {
  if (company.slug) return
  company.slug = company.name
    .toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

let slugDebounce: ReturnType<typeof setTimeout> | undefined
function checkSlugAvailability() {
  slugStatus.value = 'checking'
  clearTimeout(slugDebounce)
  slugDebounce = setTimeout(async () => {
    if (!company.slug) { slugStatus.value = 'idle'; return }
    const { data } = await api.get<{ available: boolean }>('/api/public/check-slug', {
      params: { slug: company.slug },
    })
    slugStatus.value = data.available ? 'available' : 'taken'
  }, 400)
}

function goNext() {
  errorMessage.value = ''
  if (step.value === 1) {
    if (!admin.name || !admin.email || !admin.password) {
      errorMessage.value = 'Preencha todos os campos.'
      return
    }
    if (admin.password !== adminConfirm.value) {
      errorMessage.value = 'Senhas não coincidem.'
      return
    }
    if (admin.password.length < 8) {
      errorMessage.value = 'Senha deve ter ao menos 8 caracteres.'
      return
    }
  }
  if (step.value === 2) {
    if (!company.name || !company.cnpj || !company.phone || !company.slug) {
      errorMessage.value = 'Preencha todos os campos.'
      return
    }
    if (slugStatus.value === 'taken') {
      errorMessage.value = 'Escolha outro slug.'
      return
    }
  }
  step.value++
}

async function handleSignup() {
  errorMessage.value = ''
  if (!selectedPlanId.value) {
    errorMessage.value = 'Selecione um plano.'
    return
  }
  loading.value = true
  try {
    const result = await auth.signup({
      admin: { name: admin.name, email: admin.email, password: admin.password },
      company: {
        name: company.name,
        cnpj: company.cnpj.replace(/\D/g, ''),
        phone: company.phone.replace(/\D/g, ''),
        slug: company.slug,
      },
      plan: { saasPlanId: selectedPlanId.value, paymentMethod: paymentMethod.value },
    })
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao criar conta. Tente novamente.'
    step.value = 2
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell { display: flex; min-height: 100dvh; }
.brand-panel {
  display: none; flex-direction: column; justify-content: space-between;
  padding: 48px; background: var(--dark-surface); position: relative; overflow: hidden;
}
@media (min-width: 768px) { .brand-panel { display: flex; width: 44%; } }
.brand-logo {
  width: 48px; height: 48px; border-radius: 14px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo); display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.brand-logo-w { font-family: var(--font-display); font-weight: 800; font-size: 22px; color: #fff; line-height: 1; }
.brand-copy { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; padding-bottom: 40px; }
.brand-title { font-family: var(--font-display); font-size: 32px; font-weight: 700; color: #fff; line-height: 1.15; margin-bottom: 12px; }
.brand-sub { font-size: 15px; color: var(--neutral-500); line-height: 1.6; max-width: 280px; }
.brand-dots {
  position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(ellipse at 60% 40%, black 30%, transparent 80%);
}
.form-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 32px 24px; background: #fff; }
.form-box { width: 100%; max-width: 420px; }
.form-eyebrow {
  font-family: var(--font-mono); font-size: 11px; font-weight: 500; color: var(--blue-500);
  letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 8px;
}
.form-title { font-family: var(--font-display); font-size: 26px; font-weight: 600; color: var(--neutral-900); line-height: 1.25; margin-bottom: 28px; }
.form-fields { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.field-hint { font-weight: 400; color: var(--neutral-500); }
.field-error { font-size: 12px; color: var(--error-text); font-weight: 500; }
.field-success { font-size: 12px; color: #16a34a; font-weight: 500; }
.error-banner {
  display: flex; align-items: center; gap: 8px; background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text); font-size: 13px; font-weight: 500; padding: 10px 14px;
}
.error-banner .pi { font-size: 14px; }
.wizard-actions { display: flex; gap: 12px; margin-top: 8px; }
.wizard-actions .submit-btn { flex: 1; width: 100% !important; justify-content: center; }
.plan-cards { display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px; }
.plan-card {
  display: flex; flex-direction: column; gap: 2px; padding: 14px;
  border: 1px solid #ddd; border-radius: var(--radius-md); background: #fff; cursor: pointer; text-align: left;
}
.plan-card.selected { border-color: var(--blue-500); box-shadow: 0 0 0 2px var(--blue-500) inset; }
.plan-name { font-weight: 600; font-size: 14px; }
.plan-price { font-size: 13px; color: var(--blue-500); font-weight: 600; }
.plan-desc { font-size: 12px; color: var(--neutral-600); }
.payment-options { display: flex; gap: 16px; font-size: 13px; }
.payment-option { display: flex; align-items: center; gap: 6px; }
.form-footer { margin-top: 20px; font-size: 13px; color: var(--neutral-600); }
.link { color: var(--blue-500); text-decoration: none; font-weight: 500; }
.link:hover { color: var(--blue-700); }
</style>
