<script setup lang="ts">
import { ref } from 'vue';
import { useTheme } from 'vuetify';
import { useCustomizerStore } from '@/stores/customizer';

const theme = useTheme();
const customizer = useCustomizerStore();

// template skin color options
const themeColors = ref([
    {
        name: 'BLUE_THEME',
        bg: 'togglethemeBlue'
    },
    {
        name: 'DARK_BLUE_THEME',
        bg: 'togglethemeDarkBlue'
    }
]);
</script>

<template>
    <div class="position-relative">
        <v-item-group mandatory v-model="customizer.actTheme" class="d-flex">
            <div v-for="theme in themeColors" :key="theme.name">
                <v-item v-slot="{ toggle }" :value="theme.name" >
                    <v-btn icon :class="theme.bg" variant="text" color="primary" @click="toggle" size="small" class="ms-md-2 custom-hover-primary-white">
                        <Icon v-if="theme.bg == 'togglethemeBlue'" icon="solar:sun-2-line-duotone" :class="theme.bg" height="20"  />
                        <Icon v-if="theme.bg == 'togglethemeDarkBlue'" icon="solar:moon-line-duotone" :class="theme.bg" height="20"  />
                    </v-btn>
                </v-item>
            </div>
        </v-item-group>
    </div>
</template>
