<template>
  <div class="site-root">
    <div class="bg-grid"></div>
    <div class="orb orb-one"></div>
    <div class="orb orb-two"></div>

    <header class="topbar">
      <div class="brand-wrap">
        <button class="menu-btn" type="button" @click="toggleSidebar" aria-label="打开导航">
          <span></span>
          <span></span>
          <span></span>
        </button>

        <RouterLink to="/" class="brand-link">
          <img src="/logo.svg" alt="Brick BootKit" class="brand-logo" />
          <div>
            <p class="brand-title">{{ siteMeta.product }}</p>
            <p class="brand-sub">Version {{ siteMeta.version }}</p>
          </div>
        </RouterLink>
      </div>

      <nav class="top-nav">
        <RouterLink
          v-for="item in topNav"
          :key="item.path"
          :to="item.path"
          :class="['top-nav-link', route.path === item.path ? 'is-active' : '']"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="top-actions">
        <button class="search-shortcut" type="button" @click="openSearch">
          <span class="search-shortcut-label">搜索文档</span>
          <span class="search-shortcut-kbd">Ctrl/Cmd + K</span>
        </button>
        <a class="repo-link" :href="siteMeta.repo" target="_blank" rel="noreferrer">GitHub</a>
      </div>
    </header>

    <div class="progress-line">
      <span :style="{ width: `${readingProgress}%` }"></span>
    </div>

    <div class="layout-shell">
      <aside :class="['sidebar', sidebarOpen ? 'open' : '']">
        <div class="sidebar-scroll">
          <div v-for="group in sidebarGroups" :key="group.title" class="sidebar-group">
            <p class="sidebar-group-title">{{ group.title }}</p>
            <RouterLink
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              :class="['sidebar-link', route.path === item.path ? 'is-active' : '']"
              @click="closeSidebar"
            >
              {{ item.label }}
            </RouterLink>
          </div>
        </div>
      </aside>

      <main class="content-area">
        <Transition name="page" mode="out-in">
          <RouterView />
        </Transition>
      </main>

      <aside class="toc-panel">
        <div class="panel toc-card">
          <p class="toc-title">本页目录</p>
          <a
            v-for="item in tocItems"
            :key="item.id"
            :href="`#${item.id}`"
            :class="['toc-link', activeSection === item.id ? 'is-active' : '']"
          >
            {{ item.title }}
          </a>
          <p class="toc-tip">源码优先，文档随代码更新。</p>
        </div>
      </aside>
    </div>

    <div :class="['mobile-mask', sidebarOpen ? 'show' : '']" @click="closeSidebar"></div>

    <Teleport to="body">
      <div v-if="searchOpen" class="search-dialog-wrap" @click.self="closeSearch">
        <div class="search-dialog panel">
          <div class="search-head">
            <input
              ref="searchInputEl"
              v-model.trim="searchKeyword"
              type="text"
              class="search-input"
              placeholder="搜索页面、章节、配置参数..."
            />
            <button type="button" class="search-close" @click="closeSearch" aria-label="关闭搜索">Esc</button>
          </div>

          <div class="search-results">
            <RouterLink
              v-for="item in searchResults"
              :key="item.key"
              :to="{ path: item.path, hash: item.hash ? `#${item.hash}` : '' }"
              class="search-item"
              @click="closeSearch"
            >
              <p class="search-item-title">{{ item.title }}</p>
              <p v-if="item.snippet" class="search-item-snippet">{{ item.snippet }}</p>
            </RouterLink>
            <p v-if="!searchResults.length" class="search-empty">没有匹配结果，试试关键词如 `rollout` / `auth-mode` / `PluginManager`。</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { getPageByPath, searchDocs, sidebarGroups, siteMeta, topNav } from './content/docs';

const route = useRoute();
const sidebarOpen = ref(false);
const readingProgress = ref(0);
const activeSection = ref('');
const searchOpen = ref(false);
const searchKeyword = ref('');
const searchInputEl = ref(null);

const currentPage = computed(() => getPageByPath(route.path) || getPageByPath('/'));
const tocItems = computed(() => {
  return (currentPage.value?.sections || []).map((section) => ({
    id: section.id,
    title: section.title
  }));
});
const searchResults = computed(() => searchDocs(searchKeyword.value));

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value;
}

function closeSidebar() {
  sidebarOpen.value = false;
}

function openSearch() {
  searchOpen.value = true;
  nextTick(() => {
    searchInputEl.value?.focus();
  });
}

function closeSearch() {
  searchOpen.value = false;
  searchKeyword.value = '';
}

function updateActiveSection() {
  const sections = Array.from(document.querySelectorAll('[data-doc-section]'));
  if (!sections.length) {
    activeSection.value = '';
    return;
  }

  const offset = 160;
  let current = sections[0].id;

  sections.forEach((section) => {
    const top = section.getBoundingClientRect().top;
    if (top - offset <= 0) {
      current = section.id;
    }
  });

  activeSection.value = current;
}

function updateProgress() {
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
  if (height <= 0) {
    readingProgress.value = 0;
    return;
  }
  readingProgress.value = Math.min(100, Math.max(0, (scrollTop / height) * 100));
}

function handleScroll() {
  updateProgress();
  updateActiveSection();
}

function handleKeydown(event) {
  const isK = event.key.toLowerCase() === 'k';
  if ((event.ctrlKey || event.metaKey) && isK) {
    event.preventDefault();
    openSearch();
    return;
  }

  if (event.key === 'Escape' && searchOpen.value) {
    closeSearch();
  }
}

watch(
  () => route.fullPath,
  async () => {
    closeSidebar();
    closeSearch();
    await nextTick();
    requestAnimationFrame(() => {
      handleScroll();
    });
  }
);

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('keydown', handleKeydown);
  handleScroll();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('keydown', handleKeydown);
});
</script>
