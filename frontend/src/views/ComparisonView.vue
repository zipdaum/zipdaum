<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import { getFavoriteProperties } from "../api/favorite";
import { comparePropertiesByAi, searchProperties } from "../api/property";

const router = useRouter();

const favoriteProperties = ref([]);
const propertySearchKeyword = ref("");
const propertySearchResults = ref([]);
const propertySearchMessage = ref("");
const comparisonSelection = ref([]);
const comparisonResult = ref(null);
const comparisonResultPanel = ref(null);
const comparisonErrorMessage = ref("");
const favoritePropertiesErrorMessage = ref("");
const hasLoadedFavoriteProperties = ref(false);
const isSearchingProperties = ref(false);
const isLoadingFavoriteProperties = ref(false);
const isComparing = ref(false);

const MAX_PROPERTY_SEARCH_RESULTS = 8;

const selectedPropertyIds = computed(() =>
  comparisonSelection.value.map((property) => property.propertyId),
);
const canRequestComparison = computed(
  () => selectedPropertyIds.value.length === 2 && !isComparing.value,
);
const comparisonRecommendationText = computed(() => {
  const recommendedProperty = comparisonResult.value?.recommendedProperty;
  if (recommendedProperty === "A" || recommendedProperty === "B") {
    return `AI는 ${recommendedProperty}를 더 추천합니다`;
  }
  return "AI는 판단을 보류합니다";
});

onMounted(() => {
  loadFavoriteProperties();
});

function goHome() {
  router.push({ name: "home" });
}

async function loadFavoriteProperties() {
  if (hasLoadedFavoriteProperties.value || isLoadingFavoriteProperties.value) {
    return;
  }

  isLoadingFavoriteProperties.value = true;
  favoritePropertiesErrorMessage.value = "";

  try {
    favoriteProperties.value = (await getFavoriteProperties()).map(mapFavoriteProperty);
    hasLoadedFavoriteProperties.value = true;
  } catch (error) {
    favoriteProperties.value = [];
    favoritePropertiesErrorMessage.value = "관심 주택을 불러오지 못했습니다.";
  } finally {
    isLoadingFavoriteProperties.value = false;
  }
}

function retryFavoriteProperties() {
  loadFavoriteProperties();
}

async function handlePropertySearch() {
  const keyword = propertySearchKeyword.value.trim();

  if (!keyword) {
    propertySearchMessage.value = "검색할 주택명이나 읍면동을 입력해주세요.";
    propertySearchResults.value = [];
    return;
  }

  isSearchingProperties.value = true;
  propertySearchMessage.value = "";

  try {
    const response = await searchProperties({
      name: keyword,
      sortBy: "NAME",
      sortDirection: "ASC",
      page: 1,
      size: MAX_PROPERTY_SEARCH_RESULTS,
    });
    const properties = Array.isArray(response) ? response : response.content || [];

    propertySearchResults.value = properties.map(mapSearchProperty);
    propertySearchMessage.value =
      propertySearchResults.value.length === 0 ? "검색 결과가 없습니다." : "";
  } catch (error) {
    propertySearchResults.value = [];
    propertySearchMessage.value = "주택 검색 결과를 불러오지 못했습니다.";
  } finally {
    isSearchingProperties.value = false;
  }
}

function toggleComparisonSelection(property) {
  comparisonResult.value = null;
  comparisonErrorMessage.value = "";

  if (isComparisonSelected(property.propertyId)) {
    comparisonSelection.value = comparisonSelection.value.filter(
      (selectedProperty) => selectedProperty.propertyId !== property.propertyId,
    );
    return;
  }

  if (comparisonSelection.value.length >= 2) {
    comparisonErrorMessage.value = "비교할 주택은 2개까지만 선택할 수 있습니다.";
    return;
  }

  comparisonSelection.value = [...comparisonSelection.value, property];
}

function isComparisonSelected(propertyId) {
  return selectedPropertyIds.value.includes(Number(propertyId));
}

function clearComparisonSelection() {
  comparisonSelection.value = [];
  comparisonResult.value = null;
  comparisonErrorMessage.value = "";
}

async function requestAiComparison() {
  if (!canRequestComparison.value) {
    comparisonErrorMessage.value = "비교할 주택 2개를 선택해주세요.";
    return;
  }

  isComparing.value = true;
  comparisonErrorMessage.value = "";
  comparisonResult.value = null;

  try {
    comparisonResult.value = await comparePropertiesByAi({
      propertyIds: selectedPropertyIds.value,
      comparisonPurpose: "사용자 선호 조건과 최근 본 주택을 참고한 실거주 관점 비교",
    });
    await nextTick();
    comparisonResultPanel.value?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  } catch (error) {
    comparisonErrorMessage.value =
      error.response?.data?.message || "AI 비교 결과를 생성하지 못했습니다.";
  } finally {
    isComparing.value = false;
  }
}

