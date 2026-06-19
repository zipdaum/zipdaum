<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import {
  getPropertyDealHistories as fetchPropertyDealHistories,
  getPropertyDetail as fetchPropertyDetail,
  getPropertyRecommendationScore as fetchPropertyRecommendationScore,
  getSurroundings as fetchSurroundings,
} from "../api/property";

const route = useRoute();
const router = useRouter();

const preferenceTypeLabels = {
  SALE_PRICE: "매매 예산",
  DEPOSIT: "전월세 보증금",
  MONTHLY_RENT: "월세",
  AREA: "면적",
  BUILD_YEAR: "건축연도",
  REGION: "선호 지역",
  BUS: "버스",
  SUBWAY: "지하철역",
  HOSPITAL: "병원",
  CCTV: "CCTV",
  PARK: "공원",
  RECENT_PROPERTY: "최근 본 주택",
};

const facilityRecommendationRadiusMeters = {
  BUS: 500,
  SUBWAY: 1000,
  HOSPITAL: 1500,
  CCTV: 500,
  PARK: 1000,
};

const property = ref(null);
const recommendationScore = ref(null);
const surroundings = ref(null);
const histories = ref(null);
const isLoading = ref(false);
const errorMessage = ref("");
const allowHomeNavigation = ref(false);
const conditionPage = ref(1);
const conditionPageSize = 3;

const propertyId = computed(() => Number(route.params.propertyId));
const recommendationConditions = computed(
  () => sortConditionsByPriority(recommendationScore.value?.conditions || []),
);
const paginatedRecommendationConditions = computed(() => {
  const startIndex = (conditionPage.value - 1) * conditionPageSize;
  return recommendationConditions.value.slice(
    startIndex,
    startIndex + conditionPageSize,
  );
});
const conditionTotalPages = computed(() =>
  Math.max(
    Math.ceil(recommendationConditions.value.length / conditionPageSize),
    1,
  ),
);
const conditionPageLabel = computed(
  () => `${conditionPage.value} / ${conditionTotalPages.value}`,
);
const recommendationScoreValue = computed(
  () => recommendationScore.value?.score ?? 0,
);
const recommendationScoreStyle = computed(() => ({
  "--score-progress": `${Math.min(
    Math.max(recommendationScoreValue.value, 0),
    100,
  )}%`,
}));
const latestDealArea = computed(() => {
  const deal = getRecentDeals().find((item) => item.exclusiveArea);
  return deal?.exclusiveArea ? formatArea(deal.exclusiveArea) : "면적 정보 없음";
});

onMounted(loadRecommendationScoreDetail);

watch(recommendationConditions, () => {
  conditionPage.value = 1;
});

watch(conditionTotalPages, (totalPages) => {
  if (conditionPage.value > totalPages) {
    conditionPage.value = totalPages;
  }
});

onBeforeRouteLeave((to) => {
  if (allowHomeNavigation.value || to.name !== "home") {
    return true;
  }

  if (to.query?.view === "detail" && to.query?.propertyId) {
    return true;
  }

  return {
    name: "home",
    query: {
      view: "detail",
      propertyId: String(propertyId.value),
    },
  };
});

async function loadRecommendationScoreDetail() {
  if (!Number.isInteger(propertyId.value) || propertyId.value <= 0) {
    errorMessage.value = "적합도 상세 정보를 불러올 주택을 찾지 못했습니다.";
    return;
  }

  isLoading.value = true;
  errorMessage.value = "";

  try {
    const [propertyDetail, scoreDetail] = await Promise.all([
      fetchPropertyDetail(propertyId.value),
      fetchPropertyRecommendationScore(propertyId.value),
    ]);

    property.value = propertyDetail;
    recommendationScore.value = scoreDetail;

    await Promise.all([loadSurroundings(), loadHistories()]);
  } catch (error) {
    const status = error.response?.status;
    errorMessage.value =
      status === 404
        ? "등록된 맞춤 조건이 없어 적합도 상세 정보를 불러오지 못했습니다."
        : "적합도 상세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    isLoading.value = false;
  }
}

async function loadSurroundings() {
  try {
    surroundings.value = await fetchSurroundings(propertyId.value, {
      radiusMeters: 1500,
    });
  } catch (error) {
    surroundings.value = null;
  }
}

