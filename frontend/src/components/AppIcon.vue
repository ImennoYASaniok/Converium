<template>
  <span
    v-bind="$attrs"
    :class="['app-icon', $attrs.class]"
    :style="wrapperStyle"
    role="img"
    aria-hidden="true"
  >
    <template v-if="isLogo">
      <img :src="iconUrl" class="img-icon" alt="" />
    </template>
    <template v-else>
      <span class="icon" :style="iconStyle"></span>
    </template>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  color: { type: String, default: null }, // fill color of the icon (overrides var(--text))
  size: { type: [String, Number], default: 'auto' },
})

const sizePx = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))

const iconUrl = computed(() => {
  // support subfolders in name like "eye/eye_open"
  const path = `../assets/imgs/${props.name}.svg`
  try {
    return new URL(path, import.meta.url).href
  } catch (e) {
    return ''
  }
})

const isLogo = computed(() => props.name === 'logo')

const wrapperStyle = computed(() => {
  if (sizePx.value === 'auto') return {}
  return { width: sizePx.value, height: sizePx.value }
})

const iconStyle = computed(() => ({
  WebkitMaskImage: `url(${iconUrl.value})`,
  maskImage: `url(${iconUrl.value})`,
  backgroundColor: props.color || 'currentColor',
  width: '100%',
  height: '100%',
}))
</script>

<style>
.app-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  transition: transform 0.12s ease;
  box-sizing: border-box;
  color: var(--text);
}

.app-icon:active {
  transform: scale(0.98);
}

.app-icon .icon {
  display: block;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  -webkit-mask-position: center;
  mask-position: center;
  -webkit-mask-size: contain;
  mask-size: contain;
  background-color: currentColor;
  transition: background-color 0.15s ease, opacity 0.15s ease;
  box-sizing: border-box;
}

.img-icon {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* Default fill: text; on hover: accent */
.app-icon:hover { color: var(--accent); }

/* Also react when parent buttons are hovered */
button:hover .app-icon,
.path-button:hover .app-icon,
.password-toggle:hover .app-icon,
.header-button:hover .app-icon,
.avatar-button:hover .app-icon {
  color: var(--accent);
}

/* Allow external sizing via class like .eye-icon */
.eye-icon { width: 20px; height: 20px; }
</style>