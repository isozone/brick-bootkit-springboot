<template>
  <article class="doc-page">
    <header class="doc-hero panel">
      <p class="doc-kicker">{{ siteMeta.product }}</p>
      <h1 class="doc-title">{{ page.title }}</h1>
      <p class="doc-lead">{{ page.lead }}</p>
      <div v-if="page.badges?.length" class="badge-row">
        <span v-for="badge in page.badges" :key="badge" class="badge">{{ badge }}</span>
      </div>
    </header>

    <section
      v-for="(section, index) in page.sections"
      :id="section.id"
      :key="section.id"
      :style="{ '--section-index': index }"
      class="doc-section panel"
      data-doc-section
    >
      <header class="section-head">
        <h2>{{ section.title }}</h2>
        <p v-if="section.lead">{{ section.lead }}</p>
      </header>

      <p v-for="paragraph in section.paragraphs || []" :key="paragraph" class="section-paragraph">
        {{ paragraph }}
      </p>

      <ul v-if="section.bullets?.length" class="bullet-list">
        <li v-for="bullet in section.bullets" :key="bullet">{{ bullet }}</li>
      </ul>

      <div v-if="section.callout" :class="['callout', `callout-${section.callout.tone || 'info'}`]">
        <h3>{{ section.callout.title }}</h3>
        <p>{{ section.callout.body }}</p>
      </div>

      <div v-if="section.table" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th v-for="column in section.table.columns" :key="column">{{ column }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in section.table.rows" :key="row.join('-')">
              <td v-for="cell in row" :key="cell">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <figure v-if="section.code" class="code-block">
        <figcaption>
          <span class="code-meta">{{ section.code.filename || '代码示例' }}</span>
          <button type="button" class="copy-btn" @click="copyCode(section.id, section.code.content)">
            {{ copyState[section.id] || '复制' }}
          </button>
        </figcaption>
        <pre><code>{{ section.code.content }}</code></pre>
      </figure>

      <div v-if="section.sources?.length" class="source-box">
        <h3>源码依据</h3>
        <ul>
          <li v-for="source in section.sources" :key="source">
            <a :href="sourceToUrl(source)" target="_blank" rel="noreferrer" class="source-link">
              <code>{{ source }}</code>
            </a>
          </li>
        </ul>
      </div>
    </section>

    <nav class="doc-pagination">
      <RouterLink v-if="previousPage" :to="previousPage.path" class="pager-link">
        <p class="pager-label">上一页</p>
        <p class="pager-title">{{ previousPage.title }}</p>
      </RouterLink>
      <div v-else></div>

      <RouterLink v-if="nextPage" :to="nextPage.path" class="pager-link">
        <p class="pager-label">下一页</p>
        <p class="pager-title">{{ nextPage.title }}</p>
      </RouterLink>
      <div v-else></div>
    </nav>
  </article>
</template>

<script setup>
import { computed, reactive } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { getAdjacentPages, getPageByPath, siteMeta, sourceToUrl } from '../content/docs';

const route = useRoute();
const copyState = reactive({});

const page = computed(() => {
  return getPageByPath(route.path) || getPageByPath('/');
});
const adjacentPages = computed(() => getAdjacentPages(page.value?.path || '/'));
const previousPage = computed(() => adjacentPages.value.previous);
const nextPage = computed(() => adjacentPages.value.next);

async function copyCode(sectionId, codeText) {
  try {
    await navigator.clipboard.writeText(codeText);
    copyState[sectionId] = '已复制';
    window.setTimeout(() => {
      copyState[sectionId] = '';
    }, 1500);
  } catch (error) {
    copyState[sectionId] = '复制失败';
    window.setTimeout(() => {
      copyState[sectionId] = '';
    }, 1500);
  }
}
</script>
