<script setup>
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { searchProperties } from '../api/property'

const dealTypes = [
  { label: '전체', value: '' },
  { label: '매매', value: 'SALE' },
  { label: '전세', value: 'JEONSE' },
  { label: '월세', value: 'MONTHLY_RENT' }
]

const regions = [
  { label: '부산광역시 해운대구', value: '26350' },
  { label: '부산광역시 부산진구', value: '26230' },
  { label: '부산광역시 동래구', value: '26260' },
  { label: '부산광역시 남구', value: '26290' },
  { label: '부산광역시 북구', value: '26320' },
  { label: '부산광역시 사하구', value: '26380' },
  { label: '부산광역시 금정구', value: '26410' },
  { label: '부산광역시 강서구', value: '26440' },
  { label: '부산광역시 연제구', value: '26470' },
  { label: '부산광역시 수영구', value: '26500' },
  { label: '부산광역시 사상구', value: '26530' },
  { label: '부산광역시 기장군', value: '26710' },
  { label: '부산광역시 중구', value: '26110' },
  { label: '부산광역시 서구', value: '26140' },
  { label: '부산광역시 동구', value: '26170' },
  { label: '부산광역시 영도구', value: '26200' }
]

const priceRanges = [
  { label: '전체', minPrice: '', maxPrice: '' },
  { label: '3억 이하', minPrice: '', maxPrice: 30000 },
  { label: '3억 - 5억', minPrice: 30000, maxPrice: 50000 },
  { label: '5억 - 10억', minPrice: 50000, maxPrice: 100000 },
  { label: '10억 이상', minPrice: 100000, maxPrice: '' }
]

const propertyTypes = [
  { label: '전체', value: '' },
  { label: '아파트', value: 'APARTMENT' },
  { label: '연립/다세대', value: 'VILLA' }
]

const sortOptions = [
  { label: '최신순', sortBy: 'LATEST', sortDirection: 'DESC' },
  { label: '가격 높은순', sortBy: 'PRICE', sortDirection: 'DESC' },
  { label: '가격 낮은순', sortBy: 'PRICE', sortDirection: 'ASC' },
  { label: '이름순', sortBy: 'NAME', sortDirection: 'ASC' }
]

const searchForm = ref({
  sggCd: '26350',
  umdNm: '',
  name: '',
  propertyType: '',
  dealType: '',
  priceRangeIndex: 0,
  sortIndex: 0
})

const currentView = ref('home')
const searchResults = ref([])
const hasSearched = ref(false)
const isLoading = ref(false)
const isResultHighlighted = ref(false)
const errorMessage = ref('')
const resultPanel = ref(null)
const appliedSearchSummary = ref([])
let resultHighlightTimer = null

onUnmounted(() => {
  clearResultHighlightTimer()
})

const userSummary = [
  { label: '예산', value: '3억 - 5억' },
  { label: '전용면적', value: '50㎡ - 84㎡' },
  { label: '거래유형', value: '매매, 전세' },
  { label: '생활편의', value: '교통, 학교' }
]

const recommendedHomes = [
  { rank: 1, name: '해운대 아이파크', region: '해운대구 우동', price: '12.8억원', detail: '매매 기준' },
  { rank: 2, name: '삼익비치', region: '수영구 남천동', price: '9.6억원', detail: '매매 기준' },
  { rank: 3, name: '대연 힐스테이트푸르지오', region: '남구 대연동', price: '6.4억원', detail: '매매 기준' }
]

const recentDeals = [
  { name: '해운대 아이파크', price: '12.8억원', date: '2024.05.20' },
  { name: '삼익비치', price: '9.6억원', date: '2024.05.19' },
  { name: '동래 래미안 아이파크', price: '7.2억원', date: '2024.05.18' }
]

const regionTrends = [
  { name: '해운대구', change: '+0.8%' },
  { name: '수영구', change: '+1.2%' },
  { name: '동래구', change: '-0.4%' }
]

const displayedHomes = computed(() => {
  if (!hasSearched.value) {
    return recommendedHomes
  }

  return searchResults.value.slice(0, 5).map(mapPropertyToHomeCard)
})

const allResultRows = computed(() => searchResults.value.map((property, index) => ({
  ...mapPropertyToHomeCard(property, index),
  propertyType: getPropertyTypeLabel(property.propertyType),
  buildYearLabel: getBuildYearLabel(property.buildYear)
})))

