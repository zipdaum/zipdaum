<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import {
  getUserPreferenceRegionCandidates,
  getUserPreferences,
  saveUserPreferences,
} from "../api/preference";

const router = useRouter();

const form = ref(createDefaultForm());
const isLoading = ref(true);
const isSaving = ref(false);
const message = ref("");
const toastMessage = ref("");
const toastVariant = ref("success");
const toastKey = ref(0);
const regionKeyword = ref("");
const regionCandidates = ref([]);
const isSearchingRegions = ref(false);
const hasSearchedRegions = ref(false);
const isComposingRegionKeyword = ref(false);
let regionSearchSeq = 0;
let regionSearchTimerId = null;
let shouldSkipNextRegionInput = false;
let toastTimer = null;

const MAX_SELECTED_REGION_COUNT = 10;
const REGION_SEARCH_DELAY_MS = 120;

const selectedFacilityCount = computed(() =>
  Object.values(form.value.facilities).filter(Boolean).length,
);
const shouldShowRegionSearchPanel = computed(() =>
  regionKeyword.value.trim().length >= 2 || regionCandidates.value.length > 0,
);
const hasNoRegionSearchResult = computed(() =>
  hasSearchedRegions.value &&
  !isSearchingRegions.value &&
  regionKeyword.value.trim().length >= 2 &&
  regionCandidates.value.length === 0,
);
const toastLabel = computed(() =>
  toastVariant.value === "error" ? "오류" : "완료",
);

onMounted(loadPreferences);
onBeforeUnmount(() => {
  clearRegionSearchTimer();
  clearToastTimer();
});

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
      showMessage("맞춤 조건을 불러오지 못했습니다.");
    }
  } finally {
    isLoading.value = false;
  }
}

async function handleSavePreferences() {
  message.value = "";

  const payload = toPreferencePayload();

  if (payload.length === 0) {
    showMessage("저장할 맞춤 조건을 하나 이상 입력해주세요.");
    return;
  }

  isSaving.value = true;

  try {
    await saveUserPreferences(payload);
    showToast("맞춤 조건을 저장했습니다.");
  } catch (error) {
    showToast(
      getErrorMessage(error, "맞춤 조건을 저장하지 못했습니다."),
      "error",
    );
  } finally {
    isSaving.value = false;
  }
}

function resetForm() {
  form.value = createDefaultForm();
  regionKeyword.value = "";
  regionCandidates.value = [];
  hasSearchedRegions.value = false;
  message.value = "";
}

