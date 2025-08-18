<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

import { useGalleriesStore } from '@/stores/galleries';
import { useBuildingsStore } from '@/stores/buildings';
import { useBookingsStore } from "@/stores/bookings";
import { Form } from 'vee-validate';


import dayjs from "dayjs";
import GalleryDescriptionCard from '@/components/galleries/GalleryDescriptionCard.vue';
import GalleryImagesCard from '@/components/galleries/GalleryImagesCard.vue';

const galleriesStore = useGalleriesStore();


onMounted(() => {
  galleriesStore.getAllCategories().then(() => {
    // console.log(getCurrentSeason)
    setTimeout(function() {
      selectedCategoryId.value = getCurrentCategory.value.id

      galleriesStore.getGalleriesForCategory(selectedCategoryId.value)

      onCategorySelect()
    }, 500)
  })

});

const getCategories: any = computed(() => {
  return galleriesStore.galleryCategories;
});

const getCurrentCategory: any = computed(() => {
  return galleriesStore.currentCategory;
});

const getGalleries: any = computed(() => {
  return galleriesStore.galleries;
});



const rateValid = ref(false);
const categoryDialog = ref(false);
const galleryDialog = ref(false);
const galleryImagesDialog = ref(false);
const search = ref('');
const selectedCategoryId = ref(0)

const requiredFieldRules = ref([
  (v: string) => !!v || 'This field is required'
]);

const editedCategoryIndex = ref(-1);
const editedCategoryItem = ref({
  id: 0,
  name: '',
  description: '',
});
const defaultCategoryItem = ref({
  id: 0,
  name: '',
  description: '',
});

const editedGalleryIndex = ref(-1);
const editedGalleryItem = ref({
  id: 0,
  identifier: '',
  description: '',
  gallery_category_id: 0,
  order_by: 0,
  num_images: 0,
});
const defaultGalleryItem = ref({
  id: 0,
  identifier: '',
  description: '',
  gallery_category_id: 0,
  order_by: 0,
  num_images: 0,
});


function editGalleryItem(item: any) {
  editedGalleryIndex.value = getGalleries.value.indexOf(item);
  editedGalleryItem.value = Object.assign({}, item);
  galleryDialog.value = true;
}

function editGalleryImages(item: any) {
  editedGalleryIndex.value = getGalleries.value.indexOf(item);
  editedGalleryItem.value = Object.assign({}, item);
  galleryImagesDialog.value = true;
}


function closeCategory() {
  categoryDialog.value = false;
}

function closeGallery() {
  galleryDialog.value = false;
}


function closeGalleryImages() {
  galleryImagesDialog.value = false;
}

function saveCategory() {
  const category = Object.assign({}, editedCategoryItem.value);

  galleriesStore.updateCategory(category)
    .then(() => {
      setTimeout(function() {
        galleriesStore.getAllCategories().then(() => {
          setTimeout(function() {
            onCategorySelect()
            closeCategory()
          }, 500);
        })
      }, 500)
    })
}

function saveGallery() {
  const category = Object.assign({}, editedGalleryItem.value);

  galleriesStore.updateGallery(category)
    .then(() => {
      setTimeout(function() {
        galleriesStore.getGalleriesForCategory(selectedCategoryId.value)

        closeGallery()
      }, 500)
    })
}

//Computed Property
const categoryFormTitle = computed(() => {
  return editedCategoryIndex.value === -1 ? 'New Category' : 'Edit Category';
});

const isCategoryValid = computed(() => {
  const obj = editedCategoryItem.value

  if(obj.name == null || obj.name == ''){
    return false
  }

  return true
});

function onCategorySelect() {
  const selectedCategory = getCategories.value.find((item: any) => { return item.id === selectedCategoryId.value })
  editedCategoryIndex.value = getCategories.value.indexOf(selectedCategory);
  editedCategoryItem.value = Object.assign({}, selectedCategory);

  galleriesStore.getGalleriesForCategory(selectedCategoryId.value)
}

function truncate(text: string, length: number, suffix: string) {
  if (text && text.length > length) {
    return text.substring(0, length) + suffix;
  } else {
    return text;
  }
}

function reloadGallery() {
  galleriesStore.getGalleriesForCategory(selectedCategoryId.value)
}

