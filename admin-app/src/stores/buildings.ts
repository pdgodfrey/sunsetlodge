import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useBuildingsStore = defineStore({
    id: 'Buildings',
    state: () => ({
      buildings: {}
    }),
    actions: {
        async getAll() {
            this.buildings = { loading: true };
            fetchWrapper
                .get(`${baseUrl}/api/buildings`)
                .then((response) => {
                  this.buildings = response.rows
                })
                .catch((error) => (this.buildings = { error }));
        },
    }
});
