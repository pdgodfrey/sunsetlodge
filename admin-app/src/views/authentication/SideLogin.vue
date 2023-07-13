<script setup lang="ts">
import Logo from '@/layouts/full/logo/LogoDark.vue';
/* Login form */
import LoginForm from '@/components/auth/LoginForm.vue';
import ForgotPasswordForm from '@/components/auth/ForgotPasswordForm.vue';
import SetPasswordForm from '@/components/auth/SetPasswordForm.vue';
import {computed} from "vue";
import {useRoute} from "vue-router";
import {useAuthStore} from "@/stores/auth";

const getCurrentRoute: any = computed(() => {
  const route = useRoute()
  const currentRouteName = computed(() => route.name)
  return currentRouteName
});


</script>

<template>
    <div class="pa-3">
        <v-row class="h-100vh mh-100 auth">
            <v-col cols="12" lg="7" xl="8"
                class="d-lg-flex align-center justify-center authentication position-relative">
                <div class="auth-header pt-lg-6 pt-2 px-sm-6 px-3 pb-lg-6 pb-0">
                    <div class="position-relative">
                        <Logo />
                    </div>
                </div>
                <div class="">
                    <img src="@/assets/images/backgrounds/login-bg.svg" class="position-relative d-none d-lg-flex"
                        alt="login-background" />
                </div>
            </v-col>
            <v-col cols="12" lg="5" xl="4" class="d-flex align-center justify-center bg-surface">
                <div class="mt-xl-0 mt-5 mw-100 w-75">
                    <div class="text-subtitle-1 mb-6">Sunset Lodge Admin Dashboard</div>
                    <LoginForm v-if="getCurrentRoute.value === 'Login'" />
                    <ForgotPasswordForm v-if="getCurrentRoute.value === 'Forgot Password'" />
                    <SetPasswordForm v-if="getCurrentRoute.value === 'Reset Password'" />
                </div>
            </v-col>
        </v-row>
    </div>
</template>
