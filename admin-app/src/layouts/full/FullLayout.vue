<script setup lang="ts">
import { RouterView } from 'vue-router';
import VerticalSidebarVue from './vertical-sidebar/VerticalSidebar.vue';
import VerticalHeaderVue from './vertical-header/VerticalHeader.vue';
import HorizontalHeader from './horizontal-header/HorizontalHeader.vue';
import HorizontalSidebar from './horizontal-sidebar/HorizontalSidebar.vue';
import Customizer from './customizer/Customizer.vue';
import { useCustomizerStore } from '../../stores/customizer';
import { pl, zhHans } from 'vuetify/locale'
import {useAuthStore} from "@/stores/auth";
const customizer = useCustomizerStore();


const authStore = useAuthStore();
if(authStore.user !== "{}"){
  authStore.startRefreshTokenTimer()
}
</script>

<template>
    <v-locale-provider >
        <v-app
            :theme="customizer.actTheme"
            :class="[
                customizer.actTheme,
                customizer.mini_sidebar ? 'mini-sidebar' : '',
                customizer.setHorizontalLayout ? 'horizontalLayout' : 'verticalLayout',
                customizer.setBorderCard ? 'cardBordered' : '',
                customizer.inputBg ? 'inputWithbg' : ''
            ]"
        >
            <Customizer />
            <VerticalHeaderVue v-if="!customizer.setHorizontalLayout" />
            <VerticalSidebarVue v-if="!customizer.setHorizontalLayout" />
            <HorizontalHeader v-if="customizer.setHorizontalLayout" />
            <HorizontalSidebar v-if="customizer.setHorizontalLayout" />

            <v-main>
                <v-container fluid class="page-wrapper pb-sm-15 pb-10">
                    <div :class="customizer.boxed ? 'maxWidth' : ''">
                        <RouterView />
                    </div>
                </v-container>
            </v-main>
        </v-app>
    </v-locale-provider>
</template>