function createDefaultForm() {
  return {
    salePrice: "",
    deposit: "",
    monthlyRent: "",
    area: "",
    buildYear: "",
    regions: [],
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
        addLoadedRegion(nextForm, preference.value);
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
  for (const region of form.value.regions) {
    addPreference(items, "REGION", region.value);
  }

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

function addLoadedRegion(targetForm, value) {
  const regionValue = toFullRegionName(value);
  if (!regionValue) {
    return;
  }
  if (hasParentRegionValue(targetForm.regions, regionValue)) {
    return;
  }

  if (isParentRegionDisplayName(regionValue)) {
    targetForm.regions = targetForm.regions.filter(
      (region) => !isParentOfDisplayName(regionValue, region.value),
    );
  }

  if (isSelectedRegionValue(targetForm.regions, regionValue)) {
    return;
  }

  targetForm.regions.push({
    key: createRegionKeyFromDisplayName(regionValue),
    value: regionValue,
    displayName: toShortRegionName(regionValue),
  });
}

function handleRegionKeywordInput() {
  if (shouldSkipNextRegionInput) {
    shouldSkipNextRegionInput = false;
    return;
  }
  if (isComposingRegionKeyword.value) {
    return;
  }
  searchRegions();
}

function handleRegionKeywordCompositionEnd() {
  isComposingRegionKeyword.value = false;
  shouldSkipNextRegionInput = true;
  searchRegions(0);
}

function searchRegions(delayMs = REGION_SEARCH_DELAY_MS) {
  const keyword = regionKeyword.value.trim();

  if (keyword.length < 2) {
    clearRegionSearchTimer();
    regionSearchSeq += 1;
    regionCandidates.value = [];
    isSearchingRegions.value = false;
    hasSearchedRegions.value = false;
    return;
  }

  clearRegionSearchTimer();
  const seq = ++regionSearchSeq;
  isSearchingRegions.value = true;
  hasSearchedRegions.value = false;

  if (delayMs <= 0) {
    fetchRegionCandidates(keyword, seq);
    return;
  }

  regionSearchTimerId = window.setTimeout(() => {
    regionSearchTimerId = null;
    fetchRegionCandidates(keyword, seq);
  }, REGION_SEARCH_DELAY_MS);
}

async function fetchRegionCandidates(keyword, seq) {
  try {
    const candidates = await getUserPreferenceRegionCandidates(keyword);
    if (seq !== regionSearchSeq) {
      return;
    }
    regionCandidates.value = candidates.filter(
      (candidate) => !isExcludedRegionCandidate(candidate),
    );
    hasSearchedRegions.value = true;
  } catch (error) {
    if (seq === regionSearchSeq) {
      regionCandidates.value = [];
      hasSearchedRegions.value = true;
    }
  } finally {
    if (seq === regionSearchSeq) {
      isSearchingRegions.value = false;
    }
  }
}

function selectRegion(candidate) {
  if (
    !candidate?.displayName ||
    isExactSelectedRegion(candidate) ||
    hasSelectedParentRegion(candidate)
  ) {
    return;
  }

  const regionKey = createRegionKey(candidate);
  const nextRegions = isParentRegion(candidate)
    ? form.value.regions.filter((region) => !isNestedChildRegion(region, candidate))
    : form.value.regions;

  if (nextRegions.length >= MAX_SELECTED_REGION_COUNT) {
    return;
  }

  form.value.regions = nextRegions;
  form.value.regions.push({
    key: regionKey,
    value: candidate.displayName,
    displayName: toShortRegionName(candidate.displayName),
  });
  clearRegionSearchTimer();
  regionSearchSeq += 1;
  regionKeyword.value = "";
  regionCandidates.value = [];
  isSearchingRegions.value = false;
  hasSearchedRegions.value = false;
}

function removeRegion(displayName) {
  form.value.regions = form.value.regions.filter(
    (region) => region.displayName !== displayName,
  );
}

function clearRegions() {
  form.value.regions = [];
}

function isExcludedRegionCandidate(candidate) {
  return isExactSelectedRegion(candidate) || hasSelectedParentRegion(candidate);
}

function isExactSelectedRegion(candidate) {
  return form.value.regions.some((region) => isSameRegion(region, candidate));
}

function hasSelectedParentRegion(candidate) {
  if (isParentRegion(candidate)) {
    return false;
  }
  return form.value.regions.some((region) => isParentOfCandidate(region, candidate));
}

function isSelectedRegionValue(regions, displayName) {
  return regions.some((region) =>
    region.key === createRegionKeyFromDisplayName(displayName) ||
    normalizeRegionName(region.value) === normalizeRegionName(displayName)
  );
}

function hasParentRegionValue(regions, displayName) {
  return regions.some((region) => isParentOfDisplayName(region.value, displayName));
}

function isSameRegion(selectedRegion, candidate) {
  if (selectedRegion.key === createRegionKey(candidate)) {
    return true;
  }
  return normalizeRegionName(selectedRegion.value) === normalizeRegionName(candidate.displayName);
}

function isNestedChildRegion(selectedRegion, parentCandidate) {
  if (!isParentRegion(parentCandidate) || isParentRegionValue(selectedRegion)) {
    return false;
  }
  if (isParentOfCandidate(selectedRegion, parentCandidate)) {
    return false;
  }
  return isParentOfDisplayName(parentCandidate.displayName, selectedRegion.value);
}

function isParentOfCandidate(selectedRegion, candidate) {
  const selectedKey = selectedRegion.key;
  const candidateKey = createRegionKey(candidate);
  if (isParentRegionKey(selectedKey, candidateKey)) {
    return true;
  }
  return isParentOfDisplayName(selectedRegion.value, candidate.displayName);
}

function isParentRegionKey(parentKey, childKey) {
  if (!parentKey || !childKey || parentKey.startsWith("name:") || childKey.startsWith("name:")) {
    return false;
  }

  const [selectedSggCd, selectedUmdCd] = parentKey.split(":");
  const [candidateSggCd, candidateUmdCd] = childKey.split(":");
  return selectedUmdCd === "sgg" &&
    candidateUmdCd !== "sgg" &&
    selectedSggCd &&
    selectedSggCd === candidateSggCd &&
    Boolean(candidateUmdCd);
}

function isParentOfDisplayName(parent, child) {
  const normalizedParent = normalizeRegionName(parent);
  const normalizedChild = normalizeRegionName(child);
  return normalizedParent !== normalizedChild &&
    normalizedChild.startsWith(normalizedParent);
}

function isParentRegion(candidate) {
  return !String(candidate?.umdCd || "").trim();
}

function isParentRegionValue(region) {
  if (!region) {
    return false;
  }
  if (region.key && !region.key.startsWith("name:")) {
    return region.key.endsWith(":sgg");
  }
  return /[군구]$/.test(normalizeRegionName(region.value));
}

function isParentRegionDisplayName(displayName) {
  return /[군구]$/.test(normalizeRegionName(displayName));
}

function createRegionKey(candidate) {
  if (!candidate) {
    return "";
  }
  const sggCd = String(candidate.sggCd || "").trim();
  const umdCd = String(candidate.umdCd || "").trim();

  if (sggCd && umdCd) {
    return `${sggCd}:${umdCd}`;
  }
  if (sggCd) {
    return `${sggCd}:sgg`;
  }
  return createRegionKeyFromDisplayName(candidate.displayName);
}

function createRegionKeyFromDisplayName(displayName) {
  return `name:${normalizeRegionName(displayName)}`;
}

function normalizeRegionName(value) {
  return String(value || "")
    .replace(/^부산광역시\s*/, "")
    .replace(/\s+/g, "");
}

function toShortRegionName(value) {
  return String(value || "")
    .trim()
    .replace(/^부산광역시\s*/, "");
}

function toFullRegionName(value) {
  const regionName = String(value || "").trim();
  if (!regionName) {
    return "";
  }
  if (regionName.startsWith("부산광역시")) {
    return regionName;
  }
  return `부산광역시 ${regionName}`;
}

function clearRegionSearchTimer() {
  if (regionSearchTimerId !== null) {
    window.clearTimeout(regionSearchTimerId);
    regionSearchTimerId = null;
  }
}

function showMessage(text) {
  message.value = text;
}

function showToast(text, variant = "success") {
  toastMessage.value = text;
  toastVariant.value = variant;
  toastKey.value += 1;
  clearToastTimer();

  toastTimer = window.setTimeout(() => {
    toastMessage.value = "";
    toastTimer = null;
  }, 2500);
}

function clearToastTimer() {
  if (toastTimer) {
    window.clearTimeout(toastTimer);
    toastTimer = null;
  }
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
        <h1 id="preference-setting-title">맞춤 조건 설정</h1>
        <p>맞춤형 매물 추천을 위해 선호 조건을 설정하세요.</p>
      </div>
      <button class="ghost-button" type="button" @click="goMyPage">
        마이페이지
      </button>
    </section>

    <p v-if="isLoading" class="empty-message">맞춤 조건을 불러오는 중입니다.</p>

    <form v-else class="preference-setting-layout" @submit.prevent="handleSavePreferences">
      <div class="preference-main-grid">
        <section class="preference-setting-panel" aria-labelledby="base-condition-title">
          <div class="panel-title-row">
            <h2 id="base-condition-title">기본 조건</h2>
          </div>

          <div class="condition-table">
            <div class="condition-table-head" aria-hidden="true">
              <span>항목</span>
              <span>값</span>
              <span>단위</span>
            </div>
            <label class="condition-row">
              <span>매매가</span>
              <input
                v-model.trim="form.salePrice"
                inputmode="numeric"
                placeholder="80000"
                type="text"
              />
              <span>만원</span>
            </label>
            <label class="condition-row">
              <span>보증금</span>
              <input
                v-model.trim="form.deposit"
                inputmode="numeric"
                placeholder="50000"
                type="text"
              />
              <span>만원</span>
            </label>
            <label class="condition-row">
              <span>월세</span>
              <input
                v-model.trim="form.monthlyRent"
                inputmode="numeric"
                placeholder="300"
                type="text"
              />
              <span>만원</span>
            </label>
            <label class="condition-row">
              <span>면적</span>
              <input
                v-model.trim="form.area"
                inputmode="decimal"
                placeholder="84.5"
                type="text"
              />
              <span>m²</span>
            </label>
            <label class="condition-row">
              <span>건축연도</span>
              <input
                v-model.trim="form.buildYear"
                inputmode="numeric"
                placeholder="2010"
                type="text"
              />
              <span>년</span>
            </label>
          </div>
        </section>

        <div class="preference-side-stack">
          <section class="preference-setting-panel" aria-labelledby="region-condition-title">
            <div class="panel-title-row">
              <h2 id="region-condition-title">선호 지역</h2>
            </div>

            <div class="region-search-box">
              <label class="preference-single-field">
                <span>지역 검색</span>
                <input
                  v-model="regionKeyword"
                  autocomplete="off"
                  placeholder="해운대구 우동"
                  type="search"
                  @compositionstart="isComposingRegionKeyword = true"
                  @compositionend="handleRegionKeywordCompositionEnd"
                  @input="handleRegionKeywordInput"
                  @keydown.enter.prevent="searchRegions(0)"
                />
              </label>

              <div v-if="shouldShowRegionSearchPanel" class="region-search-panel">
                <div v-if="regionCandidates.length > 0" class="region-candidate-list">
                  <button
                    v-for="candidate in regionCandidates"
                    :key="`${candidate.sggCd}-${candidate.umdCd || 'sgg'}-${candidate.displayName}`"
                    type="button"
                    @click="selectRegion(candidate)"
                  >
                    {{ candidate.displayName }}
                  </button>
                </div>
                <p v-else-if="isSearchingRegions" class="region-search-state">
                  지역을 검색하는 중입니다.
                </p>
                <p v-else-if="hasNoRegionSearchResult" class="region-search-state">
                  검색 결과가 없습니다.
                </p>
              </div>
            </div>

            <div class="selected-region-summary">
              <span>
                선택된 지역
                ({{ form.regions.length }}/{{ MAX_SELECTED_REGION_COUNT }})
              </span>
              <button
                v-if="form.regions.length > 0"
                type="button"
                @click="clearRegions"
              >
                전체 삭제
              </button>
            </div>

            <div class="selected-region-list">
              <button
                v-for="region in form.regions"
                :key="region.key"
                class="selected-region-chip"
                type="button"
                @click="removeRegion(region.displayName)"
              >
                {{ region.displayName }}
                <span aria-hidden="true">×</span>
              </button>
            </div>
          </section>

          <section class="preference-setting-panel" aria-labelledby="facility-condition-title">
            <div class="panel-title-row">
              <h2 id="facility-condition-title">생활 편의 조건</h2>
              <span>{{ selectedFacilityCount.toLocaleString() }}/5 선택</span>
            </div>

            <div class="facility-toggle-grid">
              <label :class="{ selected: form.facilities.BUS }">
                <span>버스</span>
                <input v-model="form.facilities.BUS" type="checkbox" />
              </label>
              <label :class="{ selected: form.facilities.SUBWAY }">
                <span>지하철</span>
                <input v-model="form.facilities.SUBWAY" type="checkbox" />
              </label>
              <label :class="{ selected: form.facilities.HOSPITAL }">
                <span>병원</span>
                <input v-model="form.facilities.HOSPITAL" type="checkbox" />
              </label>
              <label :class="{ selected: form.facilities.CCTV }">
                <span>CCTV</span>
                <input v-model="form.facilities.CCTV" type="checkbox" />
              </label>
              <label :class="{ selected: form.facilities.PARK }">
                <span>공원</span>
                <input v-model="form.facilities.PARK" type="checkbox" />
              </label>
            </div>
          </section>
        </div>
      </div>

      <p
        v-if="message"
        class="preference-setting-message"
        role="alert"
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

      <div
        v-if="toastMessage"
        :key="toastKey"
        class="preference-toast"
        :class="`preference-toast-${toastVariant}`"
        role="status"
        aria-live="polite"
      >
        <strong>{{ toastLabel }}</strong>
        <span>{{ toastMessage }}</span>
      </div>
    </form>
  </main>
</template>

<style scoped src="../assets/preference-setting.css"></style>
