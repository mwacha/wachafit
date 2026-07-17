<template>
  <div class="app-shell">
    <!-- Mobile overlay -->
    <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false" />

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar-open': mobileOpen }" role="navigation" aria-label="Navegação principal">
      <button class="sidebar-logo" :title="`WachaFit — ir para dashboard`" @click="goHome">
        <span class="logo-w">W</span>
      </button>

      <nav class="sidebar-nav">
        <!-- Admin links -->
        <template v-if="auth.role === 'ADMIN'">
          <RouterLink to="/admin" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/admin/users" class="nav-item" active-class="active" title="Usuários" aria-label="Usuários" @click="mobileOpen = false"><i class="pi pi-users" /></RouterLink>
          <RouterLink to="/trainer/students" class="nav-item" active-class="active" title="Alunos" aria-label="Alunos" @click="mobileOpen = false"><i class="pi pi-users" /></RouterLink>
          <RouterLink to="/admin/classes" class="nav-item" active-class="active" title="Turmas" aria-label="Turmas" @click="mobileOpen = false"><i class="pi pi-th-large" /></RouterLink>
          <RouterLink to="/admin/schedules" class="nav-item" active-class="active" title="Agenda" aria-label="Agenda" @click="mobileOpen = false"><i class="pi pi-calendar" /></RouterLink>
          <RouterLink to="/admin/membership-plans" class="nav-item" active-class="active" title="Planos" aria-label="Planos" @click="mobileOpen = false"><i class="pi pi-credit-card" /></RouterLink>
          <RouterLink to="/cashier/charges" class="nav-item" active-class="active" title="Cobranças" aria-label="Cobranças" @click="mobileOpen = false"><i class="pi pi-money-bill" /></RouterLink>
          <RouterLink to="/admin/reports/revenue" class="nav-item" active-class="active" title="Receita" aria-label="Receita" @click="mobileOpen = false"><i class="pi pi-chart-line" /></RouterLink>
          <RouterLink to="/admin/reports/commissions" class="nav-item" active-class="active" title="Comissões" aria-label="Comissões" @click="mobileOpen = false"><i class="pi pi-dollar" /></RouterLink>
          <RouterLink to="/exercises" class="nav-item" active-class="active" title="Exercícios" aria-label="Exercícios" @click="mobileOpen = false"><i class="pi pi-list" /></RouterLink>
        </template>

        <!-- Manager links -->
        <template v-else-if="auth.role === 'MANAGER'">
          <RouterLink to="/manager" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/admin/membership-plans" class="nav-item" active-class="active" title="Planos" aria-label="Planos" @click="mobileOpen = false"><i class="pi pi-credit-card" /></RouterLink>
          <RouterLink to="/admin/reports/revenue" class="nav-item" active-class="active" title="Receita" aria-label="Receita" @click="mobileOpen = false"><i class="pi pi-chart-bar" /></RouterLink>
          <RouterLink to="/admin/reports/overdue" class="nav-item" active-class="active" title="Inadimplentes" aria-label="Inadimplentes" @click="mobileOpen = false"><i class="pi pi-exclamation-triangle" /></RouterLink>
          <RouterLink to="/admin/reports/commissions" class="nav-item" active-class="active" title="Comissões" aria-label="Comissões" @click="mobileOpen = false"><i class="pi pi-dollar" /></RouterLink>
        </template>

        <!-- Cashier links -->
        <template v-else-if="auth.role === 'CASHIER'">
          <RouterLink to="/cashier" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/cashier/charges" class="nav-item" active-class="active" title="Cobranças" aria-label="Cobranças" @click="mobileOpen = false"><i class="pi pi-money-bill" /></RouterLink>
          <RouterLink to="/cashier/cash-flow" class="nav-item" active-class="active" title="Fluxo de Caixa" aria-label="Fluxo de Caixa" @click="mobileOpen = false"><i class="pi pi-chart-line" /></RouterLink>
        </template>

        <!-- Receptionist links -->
        <template v-else-if="auth.role === 'RECEPTIONIST'">
          <RouterLink to="/reception" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/reception/enroll" class="nav-item" active-class="active" title="Nova Matrícula" aria-label="Nova Matrícula" @click="mobileOpen = false"><i class="pi pi-user-plus" /></RouterLink>
          <RouterLink to="/reception/charges" class="nav-item" active-class="active" title="Cobranças" aria-label="Cobranças" @click="mobileOpen = false"><i class="pi pi-money-bill" /></RouterLink>
        </template>

        <!-- Trainer links -->
        <template v-else-if="auth.role === 'TRAINER'">
          <RouterLink to="/trainer" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/trainer/schedule" class="nav-item" active-class="active" title="Minha Agenda" aria-label="Minha Agenda" @click="mobileOpen = false"><i class="pi pi-calendar" /></RouterLink>
          <RouterLink to="/trainer/students" class="nav-item" active-class="active" title="Alunos" aria-label="Alunos" @click="mobileOpen = false"><i class="pi pi-users" /></RouterLink>
          <RouterLink to="/trainer/profile" class="nav-item" active-class="active" title="Meu Perfil" aria-label="Meu Perfil" @click="mobileOpen = false"><i class="pi pi-user" /></RouterLink>
          <RouterLink to="/exercises" class="nav-item" active-class="active" title="Exercícios" aria-label="Exercícios" @click="mobileOpen = false"><i class="pi pi-list" /></RouterLink>
        </template>

        <!-- Student links -->
        <template v-else-if="auth.role === 'STUDENT'">
          <RouterLink to="/student" class="nav-item" active-class="active" title="Dashboard" aria-label="Dashboard" @click="mobileOpen = false"><i class="pi pi-home" /></RouterLink>
          <RouterLink to="/student/schedule" class="nav-item" active-class="active" title="Aulas Disponíveis" aria-label="Aulas Disponíveis" @click="mobileOpen = false"><i class="pi pi-calendar" /></RouterLink>
          <RouterLink to="/student/bookings" class="nav-item" active-class="active" title="Minhas Reservas" aria-label="Minhas Reservas" @click="mobileOpen = false"><i class="pi pi-bookmark" /></RouterLink>
          <RouterLink to="/student/calendar" class="nav-item" active-class="active" title="Meu Calendário" aria-label="Meu Calendário" @click="mobileOpen = false"><i class="pi pi-calendar-plus" /></RouterLink>
          <RouterLink to="/student/workout" class="nav-item" active-class="active" title="Treino" aria-label="Treino" @click="mobileOpen = false"><i class="pi pi-bolt" /></RouterLink>
          <RouterLink to="/student/records" class="nav-item" active-class="active" title="Recordes" aria-label="Recordes" @click="mobileOpen = false"><i class="pi pi-trophy" /></RouterLink>
          <RouterLink to="/student/evolution" class="nav-item" active-class="active" title="Evolução" aria-label="Evolução" @click="mobileOpen = false"><i class="pi pi-chart-bar" /></RouterLink>
          <RouterLink to="/student/goals" class="nav-item" active-class="active" title="Metas" aria-label="Metas" @click="mobileOpen = false"><i class="pi pi-flag" /></RouterLink>
          <RouterLink to="/student/photos" class="nav-item" active-class="active" title="Fotos" aria-label="Fotos" @click="mobileOpen = false"><i class="pi pi-image" /></RouterLink>
          <RouterLink to="/student/profile" class="nav-item" active-class="active" title="Meu Perfil" aria-label="Meu Perfil" @click="mobileOpen = false"><i class="pi pi-user" /></RouterLink>
          <RouterLink to="/student/subscription" class="nav-item" active-class="active" title="Meu Plano" aria-label="Meu Plano" @click="mobileOpen = false"><i class="pi pi-id-card" /></RouterLink>
          <RouterLink to="/student/charges" class="nav-item" active-class="active" title="Cobranças" aria-label="Cobranças" @click="mobileOpen = false"><i class="pi pi-money-bill" /></RouterLink>
        </template>
      </nav>

      <div class="sidebar-footer">
        <button
          class="user-avatar"
          :title="`${auth.role} — clique para sair`"
          :aria-label="`Usuário ${auth.role}, clique para sair`"
          @click="handleLogout"
        >
          {{ userInitial }}
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
            <input
              class="search-input"
              type="search"
              placeholder="Buscar..."
              aria-label="Buscar"
            />
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
import { roleDashboards } from '@/utils/roleRoutes'

