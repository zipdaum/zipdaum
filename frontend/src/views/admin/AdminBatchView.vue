<template>
  <div class="admin-batch-container">
    <h2>부동산 실거래가 수동 적재 (관리자)</h2>

    <div class="batch-control-panel">
      <div class="input-group">
        <label>수집 기간:</label>
        <input type="month" v-model="startMonth" class="month-input" />
        <span class="separator">~</span>
        <input type="month" v-model="endMonth" class="month-input" />
      </div>

      <button
        @click="triggerBatch"
        :disabled="isLoading || !isValidRange"
        class="run-btn"
      >
        {{ isLoading ? '실행 요청 중...' : '데이터 일괄 수집' }}
      </button>
    </div>

    <div v-if="startMonth && endMonth && !isValidRange" class="warning-text">
      종료 월은 시작 월과 같거나 미래여야 합니다.
    </div>

    <div v-if="message" :class="['message-box', messageType]">
      {{ message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';

// 상태 관리
const startMonth = ref(''); // 예: '2025-01'
const endMonth = ref('');   // 예: '2026-06'
const isLoading = ref(false);
const message = ref('');
const messageType = ref('');

// 💡 유효성 검사 (시작 월 <= 종료 월)
const isValidRange = computed(() => {
  if (!startMonth.value || !endMonth.value) return false;
  return startMonth.value <= endMonth.value;
});

// 배치 실행 함수
const triggerBatch = async () => {
  if (!isValidRange.value) return;

  // '2025-01' -> '202501' 형태로 변환
  const formattedStart = startMonth.value.replace('-', '');
  const formattedEnd = endMonth.value.replace('-', '');

  isLoading.value = true;
  message.value = '';

  try {
    // 💡 쿼리 파라미터로 전송
    const response = await axios.get(`http://localhost:8080/api/admin/batch/property/manual?startMonth=${formattedStart}&endMonth=${formattedEnd}`);

    messageType.value = 'success';
    message.value = response.data;
  } catch (error) {
    console.error('배치 실행 중 오류 발생:', error);
    messageType.value = 'error';
    message.value = error.response?.data || '서버 통신에 실패했습니다.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.admin-batch-container {
  max-width: 650px;
  margin: 40px auto;
  padding: 25px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.batch-control-panel {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 20px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.separator {
  font-weight: bold;
}

.month-input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.run-btn {
  padding: 10px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background-color 0.3s;
}

.run-btn:disabled {
  background-color: #a0c4ff;
  cursor: not-allowed;
}

.warning-text {
  color: #dc3545;
  font-size: 0.9em;
  margin-top: 5px;
}

.message-box {
  margin-top: 20px;
  padding: 12px;
  border-radius: 4px;
  font-weight: bold;
}

.success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
</style>