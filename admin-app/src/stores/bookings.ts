import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useBookingsStore = defineStore({
    id: 'Bookings',
    state: () => ({
      bookings: {}
    }),
    actions: {
        async getBookingsForSeason(seasonId: number) {
            this.bookings = { loading: true };
            fetchWrapper
                .get(`${baseUrl}/api/bookings?season_id=${seasonId}`)
                .then((bookingsResponse) => {
                  this.bookings = bookingsResponse.rows
                })
                .catch((error) => (this.bookings = { error }));
        },
        async createBooking(booking: any) {
          fetchWrapper
            .post(`${baseUrl}/api/bookings`, booking)
        },
        async updateBooking(booking: any) {
          fetchWrapper
            .put(`${baseUrl}/api/bookings/${booking.id}`, booking)
        },
        async deleteBooking(bookingId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/bookings/${bookingId}`)
        }
    }
});
