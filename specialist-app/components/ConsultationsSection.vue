<template>
    <section class="section">
        <SectionHeader
            icon="📞"
            :title="$t('specialist.menu.consultations')"
            :subtitle="$t('consultations.headerSubtitle')"
            :show-refresh="true"
            @refresh="loadConsultations"
        >
            <template #actions>
                <AppButton @click="loadConsultations">{{
                    $t("common.refresh")
                }}</AppButton>
            </template>
        </SectionHeader>

        <LoadingState
            v-if="consultationsLoading"
            :message="$t('consultations.loading')"
        />
        <ErrorState
            v-else-if="consultationsError"
            :message="consultationsError"
        />

        <EmptyState
            v-else-if="consultations.length === 0"
            icon="📋"
            :title="$t('consultations.noConsultations')"
            :description="$t('consultations.noConsultationsSpecialist')"
        />

        <div v-else class="consultations-container">
            <!-- Consultations Table -->
            <AppTable v-if="paginatedConsultations.length > 0">
                <TableHeader
                    :columns="tableColumns"
                    :column-widths="columnWidths"
                >
                    <span>{{ $t("consultations.clientLabel") }}</span>
                    <span>{{ $t("consultations.categoryLabel") }}</span>
                    <span>{{ $t("consultations.dateTimeLabel") }}</span>
                    <span>{{ $t("consultations.durationLabel") }}</span>
                    <span>{{ $t("common.status") }}</span>
                    <span>{{ $t("consultations.priceLabel") }}</span>
                    <span>{{ $t("common.actions") }}</span>
                </TableHeader>

                <TableRow
                    v-for="consultation in paginatedConsultations"
                    :key="consultation.id"
                    :columns="tableColumns"
                    :column-widths="columnWidths"
                >
                    <span>{{
                        consultation.clientName || consultation.userId
                    }}</span>
                    <span>{{
                        consultation.categoryName || consultation.categoryId
                    }}</span>
                    <span>{{
                        formatDateTime(consultation.scheduledAt || "")
                    }}</span>
                    <span
                        >{{ consultation.duration }}
                        {{ $t("consultations.durationMin") }}</span
                    >
                    <span
                        :class="[
                            'status-badge',
                            consultation.status.toLowerCase(),
                        ]"
                    >
                        {{ consultation.status }}
                    </span>
                    <span>{{
                        consultation.price === 0
                            ? $t("common.free")
                            : `$${consultation.price}`
                    }}</span>
                    <span class="actions-cell">
                        <template v-if="isConsultationActionable(consultation)">
                            <template
                                v-if="consultation.status === 'Requested'"
                            >
                                <AppButton
                                    variant="success"
                                    size="sm"
                                    :loading="
                                        updatingConsultationId ===
                                        consultation.id
                                    "
                                    @click="
                                        approveConsultation(consultation.id)
                                    "
                                >
                                    {{
                                        updatingConsultationId ===
                                        consultation.id
                                            ? "..."
                                            : $t(
                                                  "consultations.approve.approveButton",
                                              )
                                    }}
                                </AppButton>
                                <AppButton
                                    variant="danger"
                                    size="sm"
                                    :loading="
                                        updatingConsultationId ===
                                        consultation.id
                                    "
                                    @click="
                                        declineConsultation(consultation.id)
                                    "
                                >
                                    {{
                                        updatingConsultationId ===
                                        consultation.id
                                            ? "..."
                                            : $t(
                                                  "consultations.approve.declineButton",
                                              )
                                    }}
                                </AppButton>
                            </template>
                            <template
                                v-else-if="consultation.status === 'Scheduled'"
                            >
                                <template
                                    v-if="isConsultationInFuture(consultation)"
                                >
                                    <AppButton
                                        variant="danger"
                                        size="sm"
                                        :loading="
                                            updatingConsultationId ===
                                            consultation.id
                                        "
                                        @click="
                                            declineConsultation(consultation.id)
                                        "
                                    >
                                        {{
                                            updatingConsultationId ===
                                            consultation.id
                                                ? "..."
                                                : $t(
                                                      "consultations.approve.cancelButton",
                                                  )
                                        }}
                                    </AppButton>
                                </template>
                                <template v-else>
                                    <AppButton
                                        variant="warning"
                                        size="sm"
                                        :loading="
                                            updatingConsultationId ===
                                            consultation.id
                                        "
                                        @click="markAsMissed(consultation.id)"
                                    >
                                        {{
                                            updatingConsultationId ===
                                            consultation.id
                                                ? "..."
                                                : $t(
                                                      "consultations.approve.markMissed",
                                                  )
                                        }}
                                    </AppButton>
                                </template>
                            </template>
                            <template v-else>
                                <span class="text-gray">-</span>
                            </template>
                        </template>
                        <template v-else>
                            <span class="text-gray">{{
                                $t("consultations.approve.expired")
                            }}</span>
                        </template>
                    </span>
                </TableRow>
            </AppTable>

            <!-- Pagination -->
            <Pagination
                v-if="consultationPagination.totalPages > 1"
                :current-page="consultationPagination.currentPage"
                :total-pages="consultationPagination.totalPages"
                :total-count="consultationPagination.totalCount"
                :show-labels="true"
                @page-change="goToConsultationPage"
            />
        </div>

        <!-- Approve Consultation Dialog -->
        <Modal
            v-model="showApprovalDialog"
            :title="$t('consultations.approve.title')"
            size="md"
            :show-close="true"
        >
            <p>{{ $t("consultations.approve.description") }}</p>
            <FormField
                :label="$t('consultations.approve.durationLabel')"
                input-id="approval-duration"
                type="number"
                :min="15"
                :step="15"
                :placeholder="'60'"
                :model-value="approvingConsultationDuration?.toString() || ''"
                @update:model-value="
                    approvingConsultationDuration = Number($event)
                "
                @keyup.enter="confirmApprove"
            />
            <FormMessage
                v-if="approvingConsultationDurationError"
                variant="error"
            >
                {{ approvingConsultationDurationError }}
            </FormMessage>
            <template #footer>
                <AppButton
                    variant="success"
                    :loading="updatingConsultationId !== null"
                    @click="confirmApprove"
                >
                    {{
                        updatingConsultationId !== null
                            ? $t("common.approving")
                            : $t("consultations.approve.approveButton")
                    }}
                </AppButton>
                <AppButton
                    variant="secondary"
                    :disabled="updatingConsultationId !== null"
                    @click="showApprovalDialog = false"
                >
                    {{ $t("common.cancel") }}
                </AppButton>
            </template>
        </Modal>
    </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";

