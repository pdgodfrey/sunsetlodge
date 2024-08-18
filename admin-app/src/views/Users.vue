<script setup lang="ts">
import { ref } from 'vue';
import Table from '@/components/users/Table.vue';

const page = ref({ title: 'Users' });
</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
          <Table></Table>
        </v-col>
    </v-row>
</template>
