import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { definePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'
import 'primeicons/primeicons.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { setRouter } from './services/api'

const WachafitPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50:  '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}',
    },
    colorScheme: {
      light: {
        primary: {
          color:        '#4175F5',
          inverseColor: '#ffffff',
          hoverColor:   '#2D5BD8',
          activeColor:  '#1E48C0',
        },
        surface: {
          0:   '#ffffff',
          50:  '#F8F9FC',
          100: '#F0F2F8',
          200: '#E2E6EE',
          300: '#C8CEDB',
          400: '#8890A4',
          500: '#5A627A',
          600: '#2C3144',
          700: '#141928',
          800: '#0E1117',
          900: '#0A0E16',
          950: '#060810',
        },
      },
    },
  },
})

const app = createApp(App)

app.use(createPinia())
setRouter(router)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: WachafitPreset,
    options: {
      darkModeSelector: '.dark',
    },
  },
})

app.mount('#app')
