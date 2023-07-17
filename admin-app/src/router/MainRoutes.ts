const MainRoutes = {
    path: '/',
    meta: {
        requiresAuth: true
    },
    redirect: '/bookings',
    component: () => import('@/layouts/full/FullLayout.vue'),
    children: [
        {
            name: 'Bookings',
            path: '/bookings',
            component: () => import('@/views/Bookings.vue')
        },
        {
          name: 'Seasons',
          path: '/seasons',
          component: () => import('@/views/Seasons.vue')
        },
    ]
};

export default MainRoutes;
