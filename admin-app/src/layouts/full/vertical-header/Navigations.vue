<script setup lang="ts">
import { ref } from 'vue';
import Quicklinks from '@/layouts/full/vertical-header/QuickLinks.vue';
import AppsLink from './AppsLink.vue';

</script>

<template>
    <!-- ---------------------------------------------- -->
    <!-- mega menu DD -->
    <!-- ---------------------------------------------- -->
    <v-menu open-on-hover open-on-click >
        <template v-slot:activator="{ props }">
            <v-btn class="hidden-sm-and-down ms-2 custom-hover-primary" icon variant="text" size="small"  v-bind="props">  
                <Icon icon="solar:widget-3-line-duotone"  height="20"   /> 
            </v-btn>
        </template>
        <v-sheet width="900"  elevation="10" rounded="md" class="pa-0 overflow-hidden">
            <div>
                <v-row>
                    <v-col cols="12" md="8" class="d-flex pe-0">
                        <div class="pa-6 pe-0">
                            <AppsLink />
                            <v-divider class="mt-6"></v-divider>
                            <div class="pt-5 pe-5 pl-0">
                                <div class="d-flex align-center justify-space-between">
                                    <RouterLink to="/pages/faq" class="text-decoration-none d-flex align-center">
                                        <HelpIcon size="20" stroke-width="1.5" class="text-hover-primary" />
                                        <h6 class="text-subtitle-1 font-weight-medium text-hover-primary ml-2">Frequently Asked Questions</h6>
                                    </RouterLink>
                                    <v-btn color="primary" variant="flat" rounded="pill">Check</v-btn>
                                </div>
                            </div>
                        </div>
                        <v-divider vertical></v-divider>
                    </v-col>
                    <v-col cols="12" md="4" >
                        <div class="pa-4 pb-0">
                            <h5 class="text-h5 mb-3">Quick Links</h5>
                            <Quicklinks />
                        </div>
                    </v-col>
                </v-row>
            </div>
        </v-sheet>
    </v-menu>
</template>
s