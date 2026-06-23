<template>
  <button 
    class="px-4 py-2 rounded font-semibold transition-all duration-200"
    :class="{
      'bg-primary text-white hover:shadow-glow-secondary': variant === 'primary',
      'bg-transparent text-primary border border-primary': variant === 'secondary',
      'bg-secondary text-white': variant === 'accent'
    }"
  >
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary' // 'primary', 'secondary', 'accent'
  }
})
</script>
