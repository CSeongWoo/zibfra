import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import './index.css';
import { setAccessTokenProvider } from './api/http';
import { useAuthStore } from './stores/auth';
import router from './router';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// Axios 인터셉터(PR-1)가 auth 스토어의 토큰을 읽도록 연결.
// (Pinia 설치 후에 스토어를 인스턴스화해야 한다.)
const authStore = useAuthStore();
setAccessTokenProvider(() => authStore.accessToken);

app.mount('#app');
