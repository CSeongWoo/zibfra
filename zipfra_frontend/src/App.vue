<template>
  <div class="app-container animate-fade-in">
    <!-- Header -->
    <header class="app-header glass-panel">
      <div class="logo">
        <span class="logo-icon">🏙️</span>
        <h1>Zipfra</h1>
        <span class="badge badge-info">Vue.js Build Base</span>
      </div>
      
      <div class="status-indicator">
        <span class="pulse-dot"></span>
        <span class="status-text">Build Status: <strong>READY</strong></span>
      </div>
    </header>

    <!-- Content Workspace -->
    <main class="workspace">
      <div class="welcome-card glass-panel animate-slide-up">
        <h2>🎉 Vue.js 3 빌드 환경 구축 완료</h2>
        <p class="subtitle">agent.md 및 rest-api-spec.md 구현을 위한 기본 빌드 환경이 정상적으로 생성되었습니다.</p>

        <!-- Tech Stack Badge Info -->
        <div class="stack-grid">
          <div class="stack-item glass-card">
            <span class="stack-icon">⚡</span>
            <h3>Vite 5</h3>
            <p>초고속 빌드 도구</p>
          </div>
          <div class="stack-item glass-card">
            <span class="stack-icon">🟢</span>
            <h3>Vue 3</h3>
            <p>Composition API 기반</p>
          </div>
          <div class="stack-item glass-card">
            <span class="stack-icon">🍍</span>
            <h3>Pinia</h3>
            <p>전역 상태 관리 스토어</p>
          </div>
          <div class="stack-item glass-card">
            <span class="stack-icon">📡</span>
            <h3>Axios</h3>
            <p>HTTP 비동기 통신 라이브러리</p>
          </div>
        </div>

        <!-- Spec Implementation Checklist -->
        <div class="checklist-section">
          <h3>📌 agent.md 요구사항 매핑 상태</h3>
          <ul class="checklist">
            <li class="checked">
              <span class="check-icon">✓</span>
              <div>
                <strong>Vite + Vue 3 빌드 시스템 구성</strong>
                <p>package.json, vite.config.js, index.html 설정 완료</p>
              </div>
            </li>
            <li class="checked">
              <span class="check-icon">✓</span>
              <div>
                <strong>Global CSS 스타일링 가이드라인</strong>
                <p>index.css에 다크모드, 글래스모피즘, 폼 인풋 및 버튼 테마 토큰 정의 완료</p>
              </div>
            </li>
            <li class="checked">
              <span class="check-icon">✓</span>
              <div>
                <strong>Axios API 통신 규격 준비</strong>
                <p>Vite dev server를 활용한 백엔드 프록시 (/api/v1) 설정 완료</p>
              </div>
            </li>
          </ul>
        </div>

        <!-- Footer actions -->
        <div class="card-footer">
          <p class="info-note">이제 로컬에서 <code>pnpm dev</code>를 통해 개발 서버를 가동하거나 <code>pnpm build</code>를 실행할 수 있습니다.</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
// Ready for component scripts
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
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background-image: 
    radial-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 0);
  background-size: 24px 24px;
}

.welcome-card {
  max-width: 680px;
  width: 100%;
  border-radius: var(--radius-lg);
  padding: 36px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-card h2 {
  font-size: 24px;
  font-family: var(--font-heading);
  background: linear-gradient(135deg, #fff 0%, var(--text-secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.stack-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stack-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px;
  border-radius: var(--radius-md);
}

.stack-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.stack-item h3 {
  font-size: 14px;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.stack-item p {
  font-size: 11px;
  color: var(--text-secondary);
}

.checklist-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-top: 1px solid var(--border-color);
  padding-top: 20px;
}

.checklist-section h3 {
  font-size: 14px;
  color: var(--accent-color);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.checklist {
  list-style-type: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.checklist li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.check-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid var(--color-success);
  color: var(--color-success);
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
  flex-shrink: 0;
  margin-top: 2px;
}

.checklist li strong {
  font-size: 13px;
  color: var(--text-primary);
  display: block;
}

.checklist li p {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.card-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.info-note code {
  background: rgba(255, 255, 255, 0.05);
  color: var(--accent-color);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}
</style>