const galleryName = computed(() => {
  const selectedCategory = getCategories.value.find((item: any) => { return item.id === selectedCategoryId.value })

  return selectedCategory.name
})

</script>
<template>
  <v-dialog v-model="galleryDialog" max-width="600" min-height="600" persistent>
    <gallery-description-card
      :edited-gallery-item="editedGalleryItem"
      :category-name="galleryName"
      @closeGallery="closeGallery"
      @saveGallery="saveGallery"
    ></gallery-description-card>
  </v-dialog>
  <v-dialog v-model="galleryImagesDialog" max-width="600" min-height="600" persistent>
    <gallery-images-card
      :edited-gallery-item="editedGalleryItem"
      @closeGalleryImages="closeGalleryImages"
      @reloadGallery="reloadGallery"
    ></gallery-images-card>
  </v-dialog>
  <v-row>
    <v-col cols="8" class="text-left">
      <v-row>
        <v-col cols="3" sm="6">
          <v-select
            v-model="selectedCategoryId"
            :items="getCategories"
            item-title="name"
            item-value="id"
            label="Select"
            single-line
            @update:modelValue="onCategorySelect"
          ></v-select>
        </v-col>
        <v-col cols="3" sm="6">
          <v-dialog v-model="categoryDialog" max-width="600" min-height="600" persistent
          v-if="editedCategoryItem && editedCategoryItem.name != 'Backgrounds'">
            <template v-slot:activator="{ props }">
              <v-btn color="primary" v-bind="props" flat class="ml-auto">
                <v-icon class="mr-2">mdi-account-multiple-plus</v-icon>Edit Category Description
              </v-btn>
            </template>
            <v-card height="100vh">
              <v-card-title class="pa-4 bg-secondary">
                <span class="title text-white">{{ categoryFormTitle }}</span>
              </v-card-title>

              <v-card-text>
                <v-form ref="form" lazy-validation>
                  <v-row>

                    <v-col cols="12">
                      {{ editedCategoryItem.name }}
                    </v-col>
                    <v-col cols="12">
                      <v-label>Description</v-label>
                      <v-textarea
                        v-model="editedCategoryItem.description"
                        rows="8"
                      ></v-textarea>
                    </v-col>
                  </v-row>
                </v-form>
              </v-card-text>

              <v-card-actions class="pa-4">
                <v-spacer></v-spacer>
                <v-btn color="error" @click="closeCategory">Cancel</v-btn>
                <v-btn
                  color="secondary"
                  variant="flat"
                  @click="saveCategory"
                >Save</v-btn
                >
              </v-card-actions>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
    </v-col>
  </v-row>
  <v-table class="mt-5">
    <thead>
    <tr>
      <th class="text-subtitle-1 font-weight-semibold">Id</th>
      <th class="text-subtitle-1 font-weight-semibold">Name</th>
      <th class="text-subtitle-1 font-weight-semibold">Description</th>
      <th class="text-subtitle-1 font-weight-semibold">Num Images</th>
    </tr>
    </thead>
    <tbody>
    <tr></tr>
    <tr v-for="item in getGalleries" :key="item.id">
      <td class="text-subtitle-1">{{ item.id }}</td>
      <td class="text-subtitle-1">{{ item.identifier }}</td>
      <td class="text-subtitle-1">{{ truncate(item.description ,100, "...") }}</td>
      <td class="text-subtitle-1">{{ item.num_images }}</td>
      <td>
        <div class="d-flex align-center">
          <v-tooltip text="Edit">
            <template v-slot:activator="{ props }">
              <v-btn icon flat @click="editGalleryItem(item)" v-bind="props"
              ><PencilIcon stroke-width="1.5" size="20" class="text-primary"
              /></v-btn>
            </template>
          </v-tooltip>
          <v-tooltip text="Edit Images">
            <template v-slot:activator="{ props }">
              <v-btn icon flat @click="editGalleryImages(item)" v-bind="props"
              ><PhotoIcon stroke-width="1.5" size="20" class="text-error"
              /></v-btn>
            </template>
          </v-tooltip>
        </div>
      </td>
    </tr>
    </tbody>
  </v-table>
</template>
