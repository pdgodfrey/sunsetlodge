<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import Logo from '@/layouts/full/logo/Logo.vue';
import {useAuthStore} from "@/stores/auth";
import { Form } from 'vee-validate';
import {useRoute} from "vue-router";

// console.log(vuetify.$route.query.test)

const route = useRoute()

const token = ref(route.params.token)

const valid = ref(false);

const email = ref('');
const newPassword = ref('');
const newPasswordConfirmation = ref('');
const errors = ref(null);
const emailRules = ref([(v: string) => !!v || 'E-mail is required', (v: string) => /.+@.+\..+/.test(v) || 'E-mail must be valid']);


const passwordRules = ref([
  (v: string) => !!v || 'Password is required',
  (v: string) => (v && v.length > 8) || 'Password must be more than 8 characters'
]);

const passwordConfirmationRules = ref([
  (v: string) => (v === newPassword.value) || 'Password confirmation must match'
]);

const submitted = ref(false);

function onSubmit() {
  const authStore = useAuthStore();
  authStore.setPassword(email.value, newPassword.value, ""+token.value)
    .then((res) => {
      submitted.value = true
    })
    .catch((error) => {
      errors.value = error
    });
}
</script>
<template>
    <v-form @submit.prevent="onSubmit"
            v-model="valid"
            v-slot="{ isSubmitting }"
            class="mt-5">
      <template v-if="!submitted">
        <p class="text-subtitle-1 mb-6">Enter your email address below to reset your password.</p>
        <v-label class="text-subtitle-1 font-weight-semibold pb-2 text-lightText">Email Address</v-label>
        <VTextField v-model="email" :rules="emailRules" required ></VTextField>

        <v-label class="text-subtitle-1 font-weight-medium pb-2">Password</v-label>
        <VTextField
          v-model="newPassword"
          :rules="passwordRules"
          required
          variant="outlined"
          type="password"
          color="primary"
        ></VTextField>

        <v-label class="text-subtitle-1 font-weight-medium pb-2">Password Confirmation</v-label>
        <VTextField
          v-model="newPasswordConfirmation"
          :rules="passwordConfirmationRules"
          required
          variant="outlined"
          type="password"
          color="primary"
        ></VTextField>

        <v-btn size="large" :loading="isSubmitting" :disabled="!valid" color="primary" block type="submit" flat>Set Password</v-btn>

        <div v-if="errors && errors.length" class="mt-2">
          <v-alert color="error">{{ errors }}</v-alert>
        </div>
      </template>
      <template v-else>
        <p class="text-subtitle-1 mb-6">Thank you. Your password has been reset, please login.</p>
      </template>
      <div class="d-flex flex-wrap align-center my-3 ml-n2">
        <div class="ml-sm-auto">
          <RouterLink to="/auth/login" class="text-primary text-decoration-none text-body-1 opacity-1 font-weight-medium">Back to Login</RouterLink>
        </div>
      </div>
    </v-form>
</template>