// Component imports
import SectionHeader from "~/components/base/SectionHeader.vue";
import LoadingState from "~/components/base/LoadingState.vue";
import ErrorState from "~/components/base/ErrorState.vue";
import EmptyState from "~/components/base/EmptyState.vue";
import AppTable from "~/components/table/AppTable.vue";
import TableHeader from "~/components/table/TableHeader.vue";
import TableRow from "~/components/table/TableRow.vue";
import Pagination from "~/components/ui/Pagination.vue";
import Modal from "~/components/ui/Modal.vue";
import FormField from "~/components/form/FormField.vue";
import FormMessage from "~/components/form/FormMessage.vue";
import AppButton from "~/components/ui/AppButton.vue";

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

type Consultation = {
    id: string;
    userId?: string;
    categoryId?: string;
    scheduledAt?: string;
    duration: number;
    status: string;
    price: number;
};

type EnrichedConsultation = Consultation & {
    clientName?: string;
    categoryName?: string;
};

type NamedResource = {
    name?: string;
};

const consultationsLoading = ref(false);
const consultationsError = ref("");
const consultations = ref<EnrichedConsultation[]>([]);
const currentConsultationPage = ref(1);
const itemsPerPage = 10;
const totalConsultationCount = ref(0);
const updatingConsultationId = ref<string | null>(null);

