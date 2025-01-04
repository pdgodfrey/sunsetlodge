<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useSeasonsStore } from '@/stores/seasons';
import { useBuildingsStore } from '@/stores/buildings';
import { useBookingsStore } from "@/stores/bookings";
import { Form } from 'vee-validate';


import dayjs from "dayjs";

const seasonsStore = useSeasonsStore();
const buildingsStore = useBuildingsStore();
const bookingsStore = useBookingsStore();

onMounted(() => {
    seasonsStore.getAll().then(() => {
      // console.log(getCurrentSeason)
      setTimeout(function() {
        selectedSeasonId.value = getCurrentSeason.value.id

        bookingsStore.getBookingsForSeason(selectedSeasonId.value)
      }, 500)
    })
    buildingsStore.getAll();
});
const getSeasons: any = computed(() => {
    return seasonsStore.seasons;
});


const getCurrentSeason: any = computed(() => {
  return seasonsStore.currentSeason;
});

const getBuildings: any = computed(() => {
  return buildingsStore.buildings;
});

const getBookings: any = computed(() => {
  return bookingsStore.bookings;
});


const valid = ref(true);
const rateValid = ref(false);
const dialog = ref(false);
const ratesDialog = ref(false);
const search = ref('');
const selectedSeasonId = ref(0)

const requiredFieldRules = ref([
  (v: string) => !!v || 'This field is required'
]);

const editedIndex = ref(-1);
const editedItem = ref({
  id: 0,
  season_id: 0,
  building_ids: [1],
  name: '',
  start_date: '',
  end_date: '',
});
const defaultItem = ref({
  id: 0,
  season_id: 0,
  building_ids: [1],
  name: '',
  start_date: '',
  end_date: '',
});

//Methods
function editItem(item: any) {
    editedIndex.value = getBookings.value.indexOf(item);
    editedItem.value = Object.assign({}, item);
    dialog.value = true;
}
function deleteItem(item: any) {
    if(confirm('Are you sure you want to delete this item?') == true) {
      bookingsStore.deleteBooking(item.id)
        .then(() => {
          reloadBookingsStore()
        })
    }
}

function close() {
    dialog.value = false;
    setTimeout(() => {
        editedItem.value = Object.assign({}, defaultItem.value);
        editedIndex.value = -1;
    }, 300);
}
function save() {
    const booking = Object.assign({}, editedItem.value);
    booking.start_date = dayjs(booking.start_date).format("YYYY-MM-DD")
    booking.end_date = dayjs(booking.end_date).format("YYYY-MM-DD")

    if (editedIndex.value > -1) {
      bookingsStore.updateBooking(booking)
        .then(() => {
          reloadBookingsStore()
        })
    } else {
      booking.season_id = selectedSeasonId.value
      bookingsStore.createBooking(booking)
        .then(() => {
          reloadBookingsStore()
        })
    }
    close();
}

function reloadBookingsStore() {
  setTimeout(function() {
    bookingsStore.getBookingsForSeason(selectedSeasonId.value)
  }, 500)

}

//Computed Property
const formTitle = computed(() => {
    return editedIndex.value === -1 ? 'New Booking' : 'Edit Booking';
});

const isValid = computed(() => {
  const obj = editedItem.value

  if(obj.name == null || obj.name == ''){
    return false
  }
  if(obj.building_ids == null || obj.building_ids.length == 0){
    return false
  }
  if(obj.start_date == null || obj.start_date == ''){
    return false
  }
  if(obj.end_date == null || obj.end_date == ''){
    return false
  }

  return true
});

