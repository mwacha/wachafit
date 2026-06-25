<template>
  <div class="app-shell">
    <!-- Sidebar -->
    <aside class="sidebar" role="navigation" aria-label="Navegação principal">
      <button class="sidebar-logo" :title="`WachaFit — ir para dashboard`" @click="goHome">
        <span class="logo-w">W</span>
      </button>

      <nav class="sidebar-nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{
            active: isActiveItem(item),
            disabled: !item.route,
          }"
          :title="item.route ? item.label : `${item.label} — Em breve`"
          :aria-label="item.route ? item.label : `${item.label} (em breve)`"
          :disabled="!item.route"
          @click="item.route && navigateTo(item.route)"
        >
          <i :class="`pi pi-${item.icon}`" />
        </button>
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
          <Button label="+ Matrícula" class="topbar-cta" aria-label="Nova matrícula" />
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
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const currentTime = ref('')
let timer: ReturnType<typeof setInterval>

// route=null means "em breve" (Etapa 2+)
const navItems = [
  { key: 'home',     icon: 'home',      label: 'Dashboard',  route: () => dashboardRoute() },
  { key: 'users',    icon: 'users',     label: 'Alunos',     route: null },
  { key: 'calendar', icon: 'calendar',  label: 'Agenda',     route: null },
  { key: 'activity', icon: 'bolt',      label: 'Treinos',    route: null },
  { key: 'chart',    icon: 'chart-bar', label: 'Relatórios', route: null },
]

function dashboardRoute() {
  return auth.role ? roleDashboards[auth.role] : '/login'
}

function isActiveItem(item: typeof navItems[0]) {
  if (item.key === 'home') {
    return auth.role ? route.path === roleDashboards[auth.role] : false
  }
  return false
}

function navigateTo(routeFn: () => string) {
  router.push(routeFn())
}

function goHome() {
  router.push(dashboardRoute())
}

const userInitial = computed(() => auth.role?.charAt(0) ?? 'U')

const greeting = computed(() => {
  if (auth.role === 'ADMIN') return 'Admin'
  if (auth.role === 'TRAINER') return 'Profissional'
  return 'Aluno'
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
</style>
