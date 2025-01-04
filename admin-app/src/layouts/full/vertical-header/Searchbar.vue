<script setup>
import { SearchIcon } from 'vue-tabler-icons';
import { searchSugg } from '@/_mockApis/headerData';
import { ref } from 'vue';

const dialog = ref(false);

</script>

<template>
    <!-- ---------------------------------------------- -->
    <!--Search Dialog -->
    <!-- ------------------------------------------------>
    <v-btn
        icon
        variant="text"
        class="ms-md-2 custom-hover-primary-white"
        color="primary"
        size="small"
        @click="dialog = true"

    >
        <Icon icon="solar:magnifer-linear" height="20" />
    </v-btn>
    <v-dialog v-model="dialog" class="dialog-mw" transition="dialog-top-transition" top="0">
        <v-card>
            <v-card-text class="pa-4">
                <div class="d-flex align-center justify-space-between">
                    <v-text-field placeholder="Search here" color="primary" density="compact" variant="outlined" hide-details></v-text-field>
                </div>
            </v-card-text>
            <v-divider></v-divider>
            <v-card-text class="pa-4">
                <h5 class="text-h5 pb-4">Quick Page Links</h5>
                <perfect-scrollbar style="height: 300px">
                    <v-list class="pt-0 px-0" lines="two">
                        <v-list-item
                            :value="item"
                            v-for="(item, index) in searchSugg"
                            :key="index"
                            :to="item.href"
                            color="primary"
                            class="px-2 py-2 rounded-md"
                        >
                            <h6 class="text-subtitle-1 font-weight-medium mb-1">{{ item.title }}</h6>
                            <p class="text-subtitle-2 text-medium-emphasis">{{ item.href }}</p>
                        </v-list-item>
                    </v-list>
                </perfect-scrollbar>
            </v-card-text>
        </v-card>
    </v-dialog>
</template>
