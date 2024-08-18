<script setup lang="ts">
import { ref } from 'vue';
import ImageSorter from '@/components/galleries/ImageSorter.vue';

const page = ref({ title: 'Galleries' });

</script>

<template>
    <v-row>
        <v-col cols="12" md="12">
          <ImageSorter></ImageSorter>
        </v-col>
    </v-row>
</template>
