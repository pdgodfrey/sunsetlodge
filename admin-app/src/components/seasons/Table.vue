<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useSeasonsStore } from '@/stores/seasons';
import { useRatesStore } from "@/stores/rates";
import { Form } from 'vee-validate';


import dayjs from "dayjs";

const store = useSeasonsStore();
const ratesStore = useRatesStore();

onMounted(() => {
    store.getAll();
});
const getSeasons: any = computed(() => {
    return store.seasons;
});

const getRates: any = computed(() => {
  return ratesStore.rates;
});


const valid = ref(true);
const rateValid = ref(false);
const dialog = ref(false);
const ratesDialog = ref(false);
const search = ref('');

const requiredFieldRules = ref([
  (v: string) => !!v || 'This field is required'
]);

const editedIndex = ref(-1);
const editedItem = ref({
  id: 0,
  name: '',
  start_date: '',
  end_date: '',
  high_season_start_date: '',
  high_season_end_date: '',
  is_open: false,
  is_current: false
});
const defaultItem = ref({
    id: 0,
    name: '',
    start_date: '',
    end_date: '',
    high_season_start_date: '',
    high_season_end_date: '',
    is_open: false,
    is_current: false
});

//Methods
function editItem(item: any) {
    editedIndex.value = getSeasons.value.indexOf(item);
    editedItem.value = Object.assign({}, item);
    dialog.value = true;
}
function deleteItem(item: any) {
    if(confirm('Are you sure you want to delete this item?') == true) {
      store.deleteSeason(item.id)
        .then(() => {
          setTimeout(store.getAll, 500)
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
    const season = Object.assign({}, editedItem.value);
    season.start_date = dayjs(season.start_date).format("YYYY-MM-DD")
    season.end_date = dayjs(season.end_date).format("YYYY-MM-DD")
    season.high_season_start_date = dayjs(season.high_season_start_date).format("YYYY-MM-DD")
    season.high_season_end_date = dayjs(season.high_season_end_date).format("YYYY-MM-DD")

    if (editedIndex.value > -1) {
      store.updateSeason(season)
        .then(() => {
          store.getAll()
        })
    } else {
      store.createSeason(season)
        .then(() => {
          setTimeout(store.getAll, 500)
        })
    }
    close();
}

function editItemRates(item: any) {
  ratesStore.getRatesForSeason(item.id)
    .then(() => {
      ratesDialog.value = true
    })
}
function closeRatesDialog() {
  ratesDialog.value = false
}
function saveRates() {

  const promiseList: any[] = [];

  getRates.value.forEach((rate: any) => {
    const rateCopy = Object.assign({}, rate);
    if(rate.high_season_rate && rate.high_season_rate != '') {
      rateCopy.high_season_rate = parseInt(rate.high_season_rate)
    }
    if(rate.low_season_rate && rate.low_season_rate != '') {
      rateCopy.low_season_rate = parseInt(rate.low_season_rate)
    }

    ratesStore.updateRate(rateCopy)

  })

  ratesDialog.value = false
}

//Computed Property
const formTitle = computed(() => {
    return editedIndex.value === -1 ? 'New Season' : 'Edit Season';
});

const isValid = computed(() => {
  const obj = editedItem.value

  if(obj.name == null || obj.name == ''){
    return false
  }
  if(obj.start_date == null || obj.start_date == ''){
    return false
  }
  if(obj.end_date == null || obj.end_date == ''){
    return false
  }
  if(obj.high_season_start_date == null || obj.high_season_start_date == ''){
    return false
  }
  if(obj.high_season_end_date == null || obj.high_season_end_date == ''){
    return false
  }

  return true
});
</script>
<template>
    <v-row>
        <v-col cols="12" class="text-right">
            <v-dialog v-model="dialog" max-width="600" min-height="600" persistent>
                <template v-slot:activator="{ props }">
                    <v-btn color="primary" v-bind="props" flat class="ml-auto">
                        <v-icon class="mr-2">mdi-account-multiple-plus</v-icon>Add Season
                    </v-btn>
                </template>
                <v-card height="100vh">
                    <v-card-title class="pa-4 bg-secondary">
                        <span class="title text-white">{{ formTitle }}</span>
                    </v-card-title>

                    <v-card-text>
                        <v-form ref="form" v-model="valid" lazy-validation>
                            <v-row>
                                <v-col cols="12" sm="6">
                                    <v-label>Season Year</v-label>
                                    <v-text-field
                                        variant="outlined"
                                        hide-details
                                        v-model="editedItem.name"
                                        :rules="requiredFieldRules"
                                        type="number"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6">
                                  <v-checkbox
                                    label="Open For Bookings?"
                                    v-model="editedItem.is_open"
                                  ></v-checkbox>
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
                                <v-col cols="12" sm="6">
                                  <v-label>High Season Start Date</v-label>
                                  <VueDatePicker
                                    v-model="editedItem.high_season_start_date"
                                    format="MM/dd/yyyy"
                                    :enable-time-picker="false"
                                    required
                                    auto-apply
                                    text-input
                                  ></VueDatePicker>
                                </v-col>
                                <v-col cols="12" sm="6">
                                  <v-label>High Season End Date</v-label>
                                  <VueDatePicker
                                    v-model="editedItem.high_season_end_date"
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
    <v-row>
      <v-col cols="12" class="text-right">
        <v-dialog v-model="ratesDialog" max-width="600" persistent>
          <v-card height="100vh">
            <v-card-title class="pa-4 bg-secondary">
              <span class="title text-white">Set Season Rates</span>
            </v-card-title>

            <v-card-text>
              <v-form  v-model="rateValid" >
                <v-row>
                  <v-col cols="6"><span class="title">Building</span></v-col>
                  <v-col cols="3">High Rate</v-col>
                  <v-col cols="3">Low Rate</v-col>
                </v-row>
                <v-row v-for="rate in getRates">
                  <v-col cols="6">
                    <v-label>{{ rate.building_name }}</v-label>
                  </v-col>
                  <v-col cols="3"
                         v-if="rate.building_id === 1"
                  >
                    <v-text-field
                      v-model="rate.high_season_rate"
                      type="number"
                      :rules="requiredFieldRules"
                      required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="3"
                         v-else
                  >
                    <v-text-field
                      v-model="rate.high_season_rate"
                      type="number"
                      required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="3">
                    <VTextField
                      v-model="rate.low_season_rate"
                      type="number"
                      :rules="requiredFieldRules"
                      required
                    ></VTextField>
                  </v-col>
                </v-row>
              </v-form>
            </v-card-text>

            <v-card-actions class="pa-4">
              <v-spacer></v-spacer>
              <v-btn color="error" @click="closeRatesDialog">Cancel</v-btn>
              <v-btn
                color="secondary"
                @click="saveRates"
                :disabled="(rateValid === false)"
                variant="flat"
                type="submit"
              >Save</v-btn>
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
                <th class="text-subtitle-1 font-weight-semibold">Season Dates</th>
                <th class="text-subtitle-1 font-weight-semibold">Open for Booking?</th>
                <th class="text-subtitle-1 font-weight-semibold">Current Season?</th>
            </tr>
        </thead>
        <tbody>
        <tr></tr>
            <tr v-for="item in getSeasons" :key="item.id">
                <td class="text-subtitle-1">{{ item.id }}</td>
                <td class="text-subtitle-1">{{ item.name }}</td>
                <td>
                    <div class="d-flex align-center py-4">
                        <div>
                            <h4 class="text-h6">{{ dayjs(item.start_date).format("MMM DD") }} - {{ dayjs(item.end_date).format("MMM DD, YYYY") }}</h4>
                            <span class="text-subtitle-2 d-block mt-1 textSecondary">(High season: {{ dayjs(item.high_season_start_date).format("MMM DD") }}
                              - {{ dayjs(item.high_season_end_date).format("MMM DD") }})</span>
                        </div>
                    </div>
                </td>
                <td class="text-subtitle-1">
                  <v-chip v-if="item.is_open" color="success" size="small" label>Yes</v-chip>
                  <v-chip v-if="!item.is_open" size="small" label>No</v-chip>
                </td>
                <td class="text-subtitle-1">
                  <v-chip v-if="item.is_current" color="success" size="small" label>Yes</v-chip>
                  <v-chip v-if="!item.is_current" size="small" label>No</v-chip>
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
                        <v-tooltip text="Edit Rates">
                          <template v-slot:activator="{ props }">
                            <v-btn icon flat @click="editItemRates(item)" v-bind="props"
                            ><CashIcon stroke-width="1.5" size="20" class="text-primary"
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
