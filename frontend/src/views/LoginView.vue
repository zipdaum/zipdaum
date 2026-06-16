<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'
import { saveAuth } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const authResponse = await login({
      email: email.value,
      password: password.value
    })

    saveAuth(authResponse)

    await router.replace(getRedirectPath())
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '로그인 요청을 처리하지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

function getRedirectPath() {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/'
}
</script>

<template>
  <main class="login-page">
    <section class="login-card" aria-labelledby="login-title">
      <a class="brand" href="/">
        <span class="brand-mark">Z</span>
        <span>집다움</span>
      </a>

      <div class="login-heading">
        <h1 id="login-title">로그인</h1>
        <p>관심 지역과 맞춤 주거 정보를 확인하세요.</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label>
          <span>이메일</span>
          <input
            v-model.trim="email"
            type="email"
            name="email"
            autocomplete="email"
            maxlength="100"
            placeholder="user@example.com"
            required
          />
        </label>

        <label>
          <span>비밀번호</span>
          <input
            v-model="password"
            type="password"
            name="password"
            autocomplete="current-password"
            minlength="8"
            maxlength="100"
            placeholder="비밀번호를 입력해주세요"
            required
          />
        </label>

        <p v-if="errorMessage" class="form-message error-message" role="alert">
          {{ errorMessage }}
        </p>

        <button class="login-button" type="submit" :disabled="isLoading">
          {{ isLoading ? '로그인 중' : '로그인' }}
        </button>
      </form>

      <p class="signup-link">
        아직 회원이 아니신가요?
        <a href="#">회원가입</a>
      </p>
    </section>
  </main>
</template>

<style scoped src="../assets/login.css"></style>
