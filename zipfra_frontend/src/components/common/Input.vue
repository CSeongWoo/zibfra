<template>
  <div class="flex flex-col gap-2">
    <label class="text-[14px] font-semibold leading-[1.2] tracking-[0.05em] text-primary">
      {{ label }}
    </label>
    <input 
      v-model="model"
      v-bind="$attrs"
      class="px-3 py-2 bg-white border border-primary/20 rounded focus:outline-none focus:border-secondary-container focus:ring-2 focus:ring-secondary-container focus:shadow-glow-secondary transition-all"
    />
  </div>
</template>

<script setup>
const model = defineModel()
defineProps({
  label: {
    type: String,
    required: true
  }
})
</script>
