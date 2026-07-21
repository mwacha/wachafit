<template>
  <div class="app-shell">
    <!-- Mobile overlay -->
    <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false" />

    <!-- Sidebar -->
    <aside :class="['sidebar', 'sidebar-expanded', { 'sidebar-open': mobileOpen }]"
      role="navigation" aria-label="Navegação principal">

      <!-- Logo -->
      <div class="sidebar-top">
        <button class="sidebar-logo" :title="`WachaFit — ir para dashboard`" @click="goHome">
          <span class="logo-icon">W</span>
          <span class="logo-text">WachaFit</span>
        </button>
      </div>

      <!-- Nav links -->
      <nav class="sidebar-nav">
        <!-- Admin -->
        <template v-if="auth.role === 'ADMIN'">
          <RouterLink to="/admin"                    class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
          <RouterLink to="/admin/users"              class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-users" /><span class="nav-label">Usuários</span></RouterLink>
          <RouterLink to="/trainer/students"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-id-card" /><span class="nav-label">Alunos</span></RouterLink>
          <RouterLink to="/admin/classes"            class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-th-large" /><span class="nav-label">Turmas</span></RouterLink>
          <RouterLink to="/admin/schedule-grid"     class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-table" /><span class="nav-label">Grade</span></RouterLink>
          <RouterLink to="/admin/schedules"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-calendar" /><span class="nav-label">Agenda</span></RouterLink>
          <RouterLink to="/admin/membership-plans"   class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-credit-card" /><span class="nav-label">Planos</span></RouterLink>
          <RouterLink to="/admin/reports/revenue"    class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-chart-line" /><span class="nav-label">Receita</span></RouterLink>
          <RouterLink to="/admin/reports/commissions" class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-dollar" /><span class="nav-label">Comissões</span></RouterLink>
          <RouterLink to="/exercises"                class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-list" /><span class="nav-label">Exercícios</span></RouterLink>
        </template>

        <!-- Manager -->
        <template v-else-if="auth.role === 'MANAGER'">
          <RouterLink to="/manager"                  class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
          <RouterLink to="/admin/membership-plans"   class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-credit-card" /><span class="nav-label">Planos</span></RouterLink>
          <RouterLink to="/admin/reports/revenue"    class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-chart-bar" /><span class="nav-label">Receita</span></RouterLink>
          <RouterLink to="/admin/reports/overdue"    class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-exclamation-triangle" /><span class="nav-label">Inadimplentes</span></RouterLink>
          <RouterLink to="/admin/reports/commissions" class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-dollar" /><span class="nav-label">Comissões</span></RouterLink>
        </template>

        <!-- Cashier -->
        <template v-else-if="auth.role === 'CASHIER'">
          <RouterLink to="/cashier"                  class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
          <RouterLink to="/cashier/charges"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-money-bill" /><span class="nav-label">Cobranças</span></RouterLink>
          <RouterLink to="/cashier/cash-flow"        class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-chart-line" /><span class="nav-label">Fluxo de Caixa</span></RouterLink>
        </template>

        <!-- Receptionist -->
        <template v-else-if="auth.role === 'RECEPTIONIST'">
          <RouterLink to="/reception"                class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
          <RouterLink to="/reception/enroll"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-user-plus" /><span class="nav-label">Nova Matrícula</span></RouterLink>
          <RouterLink to="/reception/charges"        class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-money-bill" /><span class="nav-label">Cobranças</span></RouterLink>
        </template>

        <!-- Trainer -->
        <template v-else-if="auth.role === 'TRAINER'">
          <RouterLink to="/trainer"                  class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
          <RouterLink to="/trainer/schedule"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-calendar" /><span class="nav-label">Minha Agenda</span></RouterLink>
          <RouterLink to="/trainer/students"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-users" /><span class="nav-label">Alunos</span></RouterLink>
          <RouterLink to="/trainer/profile"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-user" /><span class="nav-label">Meu Perfil</span></RouterLink>
          <RouterLink to="/exercises"                class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-list" /><span class="nav-label">Exercícios</span></RouterLink>
        </template>

        <!-- Student -->
        <template v-else-if="auth.role === 'STUDENT'">
          <!-- Payment gate: se há pagamento vencido, exibe apenas Cobranças -->
          <template v-if="billing.hasOverduePayment">
            <div class="overdue-banner">
              <i class="pi pi-exclamation-triangle" />
              <span>Pagamento em atraso</span>
            </div>
            <RouterLink to="/student/charges" class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-money-bill" /><span class="nav-label">Cobranças</span></RouterLink>
          </template>
          <template v-else>
            <RouterLink to="/student"                  class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-home" /><span class="nav-label">Dashboard</span></RouterLink>
            <RouterLink to="/student/schedule"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-calendar" /><span class="nav-label">Aulas</span></RouterLink>
            <RouterLink to="/student/bookings"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-bookmark" /><span class="nav-label">Reservas</span></RouterLink>
            <RouterLink to="/student/calendar"         class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-calendar-plus" /><span class="nav-label">Calendário</span></RouterLink>
            <RouterLink to="/student/workout"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-bolt" /><span class="nav-label">Treino</span></RouterLink>
            <RouterLink to="/student/records"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-trophy" /><span class="nav-label">Recordes</span></RouterLink>
            <RouterLink to="/student/evolution"        class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-chart-bar" /><span class="nav-label">Evolução</span></RouterLink>
            <RouterLink to="/student/goals"            class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-flag" /><span class="nav-label">Metas</span></RouterLink>
            <RouterLink to="/student/photos"           class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-image" /><span class="nav-label">Fotos</span></RouterLink>
            <RouterLink to="/student/profile"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-user" /><span class="nav-label">Perfil</span></RouterLink>
            <RouterLink to="/student/subscription"     class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-id-card" /><span class="nav-label">Meu Plano</span></RouterLink>
            <RouterLink to="/student/charges"          class="nav-item" active-class="active" @click="mobileOpen = false"><i class="pi pi-money-bill" /><span class="nav-label">Cobranças</span></RouterLink>
          </template>
        </template>
      </nav>

      <!-- Footer -->
      <div class="sidebar-footer">
        <button class="nav-item user-btn" :title="`${auth.role} — clique para sair`" @click="handleLogout">
          <span class="user-avatar">{{ userInitial }}</span>
          <span class="nav-label user-logout-label">Sair</span>
        </button>
      </div>
    </aside>

    <!-- Main area -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="hamburger" aria-label="Abrir menu" @click="mobileOpen = !mobileOpen">
            <i class="pi pi-bars" />
          </button>
          <time class="topbar-time">{{ currentTime }}</time>
          <h1 class="topbar-greeting">Olá, {{ greeting }}</h1>
        </div>
        <div class="topbar-right">
          <div class="search-wrap">
            <i class="pi pi-search search-icon" aria-hidden="true" />
            <input class="search-input" type="search" placeholder="Buscar..." aria-label="Buscar" />
          </div>
          <button class="logout-btn" title="Sair" aria-label="Sair" @click="handleLogout">
            <i class="pi pi-sign-out" />
          </button>
        </div>
      </header>

      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useBillingStore } from '@/stores/billing.store'