async function loadHistories() {
  try {
    histories.value = await fetchPropertyDealHistories(propertyId.value, {
      salePage: 1,
      saleSize: 5,
      rentPage: 1,
      rentSize: 5,
      rentDealType: "JEONSE",
    });
  } catch (error) {
    histories.value = null;
  }
}

function goHome() {
  allowHomeNavigation.value = true;
  router.push({ name: "home" });
}

function getRecentDeals() {
  return [
    ...(histories.value?.saleDeals || []),
    ...(histories.value?.rentDeals || []),
  ].sort((left, right) => String(right.dealDate).localeCompare(left.dealDate));
}

function getRecommendationGrade(score = recommendationScore.value?.score) {
  if (score === null || score === undefined) {
    return "평가 대기";
  }

  if (score >= 85) {
    return "조건에 잘 맞아요";
  }

  if (score >= 70) {
    return "대체로 맞아요";
  }

  if (score >= 40) {
    return "일부 조건만 맞아요";
  }

  return "조건과 차이가 있어요";
}

function getRecommendationScoreLevelClass(score = recommendationScore.value?.score) {
  if (score === null || score === undefined) {
    return "score-muted";
  }

  if (score >= 85) {
    return "score-excellent";
  }

  if (score >= 70) {
    return "score-good";
  }

  if (score >= 40) {
    return "score-partial";
  }

  return "score-low";
}

function getRecommendationSummaryText() {
  if (
    recommendationScore.value?.recommendationStatus ===
    "NO_EVALUABLE_CONDITION"
  ) {
    return "평가 가능한 맞춤 조건이 없어 점수를 계산하지 않았습니다.";
  }

  const score = recommendationScore.value?.score;
  if (score >= 85) {
    return "예산, 위치, 주변시설 조건이 전반적으로 잘 맞는 주택입니다.";
  }

  if (score >= 70) {
    return "대부분의 기준에 맞아 우선 확인해볼 만합니다.";
  }

  if (score >= 40) {
    return "맞는 조건도 있지만 가격, 위치, 주변시설을 한 번 더 비교해보세요.";
  }

  return "현재 설정한 조건과 차이가 커서 다른 후보와 비교가 필요합니다.";
}

function getPreferenceTypeLabel(code) {
  return preferenceTypeLabels[code] || code || "조건";
}

function sortConditionsByPriority(conditions) {
  return [...conditions].sort((left, right) => {
    const leftPriority = Number.isFinite(Number(left.priority))
      ? Number(left.priority)
      : Number.MAX_SAFE_INTEGER;
    const rightPriority = Number.isFinite(Number(right.priority))
      ? Number(right.priority)
      : Number.MAX_SAFE_INTEGER;

    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority;
    }

    return String(left.code || "").localeCompare(String(right.code || ""));
  });
}

function setConditionPage(page) {
  conditionPage.value = Math.min(
    Math.max(page, 1),
    conditionTotalPages.value,
  );
}

function getConditionTitle(condition) {
  return condition.name || getPreferenceTypeLabel(condition.code);
}

function getConditionStatusIcon(condition) {
  if (condition.score >= 100) {
    return "✓";
  }

  if (condition.score >= 70) {
    return "?";
  }

  return "!";
}

function getConditionStatusClass(condition) {
  if (condition.score >= 100) {
    return "fit";
  }

  if (condition.score >= 70) {
    return "uncertain";
  }

  return "unfit";
}

function formatPreferenceValue(condition) {
  const value = condition.value;
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  switch (condition.code) {
    case "SALE_PRICE":
    case "DEPOSIT":
      return `${formatStoredMoney(value)} 이하`;
    case "MONTHLY_RENT":
      return `${formatMonthlyRent(value)} 이하`;
    case "AREA":
      return `${Number(value).toLocaleString()}㎡ 이상`;
    case "BUILD_YEAR":
      return `${value}년 이후`;
    case "BUS":
    case "SUBWAY":
    case "HOSPITAL":
    case "CCTV":
    case "PARK":
      return value === "true" ? "필요" : "제외";
    default:
      return value;
  }
}

