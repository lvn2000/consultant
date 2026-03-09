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
    <Teleport to="body">
        <Transition name="modal">
            <div
                v-if="modelValue"
                class="modal-overlay"
                @click="handleOverlayClick"
            >
                <div class="modal" :class="[sizeClass]" @click.stop>
                    <div v-if="showClose" class="modal-close">
                        <button
                            type="button"
                            class="close-btn"
                            @click="close"
                            aria-label="Close modal"
                        >
                            <span>&times;</span>
                        </button>
                    </div>
                    <div v-if="$slots.header || title" class="modal-header">
                        <slot name="header">
                            <h3>{{ title }}</h3>
                        </slot>
                    </div>
                    <div class="modal-body">
                        <slot></slot>
                    </div>
                    <div v-if="$slots.footer" class="modal-footer">
                        <slot name="footer"></slot>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
    modelValue: boolean;
    title?: string;
    size?: "sm" | "md" | "lg" | "xl";
    closeOnOverlay?: boolean;
    showClose?: boolean;
}>();

const emit = defineEmits<{
    (e: "update:modelValue", value: boolean): void;
    (e: "close"): void;
}>();

const sizeClass = computed(() => {
    const sizes: Record<string, string> = {
        sm: "modal-sm",
        md: "modal-md",
        lg: "modal-lg",
        xl: "modal-xl",
    };
    return sizes[props.size || "md"] || "";
});

const handleOverlayClick = () => {
    if (props.closeOnOverlay !== false) {
        close();
    }
};

const close = () => {
    emit("update:modelValue", false);
    emit("close");
};
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
}

.modal {
    background: white;
    border-radius: 8px;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
    max-height: 90vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
}

.modal-sm {
    max-width: 400px;
    width: 100%;
}
.modal-md {
    max-width: 500px;
    width: 90%;
}
.modal-lg {
    max-width: 700px;
    width: 90%;
}
.modal-xl {
    max-width: 900px;
    width: 90%;
}

.modal-close {
    display: flex;
    justify-content: flex-end;
    padding: 1rem 1.5rem;
}

.close-btn {
    background: none;
    border: none;
    font-size: 2rem;
    color: #6b7280;
    cursor: pointer;
    line-height: 1;
    padding: 0;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    transition:
        background 0.15s,
        color 0.15s;
}

.close-btn:hover {
    background: #f3f4f6;
    color: #1f2937;
}

.modal-header {
    padding: 1.5rem;
    border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
    margin: 0;
    color: #1f2937;
}

.modal-body {
    padding: 1.5rem;
    overflow-y: auto;
}

.modal-footer {
    padding: 1.5rem;
    border-top: 1px solid #e5e7eb;
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
}

/* Transition */
.modal-enter-active,
.modal-leave-active {
    transition: opacity 0.2s ease;
}

.modal-enter-active .modal,
.modal-leave-active .modal {
    transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}

.modal-enter-from .modal,
.modal-leave-to .modal {
    transform: scale(0.95);
}
</style>
