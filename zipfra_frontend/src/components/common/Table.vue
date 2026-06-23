<template>
  <div class="w-full overflow-x-auto rounded-lg shadow-level-1 bg-white">
    <table class="w-full text-left border-collapse">
      <thead>
        <tr class="border-b border-primary/10">
          <th v-for="col in columns" :key="col" class="py-3 px-4 font-semibold text-primary">
            {{ col }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr 
          v-for="(row, index) in data" 
          :key="index"
          class="border-b border-primary/10 last:border-b-0 even:bg-primary/[0.02]"
        >
          <td v-for="col in columns" :key="col" class="py-3 px-4 text-body-dense text-primary">
            {{ row[col] }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    required: true
  }
})
</script>
