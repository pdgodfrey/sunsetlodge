<script setup lang="ts">
import { ref, watch } from 'vue';
import Logo from '@/layouts/full/logo/Logo.vue';
import {useAuthStore} from "@/stores/auth";
import { Form } from 'vee-validate';

const valid = ref(true);
const show1 = ref(false);
const email = ref('');
const emailRules = ref([(v: string) => !!v || 'E-mail is required', (v: string) => /.+@.+\..+/.test(v) || 'E-mail must be valid']);

const submitted = ref(false);

function validate(values: any, { setErrors }: any) {
  const authStore = useAuthStore();
  authStore.forgotPassword(email.value)
    .then((res) => {
      submitted.value = true
    })
    .catch((error) => {
      setErrors({ apiError: error })
    });
}
</script>
<template>
    <Form @submit="validate" v-slot="{ errors, isSubmitting }" class="mt-5">
      <template v-if="!submitted">
        <p class="text-subtitle-1 mb-6">Enter your email address below to reset your password.</p>
        <v-label class="text-subtitle-1 font-weight-semibold pb-2 text-lightText">Email Address</v-label>
        <VTextField v-model="email" :rules="emailRules" required ></VTextField>
        <v-btn size="large" :loading="isSubmitting" color="primary" :disabled="!valid" block type="submit" flat>Reset Password</v-btn>
      </template>
      <template v-else>
        <p class="text-subtitle-1 mb-6">Thank you. Please check your email for instructions on how to reset your password.</p>
      </template>
      <div class="d-flex flex-wrap align-center my-3 ml-n2">
        <div class="ml-sm-auto">
          <RouterLink to="/auth/login" class="text-primary text-decoration-none text-body-1 opacity-1 font-weight-medium">Back to Login</RouterLink>
        </div>
      </div>
    </Form>
</template>
