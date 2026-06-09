<template>
  <div class="app-container animate-fade-in">
    <!-- Header -->
    <header class="app-header glass-panel">
      <router-link to="/" class="logo" style="text-decoration: none; color: inherit; cursor: pointer;">
        <span class="logo-icon">🏙️</span>
        <h1>Zipfra</h1>
        <span class="badge badge-info">Map Base</span>
      </router-link>

      <div class="header-actions">
        <div class="status-indicator">
          <span class="pulse-dot"></span>
          <span class="status-text">
            Zoom <strong>{{ mapStore.zoom ?? '–' }}</strong> · {{ mapStore.strategy ?? 'IDLE' }}
          </span>
        </div>

        <div class="auth-section">
          <template v-if="authStore.isAuthenticated">
            <span class="user-greeting">
              안녕하세요, <strong>{{ authStore.currentUser?.nickname }}</strong>님
            </span>
            <button class="auth-btn logout-btn" @click="handleLogout" :disabled="authStore.loading">
              로그아웃
            </button>
          </template>
          <template v-else>
            <button class="auth-btn login-btn" @click="goToLogin">
              로그인 / 회원가입
            </button>
          </template>
        </div>
      </div>
    </header>

    <!-- Vue Router View -->
    <router-view />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useMapStore } from '@/stores/map';
import { useAuthStore } from '@/stores/auth';

const mapStore = useMapStore();
const authStore = useAuthStore();
const router = useRouter();

function goToLogin() {
  router.push('/login');
}

async function handleLogout() {
  try {
    await authStore.logout();
    router.push('/');
  } catch (error) {
    console.error('Logout failed:', error);
  }
}
</script>

<style>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background-color: var(--bg-primary);
}

.app-header {
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-color);
  z-index: 10;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 24px;
}

.logo h1 {
  font-size: 22px;
  font-family: var(--font-heading);
  letter-spacing: -0.5px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: var(--color-success);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--color-success);
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.workspace {
  flex: 1;
  position: relative;
}

.map-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 5;
  max-width: 340px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  font-size: 12px;
}

.map-overlay h3 {
  font-size: 12px;
  color: var(--accent-color);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.map-overlay dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
}

.map-overlay dt {
  color: var(--text-secondary);
}

.map-overlay dd {
  color: var(--text-primary);
  font-family: monospace;
  word-break: break-all;
}

.map-overlay .hint {
  margin-top: 10px;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}

/* Header button and user styles */
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.auth-section {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.user-greeting {
  color: var(--text-primary);
}

.auth-btn {
  padding: 6px 16px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.25s ease;
  outline: none;
}

.login-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  color: #ffffff;
}

.login-btn:hover {
  opacity: 0.95;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.logout-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}
</style>
