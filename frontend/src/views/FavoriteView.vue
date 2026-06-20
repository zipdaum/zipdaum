<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import {
  deleteFavoriteProperty,
  deleteFavoriteRegion,
  getFavoriteProperties,
  getFavoriteRegions,
} from "../api/favorite";

const router = useRouter();

const favoriteRegions = ref([]);
const favoriteProperties = ref([]);
const isLoading = ref(true);
const hasLoadedFavorites = ref(false);
const errorMessage = ref("");
const regionMessage = ref("");
const propertyMessage = ref("");
const favoritePageSize = 3;
const regionPage = ref(1);
const propertyPage = ref(1);

const totalFavoriteCount = computed(
  () => favoriteRegions.value.length + favoriteProperties.value.length,
);
const regionPageCount = computed(() =>
  calculatePageCount(favoriteRegions.value.length),
);
const propertyPageCount = computed(() =>
  calculatePageCount(favoriteProperties.value.length),
);
const paginatedFavoriteRegions = computed(() =>
  getPaginatedItems(favoriteRegions.value, regionPage.value),
);
const paginatedFavoriteProperties = computed(() =>
  getPaginatedItems(favoriteProperties.value, propertyPage.value),
);

onMounted(loadFavorites);

async function loadFavorites() {
  isLoading.value = true;
  errorMessage.value = "";

  try {
    const [regions, properties] = await Promise.all([
      getFavoriteRegions(),
      getFavoriteProperties(),
    ]);

    favoriteRegions.value = regions.map(mapFavoriteRegion);
    favoriteProperties.value = properties.map(mapFavoriteProperty);
    hasLoadedFavorites.value = true;
    clampFavoritePages();
  } catch (error) {
    if (error.response?.status !== 401) {
      hasLoadedFavorites.value = true;
    }

    errorMessage.value = getErrorMessage(
      error,
      "관심 목록을 불러오지 못했습니다.",
    );
  } finally {
    isLoading.value = false;
  }
}

function goHome() {
  router.push({ name: "home" });
}

function openRegionDeals(region) {
  router.push({
    name: "home",
    query: {
      view: "results",
      sggCd: region.sggCd,
      umdNm: region.umdNm,
    },
  });
}

function openPropertyDeals(property) {
  router.push({
    name: "home",
    query: {
      view: "detail",
      propertyId: property.propertyId,
    },
  });
}

async function handleDeleteRegion(region) {
  regionMessage.value = "";
  errorMessage.value = "";

  try {
    await deleteFavoriteRegion({
      sggCd: region.sggCd,
      umdNm: region.umdNm,
    });
    regionMessage.value = "관심 지역을 해제했습니다.";
    await loadFavorites();
  } catch (error) {
    regionMessage.value = getErrorMessage(
      error,
      "관심 지역을 해제하지 못했습니다.",
    );
  }
}

async function handleDeleteProperty(property) {
  propertyMessage.value = "";
  errorMessage.value = "";

  try {
    await deleteFavoriteProperty(property.propertyId);
    propertyMessage.value = "관심 주택을 해제했습니다.";
    await loadFavorites();
  } catch (error) {
    propertyMessage.value = getErrorMessage(
      error,
      "관심 주택을 해제하지 못했습니다.",
    );
  }
}

function mapFavoriteRegion(region) {
  return {
    id: region.umdCd || `${region.sggCd}-${region.umdNm}`,
    sggCd: region.sggCd,
    sggNm: region.sggNm,
    umdCd: region.umdCd,
    umdNm: region.umdNm,
    displayName:
      region.displayName ||
      [region.sggNm, region.umdNm].filter(Boolean).join(" ") ||
      [region.sggCd, region.umdNm].filter(Boolean).join(" "),
    salePrice: region.latestSalePrice,
    jeonseDeposit: region.latestJeonseDeposit,
    monthlyRentDeposit: region.latestMonthlyRentDeposit,
    monthlyRent: region.latestMonthlyRent,
    yearlyDealCounts: {
      sale: region.saleDealCount,
      jeonse: region.jeonseDealCount,
      monthlyRent: region.monthlyRentDealCount,
    },
  };
}

function mapFavoriteProperty(property) {
  return {
    id: property.propertyId,
    propertyId: property.propertyId,
    propertyName: property.name || "주택명 미상",
    propertyType: property.propertyType,
    sggCd: property.sggCd,
    umdNm: property.umdNm,
    regionName:
      property.regionName ||
      [property.sggCd, property.umdNm].filter(Boolean).join(" "),
    salePrice: property.latestSalePrice,
    jeonseDeposit: property.latestJeonseDeposit,
    monthlyRentDeposit: property.latestMonthlyRentDeposit,
    monthlyRent: property.latestMonthlyRent,
    yearlyDealCounts: {
      sale: property.saleDealCount,
      jeonse: property.jeonseDealCount,
      monthlyRent: property.monthlyRentDealCount,
    },
  };
}

