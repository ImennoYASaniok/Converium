<script>
// import { RouterLink, RouterView } from 'vue-router'
import { ref } from 'vue'
import CourseEditor from './components/course/CourseEditor.vue';

const isLoading = ref(false)

export default {
  components: {
    CourseEditor
  },

  data() {
    return {
      
    }
  },

  methods: {

  },

  created() {

  }
}
</script>

<template>
  <CourseEditor></CourseEditor>
</template>

<style scoped>

</style>
