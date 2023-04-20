import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  //history: createWebHistory(import.meta.env.BASE_URL),
  history: createWebHistory("/"),
  routes: [
    {
      path: "/",
      // redirect: "/dashboards/analytical",
      component: () => import("@/layouts/full/Layout.vue"),
      children: [
        {
          name: "Home",
          path: "/",
          component: () =>
            import("@/components/HelloWorld.vue"),
        },
        {
          name: "Hi",
          path: "/hi",
          component: () =>
            import("@/components/Hi.vue"),
        },
      ],
    // },
    // {
    //   path: "/authentication",
    //   component: () => import("@/layouts/blank/BlankLayout.vue"),
    //   children: [
    //     {
    //       name: "Login",
    //       path: "/authentication/fulllogin",
    //       component: () => import("@/views/authentication/FullLogin.vue"),
    //     },
    //     {
    //       name: "Boxed Login",
    //       path: "/authentication/boxedlogin",
    //       component: () => import("@/views/authentication/BoxedLogin.vue"),
    //     },
    //     {
    //       name: "Error",
    //       path: "/authentication/error",
    //       component: () => import("@/views/authentication/Error.vue"),
    //     },
    //     {
    //       name: "Register",
    //       path: "/authentication/fullregister",
    //       component: () => import("@/views/authentication/FullRegister.vue"),
    //     },
    //     {
    //       name: "Boxed Register",
    //       path: "/authentication/boxedregister",
    //       component: () => import("@/views/authentication/BoxedRegister.vue"),
    //     },
    //   ],
    },
  ],
});

export default router;
