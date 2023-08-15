import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useGalleriesStore = defineStore({
    id: 'Galleries',
    state: () => ({
      galleryCategories: [],
      currentCategory: {},
      galleries: [],
      currentGallery: {},
    }),
    actions: {
        async getAllCategories() {
          fetchWrapper
            .get(`${baseUrl}/api/galleries/gallery-categories`)
            .then((response) => {
              this.galleryCategories = response.rows

              this.currentCategory = this.galleryCategories[0]
            })
            .catch((error) => (this.galleryCategories = []));
        },
        async updateCategory(category: any) {
          fetchWrapper
            .put(`${baseUrl}/api/galleries/gallery-categories/${category.id}`, category)
        },
        async getGalleriesForCategory(categoryId: number) {
            fetchWrapper
                .get(`${baseUrl}/api/galleries?gallery_category_id=${categoryId}`)
                .then((response) => {
                  this.galleries = response.rows

                })
                .catch((error) => (this.galleries = []));
        },
        async updateGallery(gallery: any) {
          fetchWrapper
            .put(`${baseUrl}/api/galleries/${gallery.id}`, gallery)
        },
    }
});