import { roleDashboards } from '@/utils/roleRoutes'

const auth = useAuthStore()
const billing = useBillingStore()
const router = useRouter()
const currentTime = ref('')
const mobileOpen = ref(false)

let timer: ReturnType<typeof setInterval>

function dashboardRoute() { return auth.role ? roleDashboards[auth.role] : '/login' }
function goHome() { router.push(dashboardRoute()); mobileOpen.value = false }

const userInitial = computed(() => auth.role?.charAt(0) ?? 'U')
const greeting = computed(() => {
  const labels: Record<string, string> = {
    ADMIN: 'Admin', MANAGER: 'Gerente', CASHIER: 'Caixa',
    RECEPTIONIST: 'Recepção', TRAINER: 'Profissional', STUDENT: 'Aluno',
  }
  return auth.role ? (labels[auth.role] ?? auth.role) : 'Usuário'
})

function tick() {
  currentTime.value = new Date().toLocaleString('pt-BR', {
    weekday: 'short', day: '2-digit', month: 'short',
    hour: '2-digit', minute: '2-digit',
  })
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 60_000)
  if (auth.role === 'STUDENT') {
    await billing.fetchPaymentStatus()
    if (billing.hasOverduePayment && router.currentRoute.value.path !== '/student/charges') {
      router.replace('/student/charges')
    }
  }
})
onUnmounted(() => clearInterval(timer))

function handleLogout() { auth.logout(); router.push('/login') }
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100dvh;
  background: var(--neutral-50);
}

