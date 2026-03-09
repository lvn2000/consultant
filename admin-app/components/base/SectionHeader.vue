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
    <div class="section-header">
        <h2>{{ title }}</h2>
        <div class="header-actions">
            <slot name="actions">
                <BaseButton
                    v-if="showRefresh"
                    variant="default"
                    size="small"
                    @click="$emit('refresh')"
                >
                    🔄 {{ $t("common.refresh") }}
                </BaseButton>
            </slot>
        </div>
    </div>
</template>

<script setup lang="ts">
import BaseButton from "./BaseButton.vue";

defineProps<{
    title: string;
    showRefresh?: boolean;
}>();

defineEmits<{
    refresh: [];
}>();
</script>

<style scoped>
.section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
}

.section-header h2 {
    margin: 0;
    font-size: 1.25rem;
    color: #111827;
}

.header-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
}
</style>
