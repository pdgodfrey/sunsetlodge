<script setup lang="ts">
import { ref } from 'vue';
import AppsLink from './AppsLink.vue';
import Quicklinks from '@/layouts/full/vertical-header/QuickLinks.vue';
</script>

<template>

<v-menu open-on-hover open-on-click >
        <template v-slot:activator="{ props }">
            <v-btn icon class="custom-hover-primary" size="small" variant="text" color="primary" v-bind="props">
                <Icon icon="solar:widget-line-duotone" height="22"   />
            </v-btn>
        </template>
        <v-sheet rounded="md" width="360" elevation="10">
            <perfect-scrollbar style="height: 370px">
                <div class="pa-6 pb-0">
                    <AppsLink />
                </div>
                <div class="pa-6 ">
                    <Quicklinks />
                </div>
            </perfect-scrollbar>
        </v-sheet>
    </v-menu>
</template>

