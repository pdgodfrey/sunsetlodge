const AuthRoutes = {
    path: '/auth',
    component: () => import('@/layouts/blank/BlankLayout.vue'),
    meta: {
        requiresAuth: false
    },
    children: [
        {
          name: 'Login',
          path: '/auth/login',
          component: () => import('@/views/authentication/SideLogin.vue')
        },
        {
          name: 'Forgot Password',
          path: '/auth/forgot-password',
          component: () => import('@/views/authentication/SideLogin.vue')
        },
        {
          name: 'Reset Password',
          path: '/auth/reset-password/:token',
          component: () => import('@/views/authentication/SideLogin.vue')
        },
        {
            name: 'Error',
            path: '/auth/404',
            component: () => import('@/views/authentication/Error.vue')
        }
    ]
};

export default AuthRoutes;
