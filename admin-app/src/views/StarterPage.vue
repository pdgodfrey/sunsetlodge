<script setup lang="ts">
import { ref } from 'vue';

import UiParentCard from '@/components/shared/UiParentCard.vue';
import {useAuthStore} from "@/stores/auth";

const page = ref({ title: 'Sample Page' });
const breadcrumbs = ref([
    {
        text: 'Seasons',
        disabled: true,
        href: '#'
    }
]);

const authStore = useAuthStore();
if(authStore.user !== "{}"){
  authStore.startRefreshTokenTimer()
}
// console.log(authStore.getUser)
</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
            <UiParentCard title="Seasons"> test </UiParentCard>
        </v-col>
    </v-row>
</template>
