<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { runPropertyBatchByRange } from '../../api/admin'
import { clearAuth } from '../../stores/auth' // ⭐ 로그아웃 처리를 위해 가져오기

const router = useRouter()

const startMonth = ref('')
const endMonth = ref('')
const isLoading = ref(false)
const statusMessage = ref('')
const statusType = ref('')

const validStartMonth = computed(() => getValidMonth(startMonth.value))
const validEndMonth = computed(() => getValidMonth(endMonth.value))

const isValidRange = computed(() => {
  if (!validStartMonth.value || !validEndMonth.value) {
    return false
  }
  return validStartMonth.value <= validEndMonth.value
})

const canRunBatch = computed(
  () => Boolean(validStartMonth.value && validEndMonth.value && isValidRange.value && !isLoading.value),
)

const hasInvalidMonthFormat = computed(
  () =>
    (startMonth.value && !validStartMonth.value) ||
    (endMonth.value && !validEndMonth.value),
)

// ⭐ 로그아웃 함수 추가
function handleLogout() {
  if (confirm('로그아웃 하시겠습니까?')) {
    clearAuth() // 스토어(로컬스토리지)에서 토큰 및 유저 정보 삭제
    router.replace({ name: 'login' }) // 로그인 페이지로 이동
  }
}

async function triggerBatch() {
  if (!isValidRange.value) {
    return
  }

  isLoading.value = true
  statusMessage.value = ''
  statusType.value = ''

  try {
    await runPropertyBatchByRange({
      startMonth: validStartMonth.value,
      endMonth: validEndMonth.value,
    })
    statusMessage.value = `${validStartMonth.value} ~ ${validEndMonth.value} 기간의 데이터 수집이 완료되었습니다.`
    statusType.value = 'success'
  } catch (error) {
    statusMessage.value =
      error.response?.data?.message ||
      error.response?.data ||
      '배치 실행 요청을 처리하지 못했습니다.'
    statusType.value = 'error'
  } finally {
    isLoading.value = false
  }
}

function handleStartMonthInput(event) {
  startMonth.value = sanitizeMonthInput(event.target.value)
}

function handleEndMonthInput(event) {
  endMonth.value = sanitizeMonthInput(event.target.value)
}

function sanitizeMonthInput(value) {
  return value.replace(/\D/g, '').slice(0, 6)
}

function getValidMonth(value) {
  const digits = value.replace(/\D/g, '')

  if (digits.length !== 6) {
    return ''
  }

  const year = digits.slice(0, 4)
  const month = digits.slice(4, 6)
  const monthNumber = Number(month)

  if (monthNumber < 1 || monthNumber > 12) {
    return ''
  }

  return `${year}${month}`
}
</script>

<template>
  <main class="app-shell admin-batch-page">
    <header class="admin-header">
      <div class="admin-header-title">
        <span class="brand-mark">Z</span>
        <h1>집다움 관리자</h1>
      </div>
      <button @click="handleLogout" class="secondary-button logout-button">
        로그아웃
      </button>
    </header>

    <section class="admin-batch-grid" aria-label="배치 작업 설정">
      <article class="panel admin-control-panel">
        <div class="panel-title-row">
          <div>
            <h2>작업 설정</h2>
          </div>
          <span>{{ isLoading ? '실행 중' : '대기' }}</span>
        </div>

        <form class="admin-batch-form" @submit.prevent="triggerBatch">
          <div class="month-range-fields">
            <label>
              <span>시작 월</span>
              <input
                :value="startMonth"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="YYYYMM"
                required
                @input="handleStartMonthInput"
              />
            </label>

            <label>
              <span>종료 월</span>
              <input
                :value="endMonth"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="YYYYMM"
                required
                @input="handleEndMonthInput"
              />
            </label>
          </div>

          <p v-if="hasInvalidMonthFormat" class="form-message" role="alert">
            연월은 202604처럼 6자리 숫자로 입력해주세요.
          </p>

          <p
            v-else-if="startMonth && endMonth && !isValidRange"
            class="form-message"
            role="alert"
          >
            종료 월은 시작 월과 같거나 이후여야 합니다.
          </p>

          <button class="primary-button admin-run-button" type="submit" :disabled="!canRunBatch">
            {{ isLoading ? '실행 요청 중' : '데이터 수집 실행' }}
          </button>
        </form>
      </article>

      <aside class="panel admin-status-panel" aria-labelledby="admin-status-title">
        <div class="panel-title-row">
          <div>
            <h2 id="admin-status-title">작업 상태</h2>
          </div>
        </div>

        <div v-if="isLoading" class="admin-status-box loading" role="status">
          <span class="admin-loading-spinner" aria-hidden="true"></span>
          <div>
            <strong>데이터 수집 중입니다.</strong>
            <p>데이터 수집이 완료될 때까지 잠시만 기다려주세요.</p>
          </div>
        </div>

        <div v-else-if="statusMessage" :class="['admin-status-box', statusType]" role="status">
          <strong>{{ statusType === 'success' ? '데이터 수집 완료' : '요청 실패' }}</strong>
          <p>{{ statusMessage }}</p>
        </div>

        <p v-else class="empty-message compact-empty-message">
          아직 실행한 배치 작업이 없습니다.
        </p>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.admin-batch-page {
  display: grid;
  gap: 18px;
}