function getPropertyTypeLabel(propertyType) {
  const labels = {
    APARTMENT: "아파트",
    VILLA: "빌라/다세대",
  };

  return labels[propertyType] || "주택";
}

function getComparablePropertyAddress(property) {
  return (
    property.regionName ||
    [property.umdNm, property.jibun].filter(Boolean).join(" ") ||
    property.sggCd ||
    "-"
  );
}

function getComparablePropertyPrice(property) {
  if (property.salePrice || property.latestSalePrice) {
    return `매매 ${formatPrice(property.salePrice || property.latestSalePrice)}`;
  }

  if (property.jeonseDeposit) {
    return `전세 ${formatPrice(property.jeonseDeposit)}`;
  }

  if (property.monthlyRentDeposit || property.monthlyRent) {
    return `월세 ${formatRent(property.monthlyRentDeposit, property.monthlyRent)}`;
  }

  if (property.latestDeposit || property.latestMonthlyRent) {
    return `전월세 ${formatRent(property.latestDeposit, property.latestMonthlyRent)}`;
  }

  return "가격 정보 없음";
}

function getComparisonLabel(index) {
  return index === 0 ? "A 주택" : "B 주택";
}

function mapFavoriteProperty(property) {
  return {
    id: property.propertyId,
    propertyId: property.propertyId,
    name: property.name || "주택명 미상",
    propertyType: property.propertyType,
    sggCd: property.sggCd,
    umdNm: property.umdNm,
    regionName: property.regionName || [property.sggCd, property.umdNm].filter(Boolean).join(" "),
    salePrice: property.latestSalePrice,
    jeonseDeposit: property.latestJeonseDeposit,
    monthlyRentDeposit: property.latestMonthlyRentDeposit,
    monthlyRent: property.latestMonthlyRent,
    source: "관심목록",
  };
}

