<script setup lang="ts">
import { ref } from 'vue';

const page = ref({ title: 'Users' });

import Table from '@/components/users/Table.vue';
</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
          <Table></Table>
        </v-col>
    </v-row>
</template>