/* ⭐ 추가된 헤더 및 로그아웃 버튼 스타일 */
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2ebfb;
}

.admin-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-header-title .brand-mark {
  background: #0b5cff;
  color: #fff;
  font-weight: 900;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 16px;
}

.admin-header h1 {
  font-size: 18px;
  font-weight: 800;
  color: #17233f;
  margin: 0;
}

.logout-button {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 700;
  color: #53617c;
  background-color: #f1f5fb;
  border: 1px solid #d8e4fb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-button:hover {
  background-color: #e2ebfb;
  color: #17233f;
}

/* 기존 스타일 유지 */
.admin-batch-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.62fr) minmax(320px, 0.38fr);
  gap: 18px;
  align-items: start;
}

.admin-batch-form {
  display: grid;
  gap: 16px;
}

.admin-control-panel,
.admin-status-panel {
  min-height: 220px;
}

.month-range-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.month-range-fields label {
  display: grid;
  gap: 8px;
  color: #5e6b85;
  font-size: 13px;
  font-weight: 800;
}

.month-range-fields input {
  width: 100%;
  min-height: 42px;
  border: 1px solid #d8e4fb;
  border-radius: 6px;
  background: #ffffff;
  color: #14213d;
  padding: 0 12px;
}

.month-range-fields input:focus {
  border-color: #0b5cff;
  outline: 3px solid rgba(11, 92, 255, 0.14);
}

.admin-run-button {
  width: 100%;
  min-height: 42px;
}

.admin-run-button:disabled {
  border-color: #9fb8f2;
  background: #9fb8f2;
  cursor: default;
}

.admin-status-box {
  display: grid;
  gap: 8px;
  border: 1px solid #e2ebfb;
  border-radius: 8px;
  background: #f8fbff;
  padding: 18px;
}

.admin-status-box strong {
  color: #17233f;
  font-size: 16px;
  font-weight: 900;
}

.admin-status-box p {
  margin: 0;
  color: #53617c;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.6;
}

.admin-status-box.success {
  border-color: #caefd9;
  background: #f6fdf8;
}

.admin-status-box.success strong {
  color: #128a47;
}

.admin-status-box.error {
  border-color: #fed7d7;
  background: #fff5f5;
}

.admin-status-box.error strong {
  color: #c53030;
}

.admin-status-box.loading strong {
  color: #0b5cff;
}

.admin-status-box.loading {
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: start;
}

.admin-loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #dbe7ff;
  border-top-color: #0b5cff;
  border-radius: 999px;
  animation: admin-spin 0.8s linear infinite;
}

@keyframes admin-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .admin-batch-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .month-range-fields {
    grid-template-columns: 1fr;
  }
  
  .admin-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .logout-button {
    width: 100%;
  }
}
</style>