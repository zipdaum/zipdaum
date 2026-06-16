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
const isLoading = ref(false);
const errorMessage = ref("");
const regionMessage = ref("");
const propertyMessage = ref("");

const totalFavoriteCount = computed(
  () => favoriteRegions.value.length + favoriteProperties.value.length,
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
  } catch (error) {
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
      region: region.regionName,
    },
  });
}

function openPropertyDeals(property) {
  router.push({
    name: "home",
    query: {
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
    id: `${region.sggCd}-${region.umdNm}`,
    sggCd: region.sggCd,
    umdNm: region.umdNm,
    regionName:
      region.regionName ||
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

function getErrorMessage(error, fallbackMessage) {
  return (
    error.response?.data?.message || error.response?.data || fallbackMessage
  );
}
</script>

<template>
  <main class="app-shell favorite-page">
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

        <div v-else class="favorite-list">
          <article
            v-for="region in favoriteRegions"
            :key="region.id"
            class="favorite-row region-row"
          >
            <div class="favorite-main">
              <strong>{{ region.regionName }}</strong>
              <span>최근 거래 기준 요약</span>
            </div>

            <dl class="price-summary">
              <div>
                <dt>매매 최근 거래가</dt>
                <dd>{{ formatPrice(region.salePrice) }}</dd>
              </div>
              <div>
                <dt>전세 최근 보증금</dt>
                <dd>{{ formatPrice(region.jeonseDeposit) }}</dd>
              </div>
              <div>
                <dt>월세 최근 보증금 / 월세</dt>
                <dd>
                  {{
                    formatMonthlyRent(
                      region.monthlyRentDeposit,
                      region.monthlyRent,
                    )
                  }}
                </dd>
              </div>
            </dl>

            <div class="deal-counts" aria-label="최근 1년간 거래 건수">
              <span>매매 {{ formatDealCount(region.yearlyDealCounts.sale) }}</span>
              <span>전세 {{ formatDealCount(region.yearlyDealCounts.jeonse) }}</span>
              <span>
                월세 {{ formatDealCount(region.yearlyDealCounts.monthlyRent) }}
              </span>
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
          </article>
        </div>
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

        <div v-else class="favorite-list">
          <article
            v-for="property in favoriteProperties"
            :key="property.id"
            class="favorite-row property-row"
          >
            <div class="favorite-main">
              <strong>{{ property.propertyName }}</strong>
              <span>
                {{ getPropertyTypeLabel(property.propertyType) }} ·
                {{ property.regionName }}
              </span>
            </div>

            <dl class="price-summary">
              <div>
                <dt>매매 최근 거래가</dt>
                <dd>{{ formatPrice(property.salePrice) }}</dd>
              </div>
              <div>
                <dt>전세 최근 보증금</dt>
                <dd>{{ formatPrice(property.jeonseDeposit) }}</dd>
              </div>
              <div>
                <dt>월세 최근 보증금 / 월세</dt>
                <dd>
                  {{
                    formatMonthlyRent(
                      property.monthlyRentDeposit,
                      property.monthlyRent,
                    )
                  }}
                </dd>
              </div>
            </dl>

            <div class="deal-counts" aria-label="최근 1년간 거래 건수">
              <span>매매 {{ formatDealCount(property.yearlyDealCounts.sale) }}</span>
              <span>전세 {{ formatDealCount(property.yearlyDealCounts.jeonse) }}</span>
              <span>
                월세 {{ formatDealCount(property.yearlyDealCounts.monthlyRent) }}
              </span>
            </div>

            <div class="favorite-actions">
              <button
                class="secondary-button"
                type="button"
                @click="openPropertyDeals(property)"
              >
                전체 거래 조회
              </button>
              <button
                class="ghost-button"
                type="button"
                @click="handleDeleteProperty(property)"
              >
                관심 해제
              </button>
            </div>
          </article>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped src="../assets/favorite.css"></style>
