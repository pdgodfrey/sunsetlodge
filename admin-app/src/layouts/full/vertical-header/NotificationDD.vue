<script setup lang="ts">
import { notifications } from '@/_mockApis/headerData';
</script>
<template>
    <!-- ---------------------------------------------- -->
    <!-- notifications DD -->
    <!-- ---------------------------------------------- -->
    <v-menu open-on-hover open-on-click class="notification_popup">
        <template v-slot:activator="{ props }">

            <v-btn icon class="custom-hover-primary mx-1"  size="small" variant="text" color="primary" v-bind="props">
                <Icon icon="solar:bell-bing-line-duotone" height="20" />
                <div class="notify">
                    <span class="heartbit"></span>
                    <span class="point"></span>
                </div>
            </v-btn>
        </template>
        <v-sheet rounded="md" width="360" elevation="10">
            <div class="px-6 pt-4 pb-4 bg-primary">
                <h6 class="text-h5">Notifications</h6>
                <p class="text-12">You have 4 Notifications</p>
            </div>
            <perfect-scrollbar style="height: 350px">
                <v-list class="py-0 theme-list" lines="two">
                    <v-list-item v-for="item in notifications" :key="item.title" :value="item" color="primary" class="py-4 px-6 border-b">
                        <template v-slot:prepend>
                            <v-avatar size="40" :color="'light' + item.color" rounded="circle">
                                <Icon :icon="'solar:' + item.avatar" height="20" :class="'text-' + item.color" />
                            </v-avatar>
                        </template>
                        <div class="d-flex justify-space-between">
                            <h6 class="text-subtitle-1 textPrimary font-weight-medium">{{ item.title }}</h6>
                            <span class="text-12 textSecondary">{{ item.time }}</span>
                        </div>
                        <p class="text-subtitle-2 font-weight-regular textSecondary">{{ item.subtitle }}</p>
                    </v-list-item>
                </v-list>
            </perfect-scrollbar>
            <div class="py-4 px-6 text-center">
                <v-btn color="primary" variant="flat"  rounded="pill" block>Check all Notifications</v-btn>
            </div>
        </v-sheet>
    </v-menu>
</template>
