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

                  seasonsResponse.rows.forEach((season: any) => {
                    season.start_date = dayjs(season.start_date)
                    season.end_date = dayjs(season.end_date)
                    season.high_season_start_date = dayjs(season.high_season_start_date)
                    season.high_season_end_date = dayjs(season.high_season_end_date)
                    if(season.is_current) {
                      this.currentSeason = season
                    }
                  })

                  this.seasons = seasonsResponse.rows

                })
                .catch((error) => (this.seasons = []));
        },
        async createSeason(season: any) {
          fetchWrapper
            .post(`${baseUrl}/api/seasons`, season)
            .catch((err) =>{
              alert(err)
            })
        },
        async updateSeason(season: any) {
          fetchWrapper
            .put(`${baseUrl}/api/seasons/${season.id}`, season)
            .catch((err) =>{
              alert(err)
            })
        },
        async deleteSeason(seasonId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/seasons/${seasonId}`)
        }
    }
});
