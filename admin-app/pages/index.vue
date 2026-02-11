<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="max-w-2xl w-full mx-auto p-8">
      <div class="bg-white rounded-lg shadow-lg p-8">
        <h1 class="text-4xl font-bold text-gray-900 mb-4">
          {{ $t('admin.welcome.panelTitle') }}
        </h1>
        <p class="text-lg text-gray-600 mb-6">
          {{ $t('admin.welcome.dashboardSubtitle') }}
        </p>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="bg-blue-50 p-4 rounded-lg">
            <h3 class="text-lg font-semibold text-blue-900 mb-2">{{ $t('admin.stats.users') }}</h3>
            <p class="text-3xl font-bold text-blue-600">0</p>
          </div>
          <div class="bg-green-50 p-4 rounded-lg">
            <h3 class="text-lg font-semibold text-green-900 mb-2">{{ $t('admin.stats.activeSessions') }}</h3>
            <p class="text-3xl font-bold text-green-600">0</p>
          </div>
        </div>

        <div class="flex gap-4">
          <button class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
            {{ $t('admin.buttons.manageUsers') }}
          </button>
          <button class="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition">
            {{ $t('admin.buttons.settings') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Admin dashboard logic will go here
</script>
