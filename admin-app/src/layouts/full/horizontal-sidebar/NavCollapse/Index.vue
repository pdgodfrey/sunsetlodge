<script setup>
import NavItem from '../NavItem/Index.vue';
import { Icon } from '@iconify/vue';

const props = defineProps({ item: Object, level: Number });
</script>

<template>
    <!---Dropdown  -->
    <a class="navItemLink rounded-pill cursor-pointer navcollapse">
        <!---Icon  -->
        <i class="navIcon"><Icon :icon="'solar:' + item.icon" height="18" width="18" :level="level" class="dot" :class="'text-' + item.BgColor"/></i>
        <!---Title  -->
        <span class="mr-auto">{{ item.title }}</span>
        <!---If Caption-->
        <small v-if="item.subCaption" class="text-caption mt-n1 hide-menu">
            {{ item.subCaption }}
        </small>
        <i class="ddIcon ml-2 d-flex align-center"><ChevronDownIcon size="15" /></i>
    </a>
    <!---Sub Item-->
    <ul :class="`ddMenu ddLevel-${level + 1}`">
        <li v-for="(subitem, i) in item.children" :key="i" v-if="item.children" class="navItem">
            <Index :item="subitem" v-if="subitem.children" :level="level + 1" />
            <NavItem :item="subitem" :level="level + 1" v-else></NavItem>
        </li>
    </ul>
    <!---End Item Sub Header -->
</template>