const hasMoreResults = computed(() => hasSearched.value && searchResults.value.length > 0)

function mapPropertyToHomeCard(property, index) {
  return {
    rank: index + 1,
    name: property.name || '주택명 미상',
    region: [property.umdNm, property.jibun].filter(Boolean).join(' ') || property.sggCd,
    price: getDisplayPrice(property),
    detail: getPropertyDetail(property)
  }
}

const resultCountText = computed(() => {
  if (isLoading.value) {
    return '검색 중'
  }

  if (searchResults.value.length > 0) {
    return `${searchResults.value.length.toLocaleString()}건`
  }

  if (hasSearched.value) {
    return '0건'
  }

  return '추천'
})

const resultTitle = computed(() => {
  if (hasSearched.value) {
    return '실거래가 검색 결과'
  }

  return '부산 추천 지역 TOP 3'
})

const resultNotice = computed(() => {
  if (!hasSearched.value) {
    return '검색 조건을 입력하면 이 영역이 실거래가 결과로 바뀝니다.'
  }

  if (isLoading.value) {
    return '조건에 맞는 실거래가를 조회하고 있습니다.'
  }

  if (errorMessage.value) {
    return '검색 요청을 완료하지 못했습니다.'
  }

  if (searchResults.value.length === 0) {
    return '검색은 완료됐지만 조건에 맞는 매물이 없습니다.'
  }

  return `검색 완료: 조건에 맞는 실거래가 ${searchResults.value.length.toLocaleString()}건을 찾았습니다.`
})

function selectDealType(dealType) {
  searchForm.value.dealType = dealType
}

async function handleSearch() {
  searchForm.value.sortIndex = 0
  await runSearch()
}

async function runSearch() {
  isLoading.value = true
  isResultHighlighted.value = true
  errorMessage.value = ''
  hasSearched.value = true
  appliedSearchSummary.value = getSearchSummary()
  clearResultHighlightTimer()
  await scrollToResults()

  try {
    const selectedPriceRange = priceRanges[searchForm.value.priceRangeIndex]
    const selectedSort = sortOptions[searchForm.value.sortIndex]
    const params = removeEmptyValues({
      sggCd: searchForm.value.sggCd,
      umdNm: searchForm.value.umdNm,
      name: searchForm.value.name,
      propertyType: searchForm.value.propertyType,
      dealType: searchForm.value.dealType,
      minPrice: selectedPriceRange.minPrice,
      maxPrice: selectedPriceRange.maxPrice,
      sortBy: selectedSort.sortBy,
      sortDirection: selectedSort.sortDirection
    })

    const properties = await searchProperties(params)
    searchResults.value = sortProperties(properties, selectedSort)
  } catch (error) {
    searchResults.value = []
    errorMessage.value = '실거래가 검색 결과를 불러오지 못했습니다. 백엔드 서버와 검색 조건을 확인해주세요.'
  } finally {
    isLoading.value = false
    resultHighlightTimer = window.setTimeout(() => {
      isResultHighlighted.value = false
      resultHighlightTimer = null
    }, 1600)
    await scrollToResults()
  }
}

async function handleSortChange() {
  if (!hasSearched.value) {
    return
  }

  await runSearch()
}