function mapSearchProperty(property) {
  return {
    id: property.id,
    propertyId: property.id,
    name: property.name || "주택명 미상",
    propertyType: property.propertyType,
    sggCd: property.sggCd,
    umdNm: property.umdNm,
    jibun: property.jibun,
    regionName: [property.umdNm, property.jibun].filter(Boolean).join(" ") || property.sggCd,
    latestSalePrice: property.latestSalePrice,
    latestDeposit: property.latestDeposit,
    latestMonthlyRent: property.latestMonthlyRent,
    source: "검색결과",
  };
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
</script>

<template>
  <main class="app-shell comparison-page">
    <AppHeader @home="goHome" />

    <div class="comparison-layout">
      <div class="comparison-content">
        <section class="compare-source-grid" aria-label="비교할 주택 선택">
          <article class="comparison-panel compare-list-panel favorite-compare-panel">
            <div class="panel-title-row">
              <div>
                <h2>관심목록</h2>
              </div>
              <span>{{ favoriteProperties.length.toLocaleString() }}개</span>
            </div>

            <p v-if="isLoadingFavoriteProperties" class="empty-message">관심 주택을 불러오는 중입니다.</p>
            <div v-else-if="favoritePropertiesErrorMessage" class="compare-load-error">
              <p class="empty-message">{{ favoritePropertiesErrorMessage }}</p>
              <button class="secondary-button" type="button" @click="retryFavoriteProperties">
                다시 시도
              </button>
            </div>
            <p v-else-if="favoriteProperties.length === 0" class="empty-message">관심목록에 담은 주택이 없습니다.</p>

            <div v-else class="compare-property-list">
              <button
                v-for="property in favoriteProperties"
                :key="`favorite-${property.propertyId}`"
                type="button"
                class="compare-property-card"
                :class="{ selected: isComparisonSelected(property.propertyId) }"
                @click="toggleComparisonSelection(property)"
              >
                <span>{{ property.source }}</span>
                <strong>{{ property.name }}</strong>
                <em>{{ getPropertyTypeLabel(property.propertyType) }} · {{ getComparablePropertyAddress(property) }}</em>
                <small>{{ getComparablePropertyPrice(property) }}</small>
              </button>
            </div>
          </article>

          <article class="comparison-panel compare-list-panel search-compare-panel">
            <div class="panel-title-row">
              <div>
                <h2>다른 집 찾아보기</h2>
              </div>
            </div>

            <form class="compare-search-form" @submit.prevent="handlePropertySearch">
              <input
                v-model.trim="propertySearchKeyword"
                type="search"
                placeholder="주택명으로 검색"
                autocomplete="off"
              />
              <button class="secondary-button" type="submit" :disabled="isSearchingProperties">
                {{ isSearchingProperties ? "검색 중" : "검색" }}
              </button>
            </form>

            <p v-if="propertySearchMessage" class="empty-message">{{ propertySearchMessage }}</p>

            <div v-if="propertySearchResults.length > 0" class="compare-property-list">
              <button
                v-for="property in propertySearchResults"
                :key="`search-${property.propertyId}`"
                type="button"
                class="compare-property-card"
                :class="{ selected: isComparisonSelected(property.propertyId) }"
                @click="toggleComparisonSelection(property)"
              >
                <span>{{ property.source }}</span>
                <strong>{{ property.name }}</strong>
                <em>{{ getPropertyTypeLabel(property.propertyType) }} · {{ getComparablePropertyAddress(property) }}</em>
                <small>{{ getComparablePropertyPrice(property) }}</small>
              </button>
            </div>
            <div v-else class="compare-property-list compare-empty-list" aria-hidden="true"></div>
          </article>
        </section>

        <section class="comparison-panel compare-control-panel" aria-labelledby="compare-title">
          <div class="panel-title-row">
            <div>
              <h2 id="compare-title">집 비교하기</h2>
              <p>선택한 주택 2개를 확인하고 AI 비교를 요청합니다.</p>
            </div>
            <span>{{ comparisonSelection.length }} / 2 선택</span>
          </div>

          <div class="selected-property-row">
            <article
              v-for="index in 2"
              :key="index"
              class="selected-property-slot"
            >
              <span>{{ getComparisonLabel(index - 1) }}</span>
              <strong>{{ comparisonSelection[index - 1]?.name || "선택 대기" }}</strong>
              <small>{{ comparisonSelection[index - 1] ? getComparablePropertyPrice(comparisonSelection[index - 1]) : "주택을 선택해주세요" }}</small>
            </article>
          </div>

          <p v-if="comparisonErrorMessage" class="form-message" role="alert">
            {{ comparisonErrorMessage }}
          </p>

          <div class="compare-actions">
            <button
              class="secondary-button"
              type="button"
              :disabled="comparisonSelection.length === 0 || isComparing"
              @click="clearComparisonSelection"
            >
              선택 초기화
            </button>
            <button
              class="primary-button"
              type="button"
              :disabled="!canRequestComparison"
              @click="requestAiComparison"
            >
              {{ isComparing ? "AI 비교 중" : "AI로 비교하기" }}
            </button>
          </div>
        </section>

        <section
          v-if="comparisonResult"
          ref="comparisonResultPanel"
          class="comparison-panel compare-result-panel"
          aria-labelledby="compare-result-title"
        >
          <div class="panel-title-row compare-result-title-row">
            <div class="compare-result-heading">
              <h2 id="compare-result-title">AI 비교 결과</h2>
              <span>{{ comparisonRecommendationText }}</span>
            </div>
          </div>

          <div class="compare-result-hero">
            <p class="compare-summary">{{ comparisonResult.oneLineSummary }}</p>
            <p class="compare-reason">{{ comparisonResult.recommendationReason }}</p>
          </div>

          <div v-if="comparisonResult.comparisonTable?.length" class="compare-result-table">
            <div class="compare-table-header">
              <strong>기준</strong>
              <span>A 주택</span>
              <span>B 주택</span>
            </div>
            <div v-for="item in comparisonResult.comparisonTable" :key="item.criterion">
              <strong>{{ item.criterion }}</strong>
              <span>
                {{ item.propertyA }}
                <small v-if="item.better === 'A'">추천</small>
              </span>
              <span>
                {{ item.propertyB }}
                <small v-if="item.better === 'B'">추천</small>
              </span>
              <em v-if="item.reason">{{ item.reason }}</em>
            </div>
          </div>

          <div class="compare-insight-grid">
            <article class="positive-insight">
              <h3>A 주택 장점</h3>
              <ul v-if="comparisonResult.propertyAPros?.length">
                <li v-for="item in comparisonResult.propertyAPros" :key="`a-pro-${item}`">{{ item }}</li>
              </ul>
              <p v-else>-</p>
            </article>
            <article class="caution-insight">
              <h3>A 주택 주의점</h3>
              <ul v-if="comparisonResult.propertyACons?.length">
                <li v-for="item in comparisonResult.propertyACons" :key="`a-con-${item}`">{{ item }}</li>
              </ul>
              <p v-else>-</p>
            </article>
            <article class="positive-insight">
              <h3>B 주택 장점</h3>
              <ul v-if="comparisonResult.propertyBPros?.length">
                <li v-for="item in comparisonResult.propertyBPros" :key="`b-pro-${item}`">{{ item }}</li>
              </ul>
              <p v-else>-</p>
            </article>
            <article class="caution-insight">
              <h3>B 주택 주의점</h3>
              <ul v-if="comparisonResult.propertyBCons?.length">
                <li v-for="item in comparisonResult.propertyBCons" :key="`b-con-${item}`">{{ item }}</li>
              </ul>
              <p v-else>-</p>
            </article>
          </div>

          <div v-if="comparisonResult.cautions?.length" class="compare-cautions">
            <strong>확인 필요</strong>
            <ul>
              <li v-for="item in comparisonResult.cautions" :key="`caution-${item}`">{{ item }}</li>
            </ul>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped src="../assets/comparison.css"></style>