// Approval dialog state
const showApprovalDialog = ref(false);
const approvingConsultationId = ref<string | null>(null);
const approvingConsultationDuration = ref<number | null>(null);
const approvingConsultationDurationError = ref("");

// Table column configuration
const tableColumns = [
    "client",
    "category",
    "dateTime",
    "duration",
    "status",
    "price",
    "actions",
];
const columnWidths = ["1.5fr", "1.5fr", "2fr", "1fr", "1fr", "1fr", "1.5fr"];

const consultationPagination = computed(() => {
    const totalPages = Math.ceil(totalConsultationCount.value / itemsPerPage);
    return {
        currentPage: currentConsultationPage.value,
        totalPages: totalPages || 1,
        totalCount: totalConsultationCount.value,
    };
});

const paginatedConsultations = computed(() => {
    const start = (currentConsultationPage.value - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    return consultations.value.slice(start, end);
});

const loadConsultations = async () => {
    consultationsLoading.value = true;
    consultationsError.value = "";
    currentConsultationPage.value = 1;
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            consultationsError.value = t("auth.userIdNotFound");
            return;
        }
                
        // Calculate offset based on current page and page size
        const offset = (currentConsultationPage.value - 1) * itemsPerPage;
        const limit = itemsPerPage;
                
        const response = await $fetch<{ consultations: any[], totalCount: number, offset: number, limit: number }>
            (`${config.public.apiBase}/consultations/specialist/${userId}?offset=${offset}&limit=${limit}`,
        );
        const consultationsData = response.consultations || [];
        
        // Update the total count from server response
        totalConsultationCount.value = response.totalCount || 0;

        // Enrich consultations with client names and category names
        const enrichedConsultations = await Promise.all(
            consultationsData.map(async (consultation: any) => {
                try {
                    // Fetch client name
                    let clientName = t("consultations.unknownClient");
                    if (consultation.userId) {
                        try {
                            const clientData = await $fetch<NamedResource>(
                                `${config.public.apiBase}/users/${consultation.userId}`,
                            );
                            clientName =
                                clientData?.name || consultation.userId;
                        } catch (e: any) {
                            clientName = t("consultations.clientFallback", {
                                id: consultation.userId,
                            });
                        }
                    }

                    // Fetch category name
                    let categoryName = t("consultations.unknownCategory");
                    if (consultation.categoryId) {
                        try {
                            const categoryData = await $fetch<NamedResource>(
                                `${config.public.apiBase}/categories/${consultation.categoryId}`,
                            );
                            categoryName =
                                categoryData?.name || consultation.categoryId;
                        } catch (e: any) {
                            categoryName = t("consultations.categoryFallback", {
                                id: consultation.categoryId,
                            });
                        }
                    }

                    return {
                        ...consultation,
                        clientName,
                        categoryName,
                    };
                } catch (e) {
                    return consultation;
                }
            }),
        );

        consultations.value = enrichedConsultations;
    } catch (error: any) {
        consultationsError.value =
            error.data?.message ||
            error.message ||
            t("consultations.failedToLoad");
    } finally {
        consultationsLoading.value = false;
    }
};

const goToConsultationPage = (page: number) => {
    const totalPages = consultationPagination.value.totalPages;
    if (page >= 1 && page <= totalPages) {
        currentConsultationPage.value = page;
    }
};

const approveConsultation = async (consultationId: string) => {
    approvingConsultationId.value = consultationId;
    approvingConsultationDuration.value = null;
    approvingConsultationDurationError.value = "";
    showApprovalDialog.value = true;
};

