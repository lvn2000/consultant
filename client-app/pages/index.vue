/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
// Client platform logic will go here
</script>
