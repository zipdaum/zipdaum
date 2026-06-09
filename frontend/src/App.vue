<script setup>
const sections = [
  { title: 'Frontend', detail: 'Vue 3 + Vite' },
  { title: 'Backend', detail: 'Spring Boot API' },
  { title: 'Database', detail: 'MySQL docker-compose' }
]
</script>

<template>
  <main class="page">
    <section class="intro">
      <p class="eyebrow">Zipdaum</p>
      <h1>Vue와 Spring Boot를 분리한 프로젝트 구조</h1>
      <p class="description">
        프론트엔드는 <code>frontend</code>, 백엔드는 <code>backend</code>에서 각각 실행합니다.
      </p>
    </section>

    <section class="grid" aria-label="Project structure">
      <article v-for="section in sections" :key="section.title" class="card">
        <h2>{{ section.title }}</h2>
        <p>{{ section.detail }}</p>
      </article>
    </section>
  </main>
</template>
