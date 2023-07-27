import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useSeasonsStore = defineStore({
    id: 'Seasons',
    state: () => ({
      seasons: [],
      currentSeason: {},
    }),
    actions: {
        async getAll() {
            // this.seasons = { loading: true };
            fetchWrapper
                .get(`${baseUrl}/api/seasons`)
                .then((seasonsResponse) => {
                  this.seasons = seasonsResponse.rows

                  seasonsResponse.rows.forEach((season: any) => {
                    if(season.is_current) {
                      this.currentSeason = season
                    }
                  })

                })
                .catch((error) => (this.seasons = []));
        },
        async createSeason(season: any) {
          fetchWrapper
            .post(`${baseUrl}/api/seasons`, season)
        },
        async updateSeason(season: any) {
          fetchWrapper
            .put(`${baseUrl}/api/seasons/${season.id}`, season)
        },
        async deleteSeason(seasonId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/seasons/${seasonId}`)
        }
    }
});