const auth = useAuthStore()
const router = useRouter()
const currentTime = ref('')
const mobileOpen = ref(false)
let timer: ReturnType<typeof setInterval>

function dashboardRoute() {
  return auth.role ? roleDashboards[auth.role] : '/login'
}

function goHome() {
  router.push(dashboardRoute())
  mobileOpen.value = false
}

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

onMounted(() => { tick(); timer = setInterval(tick, 60_000) })
onUnmounted(() => clearInterval(timer))

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100dvh;
  background: var(--neutral-50);
}

/* ── Sidebar ── */
.sidebar {
  width: 68px;
  flex-shrink: 0;
  background: var(--dark-surface);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  position: sticky;
  top: 0;
  height: 100dvh;
}

.sidebar-logo {
  width: 38px; height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  margin-bottom: 20px;
  cursor: pointer;
  border: none;
}
.logo-w {
  font-family: var(--font-display);
  font-weight: 800; font-size: 18px; color: #fff; line-height: 1;
  user-select: none;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.nav-item {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--neutral-600);
  font-size: 18px;
  transition: background 0.15s, color 0.15s;
}
.nav-item:not(.disabled):hover { background: rgba(255,255,255,0.05); color: var(--neutral-300); }
.nav-item.active { background: var(--blue-500); color: #fff; }
.nav-item:focus-visible { outline: 2px solid var(--blue-400); outline-offset: 2px; }
.nav-item.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.sidebar-footer { margin-top: auto; padding-top: 12px; }

.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: 0 4px 14px rgba(65,117,245,0.38);
  border: none; cursor: pointer;
  color: #fff; font-family: var(--font-display);
  font-weight: 700; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.15s;
}
.user-avatar:hover { transform: scale(1.05); }

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

.topbar-cta { height: 38px !important; padding: 0 16px !important; font-size: 13px !important; white-space: nowrap; }

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
    position: fixed; left: -68px; top: 0; height: 100dvh;
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
