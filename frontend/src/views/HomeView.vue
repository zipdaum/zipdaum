<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import {
  getPropertyDealHistories as fetchPropertyDealHistories,
  getPropertyDetail as fetchPropertyDetail,
  getSurroundings as fetchSurroundings,
  searchProperties,
} from "../api/property";

const dealTypes = [
  { label: "전체", value: "" },
  { label: "매매", value: "SALE" },
  { label: "전세", value: "JEONSE" },
  { label: "월세", value: "MONTHLY_RENT" },
];

const regions = [
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

const searchForm = ref({
  sggCd: "26350",
  umdNm: "",
  name: "",
  propertyType: "",
  dealType: "",
  priceRangeIndex: 0,
  sortIndex: 0,
});

const currentView = ref("home");
const searchResults = ref([]);
const hasSearched = ref(false);
const isLoading = ref(false);
const isResultHighlighted = ref(false);
const errorMessage = ref("");
const isDetailLoading = ref(false);
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
const mapZoom = ref(1);
const mapPan = ref({ x: 0, y: 0 });
const isMapDragging = ref(false);
const saleHistoryPage = ref(1);
const rentHistoryPage = ref(1);
const favoritePropertyIds = ref([]);
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
let mapDragStart = null;

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
  await restoreViewFromUrl();
  window.addEventListener("popstate", handleBrowserBack);
});

onUnmounted(() => {
  clearResultHighlightTimer();
  stopMapDrag();
  window.removeEventListener("popstate", handleBrowserBack);
});

const userSummary = [
  { label: "예산", value: "3억 - 5억" },
  { label: "전용면적", value: "50㎡ - 84㎡" },
  { label: "거래유형", value: "매매, 전세" },
  { label: "생활편의", value: "교통, 학교" },
];

const recommendedHomes = [
  {
    rank: 1,
    name: "해운대 아이파크",
    region: "해운대구 우동",
    price: "12.8억원",
    detail: "매매 기준",
  },
  {
    rank: 2,
    name: "삼익비치",
    region: "수영구 남천동",
    price: "9.6억원",
    detail: "매매 기준",
  },
  {
    rank: 3,
    name: "대연 힐스테이트푸르지오",
    region: "남구 대연동",
    price: "6.4억원",
    detail: "매매 기준",
  },
];

const recentDeals = [
  { name: "해운대 아이파크", price: "12.8억원", date: "2024.05.20" },
  { name: "삼익비치", price: "9.6억원", date: "2024.05.19" },
  { name: "동래 래미안 아이파크", price: "7.2억원", date: "2024.05.18" },
];

const regionTrends = [
  { name: "해운대구", change: "+0.8%" },
  { name: "수영구", change: "+1.2%" },
  { name: "동래구", change: "-0.4%" },
];

