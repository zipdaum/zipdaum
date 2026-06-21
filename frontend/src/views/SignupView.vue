<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { requestVerificationCode, signUp, verifyEmailCode } from '../api/auth'

const router = useRouter()

const email = ref('')
const verificationCode = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const emailInput = ref(null)
const verificationCodeInput = ref(null)
const nameInput = ref(null)
const isRequestingCode = ref(false)
const isVerifyingCode = ref(false)
const isSigningUp = ref(false)
const isEmailVerified = ref(false)
const isVerificationCodeVisible = ref(false)
const verifiedEmail = ref('')
const emailMessage = ref('')
const emailMessageType = ref('success')
const codeMessage = ref('')
const codeMessageType = ref('success')
const signUpErrorMessage = ref('')

const isPasswordMatched = computed(
  () => password.value.length > 0 && password.value === passwordConfirm.value
)
const canSubmit = computed(
  () =>
    isEmailVerified.value &&
    name.value.length > 0 &&
    isPasswordMatched.value &&
    !isSigningUp.value &&
    !isRequestingCode.value &&
    !isVerifyingCode.value
)

onMounted(() => {
  emailInput.value?.focus()
})

watch(email, () => {
  if (email.value !== verifiedEmail.value) {
    isEmailVerified.value = false
    isVerificationCodeVisible.value = false
    verificationCode.value = ''
    clearEmailMessage()
    clearCodeMessage()
    signUpErrorMessage.value = ''
  }
})

async function handleRequestCode() {
  clearEmailMessage()
  clearCodeMessage()
  signUpErrorMessage.value = ''
  isRequestingCode.value = true

  try {
    await requestVerificationCode({ email: email.value })
    verificationCode.value = ''
    isVerificationCodeVisible.value = true
    isEmailVerified.value = false
    setEmailMessage('인증코드를 보냈습니다. 메일함에서 6자리 숫자를 확인해주세요.')
    await nextTick()
    verificationCodeInput.value?.focus()
  } catch (error) {
    isVerificationCodeVisible.value = false
    setEmailMessage(getErrorMessage(error, '인증코드 발송을 처리하지 못했습니다.'), 'error')
  } finally {
    isRequestingCode.value = false
  }
}

async function handleVerifyCode() {
  signUpErrorMessage.value = ''
  isVerifyingCode.value = true

  try {
    await verifyEmailCode({
      email: email.value,
      code: verificationCode.value
    })
    isEmailVerified.value = true
    verifiedEmail.value = email.value
    setCodeMessage('인증이 완료되었습니다.')
    await nextTick()
    nameInput.value?.focus()
  } catch (error) {
    isEmailVerified.value = false
    setCodeMessage(getErrorMessage(error, '이메일 인증을 처리하지 못했습니다.'), 'error')
  } finally {
    isVerifyingCode.value = false
  }
}

async function handleSignUp() {
  signUpErrorMessage.value = ''

  if (!isEmailVerified.value) {
    isVerificationCodeVisible.value = true
    setCodeMessage('이메일 인증을 먼저 완료해주세요.', 'error')
    return
  }

  if (!isPasswordMatched.value) {
    signUpErrorMessage.value = '비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  isSigningUp.value = true

  try {
    await signUp({
      email: email.value,
      password: password.value,
      name: name.value
    })

    await router.replace({
      name: 'login',
      query: { reason: 'signup-success' }
    })
  } catch (error) {
    const message = getErrorMessage(error, '회원가입 요청을 처리하지 못했습니다.')

    if (message.includes('이메일')) {
      setEmailMessage(message, 'error')
      return
    }

    signUpErrorMessage.value = message
  } finally {
    isSigningUp.value = false
  }
}

function setEmailMessage(message, type = 'success') {
  emailMessage.value = message
  emailMessageType.value = type
}

function clearEmailMessage() {
  emailMessage.value = ''
  emailMessageType.value = 'success'
}

function setCodeMessage(message, type = 'success') {
  codeMessage.value = message
  codeMessageType.value = type
}

function clearCodeMessage() {
  codeMessage.value = ''
  codeMessageType.value = 'success'
}

function getErrorMessage(error, fallbackMessage) {
  return error.response?.data?.message || error.response?.data || fallbackMessage
}
</script>

<template>
  <main class="login-page">
    <section class="login-card signup-card" aria-labelledby="signup-title">
      <RouterLink class="brand" :to="{ name: 'home' }">
        <span class="brand-mark">Z</span>
        <span>집다움</span>
      </RouterLink>

      <div class="login-heading">
        <h1 id="signup-title">회원가입</h1>
        <p>이메일 인증 후 관심 지역과 맞춤 주거 정보를 이용할 수 있습니다.</p>
      </div>

      <form class="login-form" @submit.prevent="handleSignUp">
        <div class="form-action-row">
          <label>
            <span>이메일</span>
            <input
              ref="emailInput"
              v-model.trim="email"
              type="email"
              name="email"
              autocomplete="email"
              maxlength="100"
              placeholder="user@example.com"
              required
            />
          </label>

          <button
            class="sub-button"
            type="button"
            :disabled="isRequestingCode || !email"
            @click="handleRequestCode"
          >
            {{ isRequestingCode ? '발송 중' : '인증 코드 보내기' }}
          </button>
        </div>

        <p
          v-if="emailMessage"
          class="field-message"
          :class="emailMessageType === 'error' ? 'error' : 'success'"
          :role="emailMessageType === 'error' ? 'alert' : 'status'"
        >
          {{ emailMessage }}
        </p>

        <div v-if="isVerificationCodeVisible" class="form-action-row">
          <label>
            <span>인증번호</span>
            <input
              ref="verificationCodeInput"
              v-model.trim="verificationCode"
              type="text"
              name="verificationCode"
              inputmode="numeric"
              pattern="[0-9]{6}"
              maxlength="6"
              placeholder="6자리 숫자"
              required
            />
          </label>

          <button
            class="sub-button"
            type="button"
            :disabled="isVerifyingCode || !email || verificationCode.length !== 6"
            @click="handleVerifyCode"
          >
            {{ isVerifyingCode ? '인증 중' : '인증하기' }}
          </button>
        </div>

        <p
          v-if="isVerificationCodeVisible && codeMessage"
          class="field-message"
          :class="codeMessageType === 'error' ? 'error' : 'success'"
          :role="codeMessageType === 'error' ? 'alert' : 'status'"
        >
          {{ codeMessage }}
        </p>

        <label>
          <span>이름</span>
          <input
            ref="nameInput"
            v-model.trim="name"
            type="text"
            name="name"
            autocomplete="name"
            maxlength="30"
            placeholder="이름을 입력해주세요"
            required
          />
        </label>

        <label>
          <span>비밀번호</span>
          <input
            v-model="password"
            type="password"
            name="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="100"
            placeholder="8자 이상 입력해주세요"
            required
          />
        </label>

        <label>
          <span>비밀번호 확인</span>
          <input
            v-model="passwordConfirm"
            type="password"
            name="passwordConfirm"
            autocomplete="new-password"
            minlength="8"
            maxlength="100"
            placeholder="비밀번호를 다시 입력해주세요"
            required
          />
        </label>

        <p v-if="signUpErrorMessage" class="form-message error-message" role="alert">
          {{ signUpErrorMessage }}
        </p>

        <button class="login-button" type="submit" :disabled="!canSubmit">
          {{ isSigningUp ? '가입 중' : '회원가입' }}
        </button>
      </form>

      <p class="signup-link">
        이미 계정이 있으신가요?
        <RouterLink :to="{ name: 'login' }">로그인</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped src="../assets/login.css"></style>
