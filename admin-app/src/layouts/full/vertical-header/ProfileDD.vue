<script setup lang="ts">
import { MailIcon } from 'vue-tabler-icons';
import { profileDD } from '@/_mockApis/headerData';

import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
</script>

<template>
    <!-- ---------------------------------------------- -->
    <!-- notifications DD -->
    <!-- ---------------------------------------------- -->
    <v-menu open-on-hover open-on-click  class="profile_popup">
        <template v-slot:activator="{ props }">
            <v-btn variant="text" v-bind="props" icon size="small" color="primary" class="custom-hover-primary ms-md-2">
                <v-avatar size="30">
                    <img src="@/assets/images/profile/user-1.jpg" width="30" alt="user" />
                </v-avatar>
            </v-btn>
        </template>
        <v-sheet rounded="md" width="350" elevation="10">
            <div class="pb-4">
                <div class="d-flex align-center pa-4">
                    <v-avatar size="50">
                        <img src="@/assets/images/profile/user-1.jpg" width="50" />
                    </v-avatar>
                    <div class="ml-3">
                        <h6 class="text-h6  textPrimary font-weight-medium">{{authStore.getUser.name}}</h6>
                        <span class="text-12 textSecondary">{{authStore.getUser.email}}</span>
                    </div>
                </div>
                <v-divider></v-divider>
<!--                <perfect-scrollbar style="height: calc(100vh - 240px); max-height: 240px">-->
<!--                  <v-list class="py-0 px-4 theme-list mt-3" lines="one">-->
<!--                      <v-list-item v-for="item in profileDD" :key="item.title" class="py-0 px-2 rounded-md custom-text-primary" color="primary"  :to="item.href">-->
<!--                          <div class="d-flex gap-3 align-center">-->
<!--                              <p class="text-subtitle-1 textPrimary hover-primary font-weight-medium">{{ item.title }}</p>-->
<!--                              <v-chip size="small" color="info" v-if="item.badge">4</v-chip>-->
<!--                          </div>-->
<!--                      </v-list-item>-->
<!--                  </v-list>-->
<!--              </perfect-scrollbar>-->
              <div class="pt-4 pb-6 px-8 text-center">
                <v-btn color="primary" variant="outlined" block @click="authStore.logout()">Logout</v-btn>
              </div>
            </div>
        </v-sheet>
    </v-menu>
</template>
