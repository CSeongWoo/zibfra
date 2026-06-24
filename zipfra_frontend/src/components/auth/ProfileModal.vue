<template>
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 w-full h-full bg-slate-900/40 backdrop-blur-sm flex justify-center items-center z-[9999]" @click.self="$emit('close')">
      <Transition name="slide-up">
        <Card class="relative w-full max-w-[440px] m-4 !shadow-level-2">
          <!-- Close Button -->
          <button class="absolute top-5 right-5 bg-transparent border-none text-[28px] text-primary/40 cursor-pointer leading-none transition-colors hover:text-primary" @click="$emit('close')" aria-label="Close modal">
            &times;
          </button>

          <!-- Title -->
          <div class="text-center mb-6 mt-2">
            <h2 class="text-[24px] font-bold text-primary mb-1">정보 수정</h2>
            <p class="text-[13px] text-primary/60">닉네임과 비밀번호를 변경할 수 있습니다.</p>
          </div>

          <!-- Error/Success Messages -->
          <Transition name="fade">
            <div v-if="errorMessage" class="p-3.5 rounded text-[13px] mb-5 leading-relaxed bg-red-50 border border-red-200 text-red-600">
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>
          <Transition name="fade">
            <div v-if="successMessage" class="p-3.5 rounded text-[13px] mb-5 leading-relaxed bg-green-50 border border-green-200 text-green-600">
              <span>{{ successMessage }}</span>
            </div>
          </Transition>

          <!-- Update Form -->
          <form @submit.prevent="handleUpdate" class="flex flex-col gap-4">
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
            <div class="flex flex-col gap-1">
              <Input
                label="새 비밀번호 (선택)"
                id="password"
                v-model="form.password"
                type="password"
                placeholder="변경하려면 입력하세요"
                autocomplete="new-password"
              />
              <span class="text-[11px] text-primary/50 leading-[1.4] mt-1">
                8자 이상, 영문, 숫자, 특수문자를 각각 최소 1자 이상 포함해야 합니다.
              </span>
            </div>

            <!-- Submit Button -->
            <Button variant="primary" type="submit" class="w-full mt-4 py-3 flex justify-center items-center" :disabled="authStore.loading">
              <span v-if="authStore.loading && !isDeactivating" class="w-5 h-5 border-2 border-white/30 rounded-full border-t-white animate-spin"></span>
              <span v-else>정보 저장</span>
            </Button>
          </form>

          <hr class="my-6 border-t border-primary/10" />

          <!-- Danger Zone: Deactivate Account -->
          <div class="flex justify-between items-center bg-red-50/50 p-4 rounded-lg border border-red-100">
            <div>
              <h3 class="text-[14px] font-bold text-red-700 m-0">회원 탈퇴</h3>
              <p class="text-[11px] text-red-600/80 mt-1 m-0">탈퇴 시 모든 정보가 비활성화됩니다.</p>
            </div>
            <button 
              type="button" 
              class="bg-red-500 hover:bg-red-600 text-white border-none rounded px-4 py-2 text-[12px] font-bold cursor-pointer transition-colors"
              @click="handleDeactivate"
              :disabled="authStore.loading"
            >
              <span v-if="isDeactivating" class="w-4 h-4 border-2 border-white/30 rounded-full border-t-white animate-spin inline-block align-middle"></span>
              <span v-else>탈퇴하기</span>
            </button>
          </div>
        </Card>
      </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
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

const form = reactive({
  nickname: '',
  password: '',
});

watch(() => props.isOpen, (val) => {
  if (val) {
    resetForm();
  }
});

function resetForm() {
  form.nickname = authStore.currentUser?.nickname || '';
  form.password = '';
  errorMessage.value = '';
  successMessage.value = '';
  isDeactivating.value = false;
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

  if (!newNickname && !newPassword) {
    errorMessage.value = '변경할 내용을 입력해주세요.';
    return;
  }

  if (newNickname && (newNickname.length < 2 || newNickname.length > 20)) {
    errorMessage.value = '닉네임은 2~20자 사이여야 합니다.';
    return;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (newNickname && emailRegex.test(newNickname)) {
    errorMessage.value = '닉네임으로 이메일 주소를 사용할 수 없습니다.';
    return;
  }

  if (newPassword && !validatePassword(newPassword)) {
    errorMessage.value = '비밀번호 규칙을 만족해야 합니다. (영문, 숫자, 특수문자 포함 8자 이상)';
    return;
  }

  try {
    await authStore.updateProfile(
      newNickname !== authStore.currentUser?.nickname ? newNickname : null,
      newPassword || null
    );
    successMessage.value = '정보가 성공적으로 변경되었습니다.';
    form.password = ''; // Clear password field
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || '정보 수정에 실패했습니다.';
  }
}

async function handleDeactivate() {
  if (!confirm('정말 회원 탈퇴를 진행하시겠습니까? 탈퇴 시 로그아웃됩니다.')) {
    return;
  }
  
  errorMessage.value = '';
  isDeactivating.value = true;
  
  try {
    await authStore.deactivateAccount();
    emit('close'); // Modal automatically closes because we are logged out, but emit anyway
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
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.35s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}
</style>
