<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useCustomizerStore } from '../../../stores/customizer';

// Icon Imports
import { GridDotsIcon, LanguageIcon, SearchIcon, Menu2Icon, BellRingingIcon, ShoppingCartIcon } from 'vue-tabler-icons';
import Logo from '../logo/Logo.vue';
import LogoIcon from '../logo/LogoIcon.vue';
import RtlLogo from '../logo/RtlLogo.vue';
// dropdown imports
import LanguageDD from '../vertical-header/LanguageDD.vue';
import NotificationDD from '../vertical-header/NotificationDD.vue';
import ProfileDD from '../vertical-header/ProfileDD.vue';
import Searchbar from '../vertical-header/Searchbar.vue';
import ThemeToggler from '../vertical-header/ThemeToggler.vue';
import Navigations from '../vertical-header/Navigations.vue';
import Messages from '../vertical-header/Messages.vue';
import RightMobileSidebar from '../vertical-header/RightMobileSidebar.vue';

const customizer = useCustomizerStore();
const showSearch = ref(false);
const drawer = ref(false);
const appsdrawer = ref(false);
const priority = ref(customizer.setHorizontalLayout ? 0 : 0);
function searchbox() {
    showSearch.value = !showSearch.value;
}
watch(priority, (newPriority) => {
    // yes, console.log() is a side effect
    priority.value = newPriority;
});


</script>

<template>
    <v-app-bar elevation="10" :priority="priority" height="64" class="horizontal-header main-head" color="primary">
        <div :class="customizer.boxed ? 'maxWidth v-toolbar__content px-lg-0 px-4' : 'v-toolbar__content px-6'">
            <div class="hidden-sm-and-down">
            <v-locale-provider v-if="customizer.setRTLLayout" rtl>
                <div class="pt-2 pr-4 d-sm-flex d-none">
                    <RtlLogo />
                </div>
                <div class="pt-2 pr-2 d-sm-none d-flex">
                    <LogoIcon />
                </div>
            </v-locale-provider>
            <v-locale-provider v-else>
                <div class="pt-2 pr-4 d-sm-flex d-none">
                    <Logo />
                </div>
                <div class="pt-2 pr-2 d-sm-none d-flex">
                    <LogoIcon />
                </div>
            </v-locale-provider>
            </div>

           
            <v-btn class="hidden-lg-and-up" icon variant="text" @click.stop="customizer.SET_SIDEBAR_DRAWER" size="small">
                <Icon icon="solar:list-bold" height="22" />
            </v-btn>

            <!-- ------------------------------------------------>
            <!-- Search part -->
            <!-- ------------------------------------------------>

            <Searchbar />

            <!-- ---------------------------------------------- -->
            <!-- Mega menu -->
            <!-- ---------------------------------------------- -->
            <div class="hidden-sm-and-down">
                <Navigations />
            </div>

            <!---/Search part -->
            <v-spacer class="hidden-sm-and-down" />
            <!-- ---------------------------------------------- -->
            <!---right part -->
            <!-- ---------------------------------------------- -->

            <!-- ---------------------------------------------- -->
            <!-- translate -->
            <!-- ---------------------------------------------- -->

            <div class="hidden-sm-and-down">
                <LanguageDD />
            </div>

            <div class="hidden-md-and-up mt-2">
                <LogoIcon />
            </div>
            <div class="me-md-1">
                <ThemeToggler />
            </div>
            <!-- ---------------------------------------------- -->
            <!-- Notification -->
            <!-- ---------------------------------------------- -->

            <!-- ---------------------------------------------- -->
            <!-- Notification -->
            <!-- ---------------------------------------------- -->
            <div class="hidden-sm-and-down">
                <NotificationDD />
            </div>
            <!-- ---------------------------------------------- -->
            <!-- Messages -->
            <!-- ---------------------------------------------- -->
            <div class="hidden-sm-and-down">
                <Messages />
            </div>

            <!-- ---------------------------------------------- -->
            <!-- ShoppingCart -->
            <!-- ---------------------------------------------- -->
            <v-btn
                icon
                variant="text"
                color="primary"
                to="/ecommerce/checkout"
                size="small"
                class="ms-1 hidden-sm-and-down custom-hover-primary"
            >
                <v-badge color="error" content="0">
                    <Icon icon="solar:cart-large-2-outline" height="22" />
                </v-badge>
            </v-btn>
            <!-- ---------------------------------------------- -->
            <!-- User Profile -->
            <!-- ---------------------------------------------- -->
            <div class="ms -2 hidden-sm-and-down">
                <ProfileDD />
            </div>

            <!----Mobile ----->
            <v-menu :close-on-content-click="true" class="mobile_popup">
                <template v-slot:activator="{ props }">
                    <v-btn icon class="hidden-md-and-up" color="primary" variant="text" v-bind="props" size="small">
                        <Icon icon="solar:menu-dots-bold-duotone" height="22" />
                    </v-btn>
                </template>
                <v-sheet rounded="lg" elevation="10" class="mt-4 dropdown-box px-4 py-3">
                    <div class="d-flex justify-space-between align-center">
                        <RightMobileSidebar />
                        <LanguageDD />
                        <v-btn icon variant="text" class="mr-sm-3 mr-2 custom-hover-primary" to="/ecommerce/checkout" size="small">
                            <v-badge color="error" content="0" offset-x="-4" offset-y="-6">
                                <Icon icon="solar:cart-large-2-outline" height="22" />
                            </v-badge>
                        </v-btn>
                        <NotificationDD />
                        <Messages />
                        <ProfileDD />
                    </div>
                </v-sheet>
            </v-menu>
        </div>
    </v-app-bar>
</template>
