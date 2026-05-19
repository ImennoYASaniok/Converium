import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
// import CourseCatalog from '@/components/course/CourseCatalog.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/HomeView.vue'),
        },
        {
            path: '/about',
            name: 'About',
            component: () => import('@/views/AboutView.vue'),
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/LoginView.vue'),
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/RegisterView.vue'),
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/ProfileView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/profile/edit',
            name: 'EditProfile',
            component: () => import('@/views/EditProfileView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/settings',
            name: 'Settings',
            component: () => import('@/views/SettingsView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/error/:code(\\d+)',
            name: 'ErrorStatus',
            component: () => import('@/views/ErrorStatusView.vue'),
        },
        // {
        //     path: '/catalog',
        //     name: 'CourseCatalog',
        //     component: CourseCatalog,
        //     meta: { requiresAuth: true }
        // },
        {
            path: '/editor/:courseId',
            name: 'CourseEditor',
            component: () => import('@/components/course/CourseEditor.vue'),
            meta: { requiresAuth: true },
            props: true
        },
        {
            path: '/course/:courseId',
            name: 'CourseView',
            component: () => import('@/views/CourseView.vue'),
            meta: { requiresAuth: true },
            props: true
        },
        {
            path: '/course/:courseId/step/:stepId',
            name: 'StepView',
            component: () => import('@/views/StepView.vue'),
            meta: { requiresAuth: true },
            props: true
        },
        {
            path: '/:pathMatch(.*)*',
            redirect: '/error/404',
        },

    ],
})

router.beforeEach((to) => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return { path: '/login' }
    }
    return true
})

export default router
