<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import { deleteUserInfo, getUserInfo } from "../api/user";
import { getUserPreferences } from "../api/preference";
import { getRecentProperties } from "../api/recent";
import { clearAuth, currentUser } from "../stores/auth";

const router = useRouter();

const userInfo = ref(null);
const preferences = ref([]);
const recentProperties = ref([]);
const isLoading = ref(false);
const hasLoadedMyPage = ref(false);
const errorMessage = ref("");
const deleteTextInput = ref("");
const deleteErrorMessage = ref("");
const isDeleteConfirmOpen = ref(false);
const isDeleting = ref(false);

const MAX_PREFERENCE_SUMMARY_ITEMS = 5;
const DELETE_CONFIRMATION_PREFIX = "delete/";

const displayedUser = computed(() => userInfo.value || currentUser.value || {});
const deleteTargetName = computed(() => displayedUser.value.name || "");
const deleteConfirmationText = computed(() =>
  deleteTargetName.value ? `${DELETE_CONFIRMATION_PREFIX}${deleteTargetName.value}` : "",
);
const canRequestDeletion = computed(
  () =>
    deleteTargetName.value.length > 0
    && deleteTextInput.value === deleteConfirmationText.value
    && !isDeleting.value,
);
const preferenceSummary = computed(() =>
  createPreferenceSummary(preferences.value).slice(0, MAX_PREFERENCE_SUMMARY_ITEMS),
);
const preferenceCount = computed(() => preferences.value.length);
const recentPreviewProperties = computed(() => recentProperties.value.slice(0, 6));

onMounted(() => {
  loadMyPage();
});

function goHome() {
  router.push({ name: "home" });
}

function openPreferenceSettings() {
  router.push({ name: "preferences" });
}

function openDeleteConfirm() {
  deleteTextInput.value = "";
  deleteErrorMessage.value = "";
  isDeleteConfirmOpen.value = true;
}

function closeDeleteConfirm() {
  if (isDeleting.value) {
    return;
  }

  deleteTextInput.value = "";
  deleteErrorMessage.value = "";
  isDeleteConfirmOpen.value = false;
}

async function requestUserDeletion() {
  if (!canRequestDeletion.value) {
    deleteErrorMessage.value = "확인 문구를 정확히 입력해주세요.";
    return;
  }

  isDeleting.value = true;
  deleteErrorMessage.value = "";

  try {
    await deleteUserInfo({
      name: deleteTargetName.value,
      confirmationText: deleteTextInput.value,
    });

    clearAuth();
    await router.replace({
      name: "login",
      query: {
        reason: "account-deleted",
      },
    });
  } catch (error) {
    deleteErrorMessage.value =
      error.response?.data?.message || "회원 탈퇴 신청을 처리하지 못했습니다.";
  } finally {
    isDeleting.value = false;
  }
}

async function loadMyPage() {
  if (hasLoadedMyPage.value || isLoading.value) {
    return;
  }

  isLoading.value = true;
  errorMessage.value = "";

  const [userResult, preferenceResult, recentResult] = await Promise.allSettled([
    getUserInfo(),
    getUserPreferences(),
    getRecentProperties(),
  ]);

  if (userResult.status === "fulfilled") {
    userInfo.value = userResult.value;
  }

  if (preferenceResult.status === "fulfilled") {
    preferences.value = preferenceResult.value;
  } else if (preferenceResult.reason?.response?.status !== 404) {
    errorMessage.value = "맞춤 조건 정보를 불러오지 못했습니다.";
  } else {
    preferences.value = [];
  }

  if (recentResult.status === "fulfilled") {
    recentProperties.value = recentResult.value;
  } else if (!errorMessage.value) {
    errorMessage.value = "최근 본 주택 목록을 불러오지 못했습니다.";
  }

  if (userResult.status === "rejected" && !errorMessage.value) {
    errorMessage.value = "내 정보를 불러오지 못했습니다.";
  }

  hasLoadedMyPage.value =
    userResult.status === "fulfilled"
    && (preferenceResult.status === "fulfilled" || preferenceResult.reason?.response?.status === 404)
    && recentResult.status === "fulfilled";
  isLoading.value = false;
}

function openPropertyDetail(propertyId) {
  if (!propertyId) {
    return;
  }

  router.push({
    name: "home",
    query: {
      view: "detail",
      propertyId,
    },
  });
}

