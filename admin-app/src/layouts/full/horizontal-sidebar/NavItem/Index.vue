<script setup>
import { Icon } from '@iconify/vue';
const props = defineProps({ item: Object, level: Number });
</script>

<template>
    <!---Single Item-->
    <router-link :to="`${item.to}`" class="navItemLink single-link rounded-pill"  :disabled="item.disabled">
        <!---If icon-->
        <i class="navIcon"> <Icon :icon="'solar:' + item.icon" height="18" width="18" :level="level" class="dot" :class="'text-' + item.BgColor"/></i>
        <span>{{ item.title }}</span>
        <!---If Caption-->
        <small v-if="item.subCaption" class="text-caption mt-n1 hide-menu">
            {{ item.subCaption }}
        </small>
        <!---If any chip or label-->
        <template v-if="item.chip">
            <v-chip
                :color="item.chipColor"
                class="sidebarchip hide-menu ml-auto"
                :size="item.chipIcon ? 'small' : 'small'"
                :variant="item.chipVariant"
                :prepend-icon="item.chipIcon"
            >
                {{ item.chip }}
            </v-chip>
        </template>
    </router-link>
</template>
