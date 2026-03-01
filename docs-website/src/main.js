import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import './style.css';
import App from './App.vue';
import DocPage from './views/DocPage.vue';
import { docPages, routeAliases } from './content/docs';

const routes = docPages.map((page) => ({
  path: page.path,
  name: page.id,
  component: DocPage
}));

Object.entries(routeAliases).forEach(([from, to]) => {
  if (from !== to) {
    routes.push({
      path: from,
      redirect: to
    });
  }
});

routes.push({
  path: '/:pathMatch(.*)*',
  redirect: '/'
});

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    if (to.hash) {
      return {
        el: to.hash,
        top: 84,
        behavior: 'smooth'
      };
    }
    return {
      top: 0,
      behavior: 'smooth'
    };
  }
});

createApp(App).use(router).mount('#app');
