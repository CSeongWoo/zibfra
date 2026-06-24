<template>
  <Transition name="slide-up">
    <!-- 로그인/로그아웃 관계없이 항상 떠 있도록 v-if 제거, 또는 v-show="true" (애니메이션 위해) -->
    <div
      class="flex items-center justify-between w-full py-4 px-6 bg-white transition-colors duration-300"
    >
      <!-- 로그인 상태 -->
      <div v-if="authStore.isAuthenticated && authStore.currentUser" class="flex items-center gap-3 w-full">
        <!-- Avatar Placeholder or Image -->
        <div class="flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full bg-info/10 text-info border border-info/20 overflow-hidden">
          <img v-if="authStore.currentUser.profileImageUrl" :src="authStore.currentUser.profileImageUrl" alt="Profile Image" class="w-full h-full object-cover" />
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>

        <!-- User Info -->
        <div class="flex flex-col flex-1 min-w-0">
          <span class="text-[15px] font-bold text-primary truncate leading-tight">
            {{ authStore.currentUser.nickname }}
          </span>
          <span class="text-[12px] text-primary/60 truncate leading-tight mt-0.5">
            {{ authStore.currentUser.email }}
          </span>
        </div>

        <!-- Actions -->
        <div class="flex items-center gap-1 relative" ref="dropdownRef">
          <!-- Settings Button -->
          <button
            @click="toggleDropdown"
            class="p-2 text-primary/40 hover:text-primary hover:bg-primary/5 rounded-full transition-colors focus:outline-none"
            title="설정"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </button>

          <!-- Dropdown Menu -->
          <Transition name="dropdown">
            <div v-if="isDropdownOpen" class="absolute bottom-full right-[-6px] mb-1.5 w-36 bg-white border border-primary/10 shadow-level-2 rounded-2xl overflow-hidden z-50">
              <div class="flex flex-col py-1">
                <!-- 설정 -->
                <button
                  @click="openProfileModal"
                  class="flex items-center w-full px-3 py-2.5 text-[13px] font-medium text-primary hover:bg-slate-50 transition-colors"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-2.5 text-primary/60"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                  설정
                </button>
                
                <div class="h-px bg-slate-100 my-1 mx-2"></div>
                
                <!-- 로그아웃 -->
                <button
                  @click="handleLogout"
                  class="flex items-center w-full px-3 py-2.5 text-[13px] font-medium text-info hover:bg-slate-50 transition-colors"
                  :disabled="authStore.loading"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-2.5 text-info"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                  로그아웃
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <!-- 비로그인 상태 -->
      <div v-else class="flex items-center gap-3 w-full cursor-pointer" @click="goToLogin">
        <div class="flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full bg-slate-100 text-slate-400 border border-slate-200">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <div class="flex flex-col flex-1 min-w-0">
          <span class="text-[15px] font-bold text-primary truncate leading-tight">
            로그인 해주세요
          </span>
          <span class="text-[12px] text-info hover:underline truncate leading-tight mt-0.5">
            로그인 및 회원가입 가기
          </span>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Profile Modal Component (Teleported to body to escape z-index context) -->
  <Teleport to="body">
    <ProfileModal :isOpen="isProfileModalOpen" @close="isProfileModalOpen = false" />
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import ProfileModal from '@/components/auth/ProfileModal.vue';

const authStore = useAuthStore();
const router = useRouter();

const isProfileModalOpen = ref(false);
const isDropdownOpen = ref(false);
const dropdownRef = ref(null);

function toggleDropdown() {
  isDropdownOpen.value = !isDropdownOpen.value;
}

function openProfileModal() {
  isDropdownOpen.value = false;
  isProfileModalOpen.value = true;
}

function goToLogin() {
  router.push('/login');
}

async function handleLogout() {
  isDropdownOpen.value = false;
  try {
    await authStore.logout();
    router.push('/');
  } catch (error) {
    console.error('Logout failed', error);
  }
}

function handleClickOutside(event) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    isDropdownOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.5s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* Dropdown Animation */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.1s ease-out, transform 0.1s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: bottom right;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}
</style>
