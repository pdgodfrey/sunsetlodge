import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useRatesStore = defineStore({
    id: 'Rates',
    state: () => ({
      rates: []
    }),
    actions: {
        async getRatesForSeason(seasonId: number) {
            this.rates = [];
            fetchWrapper
                .get(`${baseUrl}/api/rates?season_id=${seasonId}`)
                .then((response) => {
                  this.rates = response.rows
                })
                .catch((error) => (this.rates = { error }));
        },
        async createRate(rate: any) {
          fetchWrapper
            .post(`${baseUrl}/api/rates`, rate)
        },
        async updateRate(rate: any) {
          fetchWrapper
            .put(`${baseUrl}/api/rates/${rate.id}`, rate)
        },
        async deleteRate(rateId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/rates/${rateId}`)
        }
    }
});
