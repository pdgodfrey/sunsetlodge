<script setup>
import BreadcrumbChart from './BreadcrumbChart.vue';
const props = defineProps({
    title: String,
    breadcrumbs: Array,
    icon: String,
    text: String
});
</script>

<template>
    <div class="mt-3 mb-6">
        <div class="d-flex justify-space-between">
            <div class="d-flex py-0 align-center">
                <div>
                    <h3 class="text-h5">{{ title }}</h3>
                    <v-breadcrumbs :items="breadcrumbs" class="text-subtitle-1 textSecondary pa-0 ml-n1">
                        <template v-slot:divider v-if="breadcrumbs" class="px-0">
                            <v-icon size="15">mdi-chevron-right</v-icon>
                        </template>
                        <template v-slot:title="{ item }">
                            <p>{{ item.text }}</p>
                        </template>
                    </v-breadcrumbs>
                </div>
            </div>
            <div>
                <BreadcrumbChart />
            </div>
        </div>
    </div>
</template>

<style lang="scss">
.page-breadcrumb {
    .v-toolbar {
        background: transparent;
    }
}
</style>