function getPropertyTypeLabel(propertyType) {
  const propertyTypeLabels = {
    APARTMENT: "아파트",
    VILLA: "연립/다세대",
  };

  return propertyTypeLabels[propertyType] || "주택";
}

function formatPrice(price) {
  if (!price) {
    return "-";
  }

  if (price >= 10000) {
    const hundredMillion = Math.floor(price / 10000);
    const rest = price % 10000;

    return rest > 0
      ? `${hundredMillion}억 ${rest.toLocaleString()}만원`
      : `${hundredMillion}억원`;
  }

  return `${price.toLocaleString()}만원`;
}

function formatMonthlyRent(deposit, monthlyRent) {
  if (!deposit && !monthlyRent) {
    return "-";
  }

  return `${formatPrice(deposit)} / ${Number(monthlyRent || 0).toLocaleString()}만원`;
}

function formatDealCount(count) {
  return `${Number(count || 0).toLocaleString()}건`;
}

function getFavoriteDealSummaries(item) {
  return [
    {
      type: "매매",
      price: formatPrice(item.salePrice),
      count: formatDealCount(item.yearlyDealCounts.sale),
    },
    {
      type: "전세",
      price: formatPrice(item.jeonseDeposit),
      count: formatDealCount(item.yearlyDealCounts.jeonse),
    },
    {
      type: "월세",
      price: formatMonthlyRent(item.monthlyRentDeposit, item.monthlyRent),
      count: formatDealCount(item.yearlyDealCounts.monthlyRent),
    },
  ];
}

function calculatePageCount(totalCount) {
  return Math.max(Math.ceil(totalCount / favoritePageSize), 1);
}

function getPaginatedItems(items, page) {
  const startIndex = (page - 1) * favoritePageSize;
  return items.slice(startIndex, startIndex + favoritePageSize);
}

function getVisibleFavoritePages(pageCount, currentPage) {
  const groupStart = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const groupEnd = Math.min(groupStart + 4, pageCount);

  return Array.from(
    { length: groupEnd - groupStart + 1 },
    (_, index) => groupStart + index,
  );
}

function setRegionPage(page) {
  regionPage.value = Math.min(Math.max(page, 1), regionPageCount.value);
}

function setPropertyPage(page) {
  propertyPage.value = Math.min(Math.max(page, 1), propertyPageCount.value);
}

function clampFavoritePages() {
  setRegionPage(regionPage.value);
  setPropertyPage(propertyPage.value);
}

function getErrorMessage(error, fallbackMessage) {
  return (
    error.response?.data?.message || error.response?.data || fallbackMessage
  );
}
</script>