const confirmApprove = async () => {
    if (
        !approvingConsultationDuration.value ||
        approvingConsultationDuration.value < 15
    ) {
        approvingConsultationDurationError.value = t(
            "consultations.approve.minDuration",
        );
        return;
    }

    updatingConsultationId.value = approvingConsultationId.value;
    try {
        await $fetch(
            `${config.public.apiBase}/consultations/${approvingConsultationId.value}/approve`,
            {
                method: "PUT",
                body: {
                    status: "Scheduled",
                    duration: approvingConsultationDuration.value,
                },
            },
        );

        // Update the consultation in the list
        const consultation = consultations.value.find(
            (c) => c.id === approvingConsultationId.value,
        );
        if (consultation) {
            consultation.status = "Scheduled";
            consultation.duration = approvingConsultationDuration.value;
        }
        showApprovalDialog.value = false;
    } catch (error: any) {
        approvingConsultationDurationError.value =
            error.data?.message ||
            error.message ||
            t("consultations.approve.failedToApprove");
    } finally {
        updatingConsultationId.value = null;
    }
};

const declineConsultation = async (consultationId: string) => {
    updatingConsultationId.value = consultationId;
    try {
        await $fetch(
            `${config.public.apiBase}/consultations/${consultationId}/status`,
            {
                method: "PUT",
                body: { status: "Cancelled" },
            },
        );

        // Update the consultation in the list
        const consultation = consultations.value.find(
            (c) => c.id === consultationId,
        );
        if (consultation) {
            consultation.status = "Cancelled";
        }
    } catch (error: any) {
        alert(
            error.data?.message ||
                error.message ||
                t("consultations.approve.failedToDecline"),
        );
    } finally {
        updatingConsultationId.value = null;
    }
};

const markAsMissed = async (consultationId: string) => {
    updatingConsultationId.value = consultationId;
    try {
        await $fetch(
            `${config.public.apiBase}/consultations/${consultationId}/status`,
            {
                method: "PUT",
                body: { status: "Missed" },
            },
        );

        // Update the consultation in the list
        const consultation = consultations.value.find(
            (c) => c.id === consultationId,
        );
        if (consultation) {
            consultation.status = "Missed";
        }
    } catch (error: any) {
        alert(
            error.data?.message ||
                error.message ||
                t("consultations.approve.failedToMarkMissed"),
        );
    } finally {
        updatingConsultationId.value = null;
    }
};

const isConsultationActionable = (
    consultation: EnrichedConsultation,
): boolean => {
    if (!consultation.scheduledAt) return false;

    try {
        const scheduledDate = new Date(consultation.scheduledAt);
        const now = new Date();
        return scheduledDate > now;
    } catch (e) {
        return false;
    }
};

const isConsultationInFuture = (
    consultation: EnrichedConsultation,
): boolean => {
    if (!consultation.scheduledAt) return false;

    try {
        const scheduledDate = new Date(consultation.scheduledAt);
        const now = new Date();
        return scheduledDate > now;
    } catch (e) {
        return false;
    }
};

const formatDateTime = (dateString: string) => {
    if (!dateString) return t("common.unknown");
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return t("common.unknown");

        return date.toLocaleString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false,
        });
    } catch (e: any) {
        return t("common.unknown");
    }
};

onMounted(() => {
    loadConsultations();
});

defineExpose({
    loadConsultations,
});
</script>

<style scoped>
.section {
    margin-top: 2rem;
}

.consultations-container {
    background: white;
    padding: 1rem;
    border-radius: 8px;
}

.actions-cell {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    flex-wrap: wrap;
}

.status-badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    white-space: nowrap;
}

.status-badge.requested {
    background: #dbeafe;
    color: #1e40af;
}

.status-badge.confirmed,
.status-badge.scheduled {
    background: #dcfce7;
    color: #15803d;
}

.status-badge.inprogress {
    background: #fef3c7;
    color: #92400e;
}

.status-badge.completed {
    background: #d1fae5;
    color: #065f46;
}

.status-badge.missed {
    background: #fed7aa;
    color: #92400e;
}

.status-badge.cancelled {
    background: #fee2e2;
    color: #991b1b;
}

.text-gray {
    color: #9ca3af;
    font-size: 0.875rem;
}
</style>
