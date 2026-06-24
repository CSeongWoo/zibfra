<template>
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 w-full h-full bg-slate-900/40 flex justify-center items-center z-[9999]" @click.self="$emit('close')">
      <Transition name="slide-up">
        <Card class="relative w-full max-w-[440px] m-4 !shadow-2xl border border-slate-100 bg-white rounded-2xl">
          <!-- Close Button -->
          <button class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-700 transition-colors cursor-pointer border-none" @click="$emit('close')" aria-label="Close modal">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>

          <!-- Title -->
          <div class="text-center mb-6 mt-2">
            <h2 class="text-[22px] font-extrabold text-slate-800 tracking-tight mb-1">내 프로필 설정</h2>
            <p class="text-[13px] text-slate-500 font-medium">나만의 프로필로 집프라를 더 멋지게 이용해보세요!</p>
          </div>

          <!-- Error/Success Messages -->
          <Transition name="fade">
            <div v-if="errorMessage" class="p-3.5 rounded-lg text-[13px] mb-5 font-medium bg-red-50 border border-red-100 text-red-600 flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 shrink-0"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>
          <Transition name="fade">
            <div v-if="successMessage" class="p-3.5 rounded-lg text-[13px] mb-5 font-medium bg-emerald-50 border border-emerald-100 text-emerald-600 flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 shrink-0"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
              <span>{{ successMessage }}</span>
            </div>
          </Transition>

          <!-- Update Form -->
          <form @submit.prevent="handleUpdate" class="flex flex-col gap-5">
            <!-- Avatar Section -->
            <div class="flex flex-col items-center mb-2">
              <input type="file" ref="fileInput" accept="image/*" class="hidden" @change="onFileSelected" />
              
              <div 
                class="relative w-24 h-24 rounded-full shadow-lg border-4 border-white overflow-hidden bg-slate-100 flex justify-center items-center mb-3 group cursor-pointer"
                @click="triggerFileInput"
              >
                <img v-if="previewImageUrl" :src="previewImageUrl" alt="Profile Preview" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" @error="handleImageError" />
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="text-slate-400">
                  <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <!-- Overlay for hover effect -->
                <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>
                </div>
              </div>

              <div class="flex gap-3">
                <button type="button" @click="triggerFileInput" class="text-[12px] font-bold text-primary bg-primary/10 hover:bg-primary/20 px-3 py-1.5 rounded-md transition-colors border-none cursor-pointer">
                  사진 변경
                </button>
                <button type="button" v-if="previewImageUrl" @click="removeProfileImage" class="text-[12px] font-bold text-slate-500 bg-slate-100 hover:bg-slate-200 px-3 py-1.5 rounded-md transition-colors border-none cursor-pointer">
                  삭제
                </button>
              </div>
            </div>

            <!-- Nickname Field -->
            <Input
              label="닉네임"
              id="nickname"
              v-model="form.nickname"
              type="text"
              placeholder="변경할 닉네임을 입력하세요"
              minlength="2"
              maxlength="20"
              autocomplete="off"
            />

            <!-- Password Field -->
            <div class="flex flex-col gap-1.5">
              <Input
                label="새 비밀번호 (선택)"
                id="password"
                v-model="form.password"
                type="password"
                placeholder="변경하려면 입력하세요"
                autocomplete="new-password"
              />
              <span class="text-[11px] text-slate-400 font-medium pl-1">
                8자 이상, 영문, 숫자, 특수문자를 각각 최소 1자 이상 포함해야 합니다.
              </span>
            </div>

            <!-- Submit Button -->
            <Button variant="primary" type="submit" class="w-full mt-2 py-3.5 rounded-xl text-[15px] font-bold shadow-md hover:shadow-lg transition-all flex justify-center items-center" :disabled="authStore.loading">
              <span v-if="authStore.loading && !isDeactivating" class="w-5 h-5 border-2 border-white/30 rounded-full border-t-white animate-spin"></span>
              <span v-else>변경사항 저장하기</span>
            </Button>
          </form>

          <div class="relative flex items-center py-6">
            <div class="flex-grow border-t border-slate-100"></div>
            <span class="shrink-0 px-4 text-slate-300 text-[11px] font-semibold tracking-wider">DANGER ZONE</span>
            <div class="flex-grow border-t border-slate-100"></div>
          </div>

          <!-- Danger Zone: Deactivate Account -->
          <div class="flex justify-between items-center bg-red-50/50 p-4 rounded-xl border border-red-100 transition-colors hover:bg-red-50">
            <div>
              <h3 class="text-[14px] font-bold text-red-700 m-0 flex items-center gap-1.5">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                회원 탈퇴
              </h3>
              <p class="text-[11px] text-red-600/70 font-medium mt-1 m-0">탈퇴 시 모든 정보가 비활성화됩니다.</p>
            </div>
            <button 
              type="button" 
              class="bg-white hover:bg-red-50 text-red-600 border border-red-200 rounded-lg px-4 py-2 text-[13px] font-bold cursor-pointer transition-colors shadow-sm"
              @click="handleDeactivate"
              :disabled="authStore.loading"
            >
              <span v-if="isDeactivating" class="w-4 h-4 border-2 border-red-200 rounded-full border-t-red-600 animate-spin inline-block align-middle"></span>
              <span v-else>탈퇴하기</span>
            </button>
          </div>
        </Card>
      </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive, watch, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Card from '@/components/common/Card.vue';

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(['close']);

