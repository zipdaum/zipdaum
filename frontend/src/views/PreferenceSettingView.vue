<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import { getUserPreferences, saveUserPreferences } from "../api/preference";

const router = useRouter();

const form = ref(createDefaultForm());
const isLoading = ref(true);
const isSaving = ref(false);
const message = ref("");
const messageType = ref("error");

const selectedFacilityCount = computed(() =>
  Object.values(form.value.facilities).filter(Boolean).length,
);

onMounted(loadPreferences);

function goHome() {
  router.push({ name: "home" });
}

function goMyPage() {
  router.push({ name: "mypage" });
}

async function loadPreferences() {
  isLoading.value = true;
  message.value = "";

  try {
    const preferences = await getUserPreferences();
    form.value = toPreferenceForm(preferences);
  } catch (error) {
    if (error.response?.status !== 404) {
      showMessage("맞춤 조건을 불러오지 못했습니다.", "error");
    }
  } finally {
    isLoading.value = false;
  }
}

async function handleSavePreferences() {
  message.value = "";

  const payload = toPreferencePayload();

  if (payload.length === 0) {
    showMessage("저장할 맞춤 조건을 하나 이상 입력해주세요.", "error");
    return;
  }

  isSaving.value = true;

  try {
    await saveUserPreferences(payload);
    showMessage("맞춤 조건을 저장했습니다.", "success");
  } catch (error) {
    showMessage(getErrorMessage(error, "맞춤 조건을 저장하지 못했습니다."), "error");
  } finally {
    isSaving.value = false;
  }
}

function resetForm() {
  form.value = createDefaultForm();
  message.value = "";
}

function createDefaultForm() {
  return {
    salePrice: "",
    deposit: "",
    monthlyRent: "",
    area: "",
    buildYear: "",
    region: "",
    facilities: {
      BUS: false,
      SUBWAY: false,
      HOSPITAL: false,
      CCTV: false,
      PARK: false,
    },
  };
}

function toPreferenceForm(preferences) {
  const nextForm = createDefaultForm();

  for (const preference of preferences) {
    switch (preference.code) {
      case "SALE_PRICE":
        nextForm.salePrice = preference.value;
        break;
      case "DEPOSIT":
        nextForm.deposit = preference.value;
        break;
      case "MONTHLY_RENT":
        nextForm.monthlyRent = preference.value;
        break;
      case "AREA":
        nextForm.area = preference.value;
        break;
      case "BUILD_YEAR":
        nextForm.buildYear = preference.value;
        break;
      case "REGION":
        nextForm.region = preference.value;
        break;
      case "BUS":
      case "SUBWAY":
      case "HOSPITAL":
      case "CCTV":
      case "PARK":
        nextForm.facilities[preference.code] = preference.value === "true";
        break;
      default:
        break;
    }
  }

  return nextForm;
}

function toPreferencePayload() {
  const items = [];

  addPreference(items, "SALE_PRICE", form.value.salePrice);
  addPreference(items, "DEPOSIT", form.value.deposit);
  addPreference(items, "MONTHLY_RENT", form.value.monthlyRent);
  addPreference(items, "AREA", form.value.area);
  addPreference(items, "BUILD_YEAR", form.value.buildYear);
  addPreference(items, "REGION", form.value.region);

  for (const [code, checked] of Object.entries(form.value.facilities)) {
    if (checked) {
      addPreference(items, code, "true");
    }
  }

  return items.map((item, index) => ({
    ...item,
    priority: index + 1,
  }));
}

function addPreference(items, code, value) {
  const normalizedValue = String(value || "").trim();

  if (!normalizedValue) {
    return;
  }

  items.push({
    code,
    value: normalizedValue,
  });
}

function showMessage(text, type) {
  message.value = text;
  messageType.value = type;
}

function getErrorMessage(error, fallbackMessage) {
  return (
    error.response?.data?.message || error.response?.data || fallbackMessage
  );
}
</script>

