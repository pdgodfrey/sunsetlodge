<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useCustomizerStore } from "../../../stores/customizer";
import LanguageDD from "./LanguageDD.vue";
import NotificationDD from "./NotificationDD.vue";
import ProfileDD from "./ProfileDD.vue";
import Searchbar from "./Searchbar.vue";
import Logo from "../logo/Logo.vue";
import RtlLogo from "../logo/RtlLogo.vue";
import LogoIcon from "../logo/LogoIcon.vue";
import ThemeToggler from "./ThemeToggler.vue";
import Navigations from "./Navigations.vue";
import Messages from "./Messages.vue";
import RightMobileSidebar from "./RightMobileSidebar.vue";
const customizer = useCustomizerStore();
const showSearch = ref(false);
const priority = ref(customizer.setHorizontalLayout ? 0 : 0);
function searchbox() {
  showSearch.value = !showSearch.value;
}
watch(priority, (newPriority) => {
  priority.value = newPriority;
});
</script>

<template>
  <v-app-bar
    elevation="5"
    :priority="priority"
    height="60"
    color="primary"
    class="main-head"
    id="top"
  >
    <!---LOGO RTL/LTR--->
    <div class="hidden-sm-and-down">
      <v-locale-provider v-if="customizer.setRTLLayout" rtl>
        <div class="pt-2 d-sm-flex d-none">
          <RtlLogo />
        </div>
        <div class="pt-2 pr-2 d-sm-none d-flex">
          <LogoIcon />
        </div>
      </v-locale-provider>
      <v-locale-provider v-else>
        <div class="pt-2 d-sm-flex d-none">
          <Logo />
        </div>
        <div class="pt-2 pr-2 d-sm-none d-flex">
          <LogoIcon />
        </div>
      </v-locale-provider>
    </div>

    <v-btn
      class="hidden-md-and-down custom-hover-primary menu-toggle"
      size="small"
      icon
      color="primary"
      variant="text"
      @click.stop="customizer.SET_MINI_SIDEBAR(!customizer.mini_sidebar)"
    >
      <Icon icon="solar:list-bold" height="22" />
    </v-btn>
    <v-btn
      class="hidden-lg-and-up"
      icon
      variant="text"
      @click.stop="customizer.SET_SIDEBAR_DRAWER"
      size="small"
    >
      <Icon icon="solar:list-bold" height="22" />
    </v-btn>


    <!---/Search part -->

    <v-spacer class="hidden-sm-and-down" />

    <!-- ---------------------------------------------- -->
    <!-- User Profile -->
    <!-- ---------------------------------------------- -->
    <div class="ms -2 hidden-sm-and-down">
      <ProfileDD />
    </div>

    <!----Mobile ----->
    <v-menu :close-on-content-click="true" class="mobile_popup">
      <template v-slot:activator="{ props }">
        <v-btn
          icon
          class="hidden-md-and-up"
          color="primary"
          variant="text"
          v-bind="props"
          size="small"
        >
          <Icon icon="solar:menu-dots-bold-duotone" height="22" />
        </v-btn>
      </template>
      <v-sheet rounded="lg" elevation="10" class="mt-4 dropdown-box px-4 py-3">
        <div class="d-flex justify-space-between align-center">
          <RightMobileSidebar />
          <LanguageDD />
          <v-btn
            icon
            variant="text"
            class="mr-sm-3 mr-2 custom-hover-primary"
            to="/ecommerce/checkout"
            size="small"
          >
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
  </v-app-bar>
</template>
