<template>
  <div class="table">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
// Table container component
</script>

<style scoped>
.table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
