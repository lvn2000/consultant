<template>
    <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
        <div class="max-w-6xl mx-auto px-4 py-16">
            <div class="text-center mb-12">
                <h1 class="text-5xl font-bold text-gray-900 mb-4">
                    {{ $t("landing.title") }}
                </h1>
                <p class="text-xl text-gray-600">
                    {{ $t("landing.subtitle") }}
                </p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
                <div
                    class="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition"
                >
                    <div class="text-4xl mb-4">💼</div>
                    <h3 class="text-xl font-semibold text-gray-900 mb-2">
                        {{ $t("landing.findSpecialists") }}
                    </h3>
                    <p class="text-gray-600">
                        {{ $t("landing.findSpecialistsDesc") }}
                    </p>
                </div>

                <div
                    class="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition"
                >
                    <div class="text-4xl mb-4">📅</div>
                    <h3 class="text-xl font-semibold text-gray-900 mb-2">
                        {{ $t("landing.bookConsultations") }}
                    </h3>
                    <p class="text-gray-600">
                        {{ $t("landing.bookConsultationsDesc") }}
                    </p>
                </div>

                <div
                    class="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition"
                >
                    <div class="text-4xl mb-4">⭐</div>
                    <h3 class="text-xl font-semibold text-gray-900 mb-2">
                        {{ $t("landing.rateAndReview") }}
                    </h3>
                    <p class="text-gray-600">
                        {{ $t("landing.rateAndReviewDesc") }}
                    </p>
                </div>
            </div>

            <div class="text-center">
                <button
                    class="px-8 py-3 bg-indigo-600 text-white text-lg font-semibold rounded-lg hover:bg-indigo-700 transition shadow-lg"
                >
                    {{ $t("landing.getStarted") }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useIdleTimeout } from "~/composables/useIdleTimeout";

const { start } = useIdleTimeout();
start();
// Specialist platform logic will go here
</script>
