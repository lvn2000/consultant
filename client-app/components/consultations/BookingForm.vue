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
    <form class="booking-form" @submit.prevent="onSubmit">
        <slot name="specialist-dropdown"></slot>
        <slot name="category-dropdown"></slot>
        <slot name="slot-selector"></slot>
        <div class="form-field">
            <label for="description">Description</label>
            <textarea
                id="description"
                v-model="description"
                placeholder="Describe your consultation"
            />
        </div>
        <button class="btn btn-primary" type="submit" :disabled="creating">
            {{ creating ? "Booking..." : "Book Consultation" }}
        </button>
        <div
            v-if="message"
            :class="['form-message', success ? 'success' : 'error']"
        >
            {{ message }}
        </div>
    </form>
</template>

<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits(["submit"]);

const description = ref("");

// Props passed from parent to show booking status
const props = defineProps<{
    creating: boolean;
    message: string;
    success: boolean;
}>();

async function onSubmit() {
    console.log("[BookingForm] Submit button clicked");
    console.log(
        "[BookingForm] Emitting submit event with description:",
        description.value,
    );
    emit("submit", { description: description.value });
}
</script>

<style scoped>
.booking-form {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}
.form-field label {
    font-weight: 500;
    color: #374151;
    margin-bottom: 0.5rem;
}
.form-field textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.95rem;
    font-family: inherit;
    resize: vertical;
}
.btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 6px;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s;
    background: #4f46e5;
    color: white;
}
.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
.form-message {
    padding: 1rem;
    border-radius: 6px;
    margin-top: 1rem;
}
.form-message.success {
    background: #dcfce7;
    color: #166534;
    border: 1px solid #86efac;
}
.form-message.error {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
}
</style>
