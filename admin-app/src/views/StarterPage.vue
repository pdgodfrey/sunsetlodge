<script setup lang="ts">
import { ref } from 'vue';

import UiParentCard from '@/components/shared/UiParentCard.vue';

const page = ref({ title: 'Sample Page' });
const breadcrumbs = ref([
    {
        text: 'Seasons',
        disabled: true,
        href: '#'
    }
]);
</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
            <UiParentCard title="Seasons"> test </UiParentCard>
        </v-col>
    </v-row>
</template>
