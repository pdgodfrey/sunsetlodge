<script setup lang="ts">
import { ref } from 'vue';

import UiParentCard from '@/components/shared/UiParentCard.vue';
import {useAuthStore} from "@/stores/auth";

const page = ref({ title: 'Bookings' });

</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
            <UiParentCard title="Bookings"> test </UiParentCard>
        </v-col>
    </v-row>
</template>
