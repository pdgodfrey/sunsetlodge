<script setup lang="ts">
import { Messages } from '@/_mockApis/headerData';
</script>
<template>
    <!-- ---------------------------------------------- -->
    <!-- notifications DD -->
    <!-- ---------------------------------------------- -->
    <v-menu open-on-hover open-on-click class="notification_popup">
        <template v-slot:activator="{ props }">
            <v-btn icon class="custom-hover-primary mx-1"  size="small" variant="text" color="primary" v-bind="props">
                <Icon icon="solar:inbox-line-line-duotone" height="20" />
                <div class="notify">
                    <span class="heartbit"></span>
                    <span class="point"></span>
                </div>
            </v-btn>
        </template>
        <v-sheet rounded="md" width="360" elevation="10">
            <div class="px-6 pt-4 pb-4 bg-secondary">
                <h6 class="text-h5">Messages</h6>
                <p class="text-12">You have 5 new messages</p>
            </div>

            <perfect-scrollbar style="height: 400px">
                <v-list class="py-0 theme-list" lines="two">
                    <v-list-item v-for="item in Messages" :key="item.title" :value="item" color="primary" class="py-4 px-6 border-b">
                        <template v-slot:prepend>
                            <v-badge  dot color="success" class="x-small-badge">
                            <v-avatar size="40">
                                <v-img :src="item.avatar" width="40" :alt="item.avatar" />
                            </v-avatar>
                             </v-badge>
                        </template>

                        <div class="d-flex justify-space-between">
                            <h6 class="text-subtitle-1 textPrimary font-weight-medium">{{ item.title }}</h6>
                            <span class="text-12 textSecondary">{{ item.time }}</span>
                        </div>
                        <p class="text-subtitle-2 font-weight-regular textSecondary text-truncate">{{ item.subtitle }}</p>
                    </v-list-item>
                    <v-divider></v-divider>
                </v-list>
            </perfect-scrollbar>
            <div class="py-4 px-6 text-center">
                <v-btn color="secondary" variant="flat" rounded="pill" block>Check all Messages</v-btn>
            </div>
        </v-sheet>
    </v-menu>
</template>
