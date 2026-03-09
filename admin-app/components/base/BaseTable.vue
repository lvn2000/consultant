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
    <div class="table-container">
        <div class="table">
            <div
                class="table-header"
                :class="tableClass"
                :style="{ gridTemplateColumns }"
            >
                <span v-for="(column, index) in columns" :key="index">
                    {{ column }}
                </span>
            </div>
            <div
                v-for="(item, index) in items"
                :key="getItemKey(item, index)"
                class="table-row"
                :class="[
                    tableClass,
                    { 'table-row--selected': isSelected(item) },
                ]"
                :style="{ gridTemplateColumns }"
            >
                <slot name="row" :item="item" :index="index">
                    <span v-for="(column, colIndex) in columns" :key="colIndex">
                        {{ getColumnValue(item, column) }}
                    </span>
                </slot>
            </div>
            <div v-if="items.length === 0" class="table-empty">
                {{ emptyMessage || "No data available" }}
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
type ItemKey = string | ((item: any) => string);

const props = withDefaults(
    defineProps<{
        columns: string[];
        items: any[];
        tableClass?: string;
        columnWidths?: string[];
        itemKey?: ItemKey;
        selectedId?: string | null;
        emptyMessage?: string;
    }>(),
    {
        tableClass: "",
        columnWidths: () => [],
        itemKey: "id",
        selectedId: null,
        emptyMessage: "",
    },
);

const emit = defineEmits<{
    rowClick: [item: any];
}>();

const gridTemplateColumns = computed(() => {
    if (props.columnWidths && props.columnWidths.length > 0) {
        return props.columnWidths.join(" ");
    }
    // Default: equal columns based on column count
    return `repeat(${props.columns.length}, 1fr)`;
});

const getItemKey = (item: any, index: number): string => {
    if (typeof props.itemKey === "function") {
        return props.itemKey(item);
    }
    return item[props.itemKey] ?? `item-${index}`;
};

const getColumnValue = (item: any, column: string): string => {
    const key = column.toLowerCase().replace(/\s+/g, "");
    const value = item[key];
    if (value === null || value === undefined) return "-";
    return String(value);
};

const isSelected = (item: any): boolean => {
    if (!props.selectedId) return false;
    const itemId = getItemKey(item, 0);
    return itemId === props.selectedId;
};
</script>

<style scoped>
.table-container {
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    overflow: hidden;
    background: #ffffff;
}

.table {
    display: flex;
    flex-direction: column;
}

.table-header {
    display: grid;
    gap: 0.5rem;
    padding: 0.75rem;
    background: #f1f5f9;
    font-weight: 600;
    color: #1f2937;
    font-size: 0.875rem;
    border-bottom: 1px solid #e5e7eb;
}

.table-row {
    display: grid;
    gap: 0.5rem;
    padding: 0.75rem;
    border-top: 1px solid #e5e7eb;
    background: #ffffff;
    font-size: 0.875rem;
    color: #374151;
    transition: background 0.2s ease;
}

.table-row:hover {
    background: #f9fafb;
}

.table-row--selected {
    background: #e0e7ff;
}

.table-row--selected:hover {
    background: #c7d2fe;
}

.table-empty {
    padding: 2rem;
    text-align: center;
    color: #6b7280;
    font-style: italic;
}
</style>
