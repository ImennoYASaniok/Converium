<template>
  <div class="json-editor-container">
    <div class="line-numbers" ref="lineNumbersRef">
      <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
    </div>
    <div class="editor-content" ref="editorContentRef">
      <pre class="prism-layer" ref="prismLayerRef" aria-hidden="true"><code v-html="highlightedCode"></code></pre>
      <textarea
        ref="textareaRef"
        :value="modelValue"
        class="input-layer"
        spellcheck="false"
        @input="onInput"
        @scroll="syncScroll"
        @keydown.ctrl.s.prevent="$emit('apply')"
      ></textarea>
    </div>
  </div>
</template>

<script>
import { highlight, languages } from 'prismjs/components/prism-core'
import 'prismjs/components/prism-json'
import 'prismjs/themes/prism-tomorrow.css'

export default {
  name: 'JsonEditor',
  
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },

  emits: ['update:modelValue', 'apply'],

  computed: {
    highlightedCode() {
      try {
        return highlight(this.modelValue || '', languages.json, 'json')
      } catch (e) {
        return this.modelValue || ''
      }
    },
    
    lineCount() {
      return (this.modelValue || '').split('\n').length
    }
  },

  methods: {
    onInput(e) {
      this.$emit('update:modelValue', e.target.value)
    },
    
    syncScroll() {
      const textarea = this.$refs.textareaRef
      const prismLayer = this.$refs.prismLayerRef
      const lineNumbers = this.$refs.lineNumbersRef
      
      if (!textarea) return
      
      const scrollTop = textarea.scrollTop
      const scrollLeft = textarea.scrollLeft
      
      if (prismLayer) {
        prismLayer.scrollTop = scrollTop
        prismLayer.scrollLeft = scrollLeft
      }
      if (lineNumbers) {
        lineNumbers.scrollTop = scrollTop
      }
    }
  }
}
</script>