const authStore = useAuthStore();

const errorMessage = ref('');
const successMessage = ref('');
const isDeactivating = ref(false);

const fileInput = ref(null);
const previewImageUrl = ref('');

const form = reactive({
  profileImageFile: null,
  deleteProfileImage: false,
  nickname: '',
  password: '',
});

watch(() => props.isOpen, (val) => {
  if (val) {
    resetForm();
  } else {
    clearBlobUrl();
  }
});

onUnmounted(() => {
  clearBlobUrl();
});

function clearBlobUrl() {
  if (previewImageUrl.value && previewImageUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewImageUrl.value);
  }
}

function resetForm() {
  form.profileImageFile = null;
  form.deleteProfileImage = false;
  form.nickname = authStore.currentUser?.nickname || '';
  form.password = '';
  
  clearBlobUrl();
  previewImageUrl.value = authStore.currentUser?.profileImageUrl || '';
  
  errorMessage.value = '';
  successMessage.value = '';
  isDeactivating.value = false;
  
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

function triggerFileInput() {
  fileInput.value?.click();
}

function onFileSelected(event) {
  const file = event.target.files[0];
  if (!file) return;

  // Basic validation (e.g. max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '이미지 크기는 5MB를 초과할 수 없습니다.';
    return;
  }

  form.profileImageFile = file;
  form.deleteProfileImage = false;
  
  clearBlobUrl();
  previewImageUrl.value = URL.createObjectURL(file);
  errorMessage.value = '';
}

function removeProfileImage() {
  form.profileImageFile = null;
  form.deleteProfileImage = true;
  clearBlobUrl();
  previewImageUrl.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

function handleImageError() {
  previewImageUrl.value = '';
}

function validatePassword(password) {
  if (!password) return true; // Optional field
  const minLength = 8;
  const hasLetter = /[A-Za-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~]/.test(password);
  return password.length >= minLength && hasLetter && hasDigit && hasSpecial;
}

async function handleUpdate() {
  errorMessage.value = '';
  successMessage.value = '';
  isDeactivating.value = false;

  const newNickname = form.nickname.trim();
  const newPassword = form.password;

  const nicknameChanged = newNickname && newNickname !== authStore.currentUser?.nickname;
  const fileChanged = form.profileImageFile !== null;
  const imageDeleted = form.deleteProfileImage === true && authStore.currentUser?.profileImageUrl;

  if (!nicknameChanged && !newPassword && !fileChanged && !imageDeleted) {
    errorMessage.value = '변경된 내용이 없습니다.';
    return;
  }

  if (newNickname && (newNickname.length < 2 || newNickname.length > 20)) {
    errorMessage.value = '닉네임은 2~20자 사이여야 합니다.';
    return;
  }

  if (newPassword && !validatePassword(newPassword)) {
    errorMessage.value = '비밀번호 규칙을 만족해야 합니다. (영문, 숫자, 특수문자 포함 8자 이상)';
    return;
  }

  try {
    const formData = new FormData();
    if (nicknameChanged) formData.append('nickname', newNickname);
    if (newPassword) formData.append('password', newPassword);
    
    if (fileChanged) {
      formData.append('profileImage', form.profileImageFile);
    } else if (imageDeleted) {
      formData.append('deleteProfileImage', 'true');
    }

    await authStore.updateProfile(formData);
    
    successMessage.value = '프로필 정보가 성공적으로 업데이트되었습니다.';
    form.password = ''; 
    form.profileImageFile = null;
    form.deleteProfileImage = false;
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || '정보 수정에 실패했습니다.';
  }
}

async function handleDeactivate() {
  if (!confirm('정말 회원 탈퇴를 진행하시겠습니까? 탈퇴 시 모든 데이터가 비활성화되며 즉시 로그아웃됩니다.')) {
    return;
  }
  
  errorMessage.value = '';
  isDeactivating.value = true;
  
  try {
    await authStore.deactivateAccount();
    emit('close'); 
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || '회원 탈퇴 처리에 실패했습니다.';
    isDeactivating.value = false;
  }
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.4s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(24px) scale(0.98);
  opacity: 0;
}
</style>