function createPreferenceSummary(items) {
  const sortedPreferences = items
    .slice()
    .sort((left, right) => Number(left.priority || 0) - Number(right.priority || 0));
  const regionValues = sortedPreferences
    .filter((preference) => preference.code === "REGION")
    .map((preference) => formatPreferenceValue(preference));
  const summaries = sortedPreferences
    .filter((preference) => preference.code !== "REGION")
    .map(mapPreferenceSummary);

  if (regionValues.length === 0) {
    return summaries;
  }

  const firstRegionIndex = sortedPreferences.findIndex(
    (preference) => preference.code === "REGION",
  );
  const regionSummary = {
    code: "REGION",
    label: "지역",
    value: regionValues.join(", "),
  };
  const insertIndex = Math.min(firstRegionIndex, summaries.length);
  summaries.splice(insertIndex, 0, regionSummary);

  return summaries;
}

function mapPreferenceSummary(preference) {
  return {
    code: preference.code,
    label: getPreferenceLabel(preference),
    value: formatPreferenceValue(preference),
  };
}

function getPreferenceLabel(preference) {
  const labels = {
    SALE_PRICE: "매매가",
    DEPOSIT: "보증금",
    MONTHLY_RENT: "월세",
    AREA: "면적",
    BUILD_YEAR: "건축연도",
    REGION: "선호지역",
    BUS: "버스",
    SUBWAY: "지하철",
    HOSPITAL: "병원",
    CCTV: "CCTV",
    PARK: "공원",
  };

  return preference.name || labels[preference.code] || preference.code;
}

function formatPreferenceValue(preference) {
  const value = preference.value;

  if (value === null || value === undefined || value === "") {
    return "-";
  }

  if (["SALE_PRICE", "DEPOSIT"].includes(preference.code)) {
    return `${formatKoreanMoney(Number(value))} 이하`;
  }

  if (preference.code === "MONTHLY_RENT") {
    return `${Number(value).toLocaleString()}만원 이하`;
  }

  if (preference.code === "AREA") {
    return `${Number(value).toLocaleString()}m² 이상`;
  }

  if (preference.code === "BUILD_YEAR") {
    return `${value}년 이후`;
  }

  if (["BUS", "SUBWAY", "HOSPITAL", "CCTV", "PARK"].includes(preference.code)) {
    return value === "true" ? "필요" : "제외";
  }

  return String(value);
}

function getPropertyTypeLabel(propertyType) {
  const labels = {
    APARTMENT: "아파트",
    VILLA: "빌라/다세대",
  };

  return labels[propertyType] || "주택";
}

function getRecentPropertyAddress(property) {
  return [property.umdNm, property.jibun].filter(Boolean).join(" ") || property.sggCd || "-";
}

function getRecentPropertyPrice(property) {
  if (property.latestSalePrice) {
    return `매매 ${formatPrice(property.latestSalePrice)}`;
  }

  if (property.latestDeposit || property.latestMonthlyRent) {
    return `전월세 ${formatRent(property.latestDeposit, property.latestMonthlyRent)}`;
  }

  return "최근 거래 정보 없음";
}

function formatViewCount(viewCount) {
  return `${Number(viewCount || 1).toLocaleString()}회 조회`;
}

function formatPrice(price) {
  if (!price) {
    return "-";
  }

  return formatKoreanMoney(Number(price));
}

function formatRent(deposit, monthlyRent) {
  return `${formatPrice(deposit)} / ${Number(monthlyRent || 0).toLocaleString()}만원`;
}

