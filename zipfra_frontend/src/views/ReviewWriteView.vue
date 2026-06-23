<template>
  <div class="flex justify-center items-center h-[calc(100vh-64px)] w-full bg-background animate-fade-in">
    <Card class="w-full max-w-[500px] text-center">
      <h2 class="text-[24px] font-bold text-primary mb-2">리뷰 작성 구역 (Protected)</h2>
      <p class="text-[15px] text-primary/60 mb-6">이 페이지는 로그인한 사용자만 접근할 수 있는 보호 구역입니다.</p>
      
      <div class="p-6 rounded-md font-semibold text-green-600 bg-green-50 border border-green-200">
        <p>인증이 검증되어 접근이 성공했습니다!</p>
      </div>
    </Card>
  </div>
</template>

<script setup>
import Card from '@/components/common/Card.vue';
</script>
