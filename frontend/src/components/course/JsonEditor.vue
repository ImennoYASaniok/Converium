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

<style scoped>
.json-editor-container {
  flex: 1;
  display: flex;
  background: #2d2d2d;
  overflow: hidden;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}
.json-editor-container ::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.json-editor-container ::-webkit-scrollbar-track {
  background: #252525;
}

.json-editor-container ::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
}

.json-editor-container ::-webkit-scrollbar-thumb:hover {
  background: #777;
}

.line-numbers {
  width: 50px;
  background: #252525;
  border-right: 1px solid #3a3a3a;
  padding: 10px 0;
  text-align: right;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  user-select: none;
  overflow: hidden;

}


.line-number {
  padding: 0 10px;
  height: 1.5em;
}

.editor-content {
  flex: 1;
  position: relative;
  overflow: auto;
}

.prism-layer,
.input-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 10px;
  border: none;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  white-space: pre;
  overflow: auto;
  tab-size: 2;
  box-sizing: border-box;
}

.prism-layer {
  background: #2d2d2d;
  color: #ccc;
  pointer-events: none;
  z-index: 1;
}

.prism-layer code {
  font-family: inherit;
  background: none !important;
}

.input-layer {
  background: transparent;
  color: transparent;
  caret-color: #fff;
  z-index: 2;
  resize: none;
  outline: none;
}

.input-layer::selection {
  background: rgba(255, 255, 255, 0.2);
  color: transparent;
}
</style>