<template>
  <main class="app-shell preference-page">
    <AppHeader @home="goHome" />

    <section class="preference-header" aria-labelledby="preference-setting-title">
      <div>
        <p>Preference Settings</p>
        <h1 id="preference-setting-title">맞춤 조건 설정</h1>
      </div>
      <button class="ghost-button" type="button" @click="goMyPage">
        마이페이지
      </button>
    </section>

    <p v-if="isLoading" class="empty-message">맞춤 조건을 불러오는 중입니다.</p>

    <form v-else class="preference-setting-layout" @submit.prevent="handleSavePreferences">
      <section class="preference-setting-panel" aria-labelledby="base-condition-title">
        <div class="panel-title-row">
          <div>
            <p class="result-kicker">Basic Conditions</p>
            <h2 id="base-condition-title">기본 조건</h2>
          </div>
        </div>

        <div class="preference-field-grid">
          <label>
            <span>매매가</span>
            <input
              v-model.trim="form.salePrice"
              inputmode="numeric"
              placeholder="예: 500000000"
              type="text"
            />
          </label>
          <label>
            <span>보증금</span>
            <input
              v-model.trim="form.deposit"
              inputmode="numeric"
              placeholder="예: 300000000"
              type="text"
            />
          </label>
          <label>
            <span>월세</span>
            <input
              v-model.trim="form.monthlyRent"
              inputmode="numeric"
              placeholder="예: 80"
              type="text"
            />
          </label>
          <label>
            <span>면적</span>
            <input
              v-model.trim="form.area"
              inputmode="decimal"
              placeholder="예: 84.5"
              type="text"
            />
          </label>
          <label>
            <span>건축연도</span>
            <input
              v-model.trim="form.buildYear"
              inputmode="numeric"
              placeholder="예: 2010"
              type="text"
            />
          </label>
        </div>
      </section>

      <section class="preference-setting-panel" aria-labelledby="region-condition-title">
        <div class="panel-title-row">
          <div>
            <p class="result-kicker">Region</p>
            <h2 id="region-condition-title">선호 지역</h2>
          </div>
        </div>

        <label class="preference-single-field">
          <span>지역명</span>
          <input
            v-model.trim="form.region"
            placeholder="예: 해운대구 중동"
            type="text"
          />
        </label>
      </section>

      <section class="preference-setting-panel" aria-labelledby="facility-condition-title">
        <div class="panel-title-row">
          <div>
            <p class="result-kicker">Facilities</p>
            <h2 id="facility-condition-title">생활 편의 조건</h2>
          </div>
          <span>{{ selectedFacilityCount.toLocaleString() }}개</span>
        </div>

        <div class="facility-toggle-grid">
          <label>
            <input v-model="form.facilities.BUS" type="checkbox" />
            <span>버스</span>
          </label>
          <label>
            <input v-model="form.facilities.SUBWAY" type="checkbox" />
            <span>지하철</span>
          </label>
          <label>
            <input v-model="form.facilities.HOSPITAL" type="checkbox" />
            <span>병원</span>
          </label>
          <label>
            <input v-model="form.facilities.CCTV" type="checkbox" />
            <span>CCTV</span>
          </label>
          <label>
            <input v-model="form.facilities.PARK" type="checkbox" />
            <span>공원</span>
          </label>
        </div>
      </section>

      <p
        v-if="message"
        class="preference-setting-message"
        :class="{ success: messageType === 'success' }"
        role="status"
      >
        {{ message }}
      </p>

      <div class="preference-setting-actions">
        <button class="ghost-button" type="button" @click="resetForm">
          초기화
        </button>
        <button class="primary-button" type="submit" :disabled="isSaving">
          {{ isSaving ? "저장 중" : "저장하기" }}
        </button>
      </div>
    </form>
  </main>
</template>

<style scoped src="../assets/preference-setting.css"></style>
