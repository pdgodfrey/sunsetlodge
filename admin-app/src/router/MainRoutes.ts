const MainRoutes = {
    path: '/main',
    meta: {
        requiresAuth: true
    },
    redirect: '/main',
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
        {
            name: 'Galleries',
            path: '/galleries',
            component: () => import('@/views/Galleries.vue')
        },
        {
            name: 'Users',
            path: '/users',
            component: () => import('@/views/Users.vue')
        },
        {
            name: 'Starter',
            path: '/',
            component: () => import('@/views/StarterPage.vue')
        },
    ]
};

export default MainRoutes;
