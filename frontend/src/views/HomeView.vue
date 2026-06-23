<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import {
  getPropertyDealHistories as fetchPropertyDealHistories,
  getPropertyDetail as fetchPropertyDetail,
  getPropertyAiSummary as fetchPropertyAiSummary,
  getPropertyRecommendations as fetchPropertyRecommendations,
  getPropertyRecommendationScore as fetchPropertyRecommendationScore,
  getSurroundings as fetchSurroundings,
  savePropertyInteraction,
  savePropertyInteractionKeepalive,
  searchProperties,
} from "../api/property";
import {
  addFavoriteProperty,
  deleteFavoriteProperty,
  getFavoriteProperties,
} from "../api/favorite";
import { isLoggedIn } from "../stores/auth";

const router = useRouter();

const dealTypes = [
  { label: "전체", value: "" },
  { label: "매매", value: "SALE" },
  { label: "전세", value: "JEONSE" },
  { label: "월세", value: "MONTHLY_RENT" },
];

const regions = [
  { label: "부산광역시 전체", value: null },
  { label: "부산광역시 해운대구", value: "26350" },
  { label: "부산광역시 부산진구", value: "26230" },
  { label: "부산광역시 동래구", value: "26260" },
  { label: "부산광역시 남구", value: "26290" },
  { label: "부산광역시 북구", value: "26320" },
  { label: "부산광역시 사하구", value: "26380" },
  { label: "부산광역시 금정구", value: "26410" },
  { label: "부산광역시 강서구", value: "26440" },
  { label: "부산광역시 연제구", value: "26470" },
  { label: "부산광역시 수영구", value: "26500" },
  { label: "부산광역시 사상구", value: "26530" },
  { label: "부산광역시 기장군", value: "26710" },
  { label: "부산광역시 중구", value: "26110" },
  { label: "부산광역시 서구", value: "26140" },
  { label: "부산광역시 동구", value: "26170" },
  { label: "부산광역시 영도구", value: "26200" },
];

const priceRanges = [
  { label: "전체", minPrice: "", maxPrice: "" },
  { label: "3억 이하", minPrice: "", maxPrice: 30000 },
  { label: "3억 - 5억", minPrice: 30000, maxPrice: 50000 },
  { label: "5억 - 10억", minPrice: 50000, maxPrice: 100000 },
  { label: "10억 이상", minPrice: 100000, maxPrice: "" },
];

const propertyTypes = [
  { label: "전체", value: "" },
  { label: "아파트", value: "APARTMENT" },
  { label: "연립/다세대", value: "VILLA" },
];

const sortOptions = [
  { label: "최신순", sortBy: "LATEST", sortDirection: "DESC" },
  { label: "가격 높은순", sortBy: "PRICE", sortDirection: "DESC" },
  { label: "가격 낮은순", sortBy: "PRICE", sortDirection: "ASC" },
  { label: "이름순", sortBy: "NAME", sortDirection: "ASC" },
];

const trendTypes = [
  { label: "매매", value: "SALE" },
  { label: "전세", value: "JEONSE" },
  { label: "월세", value: "MONTHLY_RENT" },
];

const facilityLegendItems = [
  { label: "버스", value: "BUS" },
  { label: "지하철역", value: "SUBWAY" },
  { label: "병원", value: "HOSPITAL" },
  { label: "공원", value: "PARK" },
  { label: "CCTV", value: "CCTV" },
];

const facilityTypeLabels = {
  BUS: "버스",
  SUBWAY: "지하철역",
  HOSPITAL: "병원",
  CCTV: "CCTV",
  PARK: "공원",
};

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

const searchForm = ref({
  sggCd: null,
  umdNm: "",
  name: "",
  propertyType: "",
  dealType: "",
  priceRangeIndex: 0,
  sortIndex: 0,
});

const initialRoute = getCurrentRoute();
const isInitialDetailRoute =
  initialRoute.view === "detail" || initialRoute.view === "deal-history";

const currentView = ref(initialRoute.view);
const homeResultTab = ref("search");
const searchResults = ref([]);
const recommendationResults = ref([]);
const hasSearched = ref(false);
const isLoading = ref(false);
const isRecommendationListLoading = ref(false);
const isResultHighlighted = ref(false);
const errorMessage = ref("");
const recommendationListErrorMessage = ref("");
const isDetailLoading = ref(isInitialDetailRoute);
const detailErrorMessage = ref("");
const isSaleHistoryLoading = ref(false);
const isRentHistoryLoading = ref(false);
const historiesErrorMessage = ref("");
const selectedPropertyDetail = ref(null);
const activeTrendType = ref("SALE");
const hoveredTrendDot = ref(null);
const activeRentHistoryType = ref("JEONSE");
const surroundings = ref(null);
const isSurroundingsLoading = ref(false);
const surroundingsErrorMessage = ref("");
const recommendationScore = ref(null);
const isRecommendationScoreLoading = ref(false);
const recommendationScoreErrorMessage = ref("");
const propertyAiSummary = ref("");
const isPropertyAiSummaryLoading = ref(false);
const propertyAiSummaryErrorMessage = ref("");
const isPropertyAiSummaryHighlighted = ref(false);
const detailInteraction = ref(null);
const mapZoom = ref(1);
const mapPan = ref({ x: 0, y: 0 });
const isMapDragging = ref(false);
const saleHistoryPage = ref(1);
const rentHistoryPage = ref(1);
const favoritePropertyIds = ref([]);
const isFavoritePropertyLoading = ref(false);
const resultPanel = ref(null);
const appliedSearchSummary = ref([]);
const historyPageSize = 5;
const emptyHistoryMeta = {
  salePage: 1,
  saleSize: historyPageSize,
  saleTotalCount: 0,
  saleTotalPages: 1,
  rentDealType: "JEONSE",
  rentPage: 1,
  rentSize: historyPageSize,
  rentTotalCount: 0,
  rentTotalPages: 1,
  jeonseTotalCount: 0,
  monthlyRentTotalCount: 0,
  saleServerPaged: false,
  rentServerPaged: false,
};
let resultHighlightTimer = null;
let propertyAiSummaryHighlightTimer = null;
let mapDragStart = null;

const searchPage = ref(1);
const searchTotalPages = ref(1);
const searchTotalElements = ref(0);
const searchPageSize = 10;

function createEmptyRentDealsByType() {
  return {
    JEONSE: [],
    MONTHLY_RENT: [],
  };
}

function createEmptyRentMetaByType() {
  return {
    JEONSE: {
      page: 1,
      size: historyPageSize,
      totalCount: 0,
      totalPages: 1,
      serverPaged: false,
    },
    MONTHLY_RENT: {
      page: 1,
      size: historyPageSize,
      totalCount: 0,
      totalPages: 1,
      serverPaged: false,
    },
  };
}

onMounted(async () => {
  await Promise.all([
    restoreViewFromUrl(),
    loadPropertyRecommendations(),
    loadFavoritePropertyIds(),
  ]);
  window.addEventListener("pagehide", saveDetailInteractionOnPageHide);
  window.addEventListener("popstate", handleBrowserBack);
});

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadPropertyRecommendations();
    loadFavoritePropertyIds();
    return;
  }

  recommendationResults.value = [];
  recommendationListErrorMessage.value = "";
  favoritePropertyIds.value = [];
});

onUnmounted(() => {
  saveDetailInteraction();
  clearResultHighlightTimer();
  clearPropertyAiSummaryHighlightTimer();
  stopMapDrag();
  window.removeEventListener("scroll", updateDetailScrollDepth);
  window.removeEventListener("pagehide", saveDetailInteractionOnPageHide);
  window.removeEventListener("popstate", handleBrowserBack);
});

const homeResultTabs = [
  { label: "실거래가 결과", value: "search" },
  { label: "맞춤 추천", value: "recommendation" },
];

const recommendationCards = computed(() =>
  sortRecommendationsByScore(recommendationResults.value).map(
    mapRecommendationToHomeCard,
  ),
);
const displayedRecommendationCards = computed(() =>
  recommendationCards.value.slice(0, 5),
);
const hasMoreRecommendations = computed(
  () => recommendationCards.value.length > displayedRecommendationCards.value.length,
);

const displayedHomes = computed(() => {
  if (!hasSearched.value) {
    return [];
  }

  return searchResults.value.slice(0, 5).map(mapPropertyToHomeCard);
});

const allResultRows = computed(() =>
  searchResults.value.map((property, index) => ({
    ...mapPropertyToHomeCard(property, index),
    propertyType: getPropertyTypeLabel(property.propertyType),
    buildYearLabel: getBuildYearLabel(property.buildYear),
  })),
);

const hasMoreResults = computed(
  () => hasSearched.value && searchResults.value.length > 0,
);

function mapPropertyToHomeCard(property, index) {
  return {
    id: property.id,
    rank: index + 1,
    name: property.name || "주택명 미상",
    region:
      [property.umdNm, property.jibun].filter(Boolean).join(" ") ||
      property.sggCd,
    price: getDisplayPrice(property),
    detail: getPropertyDetail(property),
  };
}

function mapRecommendationToHomeCard(property, index) {
  return {
    ...mapPropertyToHomeCard(property, index),
    score: property.score,
    status: property.recommendationStatus,
  };
}

function sortRecommendationsByScore(properties) {
  return [...properties].sort((left, right) => {
    const scoreCompared =
      getComparableRecommendationScore(right) -
      getComparableRecommendationScore(left);

    if (scoreCompared !== 0) {
      return scoreCompared;
    }

    return Number(right.id || 0) - Number(left.id || 0);
  });
}

function getComparableRecommendationScore(property) {
  return Number.isFinite(Number(property.score)) ? Number(property.score) : 0;
}

async function loadPropertyRecommendations() {
  if (!isLoggedIn.value) {
    recommendationResults.value = [];
    recommendationListErrorMessage.value = "";
    return;
  }

  isRecommendationListLoading.value = true;
  recommendationListErrorMessage.value = "";

  try {
    recommendationResults.value = await fetchPropertyRecommendations();
  } catch (error) {
    recommendationResults.value = [];
    recommendationListErrorMessage.value =
      "맞춤 추천 주택 목록을 불러오지 못했습니다.";
  } finally {
    isRecommendationListLoading.value = false;
  }
}

const resultCountText = computed(() => {
  if (isLoading.value) {
    return "검색 중";
  }

  if (searchTotalElements.value > 0) {
    return `${searchTotalElements.value.toLocaleString()}건`;
  }

  if (hasSearched.value) {
    return "0건";
  }

  return "검색 전";
});

const resultTitle = computed(() => {
  return "실거래가 검색 결과";
});

const resultNotice = computed(() => {
  if (!hasSearched.value) {
    return "검색 조건을 입력하면 실거래가 결과가 표시됩니다.";
  }

  if (isLoading.value) {
    return "조건에 맞는 실거래가를 조회하고 있습니다.";
  }

  if (errorMessage.value) {
    return "검색 요청을 완료하지 못했습니다.";
  }

  if (searchResults.value.length === 0) {
    return "검색은 완료됐지만 조건에 맞는 매물이 없습니다.";
  }

  return `검색 완료: 조건에 맞는 실거래가 ${searchResults.value.length.toLocaleString()}건을 찾았습니다.`;
});

function selectDealType(dealType) {
  searchForm.value.dealType = dealType;
}

async function handleSearch() {
  homeResultTab.value = "search";
  searchForm.value.sortIndex = 0;
  searchPage.value = 1;
  await runSearch();
}

async function runSearch() {
  return runSearchWithOptions();
}

