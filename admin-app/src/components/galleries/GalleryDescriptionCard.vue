<script setup lang="ts">
  import {ref} from "vue";

  defineProps(['editedGalleryItem'])

  const valid = ref(true);
</script>

<template>
  <v-card height="100vh">
    <v-card-title class="pa-4 bg-secondary">
      <span class="title text-white">Edit Gallery Description</span>
    </v-card-title>

    <v-card-text>
      <v-form ref="form" v-model="valid" lazy-validation>
        <v-row>

          <v-col cols="12">
            {{ editedGalleryItem.identifier }}
          </v-col>
          <v-col cols="12">
            <v-label>Description</v-label>
            <v-textarea
              v-model="editedGalleryItem.description"
              rows="8"
            ></v-textarea>
          </v-col>
          <h4>Preview</h4>
          <p><b>{{ editedGalleryItem.identifier }}&nbsp;</b>
            <span v-if="editedGalleryItem.description"
                  v-html="editedGalleryItem.description.replaceAll('\n', '<br/>')"></span>
          </p>
        </v-row>
      </v-form>
    </v-card-text>

    <v-card-actions class="pa-4">
      <v-spacer></v-spacer>
      <v-btn color="error" @click="$emit('closeGallery')">Cancel</v-btn>
      <v-btn
        color="secondary"
        variant="flat"
        @click="$emit('saveGallery')"
      >Save</v-btn
      >
    </v-card-actions>
  </v-card>
</template>
