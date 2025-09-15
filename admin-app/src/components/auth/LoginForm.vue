<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { Form } from 'vee-validate';


const checkbox = ref(false);
const valid = ref(false);
const show1 = ref(false);
const password = ref('');
const username = ref('');
const errors = ref(null);
const passwordRules = ref([
  (v: string) => !!v || 'Password is required'
]);
const emailRules = ref([(v: string) => !!v || 'E-mail is required', (v: string) => /.+@.+\..+/.test(v) || 'E-mail must be valid']);

function onSubmit() {
  const authStore = useAuthStore();
  authStore
    .login(username.value, password.value)
    .catch((error) => {
      errors.value = error
    });
}
</script>

<template>
    <v-form @submit.prevent="onSubmit"
          v-model="valid"
          class="mt-5">
        <v-label class="text-subtitle-1 font-weight-semibold pb-2 text-lightText">Email</v-label>
        <VTextField
            v-model="username"
            :rules="emailRules"
            class="mb-8"
            required
            hide-details="auto"
        ></VTextField>
        <v-label class="text-subtitle-1 font-weight-semibold pb-2 text-lightText">Password</v-label>
        <VTextField
            v-model="password"
            :rules="passwordRules"
            required
            hide-details="auto"
            :append-icon="show1 ? 'mdi-eye' : 'mdi-eye-off'"
            :type="show1 ? 'text' : 'password'"
            @click:append="show1 = !show1"
            class="pwdInput"
        ></VTextField>
        <div class="d-flex flex-wrap align-center my-3 ml-n2">
            <div class="ml-sm-auto">
                <RouterLink to="/auth/forgot-password" class="text-primary text-decoration-none text-body-1 opacity-1 font-weight-medium"
                    >Forgot Password ?</RouterLink
                >
            </div>
        </div>
        <v-btn size="large" color="primary" :disabled="!valid" block type="submit" flat>Sign In</v-btn>
        <div v-if="errors" class="mt-2">
            <v-alert color="error">{{ errors }}</v-alert>
        </div>
    </v-form>
</template>