async function runSearchWithOptions({
  highlight = true,
  scroll = true,
} = {}) {
  isLoading.value = true;
  isResultHighlighted.value = highlight;
  errorMessage.value = "";
  hasSearched.value = true;
  appliedSearchSummary.value = getSearchSummary();
  clearResultHighlightTimer();
  if (scroll) {
    await scrollToResults();
  }

  try {
    const selectedPriceRange = priceRanges[searchForm.value.priceRangeIndex];
    const selectedSort = sortOptions[searchForm.value.sortIndex];
    const params = removeEmptyValues({
      sggCd: searchForm.value.sggCd,
      umdNm: searchForm.value.umdNm,
      name: searchForm.value.name,
      propertyType: searchForm.value.propertyType,
      dealType: searchForm.value.dealType,
      minPrice: selectedPriceRange.minPrice,
      maxPrice: selectedPriceRange.maxPrice,
      sortBy: selectedSort.sortBy,
      sortDirection: selectedSort.sortDirection,
      page: searchPage.value,
      size: searchPageSize,
    });

    const response = await searchProperties(params);

    searchResults.value = sortProperties(response.content || [], selectedSort);
    searchTotalPages.value = response.totalPages || 1;
    searchTotalElements.value = response.totalElements || 0;

  } catch (error) {
    searchResults.value = [];
    errorMessage.value =
      "실거래가 검색 결과를 불러오지 못했습니다. 백엔드 서버와 검색 조건을 확인해주세요.";
  } finally {
    isLoading.value = false;
    if (highlight) {
      resultHighlightTimer = window.setTimeout(() => {
        isResultHighlighted.value = false;
        resultHighlightTimer = null;
      }, 1600);
    } else {
      isResultHighlighted.value = false;
    }
    if (scroll) {
      await scrollToResults();
    }
  }
}

async function changeSearchPage(newPage) {
  if (newPage < 1 || newPage > searchTotalPages.value || isLoading.value || newPage === searchPage.value) {
    return;
  }
  searchPage.value = newPage;
  await runSearch();

  window.scrollTo({ top: 0, behavior: "smooth" });
}

function getVisibleSearchPages() {
  const start = Math.floor((searchPage.value - 1) / 5) * 5 + 1;
  const end = Math.min(start + 4, searchTotalPages.value);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
}

async function handleSortChange() {
  if (!hasSearched.value) {
    return;
  }
  searchPage.value = 1;
  await runSearch();
}