const displayedHomes = computed(() => {
  if (!hasSearched.value) {
    return recommendedHomes;
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

const resultCountText = computed(() => {
  if (isLoading.value) {
    return "검색 중";
  }

  if (searchResults.value.length > 0) {
    return `${searchResults.value.length.toLocaleString()}건`;
  }

  if (hasSearched.value) {
    return "0건";
  }

  return "추천";
});

const resultTitle = computed(() => {
  if (hasSearched.value) {
    return "실거래가 검색 결과";
  }

  return "부산 추천 지역 TOP 3";
});

const resultNotice = computed(() => {
  if (!hasSearched.value) {
    return "검색 조건을 입력하면 이 영역이 실거래가 결과로 바뀝니다.";
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
  searchForm.value.sortIndex = 0;
  await runSearch();
}

async function runSearch() {
  isLoading.value = true;
  isResultHighlighted.value = true;
  errorMessage.value = "";
  hasSearched.value = true;
  appliedSearchSummary.value = getSearchSummary();
  clearResultHighlightTimer();
  await scrollToResults();

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
    });

    const properties = await searchProperties(params);
    searchResults.value = sortProperties(properties, selectedSort);
  } catch (error) {
    searchResults.value = [];
    errorMessage.value =
      "실거래가 검색 결과를 불러오지 못했습니다. 백엔드 서버와 검색 조건을 확인해주세요.";
  } finally {
    isLoading.value = false;
    resultHighlightTimer = window.setTimeout(() => {
      isResultHighlighted.value = false;
      resultHighlightTimer = null;
    }, 1600);
    await scrollToResults();
  }
}

async function handleSortChange() {
  if (!hasSearched.value) {
    return;
  }

  await runSearch();
}

function openResultsView() {
  currentView.value = "results";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openHomeView({ updateHistory = true } = {}) {
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
  resetFacilityMap();
  saleHistoryPage.value = 1;
  rentHistoryPage.value = 1;
  hoveredTrendDot.value = null;
  window.scrollTo({ top: 0, behavior: "smooth" });

  try {
    if (
      !selectedPropertyDetail.value ||
      selectedPropertyDetail.value.id !== normalizedPropertyId
    ) {
      const detail = await fetchPropertyDetail(normalizedPropertyId);
      selectedPropertyDetail.value = createPropertyDetailState(detail);
    }

    isDetailLoading.value = false;
    await Promise.all([
      loadHistories(selectedPropertyDetail.value.id, {
        salePage: 1,
        rentPage: 1,
        rentDealType: "JEONSE",
      }),
      loadSurroundings(selectedPropertyDetail.value),
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

function createPropertyDetailState(detail) {
  return {
    ...detail,
    saleDeals: [],
    rentDeals: [],
    rentDealsByType: createEmptyRentDealsByType(),
    rentMetaByType: createEmptyRentMetaByType(),
    ...emptyHistoryMeta,
  };
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

  openHomeView({ updateHistory: false });
}

async function restoreViewFromUrl() {
  const route = getCurrentRoute();

  window.history.replaceState(
    route,
    "",
    getViewUrl(route.view, route.propertyId),
  );

  if (route.view === "deal-history") {
    await loadPropertyDetailView(route.propertyId, "deal-history");
    return;
  }

  if (route.view === "detail") {
    await loadPropertyDetailView(route.propertyId, "detail");
    return;
  }

  openHomeView({ updateHistory: false });
}

function getCurrentRoute() {
  const params = new URLSearchParams(window.location.search);
  const view = params.get("view");
  const propertyId = normalizePropertyId(params.get("propertyId"));

  if ((view === "detail" || view === "deal-history") && propertyId) {
    return { view, propertyId };
  }

  return { view: "home" };
}

function getViewUrl(view, propertyId) {
  if (view === "detail" || view === "deal-history") {
    const params = new URLSearchParams({
      view,
      propertyId: String(propertyId),
    });
    return `${window.location.pathname}?${params.toString()}`;
  }

  return window.location.pathname;
}

function normalizePropertyId(propertyId) {
  const normalized = Number(propertyId);
  return Number.isInteger(normalized) && normalized > 0 ? normalized : null;
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
  return favoritePropertyIds.value.includes(propertyId);
}

function toggleFavoriteProperty(propertyId) {
  if (isFavoriteProperty(propertyId)) {
    favoritePropertyIds.value = favoritePropertyIds.value.filter(
      (id) => id !== propertyId,
    );
    return;
  }

  favoritePropertyIds.value = [...favoritePropertyIds.value, propertyId];
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

    <template v-if="currentView === 'home'">
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

      <section class="summary-card" aria-labelledby="summary-title">
        <div class="section-heading">
          <p>내 맞춤 요약</p>
          <h2 id="summary-title">
            설정한 조건에 가까운 거래를 우선 확인하세요
          </h2>
        </div>
        <div class="summary-list">
          <article
            v-for="item in userSummary"
            :key="item.label"
            class="summary-item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </section>

      <section class="content-grid">
        <article
          ref="resultPanel"
          :class="[
            'panel',
            'recommend-panel',
            { 'searched-panel': isResultHighlighted },
          ]"
        >
          <div class="panel-title-row">
            <div>
              <p class="result-kicker">
                {{ hasSearched ? "Search Result" : "Recommendation" }}
              </p>
              <h2>{{ resultTitle }}</h2>
            </div>
            <div class="panel-actions">
              <span>{{ resultCountText }}</span>
              <label
                v-if="hasSearched"
                class="sort-control compact-sort-control"
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
          </div>

          <div
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
        </article>

        <article class="panel">
          <div class="panel-title-row">
            <h2>최근 실거래 하이라이트</h2>
            <a href="#">전체 보기</a>
          </div>

          <ul class="deal-list">
            <li v-for="deal in recentDeals" :key="deal.name">
              <div>
                <strong>{{ deal.name }}</strong>
                <span>{{ deal.date }}</span>
              </div>
              <p>{{ deal.price }}</p>
            </li>
          </ul>
        </article>

        <article class="panel">
          <div class="panel-title-row">
            <h2>관심지역 동향</h2>
            <a href="#">상세 보기</a>
          </div>

          <div class="trend-list">
            <article
              v-for="trend in regionTrends"
              :key="trend.name"
              class="trend-item"
            >
              <div>
                <strong>{{ trend.name }}</strong>
                <span class="spark-line" aria-hidden="true"></span>
              </div>
              <p>{{ trend.change }}</p>
            </article>
          </div>
        </article>
      </section>
    </template>

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
    </section>

    <section v-else class="detail-page" aria-labelledby="deal-detail-title">
      <div
        v-if="isDetailLoading || detailErrorMessage || !selectedPropertyDetail"
        class="detail-header"
      >
        <div v-if="selectedPropertyDetail">
          <p class="result-kicker">Deal Detail</p>
          <h1 id="deal-detail-title">{{ selectedPropertyDetail.name }}</h1>
          <p>{{ getPropertyAddress(selectedPropertyDetail) }}</p>
        </div>
        <div v-else>
          <p class="result-kicker">Deal Detail</p>
          <h1 id="deal-detail-title">거래 상세 조회</h1>
          <p>선택한 거래의 상세 정보를 불러오고 있습니다.</p>
        </div>
      </div>

      <p v-if="isDetailLoading" class="empty-message">
        거래 상세 정보를 불러오는 중입니다.
      </p>
      <p v-else-if="detailErrorMessage" class="form-message" role="alert">
        {{ detailErrorMessage }}
      </p>

      <template v-else-if="selectedPropertyDetail">
        <template v-if="currentView !== 'deal-history'">
          <section class="detail-hero">
            <div>
              <p class="detail-breadcrumb">홈 &gt; 검색 결과 &gt; 거래 상세</p>
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
                  :aria-pressed="isFavoriteProperty(selectedPropertyDetail.id)"
                  aria-label="관심 주택 등록"
                  @click="toggleFavoriteProperty(selectedPropertyDetail.id)"
                >
                  ★
                </button>
              </div>
              <p class="detail-address">
                {{ getPropertyAddress(selectedPropertyDetail) }}
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

            <aside class="guest-fit-panel" aria-label="비회원 조건 적합도 안내">
              <div class="panel-title-row">
                <h2>내 조건 적합도</h2>
              </div>
              <div class="fit-login-content">
                <p>
                  로그인하면 예산, 선호 지역, 생활 편의 조건을 기준으로 이
                  거래가 내 조건에 맞는지 확인할 수 있습니다.
                </p>
                <button class="secondary-button" type="button">
                  로그인하러가기
                </button>
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
                홈 &gt; 검색 결과 &gt; 거래 상세 &gt; 거래 이력
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
  </main>
</template>
