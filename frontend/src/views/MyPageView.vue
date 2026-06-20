<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import { getUserInfo } from "../api/user";
import { getUserPreferences } from "../api/preference";
import { getRecentProperties } from "../api/recent";
import { currentUser } from "../stores/auth";

const router = useRouter();

const userInfo = ref(null);
const preferences = ref([]);
const recentProperties = ref([]);
const isLoading = ref(true);
const errorMessage = ref("");

const displayedUser = computed(() => userInfo.value || currentUser.value || {});
const preferenceSummary = computed(() =>
  preferences.value
    .slice()
    .sort((left, right) => Number(left.priority || 0) - Number(right.priority || 0))
    .map(mapPreferenceSummary),
);
const recentPreviewProperties = computed(() => recentProperties.value.slice(0, 6));

onMounted(loadMyPage);

function goHome() {
  router.push({ name: "home" });
}

function openPreferenceSettings() {
  router.push({ name: "preferences" });
}

async function loadMyPage() {
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
    return value === "true" ? "우선 고려" : "제외";
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

    <section class="mypage-header" aria-labelledby="mypage-title">
      <div>
        <p>My Page</p>
        <h1 id="mypage-title">내 정보와 주거 활동을 확인하세요</h1>
      </div>
    </section>

    <p v-if="errorMessage" class="form-message" role="alert">
      {{ errorMessage }}
    </p>

    <p v-if="isLoading" class="empty-message">마이페이지 정보를 불러오는 중입니다.</p>

    <template v-else>
      <section class="mypage-summary-grid" aria-label="마이페이지 요약">
        <article class="mypage-panel profile-panel" aria-labelledby="profile-title">
          <div class="panel-title-row">
            <div>
              <p class="result-kicker">Profile</p>
              <h2 id="profile-title">내 정보</h2>
            </div>
          </div>

          <div class="profile-content">
            <div class="profile-avatar" aria-hidden="true">
              {{ (displayedUser.name || "?").slice(0, 1) }}
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
              <p class="result-kicker">Preferences</p>
              <h2 id="preference-title">내 맞춤 조건 요약</h2>
            </div>
            <div class="panel-title-actions">
              <span>{{ preferenceSummary.length.toLocaleString() }}개</span>
              <button class="secondary-button" type="button" @click="openPreferenceSettings">
                조건 수정
              </button>
            </div>
          </div>

          <p v-if="preferenceSummary.length === 0" class="empty-message compact-empty-message">
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
            <p class="result-kicker">Recently Viewed</p>
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
            <span class="home-image recent-property-image" aria-hidden="true">
              <span>{{ property.viewCount || 1 }}</span>
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
    </template>
  </main>
</template>

<style scoped src="../assets/mypage.css"></style>