function openResultsView() {
  saveDetailInteraction();
  window.history.pushState(
    { view: "results" },
    "",
    getViewUrl("results"),
  );
  currentView.value = "results";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openRecommendationResultsView() {
  saveDetailInteraction();
  window.history.pushState(
    { view: "recommendations" },
    "",
    getViewUrl("recommendations"),
  );
  currentView.value = "recommendations";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openHomeView({ updateHistory = true } = {}) {
  saveDetailInteraction();
  if (updateHistory) {
    window.history.pushState({ view: "home" }, "", getViewUrl("home"));
  }
  currentView.value = "home";
  selectedPropertyDetail.value = null;
  detailErrorMessage.value = "";
  isDetailLoading.value = false;
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function openPropertyDetail(propertyId) {
  const normalizedPropertyId = normalizePropertyId(propertyId);
  if (!normalizedPropertyId) {
    return;
  }

  window.history.pushState(
    { view: "detail", propertyId: normalizedPropertyId },
    "",
    getViewUrl("detail", normalizedPropertyId),
  );
  await loadPropertyDetailView(normalizedPropertyId, "detail");
}

async function loadPropertyDetailView(propertyId, view = "detail") {
  const normalizedPropertyId = normalizePropertyId(propertyId);
  if (!normalizedPropertyId) {
    detailErrorMessage.value =
      "거래 상세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
    return;
  }

  if (
    detailInteraction.value &&
    (detailInteraction.value.propertyId !== normalizedPropertyId || view !== "detail")
  ) {
    saveDetailInteraction();
  }

  currentView.value = view;
  if (
    !selectedPropertyDetail.value ||
    selectedPropertyDetail.value.id !== normalizedPropertyId
  ) {
    selectedPropertyDetail.value = null;
    isDetailLoading.value = true;
  }
  detailErrorMessage.value = "";
  historiesErrorMessage.value = "";
  activeTrendType.value = "SALE";
  activeRentHistoryType.value = "JEONSE";
  surroundings.value = null;
  surroundingsErrorMessage.value = "";
  recommendationScore.value = null;
  recommendationScoreErrorMessage.value = "";
  propertyAiSummary.value = "";
  propertyAiSummaryErrorMessage.value = "";
  isPropertyAiSummaryHighlighted.value = false;
  resetFacilityMap();
  saleHistoryPage.value = 1;
  rentHistoryPage.value = 1;
  hoveredTrendDot.value = null;
  window.scrollTo({ top: 0, behavior: "smooth" });
  if (view === "detail") {
    startDetailInteraction(normalizedPropertyId);
  }

  try {
    if (
      !selectedPropertyDetail.value ||
      selectedPropertyDetail.value.id !== normalizedPropertyId
    ) {
      const detail = await fetchPropertyDetail(normalizedPropertyId);
      selectedPropertyDetail.value = createPropertyDetailState(
        detail,
        normalizedPropertyId,
      );
    }

    isDetailLoading.value = false;
    await Promise.all([
      loadHistories(selectedPropertyDetail.value.id, {
        salePage: 1,
        rentPage: 1,
        rentDealType: "JEONSE",
      }),
      loadSurroundings(selectedPropertyDetail.value),
      loadRecommendationScore(normalizedPropertyId),
      loadPropertyAiSummary(normalizedPropertyId),
    ]);

    if (selectedPropertyDetail.value.monthlyRentTotalCount > 0) {
      await loadHistories(selectedPropertyDetail.value.id, {
        rentPage: 1,
        rentDealType: "MONTHLY_RENT",
        updateSale: false,
        activateRent: false,
      });
    }

    activeTrendType.value = getDefaultTrendType(selectedPropertyDetail.value);
    if (view === "deal-history") {
      activeRentHistoryType.value =
        selectedPropertyDetail.value.jeonseTotalCount > 0
          ? "JEONSE"
          : "MONTHLY_RENT";
    }
  } catch (error) {
    detailErrorMessage.value =
      "거래 상세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    isDetailLoading.value = false;
  }
}

function createPropertyDetailState(detail, propertyId) {
  return {
    ...detail,
    id: normalizePropertyId(detail?.id) || propertyId,
    saleDeals: [],
    rentDeals: [],
    rentDealsByType: createEmptyRentDealsByType(),
    rentMetaByType: createEmptyRentMetaByType(),
    ...emptyHistoryMeta,
  };
}

async function loadRecommendationScore(propertyId) {
  recommendationScore.value = null;
  recommendationScoreErrorMessage.value = "";

  if (!isLoggedIn.value) {
    return;
  }

  isRecommendationScoreLoading.value = true;

  try {
    recommendationScore.value = await fetchPropertyRecommendationScore(
      propertyId,
    );
  } catch (error) {
    const status = error.response?.status;
    recommendationScoreErrorMessage.value =
      status === 404
        ? "등록된 맞춤 조건이 없어 적합도를 계산하지 못했습니다."
        : "맞춤 적합도 정보를 불러오지 못했습니다.";
  } finally {
    isRecommendationScoreLoading.value = false;
  }
}

async function loadPropertyAiSummary(propertyId) {
  propertyAiSummary.value = "";
  propertyAiSummaryErrorMessage.value = "";

  if (!isLoggedIn.value) {
    return;
  }

  isPropertyAiSummaryLoading.value = true;

  try {
    const response = await fetchPropertyAiSummary(propertyId);
    propertyAiSummary.value = response?.summary || "";
    if (propertyAiSummary.value) {
      highlightPropertyAiSummary();
    }
  } catch (error) {
    propertyAiSummaryErrorMessage.value = "AI 요약을 불러오지 못했습니다.";
  } finally {
    isPropertyAiSummaryLoading.value = false;
  }
}

function highlightPropertyAiSummary() {
  if (propertyAiSummaryHighlightTimer) {
    clearPropertyAiSummaryHighlightTimer();
  }

  isPropertyAiSummaryHighlighted.value = true;
  propertyAiSummaryHighlightTimer = window.setTimeout(() => {
    isPropertyAiSummaryHighlighted.value = false;
    propertyAiSummaryHighlightTimer = null;
  }, 1800);
}

function clearPropertyAiSummaryHighlightTimer() {
  if (!propertyAiSummaryHighlightTimer) {
    return;
  }

  clearTimeout(propertyAiSummaryHighlightTimer);
  propertyAiSummaryHighlightTimer = null;
}

function goToLoginForRecommendation() {
  router.push({
    name: "login",
    query: {
      redirect: `${window.location.pathname}${window.location.search}`,
      reason: "login-required",
    },
  });
}

async function openRecommendationScoreDetail() {
  const propertyId = getActiveDetailPropertyId();

  if (!propertyId) {
    return;
  }

  markRecommendationDetailClicked();
  saveDetailInteraction();
  const target = {
    name: "property-recommendation-score",
    params: {
      propertyId,
    },
  };
  const targetUrl = router.resolve(target).href;

  try {
    await router.push(target);
  } catch (error) {
    window.location.assign(targetUrl);
    return;
  }

  if (!window.location.pathname.startsWith("/recommendation-score/properties/")) {
    window.location.assign(targetUrl);
  }
}

function getActiveDetailPropertyId() {
  return (
    normalizePropertyId(selectedPropertyDetail.value?.id) ||
    normalizePropertyId(detailInteraction.value?.propertyId) ||
    normalizePropertyId(
      new URLSearchParams(window.location.search).get("propertyId"),
    )
  );
}

async function loadHistories(propertyId, options = {}) {
  if (!propertyId || !selectedPropertyDetail.value) {
    return;
  }

  historiesErrorMessage.value = "";
  const rentDealType =
    options.rentDealType || activeRentHistoryType.value || "JEONSE";
  const requestedSalePage = options.salePage || saleHistoryPage.value;
  const requestedRentPage = options.rentPage || rentHistoryPage.value;
  const shouldUpdateSale = options.updateSale !== false;
  const shouldUpdateRent = options.updateRent !== false;
  const shouldActivateRent = options.activateRent !== false;

  if (shouldUpdateSale) {
    isSaleHistoryLoading.value = true;
  }
  if (shouldUpdateRent) {
    isRentHistoryLoading.value = true;
  }

  try {
    const histories = await fetchPropertyDealHistories(propertyId, {
      salePage: requestedSalePage,
      rentPage: requestedRentPage,
      rentDealType,
      size: historyPageSize,
    });
    selectedPropertyDetail.value = buildHistoryState(
      selectedPropertyDetail.value,
      histories,
      {
        rentDealType,
        requestedSalePage,
        requestedRentPage,
        shouldUpdateSale,
        shouldUpdateRent,
        shouldActivateRent,
      },
    );
    if (shouldUpdateSale) {
      saleHistoryPage.value = selectedPropertyDetail.value.salePage;
    }
    if (shouldUpdateRent && shouldActivateRent) {
      rentHistoryPage.value = selectedPropertyDetail.value.rentPage;
      activeRentHistoryType.value = selectedPropertyDetail.value.rentDealType;
    }
  } catch (error) {
    historiesErrorMessage.value = "거래 이력을 불러오지 못했습니다.";
  } finally {
    if (shouldUpdateSale) {
      isSaleHistoryLoading.value = false;
    }
    if (shouldUpdateRent) {
      isRentHistoryLoading.value = false;
    }
  }
}

function buildHistoryState(currentProperty, histories, options) {
  const {
    rentDealType,
    requestedSalePage,
    requestedRentPage,
    shouldUpdateSale,
    shouldUpdateRent,
    shouldActivateRent,
  } = options;
  const nextProperty = { ...currentProperty };

  if (shouldUpdateSale) {
    const saleTotalCount =
      histories.saleTotalCount ?? (histories.saleDeals || []).length;
    Object.assign(nextProperty, {
      saleDeals: histories.saleDeals || [],
      salePage: histories.salePage || requestedSalePage,
      saleSize: histories.saleSize || historyPageSize,
      saleTotalCount,
      saleTotalPages:
        histories.saleTotalPages || calculatePageCount(saleTotalCount),
      saleServerPaged:
        histories.salePage !== undefined ||
        histories.saleTotalCount !== undefined,
    });
  }

  if (shouldUpdateRent) {
    Object.assign(
      nextProperty,
      buildRentHistoryState(currentProperty, histories, {
        rentDealType,
        requestedRentPage,
        shouldActivateRent,
      }),
    );
  }

  return nextProperty;
}

function buildRentHistoryState(currentProperty, histories, options) {
  const { rentDealType, requestedRentPage, shouldActivateRent } = options;
  const rentDeals = histories.rentDeals || [];
  const rentTotalCount =
    histories.rentTotalCount ?? getRentDealCount(rentDeals, rentDealType);
  const rentServerPaged =
    histories.rentPage !== undefined || histories.rentTotalCount !== undefined;
  const nextRentDealsByType = {
    ...createEmptyRentDealsByType(),
    ...(currentProperty.rentDealsByType || {}),
    [rentDealType]: rentDeals,
  };
  const nextRentMetaByType = {
    ...createEmptyRentMetaByType(),
    ...(currentProperty.rentMetaByType || {}),
    [rentDealType]: {
      page: histories.rentPage || requestedRentPage,
      size: histories.rentSize || historyPageSize,
      totalCount: rentTotalCount,
      totalPages:
        histories.rentTotalPages || calculatePageCount(rentTotalCount),
      serverPaged: rentServerPaged,
    },
  };
  const activeRentMeta = nextRentMetaByType[rentDealType];

  return {
    rentDeals: shouldActivateRent
      ? nextRentDealsByType[rentDealType]
      : currentProperty.rentDeals,
    rentDealsByType: nextRentDealsByType,
    rentMetaByType: nextRentMetaByType,
    rentDealType: shouldActivateRent
      ? histories.rentDealType || rentDealType
      : currentProperty.rentDealType,
    rentPage: shouldActivateRent
      ? activeRentMeta.page
      : currentProperty.rentPage,
    rentSize: shouldActivateRent
      ? activeRentMeta.size
      : currentProperty.rentSize,
    rentTotalCount: shouldActivateRent
      ? activeRentMeta.totalCount
      : currentProperty.rentTotalCount,
    rentTotalPages: shouldActivateRent
      ? activeRentMeta.totalPages
      : currentProperty.rentTotalPages,
    jeonseTotalCount:
      histories.jeonseTotalCount ?? getJeonseDealCount(rentDeals),
    monthlyRentTotalCount:
      histories.monthlyRentTotalCount ?? getMonthlyRentDealCount(rentDeals),
    rentServerPaged: shouldActivateRent
      ? activeRentMeta.serverPaged
      : currentProperty.rentServerPaged,
  };
}

async function loadSurroundings(property) {
  if (!property?.id) {
    surroundings.value = null;
    surroundingsErrorMessage.value =
      "주택 정보가 없어 주변 시설을 표시할 수 없습니다.";
    return;
  }

  isSurroundingsLoading.value = true;
  surroundingsErrorMessage.value = "";

  try {
    surroundings.value = await fetchSurroundings(property.id, {
      radiusMeters: 1000,
    });
  } catch (error) {
    surroundings.value = null;
    surroundingsErrorMessage.value = "주변 시설 정보를 불러오지 못했습니다.";
  } finally {
    isSurroundingsLoading.value = false;
  }
}

async function openDealHistoryView() {
  if (!selectedPropertyDetail.value) {
    return;
  }

  markDealHistoryClicked();
  saveDetailInteraction();
  saleHistoryPage.value = 1;
  rentHistoryPage.value = 1;
  activeRentHistoryType.value =
    selectedPropertyDetail.value.jeonseTotalCount > 0
      ? "JEONSE"
      : "MONTHLY_RENT";
  window.history.pushState(
    { view: "deal-history", propertyId: selectedPropertyDetail.value.id },
    "",
    getViewUrl("deal-history", selectedPropertyDetail.value.id),
  );
  currentView.value = "deal-history";
  window.scrollTo({ top: 0, behavior: "smooth" });
  await loadHistories(selectedPropertyDetail.value.id, {
    salePage: 1,
    rentPage: 1,
    rentDealType: activeRentHistoryType.value,
  });
}

async function handleBrowserBack(event) {
  const route = event.state?.view ? event.state : getCurrentRoute();

  if (route.view === "deal-history") {
    await loadPropertyDetailView(route.propertyId, "deal-history");
    return;
  }

  if (route.view === "detail") {
    await loadPropertyDetailView(route.propertyId, "detail");
    return;
  }

  if (route.view === "results") {
    await loadRegionSearchView(route);
    return;
  }

  if (route.view === "recommendations") {
    await loadRecommendationResultsView();
    return;
  }

  openHomeView({ updateHistory: false });
  await loadInitialSearchResults();
}

async function restoreViewFromUrl() {
  const route = getCurrentRoute();

  window.history.replaceState(route, "", getRouteUrl(route));

  if (route.view === "deal-history") {
    await loadPropertyDetailView(route.propertyId, "deal-history");
    return;
  }

  if (route.view === "detail") {
    await loadPropertyDetailView(route.propertyId, "detail");
    return;
  }

  if (route.view === "results") {
    await loadRegionSearchView(route);
    return;
  }

  if (route.view === "recommendations") {
    await loadRecommendationResultsView();
    return;
  }

  openHomeView({ updateHistory: false });
  await loadInitialSearchResults();
}

function getCurrentRoute() {
  const params = new URLSearchParams(window.location.search);
  const view = params.get("view");
  const propertyId = normalizePropertyId(params.get("propertyId"));
  const sggCd = normalizeTextParam(params.get("sggCd"));
  const umdNm = normalizeTextParam(params.get("umdNm"));

  if ((view === "detail" || view === "deal-history") && propertyId) {
    return { view, propertyId };
  }

  if (view === "results" && (sggCd || umdNm)) {
    return { view, sggCd, umdNm };
  }

  if (view === "recommendations") {
    return { view };
  }

  return { view: "home" };
}

function getRouteUrl(route) {
  if (route.view === "results") {
    const params = new URLSearchParams({ view: "results" });

    if (route.sggCd) {
      params.set("sggCd", route.sggCd);
    }
    if (route.umdNm) {
      params.set("umdNm", route.umdNm);
    }

    return `${window.location.pathname}?${params.toString()}`;
  }

  return getViewUrl(route.view, route.propertyId);
}

function getViewUrl(view, propertyId) {
  if (view === "detail" || view === "deal-history") {
    const params = new URLSearchParams({
      view,
      propertyId: String(propertyId),
    });
    return `${window.location.pathname}?${params.toString()}`;
  }

  if (view === "recommendations") {
    return `${window.location.pathname}?view=recommendations`;
  }

  return window.location.pathname;
}

function normalizePropertyId(propertyId) {
  const normalized = Number(propertyId);
  return Number.isInteger(normalized) && normalized > 0 ? normalized : null;
}

function normalizeTextParam(value) {
  return value?.trim() || "";
}

async function loadRegionSearchView(route) {
  saveDetailInteraction();
  currentView.value = "results";
  selectedPropertyDetail.value = null;
  detailErrorMessage.value = "";
  historiesErrorMessage.value = "";
  searchForm.value = {
    ...searchForm.value,
    sggCd: route.sggCd || searchForm.value.sggCd,
    umdNm: route.umdNm || "",
    name: "",
    propertyType: "",
    dealType: "",
    priceRangeIndex: 0,
    sortIndex: 0,
  };
  await runSearch();
}

async function loadRecommendationResultsView() {
  saveDetailInteraction();
  currentView.value = "recommendations";
  selectedPropertyDetail.value = null;
  detailErrorMessage.value = "";
  historiesErrorMessage.value = "";
  window.scrollTo({ top: 0, behavior: "smooth" });
  await loadPropertyRecommendations();
}

async function loadInitialSearchResults() {
  homeResultTab.value = "search";
  searchPage.value = 1;
  await runSearchWithOptions({ highlight: false, scroll: false });
}

function startDetailInteraction(propertyId) {
  if (!isLoggedIn.value) {
    return;
  }

  if (detailInteraction.value?.propertyId === propertyId) {
    updateDetailScrollDepth();
    return;
  }

  detailInteraction.value = {
    propertyId,
    startedAt: Date.now(),
    maxScrollDepthPercent: 0,
    recommendationDetailClicked: false,
    dealHistoryClicked: false,
  };
  updateDetailScrollDepth();
  window.addEventListener("scroll", updateDetailScrollDepth, { passive: true });
}

function saveDetailInteraction() {
  const interaction = consumeDetailInteraction();
  if (!interaction) {
    return;
  }

  savePropertyInteraction(interaction.propertyId, interaction.payload).catch(() => {});
}

function saveDetailInteractionOnPageHide() {
  const interaction = consumeDetailInteraction();
  if (!interaction) {
    return;
  }

  savePropertyInteractionKeepalive(interaction.propertyId, interaction.payload);
}

function consumeDetailInteraction() {
  if (!detailInteraction.value || !isLoggedIn.value) {
    detailInteraction.value = null;
    window.removeEventListener("scroll", updateDetailScrollDepth);
    return null;
  }

  updateDetailScrollDepth();
  const interaction = detailInteraction.value;
  detailInteraction.value = null;
  window.removeEventListener("scroll", updateDetailScrollDepth);

  return {
    propertyId: interaction.propertyId,
    payload: {
      dwellTimeMillis: Math.max(Date.now() - interaction.startedAt, 0),
      maxScrollDepthPercent: interaction.maxScrollDepthPercent,
      recommendationDetailClicked: interaction.recommendationDetailClicked,
      dealHistoryClicked: interaction.dealHistoryClicked,
    },
  };
}

function updateDetailScrollDepth() {
  if (!detailInteraction.value) {
    return;
  }

  const scrollableHeight =
    document.documentElement.scrollHeight - window.innerHeight;
  const depth =
    scrollableHeight <= 0
      ? 100
      : Math.round((window.scrollY / scrollableHeight) * 100);
  detailInteraction.value.maxScrollDepthPercent = Math.min(
    Math.max(detailInteraction.value.maxScrollDepthPercent, depth),
    100,
  );
}

function markRecommendationDetailClicked() {
  if (detailInteraction.value) {
    detailInteraction.value.recommendationDetailClicked = true;
  }
}

function markDealHistoryClicked() {
  if (detailInteraction.value) {
    detailInteraction.value.dealHistoryClicked = true;
  }
}

function sortProperties(properties, selectedSort) {
  if (selectedSort.sortBy === "LATEST") {
    return properties;
  }

  return [...properties].sort((left, right) => {
    if (selectedSort.sortBy === "NAME") {
      const comparison = (left.name || "").localeCompare(
        right.name || "",
        "ko",
      );
      return selectedSort.sortDirection === "ASC" ? comparison : -comparison;
    }

    const leftPrice = getComparablePrice(left);
    const rightPrice = getComparablePrice(right);
    const comparison = leftPrice - rightPrice;

    return selectedSort.sortDirection === "ASC" ? comparison : -comparison;
  });
}

function getComparablePrice(property) {
  if (searchForm.value.dealType === "SALE") {
    return property.latestSalePrice || 0;
  }

  if (
    searchForm.value.dealType === "JEONSE" ||
    searchForm.value.dealType === "MONTHLY_RENT"
  ) {
    return property.latestDeposit || 0;
  }

  return Math.max(property.latestSalePrice || 0, property.latestDeposit || 0);
}

function clearResultHighlightTimer() {
  if (resultHighlightTimer) {
    window.clearTimeout(resultHighlightTimer);
    resultHighlightTimer = null;
  }
}

async function scrollToResults() {
  await nextTick();
  resultPanel.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function getSearchSummary() {
  const selectedRegion = regions.find(
    (region) => region.value === searchForm.value.sggCd,
  );
  const selectedDealType = dealTypes.find(
    (dealType) => dealType.value === searchForm.value.dealType,
  );
  const selectedPropertyType = propertyTypes.find(
    (type) => type.value === searchForm.value.propertyType,
  );
  const selectedPriceRange = priceRanges[searchForm.value.priceRangeIndex];

  return [
    selectedRegion?.label,
    selectedDealType?.label !== "전체" ? selectedDealType?.label : null,
    searchForm.value.umdNm ? `읍면동 ${searchForm.value.umdNm}` : null,
    searchForm.value.name ? `주택명 ${searchForm.value.name}` : null,
    selectedPropertyType?.label !== "전체" ? selectedPropertyType?.label : null,
    selectedPriceRange?.label !== "전체" ? selectedPriceRange?.label : null,
  ].filter(Boolean);
}

function removeEmptyValues(params) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== "" && value !== null && value !== undefined,
    ),
  );
}

function getDisplayPrice(property) {
  if (property.latestSalePrice > 0) {
    return formatPrice(property.latestSalePrice);
  }

  if (property.latestMonthlyRent > 0) {
    return `보증금 ${formatPrice(property.latestDeposit)} / 월세 ${property.latestMonthlyRent.toLocaleString()}만원`;
  }

  if (property.latestDeposit > 0) {
    return formatPrice(property.latestDeposit);
  }

  return "가격 정보 없음";
}

function getPropertyDetail(property) {
  const type = getPropertyTypeLabel(property.propertyType);
  const buildYear = getBuildYearLabel(property.buildYear);

  return `${type} · ${buildYear}`;
}

function getRecommendationScoreValue() {
  return recommendationScore.value?.score ?? 0;
}

function getRecommendationScoreStyle() {
  return {
    "--score-progress": `${Math.min(
      Math.max(getRecommendationScoreValue(), 0),
      100,
    )}%`,
  };
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

function getRecommendationScoreLabel(score) {
  if (score === null || score === undefined) {
    return "평가 불가";
  }

  return `${score}점`;
}

function getRecommendationConditions() {
  return sortConditionsByPriority(recommendationScore.value?.conditions || []);
}

function getVisibleRecommendationConditions() {
  const conditions = getRecommendationConditions();
  return conditions.slice(0, 4);
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

function getPreferenceTypeLabel(code) {
  return preferenceTypeLabels[code] || code || "조건";
}

function getRecommendationConditionTitle(condition) {
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

function getConditionValueLabel(condition) {
  const label = formatPreferenceValue(condition);
  return label ? `(${label})` : "";
}

function formatPreferenceValue(condition) {
  const value = condition.value;
  if (value === null || value === undefined || value === "") {
    return "";
  }

  switch (condition.code) {
    case "SALE_PRICE":
    case "DEPOSIT":
      return `${formatStoredMoneyValue(value)} 이하`;
    case "MONTHLY_RENT":
      return `${formatMonthlyRentValue(value)} 이하`;
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

function formatStoredMoneyValue(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return value;
  }

  const priceInManwon = amount >= 1000000 ? amount / 10000 : amount;
  return formatPrice(priceInManwon);
}

function formatMonthlyRentValue(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return value;
  }

  const rentInManwon = amount >= 10000 ? amount / 10000 : amount;
  return `${rentInManwon.toLocaleString()}만원`;
}

function getPropertyAddress(property) {
  return (
    [property?.umdNm, property?.jibun].filter(Boolean).join(" ") ||
    property?.sggCd ||
    "-"
  );
}

function getDetailSummaryItems(property) {
  if (!property) {
    return [];
  }

  const representativeDeal = getRecentDealRows(property)[0];

  return [
    getPropertyTypeLabel(property.propertyType),
    representativeDeal?.floor,
    representativeDeal?.area !== "-"
      ? `전용면적 ${representativeDeal.area}`
      : null,
    getBuildYearLabel(property.buildYear),
  ].filter(Boolean);
}

function getLatestRentPrice(property) {
  if (!(property.latestDeposit > 0)) {
    return "-";
  }

  if (property.latestMonthlyRent > 0) {
    return `${formatPrice(property.latestDeposit)} / 월세 ${property.latestMonthlyRent.toLocaleString()}만원`;
  }

  return formatPrice(property.latestDeposit);
}

function formatArea(area) {
  if (!area) {
    return "-";
  }

  return `${Number(area).toLocaleString()}㎡`;
}

function formatDealDate(date) {
  return date ? date.replaceAll("-", ".") : "-";
}

function getRentDealType(deal) {
  return deal.monthlyRent > 0 ? "월세" : "전세";
}

function getRentDealPrice(deal) {
  if (deal.monthlyRent > 0) {
    return `보증금 ${formatPrice(deal.deposit)} / 월세 ${deal.monthlyRent.toLocaleString()}만원`;
  }

  return formatPrice(deal.deposit);
}

function getJeonseDealCount(rentDeals) {
  return rentDeals.filter((deal) => !deal.monthlyRent || deal.monthlyRent === 0)
    .length;
}

function getMonthlyRentDealCount(rentDeals) {
  return rentDeals.filter((deal) => deal.monthlyRent > 0).length;
}

function getRentDealCount(rentDeals, dealType) {
  return dealType === "MONTHLY_RENT"
    ? getMonthlyRentDealCount(rentDeals)
    : getJeonseDealCount(rentDeals);
}

function getRentDealsByType(property, dealType) {
  if (!property) {
    return [];
  }

  const storedDeals = property.rentDealsByType?.[dealType];
  if (storedDeals) {
    return storedDeals;
  }

  const rentDeals = property.rentDeals || [];
  return dealType === "MONTHLY_RENT"
    ? rentDeals.filter((deal) => deal.monthlyRent > 0)
    : rentDeals.filter((deal) => !deal.monthlyRent || deal.monthlyRent === 0);
}

function getRentMetaByType(property, dealType) {
  if (!property) {
    return {
      page: 1,
      size: historyPageSize,
      totalCount: 0,
      totalPages: 1,
      serverPaged: false,
    };
  }

  return (
    property.rentMetaByType?.[dealType] || {
      page: property.rentPage || 1,
      size: property.rentSize || historyPageSize,
      totalCount: getDealCountByType(property, dealType),
      totalPages:
        property.rentTotalPages ||
        calculatePageCount(getDealCountByType(property, dealType)),
      serverPaged: property.rentServerPaged,
    }
  );
}

function getDefaultTrendType(property) {
  if (
    (property.saleTotalCount || 0) > 0 ||
    (property.saleDeals || []).length > 0
  ) {
    return "SALE";
  }

  if (
    (property.jeonseTotalCount || 0) > 0 ||
    getRentDealsByType(property, "JEONSE").length > 0
  ) {
    return "JEONSE";
  }

  if (
    (property.monthlyRentTotalCount || 0) > 0 ||
    getRentDealsByType(property, "MONTHLY_RENT").length > 0
  ) {
    return "MONTHLY_RENT";
  }

  return "SALE";
}

function isFavoriteProperty(propertyId) {
  const normalizedPropertyId = normalizePropertyId(propertyId);
  return favoritePropertyIds.value.includes(normalizedPropertyId);
}

async function loadFavoritePropertyIds() {
  if (!isLoggedIn.value) {
    favoritePropertyIds.value = [];
    return;
  }

  try {
    const properties = await getFavoriteProperties();
    favoritePropertyIds.value = properties
      .map((property) => normalizePropertyId(property.propertyId))
      .filter(Boolean);
  } catch (error) {
    favoritePropertyIds.value = [];
  }
}

async function toggleFavoriteProperty(propertyId) {
  const normalizedPropertyId = normalizePropertyId(propertyId);
  if (!normalizedPropertyId || isFavoritePropertyLoading.value) {
    return;
  }

  if (!isLoggedIn.value) {
    detailErrorMessage.value = "로그인 후 관심 주택을 설정할 수 있습니다.";
    return;
  }

  const wasFavorite = isFavoriteProperty(normalizedPropertyId);
  isFavoritePropertyLoading.value = true;
  detailErrorMessage.value = "";

  favoritePropertyIds.value = wasFavorite
    ? favoritePropertyIds.value.filter((id) => id !== normalizedPropertyId)
    : [...favoritePropertyIds.value, normalizedPropertyId];

  try {
    if (wasFavorite) {
      await deleteFavoriteProperty(normalizedPropertyId);
    } else {
      await addFavoriteProperty(normalizedPropertyId);
    }
  } catch (error) {
    favoritePropertyIds.value = wasFavorite
      ? [...favoritePropertyIds.value, normalizedPropertyId]
      : favoritePropertyIds.value.filter((id) => id !== normalizedPropertyId);
    detailErrorMessage.value = wasFavorite
      ? "관심 주택을 해제하지 못했습니다."
      : "관심 주택을 등록하지 못했습니다.";
  } finally {
    isFavoritePropertyLoading.value = false;
  }
}

function getPrimaryDealType(property) {
  if (property.latestSalePrice > 0) {
    return "매매";
  }

  if (property.latestMonthlyRent > 0) {
    return "월세";
  }

  if (property.latestDeposit > 0) {
    return "전세";
  }

  return "거래";
}

function getDealRowsByType(property, dealType) {
  const saleRows = (property.saleDeals || []).map((deal) => ({
    id: `sale-${deal.id}`,
    type: "매매",
    date: deal.dealDate,
    price: formatPrice(deal.dealAmount),
    numericPrice: deal.dealAmount || 0,
    area: formatArea(deal.exclusiveArea),
    floor: `${deal.floor}층`,
  }));
  const jeonseRows = getRentDealsByType(property, "JEONSE").map((deal) => ({
    id: `jeonse-${deal.id}`,
    type: "전세",
    date: deal.dealDate,
    price: getRentDealPrice(deal),
    numericPrice: deal.deposit || 0,
    area: formatArea(deal.exclusiveArea),
    floor: `${deal.floor}층`,
  }));
  const monthlyRentRows = getRentDealsByType(property, "MONTHLY_RENT").map(
    (deal) => ({
      id: `monthly-rent-${deal.id}`,
      type: "월세",
      date: deal.dealDate,
      price: getRentDealPrice(deal),
      numericPrice: deal.monthlyRent || 0,
      area: formatArea(deal.exclusiveArea),
      floor: `${deal.floor}층`,
    }),
  );

  const rowsByType = {
    SALE: saleRows,
    JEONSE: jeonseRows,
    MONTHLY_RENT: monthlyRentRows,
  };

  return rowsByType[dealType].sort((left, right) => {
    const dateCompare = (right.date || "").localeCompare(left.date || "");
    if (dateCompare !== 0) {
      return dateCompare;
    }

    return right.numericPrice - left.numericPrice;
  });
}

function getAllDealRows(property) {
  return [
    ...getDealRowsByType(property, "SALE"),
    ...getDealRowsByType(property, "JEONSE"),
    ...getDealRowsByType(property, "MONTHLY_RENT"),
  ].sort((left, right) => {
    const dateCompare = (right.date || "").localeCompare(left.date || "");
    if (dateCompare !== 0) {
      return dateCompare;
    }

    return right.numericPrice - left.numericPrice;
  });
}

function getLimitedDealRows(property, dealType) {
  return getDealRowsByType(property, dealType).slice(0, 5);
}

function getPaginatedDealRows(property, dealType, page) {
  const rows = getDealRowsByType(property, dealType);
  const isServerPaged =
    dealType === "SALE"
      ? property.saleServerPaged
      : getRentMetaByType(property, dealType).serverPaged;

  if (isServerPaged) {
    return rows.slice(0, historyPageSize);
  }

  const startIndex = (page - 1) * historyPageSize;
  return rows.slice(startIndex, startIndex + historyPageSize);
}

function getDealPageCount(property, dealType) {
  if (!property) {
    return 1;
  }

  if (dealType === "SALE") {
    return property.saleTotalPages || 1;
  }

  return getRentMetaByType(property, dealType).totalPages || 1;
}

function calculatePageCount(totalCount) {
  return Math.max(Math.ceil(totalCount / historyPageSize), 1);
}

function getVisibleHistoryPages(property, dealType, currentPage) {
  const pageCount = getDealPageCount(property, dealType);
  const groupStart = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const groupEnd = Math.min(groupStart + 4, pageCount);
  return Array.from(
    { length: groupEnd - groupStart + 1 },
    (_, index) => groupStart + index,
  );
}

async function setSaleHistoryPage(page) {
  if (isSaleHistoryLoading.value || page === saleHistoryPage.value) {
    return;
  }

  saleHistoryPage.value = Math.min(
    Math.max(page, 1),
    getDealPageCount(selectedPropertyDetail.value, "SALE"),
  );
  await loadHistories(selectedPropertyDetail.value.id, {
    salePage: saleHistoryPage.value,
    rentPage: rentHistoryPage.value,
    rentDealType: activeRentHistoryType.value,
    updateRent: false,
  });
}

async function setRentHistoryPage(page) {
  if (isRentHistoryLoading.value || page === rentHistoryPage.value) {
    return;
  }

  rentHistoryPage.value = Math.min(
    Math.max(page, 1),
    getDealPageCount(selectedPropertyDetail.value, activeRentHistoryType.value),
  );
  await loadHistories(selectedPropertyDetail.value.id, {
    salePage: saleHistoryPage.value,
    rentPage: rentHistoryPage.value,
    rentDealType: activeRentHistoryType.value,
    updateSale: false,
  });
}

async function selectRentHistoryType(dealType) {
  if (isRentHistoryLoading.value || dealType === activeRentHistoryType.value) {
    return;
  }

  activeRentHistoryType.value = dealType;
  rentHistoryPage.value = 1;
  await loadHistories(selectedPropertyDetail.value.id, {
    salePage: saleHistoryPage.value,
    rentPage: 1,
    rentDealType: dealType,
    updateSale: false,
  });
}

function getDealCountByType(property, dealType) {
  if (!property) {
    return 0;
  }

  if (dealType === "SALE") {
    return property.saleTotalCount || 0;
  }

  if (dealType === "MONTHLY_RENT") {
    return property.monthlyRentTotalCount || 0;
  }

  return property.jeonseTotalCount || 0;
}

function getTotalDealCount(property) {
  if (!property) {
    return 0;
  }

  const totalCount =
    (property.saleTotalCount || 0) +
    (property.jeonseTotalCount || 0) +
    (property.monthlyRentTotalCount || 0);

  return totalCount || getAllDealRows(property).length;
}

function getRecentDealRows(property) {
  return getAllDealRows(property).slice(0, 5);
}

function getTrendRows(property, dealType = activeTrendType.value) {
  const saleRows = (property.saleDeals || [])
    .map((deal) => ({ date: deal.dealDate, price: deal.dealAmount || 0 }))
    .filter((deal) => deal.price > 0);
  const jeonseRows = getRentDealsByType(property, "JEONSE")
    .map((deal) => ({ date: deal.dealDate, price: deal.deposit || 0 }))
    .filter((deal) => deal.price > 0);
  const monthlyRentRows = getRentDealsByType(property, "MONTHLY_RENT")
    .map((deal) => ({ date: deal.dealDate, price: deal.monthlyRent || 0 }))
    .filter((deal) => deal.price > 0);

  const rowsByType = {
    SALE: saleRows,
    JEONSE: jeonseRows,
    MONTHLY_RENT: monthlyRentRows,
  };

  return rowsByType[dealType]
    .sort((left, right) => (left.date || "").localeCompare(right.date || ""))
    .slice(-8);
}

function getTrendScale(property, dealType = activeTrendType.value) {
  const rows = getTrendRows(property, dealType);
  if (rows.length === 0) {
    return null;
  }

  const prices = rows.map((row) => row.price);
  const minPrice = Math.min(...prices);
  const maxPrice = Math.max(...prices);
  const padding = Math.max((maxPrice - minPrice) * 0.12, maxPrice * 0.04, 1);
  const min = Math.max(minPrice - padding, 0);
  const max = maxPrice + padding;

  return {
    min,
    max,
    range: Math.max(max - min, 1),
  };
}

function getTrendPoints(property, dealType = activeTrendType.value) {
  const rows = getTrendRows(property, dealType);
  const scale = getTrendScale(property, dealType);
  if (!scale) {
    return "";
  }

  return rows
    .map((row, index) => {
      const x =
        rows.length === 1 ? 190 : 78 + (index * 222) / (rows.length - 1);
      const y = 110 - ((row.price - scale.min) / scale.range) * 80;
      return `${x},${y}`;
    })
    .join(" ");
}

function getTrendYAxisTicks(property, dealType = activeTrendType.value) {
  const scale = getTrendScale(property, dealType);
  if (!scale) {
    return [];
  }

  return [
    { y: 30, label: formatPrice(scale.max) },
    { y: 70, label: formatPrice((scale.max + scale.min) / 2) },
    { y: 110, label: formatPrice(scale.min) },
  ];
}

function getTrendDots(property, dealType = activeTrendType.value) {
  const points = getTrendPoints(property, dealType);
  if (!points) {
    return [];
  }

  const rows = getTrendRows(property, dealType);

  return points.split(" ").map((point, index) => {
    const [x, y] = point.split(",").map(Number);
    return {
      x,
      y,
      label: formatPrice(rows[index]?.price),
      rawPrice: rows[index]?.price || 0,
      date: rows[index]?.date
        ? rows[index].date.slice(2, 7).replace("-", ".")
        : "",
    };
  });
}

function getTrendEmptyText(dealType) {
  const trendType = trendTypes.find((type) => type.value === dealType);
  return `${trendType?.label || "거래"} 가격 추이 데이터가 없습니다.`;
}

function getFeaturedFacilities() {
  const facilities = surroundings.value?.facilities || [];
  return facilityLegendItems.map(
    (item) =>
      facilities
        .filter((facility) => facility.type === item.value)
        .sort(
          (left, right) =>
            (left.distanceMeters ?? Infinity) -
            (right.distanceMeters ?? Infinity),
        )[0] || {
        type: item.value,
        name: `${item.label} 없음`,
        missing: true,
      },
  );
}

function getMapFacilities() {
  return getFeaturedFacilities().filter((facility) => !facility.missing);
}

function getFacilityCategoryLabel(facility) {
  if (facility.missing) {
    return `${(surroundings.value?.radiusMeters || 1000).toLocaleString()}m 반경 기준`;
  }

  return getFacilityTypeLabel(facility.type);
}

function getFacilityTypeLabel(type) {
  return facilityTypeLabels[type] || "시설";
}

function getFacilityDistanceLabel(distanceMeters) {
  if (distanceMeters === null || distanceMeters === undefined) {
    return "-";
  }

  if (distanceMeters >= 1000) {
    return `${Number((distanceMeters / 1000).toFixed(1))}km`;
  }

  return `${distanceMeters.toLocaleString()}m`;
}

function getFacilityMarkerClass(facility) {
  return [
    "facility-marker",
    `facility-marker-${String(facility.type || "")
      .toLowerCase()
      .replace("_", "-")}`,
  ];
}

function getFacilityMarkerText(facility) {
  const markerTextByType = {
    BUS: "버",
    SUBWAY: "역",
    HOSPITAL: "+",
    CCTV: "C",
    PARK: "공",
  };
  return markerTextByType[facility.type] || "F";
}

function getFacilityMoveLabel(facility) {
  if (facility.missing) {
    return "반경 내 없음";
  }

  const distance = Number(facility.distanceMeters);
  if (!Number.isFinite(distance)) {
    return "-";
  }

  if (distance > 1200 || facility.type === "HOSPITAL") {
    return `차량 ${Math.max(Math.round(distance / 450), 1)}분`;
  }

  return `도보 ${Math.max(Math.round(distance / 70), 1)}분`;
}

function getMapMarkers() {
  const facilities = getMapFacilities();
  const centerLat = Number(selectedPropertyDetail.value?.latitude);
  const centerLng = Number(selectedPropertyDetail.value?.longitude);
  if (!Number.isFinite(centerLat) || !Number.isFinite(centerLng)) {
    return [];
  }

  const points = [
    { latitude: centerLat, longitude: centerLng },
    ...facilities,
  ].filter(
    (point) =>
      Number.isFinite(Number(point.latitude)) &&
      Number.isFinite(Number(point.longitude)),
  );
  const latitudes = points.map((point) => Number(point.latitude));
  const longitudes = points.map((point) => Number(point.longitude));
  const minLat = Math.min(...latitudes);
  const maxLat = Math.max(...latitudes);
  const minLng = Math.min(...longitudes);
  const maxLng = Math.max(...longitudes);
  const latRange = Math.max(maxLat - minLat, 0.002);
  const lngRange = Math.max(maxLng - minLng, 0.002);

  return facilities
    .filter(
      (facility) =>
        Number.isFinite(Number(facility.latitude)) &&
        Number.isFinite(Number(facility.longitude)),
    )
    .map((facility) => ({
      ...facility,
      top: `${Math.min(Math.max(8 + ((maxLat - Number(facility.latitude)) / latRange) * 84, 8), 92)}%`,
      left: `${Math.min(Math.max(8 + ((Number(facility.longitude) - minLng) / lngRange) * 84, 8), 92)}%`,
    }));
}

function getFacilityMapTransform() {
  return {
    transform: `translate(${mapPan.value.x}px, ${mapPan.value.y}px) scale(${mapZoom.value})`,
  };
}

function changeMapZoom(delta) {
  mapZoom.value = Math.min(
    Math.max(Number((mapZoom.value + delta).toFixed(1)), 0.8),
    1.8,
  );
}

function resetFacilityMap() {
  mapZoom.value = 1;
  mapPan.value = { x: 0, y: 0 };
  stopMapDrag();
}

function startMapDrag(event) {
  if (event.button !== undefined && event.button !== 0) {
    return;
  }

  isMapDragging.value = true;
  mapDragStart = {
    pointerId: event.pointerId,
    x: event.clientX,
    y: event.clientY,
    panX: mapPan.value.x,
    panY: mapPan.value.y,
  };
  event.currentTarget.setPointerCapture?.(event.pointerId);
}

function moveMapDrag(event) {
  if (!isMapDragging.value || !mapDragStart) {
    return;
  }

  mapPan.value = {
    x: Math.min(
      Math.max(mapDragStart.panX + event.clientX - mapDragStart.x, -180),
      180,
    ),
    y: Math.min(
      Math.max(mapDragStart.panY + event.clientY - mapDragStart.y, -120),
      120,
    ),
  };
}

function stopMapDrag() {
  isMapDragging.value = false;
  mapDragStart = null;
}

function getBuildYearLabel(buildYear) {
  return buildYear ? `${buildYear}년 준공` : "준공연도 미상";
}

function getPropertyTypeLabel(propertyType) {
  const type = propertyTypes.find((item) => item.value === propertyType);
  return type?.label || "주택";
}

function formatPrice(price) {
  if (!price) {
    return "0만원";
  }

  if (price >= 10000) {
    const hundredMillion = price / 10000;
    return `${Number(hundredMillion.toFixed(1))}억원`;
  }

  return `${price.toLocaleString()}만원`;
}
</script>

<template>
  <main class="app-shell">
    <AppHeader @home="openHomeView" />

    <Transition name="view-switch" mode="out-in" appear>
    <div v-if="currentView === 'home'" class="view-panel home-view-panel">
      <section class="hero-section" aria-labelledby="home-title">
        <div class="hero-copy">
          <h1 id="home-title">내 조건에 맞는 주거 정보를 찾아보세요</h1>
          <p>
            부산 지역 실거래가를 기준으로 관심 지역과 주택 후보를 비교합니다.
          </p>
        </div>
      </section>

      <form
        class="search-card"
        aria-label="실거래가 검색"
        @submit.prevent="handleSearch"
      >
        <div class="deal-tabs" aria-label="거래 유형">
          <button
            v-for="dealType in dealTypes"
            :key="dealType.value"
            :class="[
              'deal-tab',
              { active: searchForm.dealType === dealType.value },
            ]"
            type="button"
            @click="selectDealType(dealType.value)"
          >
            {{ dealType.label }}
          </button>
        </div>

        <div class="search-grid">
          <label>
            <span>지역</span>
            <select v-model="searchForm.sggCd">
              <option
                v-for="region in regions"
                :key="region.value"
                :value="region.value"
              >
                {{ region.label }}
              </option>
            </select>
          </label>
          <label>
            <span>읍면동</span>
            <input
              v-model.trim="searchForm.umdNm"
              type="text"
              placeholder="예: 우동"
            />
          </label>
          <label>
            <span>주택명</span>
            <input
              v-model.trim="searchForm.name"
              type="search"
              placeholder="예: 해운대"
            />
          </label>
          <label>
            <span>주택 유형</span>
            <select v-model="searchForm.propertyType">
              <option
                v-for="type in propertyTypes"
                :key="type.value"
                :value="type.value"
              >
                {{ type.label }}
              </option>
            </select>
          </label>
          <label>
            <span>가격 범위</span>
            <select v-model.number="searchForm.priceRangeIndex">
              <option
                v-for="(range, index) in priceRanges"
                :key="range.label"
                :value="index"
              >
                {{ range.label }}
              </option>
            </select>
          </label>
          <button class="search-button" type="submit" :disabled="isLoading">
            {{ isLoading ? "검색 중" : "검색하기" }}
          </button>
        </div>

        <p v-if="errorMessage" class="form-message" role="alert">
          {{ errorMessage }}
        </p>
      </form>

      <section class="content-grid">
        <article
          ref="resultPanel"
          :class="[
            'panel',
            'tabbed-result-panel',
            { 'searched-panel': isResultHighlighted },
          ]"
        >
          <div class="panel-title-row tabbed-title-row">
            <div>
              <h2>
                {{
                  homeResultTab === "recommendation"
                    ? "맞춤 추천 결과"
                    : resultTitle
                }}
              </h2>
            </div>
            <div
              v-if="homeResultTab === 'search'"
              class="panel-actions"
              :class="{ 'is-placeholder': !hasSearched }"
            >
              <span v-if="hasSearched">{{ resultCountText }}</span>
              <span v-else aria-hidden="true">0건</span>
              <label
                class="sort-control compact-sort-control"
                :class="{ 'is-hidden': !hasSearched }"
              >
                <span>정렬</span>
                <select
                  v-model.number="searchForm.sortIndex"
                  @change="handleSortChange"
                >
                  <option
                    v-for="(sort, index) in sortOptions"
                    :key="sort.label"
                    :value="index"
                  >
                    {{ sort.label }}
                  </option>
                </select>
              </label>
            </div>
            <div v-else class="panel-actions">
              <span v-if="isLoggedIn">
                {{ recommendationCards.length.toLocaleString() }}건
              </span>
              <label class="sort-control compact-sort-control">
                <span>정렬</span>
                <select aria-label="맞춤 추천 정렬" disabled>
                  <option>추천순</option>
                </select>
              </label>
            </div>
            <div class="home-result-tabs" role="tablist" aria-label="홈 결과 유형">
              <button
                v-for="tab in homeResultTabs"
                :key="tab.value"
                type="button"
                role="tab"
                :aria-selected="homeResultTab === tab.value"
                :class="{ active: homeResultTab === tab.value }"
                @click="homeResultTab = tab.value"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>

          <template v-if="homeResultTab === 'recommendation'">
            <div v-if="!isLoggedIn" class="recommendation-login-panel">
              <strong>로그인하면 맞춤 추천 주택을 확인할 수 있습니다.</strong>
              <p>
                저장한 예산, 면적, 지역, 주변시설 조건을 기준으로 어울리는
                주택을 추천해드립니다.
              </p>
              <button
                type="button"
                class="secondary-button"
                @click="goToLoginForRecommendation"
              >
                로그인하러가기
              </button>
            </div>

            <p
              v-else-if="isRecommendationListLoading"
              class="empty-message compact-empty-message"
            >
              맞춤 추천 주택을 불러오고 있습니다.
            </p>

            <p
              v-else-if="recommendationListErrorMessage"
              class="form-message"
              role="alert"
            >
              {{ recommendationListErrorMessage }}
            </p>

            <p
              v-else-if="recommendationCards.length === 0"
              class="empty-message compact-empty-message"
            >
              아직 추천할 주택이 없습니다. 맞춤 조건을 저장하거나 주택을 더
              둘러보세요.
            </p>

            <ul v-else class="personalized-list">
              <li
                v-for="item in displayedRecommendationCards"
                :key="item.id || `${item.rank}-${item.name}`"
              >
                <button
                  :class="[
                    'personalized-item',
                    'property-card-button',
                    { 'is-clickable': item.id },
                  ]"
                  type="button"
                  :disabled="!item.id"
                  @click="openPropertyDetail(item.id)"
                >
                  <div class="home-image personalized-image" aria-hidden="true">
                    <span>{{ item.rank }}</span>
                  </div>
                  <div class="personalized-info">
                    <h3>{{ item.name }}</h3>
                    <p>{{ item.region }}</p>
                    <strong>{{ item.price }}</strong>
                  </div>
                  <div class="personalized-meta">
                    <span
                      :class="[
                        'score-pill',
                        getRecommendationScoreLevelClass(item.score),
                      ]"
                    >
                      {{ getRecommendationScoreLabel(item.score) }}
                    </span>
                    <em>{{ item.detail }}</em>
                  </div>
                </button>
              </li>
            </ul>

            <button
              v-if="hasMoreRecommendations"
              class="secondary-button full-result-button"
              type="button"
              @click="openRecommendationResultsView"
            >
              전체 결과 보기
            </button>
          </template>

          <template v-else>
            <div
              v-if="hasSearched"
              :class="[
                'result-notice',
                { 'is-searched': hasSearched, 'is-error': errorMessage },
              ]"
            >
              <strong>{{ resultNotice }}</strong>
              <div
                v-if="appliedSearchSummary.length > 0"
                class="condition-chips"
                aria-label="적용된 검색 조건"
              >
                <span
                  v-for="condition in appliedSearchSummary"
                  :key="condition"
                  >{{ condition }}</span
                >
              </div>
            </div>

            <p
              v-if="hasSearched && displayedHomes.length === 0"
              class="empty-message"
            >
              조건에 맞는 실거래가 검색 결과가 없습니다.
            </p>
            <p v-else-if="!hasSearched" class="empty-message compact-empty-message">
              검색 조건을 선택하고 검색하면 실거래가 결과가 표시됩니다.
            </p>

            <div v-else class="recommend-list">
              <button
                v-for="home in displayedHomes"
                :key="`${home.rank}-${home.name}`"
                :class="[
                  'home-card',
                  'property-card-button',
                  { 'is-clickable': home.id },
                ]"
                type="button"
                :disabled="!home.id"
                @click="openPropertyDetail(home.id)"
              >
                <div class="home-image" aria-hidden="true">
                  <span>{{ home.rank }}</span>
                </div>
                <div>
                  <h3>{{ home.name }}</h3>
                  <p>{{ home.region }}</p>
                  <strong>{{ home.price }}</strong>
                </div>
                <em>{{ home.detail }}</em>
              </button>
            </div>

            <button
              v-if="hasMoreResults"
              class="secondary-button full-result-button"
              type="button"
              @click="openResultsView"
            >
              전체 결과 보기
            </button>
          </template>
        </article>
      </section>
    </div>

    <section
      v-else-if="currentView === 'results'"
      class="result-page"
      aria-labelledby="all-result-title"
    >
      <div class="result-page-header">
        <div>
          <p class="result-kicker">All Search Results</p>
          <h1 id="all-result-title">실거래가 전체 검색 결과</h1>
          <p>{{ resultNotice }}</p>
        </div>
        <button class="secondary-button" type="button" @click="openHomeView">
          홈으로 돌아가기
        </button>
      </div>

      <div class="result-toolbar">
        <div class="condition-chips" aria-label="적용된 검색 조건">
          <span v-for="condition in appliedSearchSummary" :key="condition">{{
            condition
          }}</span>
        </div>

        <label class="sort-control">
          <span>정렬</span>
          <select
            v-model.number="searchForm.sortIndex"
            @change="handleSortChange"
          >
            <option
              v-for="(sort, index) in sortOptions"
              :key="sort.label"
              :value="index"
            >
              {{ sort.label }}
            </option>
          </select>
        </label>
      </div>

      <p v-if="errorMessage" class="form-message" role="alert">
        {{ errorMessage }}
      </p>
      <p v-else-if="allResultRows.length === 0" class="empty-message">
        조건에 맞는 실거래가 검색 결과가 없습니다.
      </p>

      <div v-else class="all-result-list">
        <button
          v-for="home in allResultRows"
          :key="`${home.rank}-${home.name}`"
          class="result-row property-card-button is-clickable"
          type="button"
          @click="openPropertyDetail(home.id)"
        >
          <div class="home-image result-image" aria-hidden="true">
            <span>{{ home.rank }}</span>
          </div>
          <div>
            <h2>{{ home.name }}</h2>
            <p>{{ home.region }}</p>
          </div>
          <div>
            <span>주택 유형</span>
            <strong>{{ home.propertyType }}</strong>
          </div>
          <div>
            <span>가격</span>
            <strong>{{ home.price }}</strong>
          </div>
          <em>{{ home.buildYearLabel }}</em>
        </button>
      </div>

      <div
        v-if="hasSearched && searchTotalPages > 1"
        class="history-pagination"
        aria-label="검색 결과 페이지"
        :aria-busy="isLoading"
        style="margin-top: 2rem; justify-content: center;"
      >
        <button
          type="button"
          :disabled="searchPage === 1"
          @click="changeSearchPage(searchPage - 1)"
        >
          이전
        </button>

        <button
          v-for="page in getVisibleSearchPages()"
          :key="`search-page-${page}`"
          :class="{ active: searchPage === page }"
          type="button"
          :aria-current="searchPage === page ? 'page' : undefined"
          @click="changeSearchPage(page)"
        >
           {{ page }}
        </button>

        <button
           type="button"
           :disabled="searchPage === searchTotalPages"
           @click="changeSearchPage(searchPage + 1)"
        >
          다음
        </button>
      </div>


    </section>

    <section
      v-else-if="currentView === 'recommendations'"
      class="result-page"
      aria-labelledby="recommendation-result-title"
    >
      <div class="result-page-header">
        <div>
          <p class="result-kicker">Personalized Recommendations</p>
          <h1 id="recommendation-result-title">맞춤 추천 전체 결과</h1>
          <p>
            저장한 맞춤 조건과 최근 확인한 주택을 반영한 추천 목록입니다.
          </p>
        </div>
        <button class="secondary-button" type="button" @click="openHomeView">
          홈으로 돌아가기
        </button>
      </div>

      <div class="result-toolbar">
        <div class="condition-chips" aria-label="추천 결과 요약">
          <span>{{ recommendationCards.length.toLocaleString() }}건</span>
          <span>추천순</span>
        </div>

        <label class="sort-control">
          <span>정렬</span>
          <select aria-label="맞춤 추천 정렬" disabled>
            <option>추천순</option>
          </select>
        </label>
      </div>

      <div v-if="!isLoggedIn" class="recommendation-login-panel">
        <strong>로그인하면 맞춤 추천 주택을 확인할 수 있습니다.</strong>
        <p>
          저장한 예산, 면적, 지역, 주변시설 조건을 기준으로 어울리는 주택을
          추천해드립니다.
        </p>
        <button
          type="button"
          class="secondary-button"
          @click="goToLoginForRecommendation"
        >
          로그인하러가기
        </button>
      </div>

      <p
        v-else-if="isRecommendationListLoading"
        class="empty-message compact-empty-message"
      >
        맞춤 추천 주택을 불러오고 있습니다.
      </p>
      <p
        v-else-if="recommendationListErrorMessage"
        class="form-message"
        role="alert"
      >
        {{ recommendationListErrorMessage }}
      </p>
      <p v-else-if="recommendationCards.length === 0" class="empty-message">
        아직 추천할 주택이 없습니다. 맞춤 조건을 저장하거나 주택을 더
        둘러보세요.
      </p>

      <div v-else class="all-result-list">
        <button
          v-for="home in recommendationCards"
          :key="home.id || `${home.rank}-${home.name}`"
          class="result-row recommendation-result-row property-card-button is-clickable"
          type="button"
          @click="openPropertyDetail(home.id)"
        >
          <div class="home-image result-image" aria-hidden="true">
            <span>{{ home.rank }}</span>
          </div>
          <div>
            <h2>{{ home.name }}</h2>
            <p>{{ home.region }}</p>
          </div>
          <div>
            <span>추천 점수</span>
            <strong>{{ getRecommendationScoreLabel(home.score) }}</strong>
          </div>
          <div>
            <span>가격</span>
            <strong>{{ home.price }}</strong>
          </div>
          <em>{{ home.detail }}</em>
        </button>
      </div>
    </section>

    <section v-else class="detail-page" aria-label="집 상세 정보">
      <p v-if="isDetailLoading" id="deal-detail-title" class="empty-message">
        거래 상세 정보를 불러오는 중입니다.
      </p>
      <p v-else-if="detailErrorMessage" class="form-message" role="alert">
        {{ detailErrorMessage }}
      </p>

      <template v-else-if="selectedPropertyDetail">
        <template v-if="currentView !== 'deal-history'">
          <section class="detail-hero">
            <div>
              <p class="detail-breadcrumb">홈 &gt; 검색 결과 &gt; 집 상세 정보</p>
              <div class="detail-title-row">
                <h1 id="deal-detail-title">
                  {{ selectedPropertyDetail.name }}
                </h1>
                <span class="deal-badge">{{
                  getPrimaryDealType(selectedPropertyDetail)
                }}</span>
                <button
                  :class="[
                    'favorite-star-button',
                    { active: isFavoriteProperty(selectedPropertyDetail.id) },
                  ]"
                  type="button"
                  :disabled="isFavoritePropertyLoading"
                  :aria-pressed="isFavoriteProperty(selectedPropertyDetail.id)"
                  :aria-label="
                    isFavoriteProperty(selectedPropertyDetail.id)
                      ? '관심 주택 해제'
                      : '관심 주택 등록'
                  "
                  @click="toggleFavoriteProperty(selectedPropertyDetail.id)"
                >
                  ★
                </button>
              </div>
              <p class="detail-address">
                {{ getPropertyAddress(selectedPropertyDetail) }}
              </p>
              <p
                v-if="isPropertyAiSummaryLoading"
                class="property-ai-summary loading"
              >
                <span class="property-ai-summary-spinner" aria-hidden="true"></span>
                <span>AI가 이 집의 적합도를 요약하고 있습니다.</span>
              </p>
              <p
                v-else-if="propertyAiSummary"
                :class="[
                  'property-ai-summary',
                  { highlighted: isPropertyAiSummaryHighlighted },
                ]"
              >
                {{ propertyAiSummary }}
              </p>
              <p
                v-else-if="propertyAiSummaryErrorMessage"
                class="property-ai-summary muted"
              >
                {{ propertyAiSummaryErrorMessage }}
              </p>
              <div class="detail-tags">
                <span
                  v-for="item in getDetailSummaryItems(selectedPropertyDetail)"
                  :key="item"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <aside class="fit-score-panel" aria-label="사용자 맞춤 조건 적합도">
              <div class="fit-score-title-row">
                <h2>내 조건 적합도</h2>
                <button
                  v-if="isLoggedIn && recommendationScore"
                  class="fit-detail-button"
                  type="button"
                  @click="openRecommendationScoreDetail"
                >
                  적합도 상세 보기
                </button>
              </div>

              <div v-if="!isLoggedIn" class="fit-login-content">
                <p>
                  로그인하면 예산, 선호 지역, 생활 편의 조건을 기준으로 이
                  거래가 내 조건에 맞는지 확인할 수 있습니다.
                </p>
                <button
                  class="secondary-button"
                  type="button"
                  @click="goToLoginForRecommendation"
                >
                  로그인하러가기
                </button>
              </div>

              <p
                v-else-if="isRecommendationScoreLoading"
                class="compact-empty-message"
              >
                맞춤 적합도를 계산하고 있습니다.
              </p>

              <p
                v-else-if="recommendationScoreErrorMessage"
                class="form-message"
                role="alert"
              >
                {{ recommendationScoreErrorMessage }}
              </p>

              <div v-else-if="recommendationScore" class="fit-score-content">
                <div
                  :class="[
                    'fit-score-circle',
                    getRecommendationScoreLevelClass(),
                    {
                      muted:
                        recommendationScore.recommendationStatus ===
                        'NO_EVALUABLE_CONDITION',
                    },
                  ]"
                  :style="getRecommendationScoreStyle()"
                >
                  <strong>{{
                    recommendationScore.score === null
                      ? "-"
                      : recommendationScore.score
                  }}</strong>
                  <span>점</span>
                </div>

                <ul
                  v-if="getRecommendationConditions().length > 0"
                  class="fit-condition-list"
                >
                  <li
                    v-for="condition in getVisibleRecommendationConditions()"
                    :key="`${condition.code}-${condition.priority}`"
                    :class="getConditionStatusClass(condition)"
                  >
                    <span class="fit-condition-icon" aria-hidden="true">
                      {{ getConditionStatusIcon(condition) }}
                    </span>
                    <div>
                      <strong>{{
                        getRecommendationConditionTitle(condition)
                      }}</strong>
                    </div>
                    <small class="fit-condition-preference">{{
                      getConditionValueLabel(condition)
                    }}</small>
                  </li>
                </ul>

              </div>
            </aside>
          </section>

          <section class="detail-dashboard">
            <article class="panel trend-panel">
              <div class="panel-title-row">
                <h2>거래 가격 추이</h2>
                <span
                  >{{
                    trendTypes.find((type) => type.value === activeTrendType)
                      ?.label
                  }}
                  기준</span
                >
              </div>
              <div class="deal-tabs compact-tabs" aria-label="거래 유형 표시">
                <button
                  v-for="trendType in trendTypes"
                  :key="trendType.value"
                  :class="[
                    'deal-tab',
                    { active: activeTrendType === trendType.value },
                  ]"
                  type="button"
                  @click="activeTrendType = trendType.value"
                >
                  {{ trendType.label }}
                </button>
              </div>
              <div class="trend-chart" aria-label="거래 가격 추이 차트">
                <svg
                  v-if="getTrendPoints(selectedPropertyDetail, activeTrendType)"
                  viewBox="0 0 320 140"
                  role="img"
                  aria-label="최근 거래 가격 추이"
                >
                  <g
                    v-for="tick in getTrendYAxisTicks(
                      selectedPropertyDetail,
                      activeTrendType,
                    )"
                    :key="tick.y"
                  >
                    <line :x1="70" :y1="tick.y" :x2="300" :y2="tick.y" />
                    <text class="trend-axis-label" x="48" :y="tick.y + 4">
                      {{ tick.label }}
                    </text>
                  </g>
                  <polyline
                    :points="
                      getTrendPoints(selectedPropertyDetail, activeTrendType)
                    "
                  />
                  <g
                    v-for="dot in getTrendDots(
                      selectedPropertyDetail,
                      activeTrendType,
                    )"
                    :key="`${dot.x}-${dot.y}`"
                    class="trend-dot-group"
                  >
                    <circle :cx="dot.x" :cy="dot.y" r="3.5" />
                    <circle
                      class="trend-hit-area"
                      :cx="dot.x"
                      :cy="dot.y"
                      r="14"
                      tabindex="0"
                      @pointerenter="hoveredTrendDot = dot"
                      @focus="hoveredTrendDot = dot"
                      @pointerleave="hoveredTrendDot = null"
                      @blur="hoveredTrendDot = null"
                    >
                      <title>{{ dot.date }} {{ dot.label }}</title>
                    </circle>
                  </g>
                  <g v-if="hoveredTrendDot" class="trend-tooltip">
                    <rect
                      :x="Math.min(Math.max(hoveredTrendDot.x - 48, 58), 222)"
                      :y="Math.max(hoveredTrendDot.y - 54, 6)"
                      width="96"
                      height="42"
                      rx="6"
                    />
                    <text
                      :x="Math.min(Math.max(hoveredTrendDot.x, 106), 270)"
                      :y="Math.max(hoveredTrendDot.y - 35, 25)"
                    >
                      {{ hoveredTrendDot.label }}
                    </text>
                    <text
                      class="trend-tooltip-date"
                      :x="Math.min(Math.max(hoveredTrendDot.x, 106), 270)"
                      :y="Math.max(hoveredTrendDot.y - 19, 41)"
                    >
                      {{ hoveredTrendDot.date }}
                    </text>
                  </g>
                </svg>
                <p v-else class="trend-empty-message">
                  {{ getTrendEmptyText(activeTrendType) }}
                </p>
              </div>
            </article>

            <article class="panel recent-deal-panel">
              <div class="panel-title-row">
                <h2>최근 실거래</h2>
                <div class="panel-actions">
                  <span
                    >{{
                      getTotalDealCount(
                        selectedPropertyDetail,
                      ).toLocaleString()
                    }}건</span
                  >
                  <button
                    class="text-action-button"
                    type="button"
                    @click="openDealHistoryView"
                  >
                    전체 거래 보러가기
                  </button>
                </div>
              </div>
              <ul class="recent-detail-list">
                <li
                  v-for="deal in getRecentDealRows(selectedPropertyDetail)"
                  :key="deal.id"
                >
                  <span>{{ formatDealDate(deal.date) }}</span>
                  <strong
                    >{{ deal.price }} <em>({{ deal.type }})</em></strong
                  >
                  <span>{{ deal.floor }}</span>
                </li>
              </ul>
            </article>
          </section>

          <section
            class="panel nearby-map-panel"
            aria-label="주변 생활 편의 시설과 지도"
          >
            <div class="nearby-map-content">
              <div class="nearby-info-section">
                <div class="panel-title-row">
                  <h2>주변 생활 편의 시설</h2>
                  <span v-if="surroundings"
                    >{{ surroundings.radiusMeters.toLocaleString() }}m
                    반경</span
                  >
                </div>

                <p v-if="isSurroundingsLoading" class="empty-message">
                  주변 시설 정보를 불러오는 중입니다.
                </p>
                <p
                  v-else-if="surroundingsErrorMessage"
                  class="form-message"
                  role="alert"
                >
                  {{ surroundingsErrorMessage }}
                </p>
                <ul v-else class="facility-list">
                  <li
                    v-for="facility in getFeaturedFacilities()"
                    :key="`${facility.type}-${facility.name}-${facility.distanceMeters || 'missing'}`"
                    :class="{ missing: facility.missing }"
                  >
                    <span :class="getFacilityMarkerClass(facility)">
                      {{ getFacilityMarkerText(facility) }}
                    </span>
                    <div>
                      <strong>{{ facility.name }}</strong>
                      <p>{{ getFacilityCategoryLabel(facility) }}</p>
                    </div>
                    <em v-if="facility.missing">{{
                      getFacilityMoveLabel(facility)
                    }}</em>
                    <em v-else
                      >{{ getFacilityMoveLabel(facility) }} ({{
                        getFacilityDistanceLabel(facility.distanceMeters)
                      }})</em
                    >
                  </li>
                </ul>
              </div>

              <div class="map-section">
                <div class="map-legend" aria-label="지도 범례">
                  <span v-for="item in facilityLegendItems" :key="item.value">
                    <i
                      :class="`legend-${String(item.value).toLowerCase()}`"
                    ></i>
                    {{ item.label }}
                  </span>
                </div>
                <div
                  :class="['facility-map', { dragging: isMapDragging }]"
                  aria-label="주택과 주변 편의시설 지도"
                >
                  <div
                    class="facility-map-canvas"
                    :style="getFacilityMapTransform()"
                    @pointerdown="startMapDrag"
                    @pointermove="moveMapDrag"
                    @pointerup="stopMapDrag"
                    @pointercancel="stopMapDrag"
                    @pointerleave="stopMapDrag"
                  >
                    <span
                      class="home-map-marker"
                      :style="{ top: '50%', left: '50%' }"
                      >집</span
                    >
                    <span
                      v-for="marker in getMapMarkers()"
                      :key="`${marker.type}-${marker.name}-${marker.latitude}-${marker.longitude}`"
                      :class="getFacilityMarkerClass(marker)"
                      :style="{ top: marker.top, left: marker.left }"
                      :title="`${getFacilityTypeLabel(marker.type)} · ${marker.name}`"
                    >
                      {{ getFacilityMarkerText(marker) }}
                    </span>
                  </div>
                  <div class="map-controls" aria-label="지도 조작">
                    <button
                      type="button"
                      aria-label="지도 확대"
                      @click="changeMapZoom(0.2)"
                    >
                      +
                    </button>
                    <button
                      type="button"
                      aria-label="지도 축소"
                      @click="changeMapZoom(-0.2)"
                    >
                      -
                    </button>
                    <button
                      type="button"
                      aria-label="지도 위치 초기화"
                      @click="resetFacilityMap"
                    >
                      ⟲
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </template>

        <template v-else>
          <section class="detail-header history-page-header">
            <div>
              <p class="detail-breadcrumb">
                홈 &gt; 검색 결과 &gt; 집 상세 정보 &gt; 거래 이력
              </p>
              <h1>전체 거래 이력</h1>
              <p>
                {{ selectedPropertyDetail.name }} ·
                {{ getPropertyAddress(selectedPropertyDetail) }}
              </p>
            </div>

            <div
              class="history-counts header-history-counts"
              aria-label="거래 이력 요약"
            >
              <article>
                <span>매매</span>
                <strong
                  >{{
                    getDealCountByType(
                      selectedPropertyDetail,
                      "SALE",
                    ).toLocaleString()
                  }}건</strong
                >
              </article>
              <article>
                <span>전세</span>
                <strong
                  >{{
                    getDealCountByType(
                      selectedPropertyDetail,
                      "JEONSE",
                    ).toLocaleString()
                  }}건</strong
                >
              </article>
              <article>
                <span>월세</span>
                <strong
                  >{{
                    getDealCountByType(
                      selectedPropertyDetail,
                      "MONTHLY_RENT",
                    ).toLocaleString()
                  }}건</strong
                >
              </article>
            </div>
          </section>

          <p v-if="historiesErrorMessage" class="form-message" role="alert">
            {{ historiesErrorMessage }}
          </p>

          <section class="history-panel-grid" aria-label="전체 거래 이력 목록">
            <article
              class="history-section"
              aria-labelledby="sale-history-title"
            >
              <div class="panel-title-row">
                <h2 id="sale-history-title">매매 거래 이력</h2>
                <span
                  >{{
                    getDealCountByType(
                      selectedPropertyDetail,
                      "SALE",
                    ).toLocaleString()
                  }}건</span
                >
              </div>

              <p
                v-if="getDealCountByType(selectedPropertyDetail, 'SALE') === 0"
                class="empty-message"
              >
                매매 거래 이력이 없습니다.
              </p>
              <template v-else>
                <div class="history-table compact-history-table">
                  <div class="history-table-head">
                    <span>거래일</span>
                    <span>거래금액</span>
                    <span>전용면적</span>
                    <span>층</span>
                  </div>
                  <article
                    v-for="deal in getPaginatedDealRows(
                      selectedPropertyDetail,
                      'SALE',
                      saleHistoryPage,
                    )"
                    :key="deal.id"
                  >
                    <span>{{ formatDealDate(deal.date) }}</span>
                    <strong>{{ deal.price }}</strong>
                    <span>{{ deal.area }}</span>
                    <span>{{ deal.floor }}</span>
                  </article>
                </div>
                <div
                  class="history-pagination"
                  aria-label="매매 거래 이력 페이지"
                  :aria-busy="isSaleHistoryLoading"
                >
                  <button
                    type="button"
                    :disabled="saleHistoryPage === 1"
                    @click="setSaleHistoryPage(saleHistoryPage - 1)"
                  >
                    이전
                  </button>
                  <button
                    v-for="page in getVisibleHistoryPages(
                      selectedPropertyDetail,
                      'SALE',
                      saleHistoryPage,
                    )"
                    :key="`sale-page-${page}`"
                    :class="{ active: saleHistoryPage === page }"
                    type="button"
                    :aria-current="
                      saleHistoryPage === page ? 'page' : undefined
                    "
                    @click="setSaleHistoryPage(page)"
                  >
                    {{ page }}
                  </button>
                  <button
                    type="button"
                    :disabled="
                      saleHistoryPage ===
                      getDealPageCount(selectedPropertyDetail, 'SALE')
                    "
                    @click="setSaleHistoryPage(saleHistoryPage + 1)"
                  >
                    다음
                  </button>
                </div>
              </template>
            </article>

            <article
              class="history-section"
              aria-labelledby="rent-history-title"
            >
              <div class="panel-title-row">
                <h2 id="rent-history-title">전월세 거래 이력</h2>
                <div class="history-title-actions">
                  <div
                    class="deal-tabs history-tabs"
                    aria-label="전월세 거래 유형"
                  >
                    <button
                      type="button"
                      :class="[
                        'deal-tab',
                        { active: activeRentHistoryType === 'JEONSE' },
                      ]"
                      @click="selectRentHistoryType('JEONSE')"
                    >
                      전세
                    </button>
                    <button
                      type="button"
                      :class="[
                        'deal-tab',
                        { active: activeRentHistoryType === 'MONTHLY_RENT' },
                      ]"
                      @click="selectRentHistoryType('MONTHLY_RENT')"
                    >
                      월세
                    </button>
                  </div>
                  <span
                    >{{
                      getDealCountByType(
                        selectedPropertyDetail,
                        activeRentHistoryType,
                      ).toLocaleString()
                    }}건</span
                  >
                </div>
              </div>

              <p
                v-if="
                  getDealCountByType(
                    selectedPropertyDetail,
                    activeRentHistoryType,
                  ) === 0
                "
                class="empty-message"
              >
                {{ activeRentHistoryType === "JEONSE" ? "전세" : "월세" }} 거래
                이력이 없습니다.
              </p>
              <template v-else>
                <div
                  :class="[
                    'history-table',
                    'rent-history-table',
                    activeRentHistoryType === 'MONTHLY_RENT'
                      ? 'monthly-rent-history-table'
                      : 'compact-history-table',
                  ]"
                >
                  <div class="history-table-head">
                    <span>거래일</span>
                    <span>거래금액</span>
                    <span>전용면적</span>
                    <span>층</span>
                  </div>
                  <article
                    v-for="deal in getPaginatedDealRows(
                      selectedPropertyDetail,
                      activeRentHistoryType,
                      rentHistoryPage,
                    )"
                    :key="deal.id"
                  >
                    <span>{{ formatDealDate(deal.date) }}</span>
                    <strong>{{ deal.price }}</strong>
                    <span>{{ deal.area }}</span>
                    <span>{{ deal.floor }}</span>
                  </article>
                </div>
                <div
                  class="history-pagination"
                  aria-label="전월세 거래 이력 페이지"
                  :aria-busy="isRentHistoryLoading"
                >
                  <button
                    type="button"
                    :disabled="rentHistoryPage === 1"
                    @click="setRentHistoryPage(rentHistoryPage - 1)"
                  >
                    이전
                  </button>
                  <button
                    v-for="page in getVisibleHistoryPages(
                      selectedPropertyDetail,
                      activeRentHistoryType,
                      rentHistoryPage,
                    )"
                    :key="`${activeRentHistoryType}-page-${page}`"
                    :class="{ active: rentHistoryPage === page }"
                    type="button"
                    :aria-current="
                      rentHistoryPage === page ? 'page' : undefined
                    "
                    @click="setRentHistoryPage(page)"
                  >
                    {{ page }}
                  </button>
                  <button
                    type="button"
                    :disabled="
                      rentHistoryPage ===
                      getDealPageCount(
                        selectedPropertyDetail,
                        activeRentHistoryType,
                      )
                    "
                    @click="setRentHistoryPage(rentHistoryPage + 1)"
                  >
                    다음
                  </button>
                </div>
              </template>
            </article>
          </section>
        </template>
      </template>
    </section>
    </Transition>
  </main>
</template>
