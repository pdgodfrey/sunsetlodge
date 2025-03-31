<script setup lang="ts">
  import {computed, onActivated, onMounted, onUnmounted, ref, watch} from "vue";


  import { useImagesStore } from '@/stores/images';
  import draggable from 'vuedraggable'

  const imagesStore = useImagesStore();

  const props = defineProps(['editedGalleryItem'])

  const imageUploads = ref([]);
  const updateOrderTimer = ref(-1);

  const getImages: any = computed(() => {
    return imagesStore.images;
  });

  async function uploadImages () {
    if(imageUploads.value.length) {


      for (let i = 0; i < imageUploads.value.length; i++) {
        const imageUpload = imageUploads.value[i]
        const data = new FormData();
        data.append("gallery_id", props.editedGalleryItem.id)
        data.append("files[]", imageUpload)

        await imagesStore.uploadImages(data, props.editedGalleryItem.id)
      }

      setTimeout(function() {
        reloadImagesStore()
      }, 2000)
      imageUploads.value = []

      // imageUploads.value.forEach(function(file:any) {
      //   data.append("files[]", file)
      // })
      //
      //
      // imagesStore.uploadImages(data, props.editedGalleryItem.id)
      //   .then(() => {
      //
      //     setTimeout(function() {
      //       reloadImagesStore()
      //     }, 1000)
      //     imageUploads.value = []
      //   })
    }
  }

  onMounted(() => {
    reloadImagesStore()
  });

  function deleteImage(image: any){
    if(confirm('Are you sure you want to delete this image?') == true) {
      imagesStore.deleteImage(image.id)
        .then(() => {
          setTimeout(function() {
            reloadImagesStore()
          }, 1000)
        })
    }
  }

  function startDragging() {
    if(updateOrderTimer.value != -1) {
      clearTimeout(updateOrderTimer.value)
      updateOrderTimer.value = -1;
    }
  }

  function stopDragging() {
    updateOrderTimer.value = setTimeout(function(){
      imagesStore.updateOrderBy()
    }, 1000)
  }

  function reloadImagesStore() {
    imagesStore.getImagesForGallery(props.editedGalleryItem.id)
  }
</script>

<template>
  <v-card height="100vh">
    <v-card-title class="pa-4 bg-secondary">
      <span class="title text-white">{{ editedGalleryItem.identifier }}: Edit Gallery Images</span>
    </v-card-title>

    <v-card-text>
      <v-container>
<!--        <v-row dense>-->

          <draggable
            :list="getImages"
            item-key="id"
            @start="startDragging"
            @end="stopDragging"
          >
            <template #item="{ element }">
              <div
                class="d-inline-block ma-2 list-group-item"
                style="width: 30%;  border: 2px solid grey; min-height: 100px"
              >
                <!--{{ element.id }}-->
                <v-img
                  :src="element.thumbnail_url"
                  height="150"
                  cover
                >
                </v-img><br/>

                <v-btn
                  color="error"
                  variant="text"
                  @click="deleteImage(element)"
                >Delete</v-btn
                >
              </div>
            </template>
          </draggable>
<!--        </v-row>-->
      </v-container>
      <v-divider></v-divider>
      <v-form ref="form" lazy-validation>
        <v-row>
          <v-col cols="12">
            Upload Files
          </v-col>
          <v-col cols="12">
            <v-file-input
              v-model="imageUploads"
              multiple
              label="File input"
            ></v-file-input>
          </v-col>
        </v-row>
      </v-form>
      <v-row>
        <v-col
          class="text-right"
        >
          <v-btn
            color="secondary"
            variant="flat"
            @click="uploadImages"
          >Upload</v-btn
          >
        </v-col>
      </v-row>
    </v-card-text>

    <v-card-actions class="pa-4">
      <v-spacer></v-spacer>
      <v-btn color="error" @click="$emit('closeGalleryImages')">Close</v-btn>
    </v-card-actions>
  </v-card>
</template>
