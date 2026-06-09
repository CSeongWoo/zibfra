<template>
  <div class="review-write-container animate-fade-in">
    <div class="glass-panel main-panel">
      <h2>리뷰 작성 구역 (Protected)</h2>
      <p class="desc">이 페이지는 로그인한 사용자만 접근할 수 있는 보호 구역입니다.</p>
      <div class="card glass-card">
        <p>인증이 검증되어 접근이 성공했습니다!</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-write-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 64px);
  width: 100%;
  background-color: var(--bg-primary);
}

.main-panel {
  max-width: 500px;
  width: 100%;
  padding: 2.5rem;
  border-radius: var(--radius-lg);
  text-align: center;
}

.desc {
  color: var(--text-secondary);
  font-size: 0.95rem;
  margin-top: 0.5rem;
  margin-bottom: 1.5rem;
}

.card {
  padding: 1.5rem;
  border-radius: var(--radius-md);
  color: var(--color-success);
  font-weight: 600;
}
</style>
