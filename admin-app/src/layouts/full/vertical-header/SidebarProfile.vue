<script setup lang="ts">
import { SideProfile } from '@/_mockApis/headerData';
import { CaretDownIcon } from 'vue-tabler-icons';
</script>

<template>
    <v-menu open-on-click>
        <template v-slot:activator="{ props }">
            <div class="d-flex justify-space-between align-center w-100 cursor-pointer" v-bind="props">
                <h5 class="text-white font-weight-medium text-15">Markarn Doe</h5>
                <Icon icon="solar:alt-arrow-down-bold" class="text-white" height="15" />
            </div>
        </template>

        <v-sheet rounded="md" width="230" elevation="10">
            <perfect-scrollbar style="height: calc(100vh - 240px); max-height: 240px">
                <v-list class="py-0 theme-list mt-3" lines="two">
                    <v-list-item v-for="item in SideProfile" :key="item.title" class="py-3  custom-text-primary" :to="item.href">
                        <div class="d-flex ga-3 align-center">
                            <Icon :icon="'solar:' + item.icon" height="20" :class="'text-' + item.color" />
                            <h6 class="text-body-1 heading  custom-title">{{ item.title }}</h6>
                        </div>
                    </v-list-item>
                </v-list>
            </perfect-scrollbar>
            <v-divider class="mt-2 pb-5" ></v-divider>
            <div class="pb-6 px-5 text-center">
                <v-btn color="primary" class="w-100" rounded="pill" block to="/pages/account-settings">View Profile</v-btn>
            </div>
        </v-sheet>
    </v-menu>
</template>