function getConditionCurrentValue(condition) {
  if (!property.value) {
    return "주택 정보 없음";
  }

  switch (condition.code) {
    case "SALE_PRICE":
      return property.value.latestSalePrice > 0
        ? formatPrice(property.value.latestSalePrice)
        : "매매가 정보 없음";
    case "DEPOSIT":
      return property.value.latestDeposit > 0
        ? formatPrice(property.value.latestDeposit)
        : "보증금 정보 없음";
    case "MONTHLY_RENT":
      return property.value.latestMonthlyRent > 0
        ? `${property.value.latestMonthlyRent.toLocaleString()}만원`
        : "월세 정보 없음";
    case "AREA":
      return latestDealArea.value;
    case "BUILD_YEAR":
      return getBuildYearLabel(property.value.buildYear);
    case "REGION":
      return getPropertyAddress(property.value);
    case "BUS":
    case "SUBWAY":
    case "HOSPITAL":
    case "CCTV":
    case "PARK":
      return getFacilityConditionCurrentValue(condition.code);
    default:
      return "확인 필요";
  }
}

function getFacilityConditionCurrentValue(code) {
  if (!surroundings.value?.facilities) {
    return "주변시설 정보 없음";
  }

  const radiusMeters = facilityRecommendationRadiusMeters[code] || 1000;
  const count = surroundings.value.facilities.filter(
    (facility) =>
      facility.type === code && facility.distanceMeters <= radiusMeters,
  ).length;
  const label = getPreferenceTypeLabel(code);

  return `${formatDistance(radiusMeters)} 이내 ${label} ${count.toLocaleString()}곳`;
}

function getConditionDescription(condition) {
  return condition.matched
    ? "설정한 기준을 충족하거나 근접한 조건입니다."
    : "조건과 차이가 있어 함께 확인이 필요합니다.";
}

function getPropertyAddress(item) {
  return (
    [item?.umdNm, item?.jibun].filter(Boolean).join(" ") || item?.sggCd || "-"
  );
}

function getBuildYearLabel(buildYear) {
  return buildYear ? `${buildYear}년 준공` : "준공연도 미상";
}

function getDisplayPrice(item) {
  if (item.latestSalePrice > 0) {
    return formatPrice(item.latestSalePrice);
  }

  if (item.latestMonthlyRent > 0) {
    return `보증금 ${formatPrice(item.latestDeposit)} / 월세 ${item.latestMonthlyRent.toLocaleString()}만원`;
  }

  if (item.latestDeposit > 0) {
    return formatPrice(item.latestDeposit);
  }

  return "가격 정보 없음";
}

function getSummaryRows() {
  if (!property.value) {
    return [];
  }

  return [
    { label: "주택", value: property.value.name || "주택명 미상" },
    { label: "주소", value: getPropertyAddress(property.value) },
    { label: "가격", value: getDisplayPrice(property.value) },
    { label: "면적", value: latestDealArea.value },
  ];
}

function formatStoredMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return value;
  }

  const priceInManwon = amount >= 1000000 ? amount / 10000 : amount;
  return formatPrice(priceInManwon);
}

function formatMonthlyRent(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return value;
  }

  const rentInManwon = amount >= 10000 ? amount / 10000 : amount;
  return `${rentInManwon.toLocaleString()}만원`;
}

function formatPrice(price) {
  if (!price) {
    return "0만원";
  }

  if (price >= 10000) {
    const hundredMillion = price / 10000;
    return `${Number(hundredMillion.toFixed(1))}억원`;
  }

  return `${Number(price).toLocaleString()}만원`;
}

function formatArea(area) {
  if (!area) {
    return "-";
  }

  return `${Number(area).toLocaleString()}㎡`;
}

function formatDistance(distanceMeters) {
  if (distanceMeters >= 1000) {
    return `${Number((distanceMeters / 1000).toFixed(1))}km`;
  }

  return `${distanceMeters.toLocaleString()}m`;
}
</script>

