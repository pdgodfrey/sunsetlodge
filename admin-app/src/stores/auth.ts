import { defineStore } from 'pinia';
import { router } from '@/router';
import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useAuthStore = defineStore({
    id: 'auth',
    state: () => ({
        // initialize state from local storage to enable user to stay logged in
        // @ts-ignore
        user: JSON.parse(localStorage.getItem('user')),
        returnUrl: null
    }),
    actions: {
        async login(username: string, password: string) {
            const authResponse = await fetchWrapper.post(`${baseUrl}/api/auth/authenticate`, {
              email: username,
              password: password
            })

          const userResponse = await fetchWrapper.get(`${baseUrl}/api/auth/user`);

          this.user = userResponse.user;
          // store user details and jwt in local storage to keep user logged in between page refreshes
          localStorage.setItem('user', JSON.stringify(userResponse.user));
            // redirect to previous url or default to home page
          router.push(this.returnUrl || '/');
        },
        logout() {
            this.user = null;

            localStorage.removeItem('user');

            router.push('/');
        },
        async forgotPassword(username: String) {
          return await fetchWrapper.post(`${baseUrl}/api/auth/reset-password`, { email: username });
        },
        async setPassword(username: String, password: String, resetToken: String) {
          return await fetchWrapper.post(`${baseUrl}/api/auth/set-password`, {
            email: username,
            reset_token: resetToken,
            password: password
          });
        }
    }
});
