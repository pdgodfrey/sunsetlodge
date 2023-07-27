<script setup lang="ts">
import { ref } from 'vue';

const page = ref({ title: 'Seasons' });

import Table from '@/components/seasons/Table.vue';
</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
          <Table></Table>
        </v-col>
    </v-row>
    <v-row>
        <v-col cols="12" md="12">
        </v-col>
    </v-row>
</template>
