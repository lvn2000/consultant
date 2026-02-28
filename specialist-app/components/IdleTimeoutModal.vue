<template>
    <Teleport to="body">
        <Transition name="fade">
            <div v-if="visible" class="idle-modal-overlay" @click.self="onStay">
                <div class="idle-modal-dialog">
                    <div class="idle-modal-icon">⏰</div>
                    <h3>{{ $t("idle.timeoutTitle") }}</h3>
                    <p>{{ $t("idle.timeoutMessage") }}</p>

                    <div class="idle-countdown">
                        <div class="idle-timer">{{ formatRemainingTime }}</div>
                        <div class="idle-label">
                            {{ $t("idle.timeRemaining") }}
                        </div>
                    </div>

                    <div class="idle-modal-actions">
                        <AppButton variant="primary" @click="onStay">
                            ✅ {{ $t("idle.stayLoggedIn") }}
                        </AppButton>
                        <AppButton variant="danger" @click="onLogout">
                            🚪 {{ $t("idle.logoutNow") }}
                        </AppButton>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<script setup lang="ts">
import AppButton from "~/components/ui/AppButton.vue";

const props = defineProps<{
    visible: boolean;
    formatRemainingTime: string;
}>();

const emit = defineEmits<{
    (e: "stay"): void;
    (e: "logout"): void;
}>();

const onStay = () => {
    emit("stay");
};

const onLogout = () => {
    emit("logout");
};
</script>

<style scoped>
.idle-modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.7);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    animation: fadeIn 0.2s ease-out;
}

.idle-modal-dialog {
    background: white;
    border-radius: 16px;
    padding: 2rem;
    max-width: 450px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
    text-align: center;
    animation: slideUp 0.3s ease-out;
}

.idle-modal-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
}

.idle-modal-dialog h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1.5rem;
    color: #111827;
    font-weight: 700;
}

.idle-modal-dialog p {
    margin: 0 0 1.5rem 0;
    color: #6b7280;
    line-height: 1.6;
}

.idle-countdown {
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    border-radius: 12px;
    padding: 1.5rem;
    margin: 1.5rem 0;
    border: 2px solid #fca5a5;
}

.idle-timer {
    font-size: 3rem;
    font-weight: 800;
    color: #dc2626;
    font-family: "Courier New", monospace;
    line-height: 1;
}

.idle-label {
    margin-top: 0.5rem;
    font-size: 0.875rem;
    color: #991b1b;
    font-weight: 600;
}

.idle-modal-actions {
    display: flex;
    gap: 1rem;
    justify-content: center;
}

.idle-modal-actions .btn {
    flex: 1;
    max-width: 160px;
}

/* Animations */
@keyframes fadeIn {
    from {
        opacity: 0;
    }
    to {
        opacity: 1;
    }
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
