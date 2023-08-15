import { defineStore } from 'pinia';

import { fetchWrapper } from '@/utils/helpers/fetch-wrapper';

import dayjs from 'dayjs';
import {array} from "yup";

const baseUrl = `${import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL : ''}`;

export const useImagesStore = defineStore({
    id: 'Images',
    state: () => ({
      images: [],
    }),
    actions: {
        async getImagesForGallery(galleryId: number) {
          fetchWrapper
            .get(`${baseUrl}/api/images?gallery_id=${galleryId}`)
            .then((response) => {
              this.images = response.rows

            })
            .catch((error) => (this.images = []));
        },
        async uploadImages(data: FormData, galleryId: number) {
          fetchWrapper
            .postForm(`${baseUrl}/api/images`,  data)
        },
        async updateOrderBy() {
          let imageIds: number[] = []
          this.images.forEach((image: any) => {
            imageIds.push(image.id)
          })

          fetchWrapper
            .post(`${baseUrl}/api/images/update-order`, {
              image_ids: imageIds
            })

        },
        async deleteImage(imageId: number) {
          fetchWrapper
            .delete(`${baseUrl}/api/images/${imageId}`)
        }
    }
});