function onSeasonSelect() {
  reloadBookingsStore()
}
</script>
<template>
    <v-row>
        <v-col cols="3" class="text-left">
          <v-label>Season:</v-label>
          <v-select
            v-model="selectedSeasonId"
            :items="getSeasons"
            item-title="name"
            item-value="id"
            label="Select"
            single-line
            @update:modelValue="onSeasonSelect"
          ></v-select>
        </v-col>
        <v-col cols="9" class="text-right">
            <v-dialog v-model="dialog" max-width="600" min-height="600" persistent>
                <template v-slot:activator="{ props }">
                    <v-btn color="primary" v-bind="props" flat class="ml-auto">
                        <v-icon class="mr-2">mdi-account-multiple-plus</v-icon>Add Booking For Season
                    </v-btn>
                </template>
                <v-card height="100vh">
                    <v-card-title class="pa-4 bg-secondary">
                        <span class="title text-white">{{ formTitle }}</span>
                    </v-card-title>

                    <v-card-text>
                        <v-form ref="form" v-model="valid" lazy-validation>
                            <v-row>

                                <v-col cols="12">
                                    <v-label>Name</v-label>
                                    <v-text-field
                                        variant="outlined"
                                        hide-details
                                        v-model="editedItem.name"
                                        :rules="requiredFieldRules"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                  <v-label>Building(s)</v-label>
                                  <v-select
                                    v-model="editedItem.building_ids"
                                    :items="getBuildings"
                                    item-title="name"
                                    item-value="id"
                                    label="Select"
                                    persistent-hint
                                    multiple
                                    single-line
                                  ></v-select>
                                </v-col>
                                <v-col cols="12" sm="6">
                                  <v-label>Start Date</v-label>
                                  <VueDatePicker
                                    v-model="editedItem.start_date"
                                    format="MM/dd/yyyy"
                                    :enable-time-picker="false"
                                    required
                                    auto-apply
                                    text-input
                                  ></VueDatePicker>
                                </v-col>
                                <v-col cols="12" sm="6">
                                  <v-label>End Date</v-label>
                                  <VueDatePicker
                                    v-model="editedItem.end_date"
                                    format="MM/dd/yyyy"
                                    :enable-time-picker="false"
                                    required
                                    auto-apply
                                    text-input
                                  ></VueDatePicker>
                                </v-col>
                            </v-row>
                        </v-form>
                    </v-card-text>

                    <v-card-actions class="pa-4">
                        <v-spacer></v-spacer>
                        <v-btn color="error" @click="close">Cancel</v-btn>
                        <v-btn
                            color="secondary"
                            :disabled="!isValid"
                            variant="flat"
                            @click="save"
                            >Save</v-btn
                        >
                    </v-card-actions>
                </v-card>
            </v-dialog>
        </v-col>
    </v-row>
    <v-table class="mt-5">
        <thead>
            <tr>
                <th class="text-subtitle-1 font-weight-semibold">Id</th>
                <th class="text-subtitle-1 font-weight-semibold">Name</th>
                <th class="text-subtitle-1 font-weight-semibold">Dates</th>
                <th class="text-subtitle-1 font-weight-semibold">Buildings</th>
            </tr>
        </thead>
        <tbody>
          <tr></tr>
            <tr v-for="item in getBookings" :key="item.id">
                <td class="text-subtitle-1">{{ item.id }}</td>
                <td class="text-subtitle-1">{{ item.name }}</td>
                <td>
                    <div class="d-flex align-center py-4">
                        <div>
                            <h4 class="text-h6">{{ dayjs(item.start_date).format("MMM DD") }} - {{ dayjs(item.end_date).format("MMM DD, YYYY") }}</h4>
                        </div>
                    </div>
                </td>
                <td class="text-subtitle-1">
                  <template v-for="(building, index) in item.buildings">
                    {{ building.name }}<template v-if="index < item.buildings.length - 1">, </template>
                  </template>
                </td>
                <td>
                    <div class="d-flex align-center">
                        <v-tooltip text="Edit">
                            <template v-slot:activator="{ props }">
                                <v-btn icon flat @click="editItem(item)" v-bind="props"
                                    ><PencilIcon stroke-width="1.5" size="20" class="text-primary"
                                /></v-btn>
                            </template>
                        </v-tooltip>
                        <v-tooltip text="Delete">
                            <template v-slot:activator="{ props }">
                                <v-btn icon flat @click="deleteItem(item)" v-bind="props"
                                    ><TrashIcon stroke-width="1.5" size="20" class="text-error"
                                /></v-btn>
                            </template>
                        </v-tooltip>
                    </div>
                </td>
            </tr>
        </tbody>
    </v-table>
</template>
