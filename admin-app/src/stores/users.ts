import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useUsersStore = defineStore({
    id: 'Users',
    state: () => ({
      users: [],
    }),
    actions: {
        async getAll() {
            // this.seasons = { loading: true };
            fetchWrapper
                .get(`${baseUrl}/api/users`)
                .then((response) => {
                  this.users = response.rows
                })
                .catch((error) => (this.users = []));
        },
        async createUser(user: any) {
          fetchWrapper
            .post(`${baseUrl}/api/users`, user)
        },
        async updateuser(user: any) {
          fetchWrapper
            .put(`${baseUrl}/api/users/${user.id}`, user)
        },
        async deleteuser(userId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/users/${userId}`)
        },
        async resetUserPassword(userId: number) {
          fetchWrapper
            .post(`${baseUrl}/api/users/reset-password?id=${userId}`)
        }
    }
});
