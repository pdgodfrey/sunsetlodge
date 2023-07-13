import { defineStore } from 'pinia';
import { router } from '@/router';
import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';
import { useStorage } from '@vueuse/core'

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useAuthStore = defineStore({
    id: 'auth',
    state: () => ({
        // initialize state from local storage to enable user to stay logged in
        // @ts-ignore
        user: useStorage('user', '{}'),
        refreshToken: useStorage('refreshToken', ''),
        returnUrl: useStorage('returnUrl', ''),
        isRefreshing: useStorage('isRefreshing', false),
        refreshTokenTimeout: -1,
        lastRefreshTime: useStorage('lastRefreshTime', new Date())
    }),
    getters: {
      getUser(): Object {
        return JSON.parse(this.user)
      }
    },
    actions: {
        async login(username: string, password: string) {
            const authResponse = await fetchWrapper.post(`${baseUrl}/api/auth/authenticate`, {
              email: username,
              password: password
            })

            this.refreshToken = authResponse.refresh_token;

            const userResponse = await fetchWrapper.get(`${baseUrl}/api/auth/user`);

            this.user = JSON.stringify(userResponse.user);

            this.startRefreshTokenTimer();

            this.lastRefreshTime = new Date()

            // redirect to previous url or default to home page
            if(this.returnUrl != '') {
              router.push(this.returnUrl);
            } else {
              router.push('/');
            }

        },
        logout() {
          fetchWrapper.post(`${baseUrl}/api/auth/logout`)
            .catch((err) => {
            })
            .then((resp) => {
              this.user = '{}';
              this.refreshToken = '';

              this.stopRefreshTokenTimer()

              // localStorage.removeItem('user');
              // localStorage.removeItem('refreshToken');

              router.push('/auth/login');
            })
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
        },
        async refreshAuthToken() {
          if(this.user === '{}'){
            this.logout()
          } else {
            this.stopRefreshTokenTimer()
            const lastRefresh = this.lastRefreshTime;
            const now = new Date()

            // @ts-ignore
            const lastUpdatedInSeconds = Math.floor(Math.abs(now-lastRefresh)/1000)

            if(this.refreshToken != '' && !this.isRefreshing) {
              if(lastUpdatedInSeconds > 120) {
                this.isRefreshing = true;


                await fetchWrapper.post(`${baseUrl}/api/auth/refresh`, {
                  refresh_token: this.refreshToken
                })
                  .then((refreshResponse) => {
                    this.refreshToken = refreshResponse.refresh_token;

                    fetchWrapper.get(`${baseUrl}/api/auth/user`);

                    this.isRefreshing = false

                    this.lastRefreshTime = now

                    console.log("foo3")
                    this.startRefreshTokenTimer();
                  })
                  .catch((err) => {

                    this.isRefreshing = false

                    console.log(err)
                    this.logout()
                  });
              } else {
                this.startRefreshTokenTimer();
              }

            } else {
              this.startRefreshTokenTimer();
            }
          }


        },
        startRefreshTokenTimer() {
          if(this.refreshTokenTimeout == -1){
            const timeout = (60 * 1000);
            this.refreshTokenTimeout = setTimeout(this.refreshAuthToken, timeout);
          }
        },
        stopRefreshTokenTimer() {
          clearTimeout(this.refreshTokenTimeout);
          this.refreshTokenTimeout = -1;
        }
    }
});