function formatKoreanMoney(value) {
  if (!value) {
    return "-";
  }

  const manwon = value >= 1000000 ? Math.round(value / 10000) : value;

  if (manwon >= 10000) {
    const hundredMillion = Math.floor(manwon / 10000);
    const rest = manwon % 10000;

    return rest > 0
      ? `${hundredMillion}억 ${rest.toLocaleString()}만원`
      : `${hundredMillion}억원`;
  }

  return `${manwon.toLocaleString()}만원`;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
</script>

<template>
  <main class="app-shell mypage-page">
    <AppHeader @home="goHome" />

    <p v-if="errorMessage" class="form-message" role="alert">
      {{ errorMessage }}
    </p>

    <p v-if="isLoading" class="empty-message loading-state-panel">
      <span class="loading-spinner" aria-hidden="true"></span>
      마이페이지 정보를 불러오는 중입니다.
    </p>

    <template v-else>
      <div class="mypage-layout">
        <div class="mypage-content">
          <section class="mypage-summary-grid" aria-label="마이페이지 요약">
            <article class="mypage-panel profile-panel" aria-labelledby="profile-title">
              <div class="panel-title-row">
                <div>
                  <h2 id="profile-title">내 정보</h2>
                </div>
                <div class="panel-title-actions">
                  <button class="danger-outline-button" type="button" @click="openDeleteConfirm">
                    회원 탈퇴
                  </button>
                </div>
              </div>

              <div class="profile-content">
                <div class="profile-avatar" aria-hidden="true">
                  <svg viewBox="0 0 48 48" focusable="false">
                    <circle cx="24" cy="17" r="9" />
                    <path d="M9 40c2.3-8.3 8.1-13 15-13s12.7 4.7 15 13" />
                  </svg>
                </div>
                <dl>
                  <div>
                    <dt>이름</dt>
                    <dd>{{ displayedUser.name || "-" }}</dd>
                  </div>
                  <div>
                    <dt>이메일</dt>
                    <dd>{{ displayedUser.email || "-" }}</dd>
                  </div>
                </dl>
              </div>
            </article>

            <article class="mypage-panel preference-panel" aria-labelledby="preference-title">
              <div class="panel-title-row">
                <div>
                  <h2 id="preference-title">맞춤 조건</h2>
                </div>
                <div class="panel-title-actions">
                  <button class="secondary-button" type="button" @click="openPreferenceSettings">
                    설정 변경
                  </button>
                </div>
              </div>

              <p v-if="preferenceCount === 0" class="empty-message compact-empty-message">
                설정한 맞춤 조건이 없습니다.
              </p>

              <dl v-else class="preference-summary-list">
                <div
                  v-for="preference in preferenceSummary"
                  :key="preference.code"
                >
                  <dt>{{ preference.label }}</dt>
                  <dd>{{ preference.value }}</dd>
                </div>
              </dl>
            </article>
          </section>

          <section class="mypage-panel recent-panel" aria-labelledby="recent-title">
            <div class="panel-title-row">
              <div>
                <h2 id="recent-title">최근 본 주택</h2>
              </div>
              <span>{{ recentProperties.length.toLocaleString() }}개</span>
            </div>

            <p v-if="recentPreviewProperties.length === 0" class="empty-message">
              최근 본 주택이 없습니다.
            </p>

            <div v-else class="recent-property-grid">
              <button
                v-for="property in recentPreviewProperties"
                :key="property.propertyId"
                class="recent-property-card"
                type="button"
                @click="openPropertyDetail(property.propertyId)"
              >
                <span class="home-image recent-property-image" aria-hidden="true"></span>
                <span class="recent-property-visit">
                  {{ formatViewCount(property.viewCount) }}
                </span>
                <span class="recent-property-info">
                  <strong>{{ property.name || "주택명 미상" }}</strong>
                  <em>{{ getPropertyTypeLabel(property.propertyType) }} · {{ getRecentPropertyAddress(property) }}</em>
                  <small>{{ getRecentPropertyPrice(property) }}</small>
                </span>
                <span class="recent-property-meta">
                  {{ formatDateTime(property.viewedAt) }}
                </span>
              </button>
            </div>
          </section>

          <div class="account-delete-entry">
            <button class="danger-outline-button" type="button" @click="openDeleteConfirm">
              회원 탈퇴
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="isDeleteConfirmOpen"
        class="delete-confirm-backdrop"
        role="presentation"
        @click.self="closeDeleteConfirm"
      >
        <section
          class="delete-confirm-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="account-delete-title"
        >
          <div class="delete-dialog-header">
            <span class="delete-dialog-icon" aria-hidden="true">!</span>
            <div>
              <h2 id="account-delete-title">회원 탈퇴</h2>
              <p>계정을 비활성화하기 전에 본인 확인이 필요합니다.</p>
            </div>
          </div>

          <div class="account-delete-description">
            <strong>탈퇴 신청 후 계정은 바로 비활성화됩니다.</strong>
            <span>회원 정보는 신청 시점으로부터 2주 뒤 완전히 삭제됩니다.</span>
          </div>

          <form class="account-delete-form" @submit.prevent="requestUserDeletion">
            <label>
              <span class="delete-confirm-copy">
                <span>아래 문구를 입력해주세요</span>
              </span>
              <input
                v-model.trim="deleteTextInput"
                type="text"
                autocomplete="off"
                :placeholder="deleteConfirmationText || 'delete/이름'"
                :disabled="isDeleting"
                required
              />
            </label>

            <p v-if="deleteErrorMessage" class="form-message" role="alert">
              {{ deleteErrorMessage }}
            </p>

            <div class="delete-confirm-actions">
              <button class="secondary-button" type="button" :disabled="isDeleting" @click="closeDeleteConfirm">
                취소
              </button>
              <button class="danger-button" type="submit" :disabled="!canRequestDeletion">
                {{ isDeleting ? "탈퇴 신청 중" : "탈퇴 신청" }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </template>
  </main>
</template>

<style scoped src="../assets/mypage.css"></style>
