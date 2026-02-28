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