<template>
  <main class="app-shell">
    <AppHeader @home="goHome" />

    <section v-if="isLoading" class="panel recommendation-loading-panel">
      <p class="compact-empty-message">적합도 상세 정보를 불러오고 있습니다.</p>
    </section>

    <section v-else-if="errorMessage" class="panel recommendation-loading-panel">
      <p class="form-message" role="alert">{{ errorMessage }}</p>
    </section>

    <template v-else-if="property && recommendationScore">
      <section class="detail-header history-page-header recommendation-page-header">
        <div>
          <p class="detail-breadcrumb">홈 &gt; 거래 상세 &gt; 적합도 상세</p>
          <h1>적합도 상세 보기</h1>
          <p>{{ property.name || "주택명 미상" }} · {{ getPropertyAddress(property) }}</p>
          <p class="recommendation-page-summary">
            {{ getRecommendationSummaryText() }}
          </p>
        </div>

        <aside
          class="recommendation-header-score"
          aria-label="적합도 요약"
        >
          <div
            :class="[
              'fit-score-circle',
              'recommendation-page-score-circle',
              getRecommendationScoreLevelClass(),
              {
                muted:
                  recommendationScore.recommendationStatus ===
                  'NO_EVALUABLE_CONDITION',
              },
            ]"
            :style="recommendationScoreStyle"
          >
            <strong>{{
              recommendationScore.score === null ? "-" : recommendationScore.score
            }}</strong>
            <span>점</span>
          </div>
          <dl class="recommendation-score-legend" aria-label="점수 색상 기준">
            <div>
              <dt><span class="score-legend-dot excellent"></span>85점 이상</dt>
              <dd>초록</dd>
            </div>
            <div>
              <dt><span class="score-legend-dot good"></span>70점 이상</dt>
              <dd>파랑</dd>
            </div>
            <div>
              <dt><span class="score-legend-dot partial"></span>40점 이상</dt>
              <dd>주황</dd>
            </div>
            <div>
              <dt><span class="score-legend-dot low"></span>40점 미만</dt>
              <dd>빨강</dd>
            </div>
            <div>
              <dt><span class="score-legend-dot muted"></span>평가 불가</dt>
              <dd>회색</dd>
            </div>
          </dl>
        </aside>
      </section>

      <section class="recommendation-detail-grid">
        <article class="panel recommendation-condition-panel">
          <div class="panel-title-row">
            <h2>내 조건과 비교</h2>
          </div>

          <p
            v-if="recommendationConditions.length === 0"
            class="compact-empty-message"
          >
            평가 가능한 맞춤 조건이 없습니다.
          </p>

          <div v-else class="recommendation-detail-list">
            <article
              v-for="condition in paginatedRecommendationConditions"
              :key="`${condition.code}-${condition.priority}`"
              :class="[
                'recommendation-detail-item',
                getConditionStatusClass(condition),
              ]"
            >
              <div class="recommendation-detail-item-header">
                <span class="fit-condition-icon" aria-hidden="true">
                  {{ getConditionStatusIcon(condition) }}
                </span>
                <div>
                  <strong>{{ getConditionTitle(condition) }}</strong>
                  <p>{{ getConditionDescription(condition) }}</p>
                </div>
              </div>

              <dl>
                <div>
                  <dt>사용자가 설정한 조건</dt>
                  <dd>{{ formatPreferenceValue(condition) }}</dd>
                </div>
                <div>
                  <dt>현재 이 주택</dt>
                  <dd>{{ getConditionCurrentValue(condition) }}</dd>
                </div>
              </dl>

            </article>
          </div>

          <div
            v-if="recommendationConditions.length > 0"
            class="condition-pagination"
            aria-label="맞춤 조건 페이지"
          >
            <button
              type="button"
              :disabled="conditionPage === 1"
              aria-label="이전 조건 보기"
              @click="setConditionPage(conditionPage - 1)"
            >
              이전
            </button>
            <span>{{ conditionPageLabel }}</span>
            <button
              type="button"
              :disabled="conditionPage === conditionTotalPages"
              aria-label="다음 조건 보기"
              @click="setConditionPage(conditionPage + 1)"
            >
              다음
            </button>
          </div>
        </article>

        <aside class="panel recommendation-property-panel">
          <div class="panel-title-row">
            <h2>주택 요약</h2>
          </div>
          <dl>
            <div v-for="row in getSummaryRows()" :key="row.label">
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>
        </aside>
      </section>
    </template>
  </main>
</template>
