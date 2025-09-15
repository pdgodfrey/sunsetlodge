import { createRouter, createWebHistory } from 'vue-router';
import MainRoutes from './MainRoutes';
import AuthRoutes from './AuthRoutes';
import { useAuthStore } from '@/stores/auth';

export const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/:pathMatch(.*)*',
            component: () => import('@/views/authentication/Error.vue')
        },
        MainRoutes,
        AuthRoutes
    ]
});

router.beforeEach(async (to, from, next) => {
    // redirect to login page if not logged in and trying to access a restricted page
    const publicPages = ['/auth/login', '/auth/forgot-password'];
    console.log(`Path ${to.path}`)
  console.log(to)
    // const authRequired = !publicPages.includes(to.path) || to.path.indexOf('/auth/reset-password/') != -1;
  const authRequired = to.meta && to.meta.requiresAuth
    console.log(`Auth Required ${authRequired}`)
    const auth: any = useAuthStore();
    console.log(authRequired)
  console.log(auth.user)

    if (authRequired) {
        if (authRequired && !auth.user) {
            auth.returnUrl = to.fullPath;
            return next('/auth/login');
        } else {
          return next();
        }
    } else {
      return next();
    }
});