<template>
  <main class="app-shell favorite-page">
    <p v-if="!hasLoadedFavorites" class="empty-message">
      관심 목록을 불러오는 중입니다.
    </p>

    <template v-else>
    <AppHeader @home="goHome" />

    <section class="favorite-header" aria-labelledby="favorite-title">
      <div>
        <p>관심 목록</p>
        <h1 id="favorite-title">관심 지역과 관심 주택을 한 번에 확인하세요</h1>
      </div>
      <div class="favorite-header-actions">
        <strong>{{ totalFavoriteCount.toLocaleString() }}개</strong>
        <button class="primary-button" type="button" @click="goHome">
          관심 목록 더 찾아보기
        </button>
      </div>
    </section>

    <p v-if="errorMessage" class="form-message" role="alert">
      {{ errorMessage }}
    </p>

    <section class="favorite-grid" aria-label="관심 목록">
      <article class="favorite-panel" aria-labelledby="favorite-region-title">
        <div class="panel-title-row">
          <div>
            <p class="result-kicker">Favorite Regions</p>
            <h2 id="favorite-region-title">관심 지역</h2>
          </div>
          <span>{{ favoriteRegions.length.toLocaleString() }}개</span>
        </div>

        <p
          v-if="regionMessage"
          class="favorite-message"
          :class="{ error: regionMessage.includes('못했습니다') }"
          role="status"
        >
          {{ regionMessage }}
        </p>

        <p v-if="isLoading" class="empty-message">
          관심 지역을 불러오는 중입니다.
        </p>

        <p v-else-if="favoriteRegions.length === 0" class="empty-message">
          등록된 관심 지역이 없습니다.
        </p>

        <template v-else>
          <div class="favorite-list">
            <article
              v-for="region in paginatedFavoriteRegions"
              :key="region.id"
              class="favorite-row region-row"
            >
              <div class="favorite-row-header">
                <div class="favorite-main">
                  <strong>{{ region.displayName }}</strong>
                  <span>최근 거래 기준 요약</span>
                </div>

                <div class="favorite-actions">
                  <button
                    class="secondary-button"
                    type="button"
                    @click="openRegionDeals(region)"
                  >
                    전체 거래 조회
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="handleDeleteRegion(region)"
                  >
                    관심 해제
                  </button>
                </div>
              </div>

              <div class="favorite-summary-list" aria-label="최근 거래 요약">
                <div
                  v-for="summary in getFavoriteDealSummaries(region)"
                  :key="`${region.id}-${summary.type}`"
                  class="favorite-summary-item"
                >
                  <span>{{ summary.type }}</span>
                  <strong>{{ summary.price }}</strong>
                  <em>{{ summary.count }}</em>
                </div>
              </div>
            </article>
          </div>

          <div
            v-if="regionPageCount > 1"
            class="favorite-pagination"
            aria-label="관심 지역 페이지"
          >
            <button
              type="button"
              :disabled="regionPage === 1"
              @click="setRegionPage(regionPage - 1)"
            >
              이전
            </button>
            <button
              v-for="page in getVisibleFavoritePages(
                regionPageCount,
                regionPage,
              )"
              :key="`region-page-${page}`"
              :class="{ active: regionPage === page }"
              type="button"
              :aria-current="regionPage === page ? 'page' : undefined"
              @click="setRegionPage(page)"
            >
              {{ page }}
            </button>
            <button
              type="button"
              :disabled="regionPage === regionPageCount"
              @click="setRegionPage(regionPage + 1)"
            >
              다음
            </button>
          </div>
        </template>
        <div
          v-if="isLoading || favoriteRegions.length === 0 || regionPageCount <= 1"
          class="favorite-pagination favorite-pagination-placeholder"
          aria-hidden="true"
        ></div>
      </article>

      <article class="favorite-panel" aria-labelledby="favorite-property-title">
        <div class="panel-title-row">
          <div>
            <p class="result-kicker">Favorite Properties</p>
            <h2 id="favorite-property-title">관심 주택</h2>
          </div>
          <span>{{ favoriteProperties.length.toLocaleString() }}개</span>
        </div>

        <p
          v-if="propertyMessage"
          class="favorite-message"
          :class="{ error: propertyMessage.includes('못했습니다') }"
          role="status"
        >
          {{ propertyMessage }}
        </p>

        <p v-if="isLoading" class="empty-message">
          관심 주택을 불러오는 중입니다.
        </p>

        <p v-else-if="favoriteProperties.length === 0" class="empty-message">
          등록된 관심 주택이 없습니다.
        </p>

        <template v-else>
          <div class="favorite-list">
            <article
              v-for="property in paginatedFavoriteProperties"
              :key="property.id"
              class="favorite-row property-row"
            >
              <div class="favorite-row-header">
                <div class="favorite-main">
                  <strong>{{ property.propertyName }}</strong>
                  <span>
                    {{ getPropertyTypeLabel(property.propertyType) }} ·
                    {{ property.regionName }}
                  </span>
                </div>

                <div class="favorite-actions">
                  <button
                    class="secondary-button"
                    type="button"
                    @click="openPropertyDeals(property)"
                  >
                    상세 거래 조회
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="handleDeleteProperty(property)"
                  >
                    관심 해제
                  </button>
                </div>
              </div>

              <div class="favorite-summary-list" aria-label="최근 거래 요약">
                <div
                  v-for="summary in getFavoriteDealSummaries(property)"
                  :key="`${property.id}-${summary.type}`"
                  class="favorite-summary-item"
                >
                  <span>{{ summary.type }}</span>
                  <strong>{{ summary.price }}</strong>
                  <em>{{ summary.count }}</em>
                </div>
              </div>
            </article>
          </div>

          <div
            v-if="propertyPageCount > 1"
            class="favorite-pagination"
            aria-label="관심 주택 페이지"
          >
            <button
              type="button"
              :disabled="propertyPage === 1"
              @click="setPropertyPage(propertyPage - 1)"
            >
              이전
            </button>
            <button
              v-for="page in getVisibleFavoritePages(
                propertyPageCount,
                propertyPage,
              )"
              :key="`property-page-${page}`"
              :class="{ active: propertyPage === page }"
              type="button"
              :aria-current="propertyPage === page ? 'page' : undefined"
              @click="setPropertyPage(page)"
            >
              {{ page }}
            </button>
            <button
              type="button"
              :disabled="propertyPage === propertyPageCount"
              @click="setPropertyPage(propertyPage + 1)"
            >
              다음
            </button>
          </div>
        </template>
        <div
          v-if="
            isLoading || favoriteProperties.length === 0 || propertyPageCount <= 1
          "
          class="favorite-pagination favorite-pagination-placeholder"
          aria-hidden="true"
        ></div>
      </article>
    </section>
    </template>
  </main>
</template>

<style scoped src="../assets/favorite.css"></style>