/* ── Sidebar ── */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--dark-surface);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 0;
  position: sticky;
  top: 0;
  height: 100dvh;
  overflow: hidden;
  z-index: 30;
}

/* ── Logo ── */
.sidebar-top {
  padding: 0 12px;
  margin-bottom: 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  min-height: 44px;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
}
.logo-icon {
  width: 36px; height: 36px; flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display);
  font-weight: 800; font-size: 17px; color: #fff;
  user-select: none;
}
.logo-text {
  font-family: var(--font-display);
  font-size: 15px; font-weight: 800;
  color: #fff;
  white-space: nowrap;
}

/* ── Nav ── */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 12px;
  scrollbar-width: none;
}
.sidebar-nav::-webkit-scrollbar { display: none; }

.nav-item {
  width: 100%; height: 42px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: flex-start;
  padding: 0 14px; gap: 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--neutral-500);
  font-size: 17px;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
}
.nav-item:hover { background: rgba(255,255,255,0.07); color: var(--neutral-200); }
.nav-item.active { background: var(--blue-500); color: #fff; }
.nav-item:focus-visible { outline: 2px solid var(--blue-400); outline-offset: 2px; }

/* Labels */
.nav-label {
  font-size: 13px;
  font-weight: 500;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
}

/* ── Footer ── */
.sidebar-footer {
  flex-shrink: 0;
  padding: 8px 12px 4px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  width: 100%;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.user-btn { font-size: 14px; color: var(--neutral-400); }
.user-btn:hover { color: var(--neutral-200); }

.user-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display); font-weight: 700;
  font-size: 12px; color: #fff;
  flex-shrink: 0;
}

.user-logout-label { color: var(--neutral-400); }

.overdue-banner {
  display: flex; align-items: center; gap: 8px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.4);
  border-radius: var(--radius-md);
  padding: 8px 12px; margin-bottom: 4px;
  color: #fca5a5; font-size: 12px; font-weight: 600;
}

/* ── Main ── */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  background: var(--neutral-50);
  border-bottom: 1px solid var(--neutral-200);
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  position: sticky;
  top: 0;
  z-index: 20;
}

.topbar-left { display: flex; flex-direction: column; gap: 2px; }
.topbar-time {
  font-family: var(--font-mono); font-size: 10px; font-weight: 400;
  color: var(--neutral-500); text-transform: capitalize;
}
.topbar-greeting {
  font-family: var(--font-display); font-size: 22px; font-weight: 700;
  color: var(--neutral-900); line-height: 1.2;
}

.topbar-right { display: flex; align-items: center; gap: 10px; }

.search-wrap { position: relative; display: flex; align-items: center; }
.search-icon {
  position: absolute; left: 12px; font-size: 13px;
  color: var(--neutral-500); pointer-events: none;
}
.search-input {
  height: 38px; padding: 0 14px 0 34px;
  border: 1.5px solid var(--neutral-200); border-radius: var(--radius-md);
  font-family: var(--font-body); font-size: 13px;
  color: var(--neutral-900); background: #fff;
  outline: none; width: 200px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus { border-color: var(--blue-500); box-shadow: var(--shadow-focus); }
.search-input::placeholder { color: var(--neutral-500); }

.logout-btn {
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1.5px solid var(--neutral-200);
  border-radius: var(--radius-md); color: var(--neutral-600);
  cursor: pointer; font-size: 15px;
  transition: border-color 0.15s, color 0.15s;
}
.logout-btn:hover { border-color: var(--neutral-300); color: var(--neutral-900); }

.page-content { flex: 1; padding: 24px; overflow-y: auto; }

/* ── Mobile ── */
.hamburger {
  display: none;
  background: none; border: none; cursor: pointer;
  font-size: 20px; color: var(--neutral-700); padding: 4px;
  margin-right: 8px; align-items: center; justify-content: center;
}

.mobile-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  z-index: 40;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed; left: -240px; top: 0; height: 100dvh;
    z-index: 50; transition: left 0.25s ease;
  }
  .sidebar.sidebar-open { left: 0; }
  .hamburger { display: flex; }
  .search-wrap { display: none; }
  .topbar { padding: 10px 14px; gap: 8px; }
  .topbar-greeting { font-size: 18px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
  .page-content { padding: 14px; }
}

@media (max-width: 400px) {
  .topbar-greeting { font-size: 16px; max-width: 150px; }
  .topbar-time { display: none; }
}
</style>
