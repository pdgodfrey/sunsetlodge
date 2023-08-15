import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useRolesStore = defineStore({
    id: 'Roles',
    state: () => ({
      roles: []
    }),
    actions: {
        async getAll() {
            fetchWrapper
                .get(`${baseUrl}/api/roles`)
                .then((response) => {
                  this.roles = response.rows
                })
                .catch((error) => (this.roles = []));
        },
    }
});
