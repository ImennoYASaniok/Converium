import { createRouter, createWebHistory } from 'vue-router'
// import CourseCatalog from '@/components/course/CourseCatalog.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
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
        
    ],
})

export default router