function openResultsView() {
  currentView.value = 'results'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openHomeView() {
  currentView.value = 'home'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function sortProperties(properties, selectedSort) {
  if (selectedSort.sortBy === 'LATEST') {
    return properties
  }

  return [...properties].sort((left, right) => {
    if (selectedSort.sortBy === 'NAME') {
      const comparison = (left.name || '').localeCompare(right.name || '', 'ko')
      return selectedSort.sortDirection === 'ASC' ? comparison : -comparison
    }

    const leftPrice = getComparablePrice(left)
    const rightPrice = getComparablePrice(right)
    const comparison = leftPrice - rightPrice

    return selectedSort.sortDirection === 'ASC' ? comparison : -comparison
  })
}

function getComparablePrice(property) {
  if (searchForm.value.dealType === 'SALE') {
    return property.latestSalePrice || 0
  }

  if (searchForm.value.dealType === 'JEONSE' || searchForm.value.dealType === 'MONTHLY_RENT') {
    return property.latestDeposit || 0
  }

  return Math.max(property.latestSalePrice || 0, property.latestDeposit || 0)
}

function clearResultHighlightTimer() {
  if (resultHighlightTimer) {
    window.clearTimeout(resultHighlightTimer)
    resultHighlightTimer = null
  }
}

async function scrollToResults() {
  await nextTick()
  resultPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function getSearchSummary() {
  const selectedRegion = regions.find((region) => region.value === searchForm.value.sggCd)
  const selectedDealType = dealTypes.find((dealType) => dealType.value === searchForm.value.dealType)
  const selectedPropertyType = propertyTypes.find((type) => type.value === searchForm.value.propertyType)
  const selectedPriceRange = priceRanges[searchForm.value.priceRangeIndex]

  return [
    selectedRegion?.label,
    selectedDealType?.label !== '전체' ? selectedDealType?.label : null,
    searchForm.value.umdNm ? `읍면동 ${searchForm.value.umdNm}` : null,
    searchForm.value.name ? `주택명 ${searchForm.value.name}` : null,
    selectedPropertyType?.label !== '전체' ? selectedPropertyType?.label : null,
    selectedPriceRange?.label !== '전체' ? selectedPriceRange?.label : null
  ].filter(Boolean)
}

function removeEmptyValues(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

function getDisplayPrice(property) {
  if (property.latestSalePrice > 0) {
    return formatPrice(property.latestSalePrice)
  }

  if (property.latestMonthlyRent > 0) {
    return `보증금 ${formatPrice(property.latestDeposit)} / 월세 ${property.latestMonthlyRent.toLocaleString()}만원`
  }

  if (property.latestDeposit > 0) {
    return formatPrice(property.latestDeposit)
  }

  return '가격 정보 없음'
}

function getPropertyDetail(property) {
  const type = getPropertyTypeLabel(property.propertyType)
  const buildYear = getBuildYearLabel(property.buildYear)

  return `${type} · ${buildYear}`
}

function getBuildYearLabel(buildYear) {
  return buildYear ? `${buildYear}년 준공` : '준공연도 미상'
}

function getPropertyTypeLabel(propertyType) {
  const type = propertyTypes.find((item) => item.value === propertyType)
  return type?.label || '주택'
}

function formatPrice(price) {
  if (!price) {
    return '0만원'
  }

  if (price >= 10000) {
    const hundredMillion = price / 10000
    return `${Number(hundredMillion.toFixed(1))}억원`
  }

  return `${price.toLocaleString()}만원`
}
</script>

<template>
  <main class="app-shell">
    <header class="top-bar">
      <a class="brand" href="#" aria-label="집다움 홈" @click.prevent="openHomeView">
        <span class="brand-mark">Z</span>
        <span>집다움</span>
      </a>

      <nav class="main-nav" aria-label="주요 메뉴">
        <a class="active" href="#" @click.prevent="openHomeView">실거래가 검색</a>
        <a href="#">관심지역</a>
        <a href="#">알림</a>
        <a href="#">마이페이지</a>
      </nav>

      <div class="account-actions">
        <button class="ghost-button" type="button">로그인</button>
        <button class="primary-button" type="button">회원가입</button>
      </div>
    </header>

    <template v-if="currentView === 'home'">
      <section class="hero-section" aria-labelledby="home-title">
        <div class="hero-copy">
          <h1 id="home-title">내 조건에 맞는 주거 정보를 찾아보세요</h1>
          <p>부산 지역 실거래가를 기준으로 관심 지역과 주택 후보를 비교합니다.</p>
        </div>
      </section>

      <form class="search-card" aria-label="실거래가 검색" @submit.prevent="handleSearch">
        <div class="deal-tabs" aria-label="거래 유형">
          <button
            v-for="dealType in dealTypes"
            :key="dealType.value"
            :class="['deal-tab', { active: searchForm.dealType === dealType.value }]"
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
              <option v-for="region in regions" :key="region.value" :value="region.value">
                {{ region.label }}
              </option>
            </select>
          </label>
          <label>
            <span>읍면동</span>
            <input v-model.trim="searchForm.umdNm" type="text" placeholder="예: 우동" />
          </label>
          <label>
            <span>주택명</span>
            <input v-model.trim="searchForm.name" type="search" placeholder="예: 해운대" />
          </label>
          <label>
            <span>주택 유형</span>
            <select v-model="searchForm.propertyType">
              <option v-for="type in propertyTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
          </label>
          <label>
            <span>가격 범위</span>
            <select v-model.number="searchForm.priceRangeIndex">
              <option v-for="(range, index) in priceRanges" :key="range.label" :value="index">
                {{ range.label }}
              </option>
            </select>
          </label>
          <button class="search-button" type="submit" :disabled="isLoading">
            {{ isLoading ? '검색 중' : '검색하기' }}
          </button>
        </div>

        <p v-if="errorMessage" class="form-message" role="alert">{{ errorMessage }}</p>
      </form>

      <section class="summary-card" aria-labelledby="summary-title">
        <div class="section-heading">
          <p>내 맞춤 요약</p>
          <h2 id="summary-title">설정한 조건에 가까운 거래를 우선 확인하세요</h2>
        </div>
        <div class="summary-list">
          <article v-for="item in userSummary" :key="item.label" class="summary-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </section>

      <section class="content-grid">
        <article ref="resultPanel" :class="['panel', 'recommend-panel', { 'searched-panel': isResultHighlighted }]">
          <div class="panel-title-row">
            <div>
              <p class="result-kicker">{{ hasSearched ? 'Search Result' : 'Recommendation' }}</p>
              <h2>{{ resultTitle }}</h2>
            </div>
            <div class="panel-actions">
              <span>{{ resultCountText }}</span>
              <label v-if="hasSearched" class="sort-control compact-sort-control">
                <span>정렬</span>
                <select v-model.number="searchForm.sortIndex" @change="handleSortChange">
                  <option v-for="(sort, index) in sortOptions" :key="sort.label" :value="index">
                    {{ sort.label }}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <div :class="['result-notice', { 'is-searched': hasSearched, 'is-error': errorMessage }]">
            <strong>{{ resultNotice }}</strong>
            <div v-if="appliedSearchSummary.length > 0" class="condition-chips" aria-label="적용된 검색 조건">
              <span v-for="condition in appliedSearchSummary" :key="condition">{{ condition }}</span>
            </div>
          </div>

          <p v-if="hasSearched && displayedHomes.length === 0" class="empty-message">
            조건에 맞는 실거래가 검색 결과가 없습니다.
          </p>

          <div v-else class="recommend-list">
            <article v-for="home in displayedHomes" :key="`${home.rank}-${home.name}`" class="home-card">
              <div class="home-image" aria-hidden="true">
                <span>{{ home.rank }}</span>
              </div>
              <div>
                <h3>{{ home.name }}</h3>
                <p>{{ home.region }}</p>
                <strong>{{ home.price }}</strong>
              </div>
              <em>{{ home.detail }}</em>
            </article>
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
            <article v-for="trend in regionTrends" :key="trend.name" class="trend-item">
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

    <section v-else class="result-page" aria-labelledby="all-result-title">
      <div class="result-page-header">
        <div>
          <p class="result-kicker">All Search Results</p>
          <h1 id="all-result-title">실거래가 전체 검색 결과</h1>
          <p>{{ resultNotice }}</p>
        </div>
        <button class="secondary-button" type="button" @click="openHomeView">홈으로 돌아가기</button>
      </div>

      <div class="result-toolbar">
        <div class="condition-chips" aria-label="적용된 검색 조건">
          <span v-for="condition in appliedSearchSummary" :key="condition">{{ condition }}</span>
        </div>

        <label class="sort-control">
          <span>정렬</span>
          <select v-model.number="searchForm.sortIndex" @change="handleSortChange">
            <option v-for="(sort, index) in sortOptions" :key="sort.label" :value="index">
              {{ sort.label }}
            </option>
          </select>
        </label>
      </div>

      <p v-if="errorMessage" class="form-message" role="alert">{{ errorMessage }}</p>
      <p v-else-if="allResultRows.length === 0" class="empty-message">
        조건에 맞는 실거래가 검색 결과가 없습니다.
      </p>

      <div v-else class="all-result-list">
        <article v-for="home in allResultRows" :key="`${home.rank}-${home.name}`" class="result-row">
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
        </article>
      </div>
    </section>
  </main>
</template>
