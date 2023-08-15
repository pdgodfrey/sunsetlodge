<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useUsersStore } from '@/stores/users';
import { useRolesStore } from '@/stores/roles';

const store = useUsersStore();
const rolesStore = useRolesStore();

onMounted(() => {
  store.getAll();
  rolesStore.getAll();
});
const getUsers: any = computed(() => {
    return store.users;
});

const getRoles: any = computed(() => {
  return rolesStore.roles;
});

const valid = ref(true);
const dialog = ref(false);
const search = ref('');

const requiredFieldRules = ref([
  (v: string) => !!v || 'This field is required'
]);

const editedIndex = ref(-1);
const editedItem = ref({
  id: 0,
  name: '',
  email: '',
  role_id: 2,
  role_name: '',
});
const defaultItem = ref({
  id: 0,
  name: '',
  email: '',
  role_id: 2,
  role_name: '',
});

//Methods
function editItem(item: any) {
    editedIndex.value = getUsers.value.indexOf(item);
    editedItem.value = Object.assign({}, item);
    dialog.value = true;
}
function resetPassword(item: any) {
  if(confirm('Are you sure you want to send a reset password email to this user?') == true) {
    store.resetUserPassword(item.id)
      .then(() => {
        alert("Email Sent")
      })
  }
}
function deleteItem(item: any) {
    if(confirm('Are you sure you want to delete this user?') == true) {
      store.deleteuser(item.id)
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
    if (editedIndex.value > -1) {
      store.updateuser(editedItem.value)
        .then(() => {
          store.getAll()
        })
    } else {
      store.createUser(editedItem.value)
        .then(() => {
          setTimeout(store.getAll, 500)
        })
    }
    close();
}

//Computed Property
const formTitle = computed(() => {
    return editedIndex.value === -1 ? 'New User' : 'Edit User';
});

const isValid = computed(() => {
  const obj = editedItem.value

  if(obj.name == null || obj.name == ''){
    return false
  }
  if(obj.email == null || obj.email == ''){
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
                        <v-icon class="mr-2">mdi-account-multiple-plus</v-icon>Add User
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
                                  <v-label>Email</v-label>
                                  <v-text-field
                                    variant="outlined"
                                    hide-details
                                    v-model="editedItem.email"
                                    :rules="requiredFieldRules"
                                  ></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                  <v-label>Role</v-label>
                                  <v-select
                                    v-model="editedItem.role_id"
                                    :items="getRoles"
                                    item-title="name"
                                    item-value="id"
                                    label="Select"
                                    single-line
                                  ></v-select>
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
                <th class="text-subtitle-1 font-weight-semibold">Email</th>
                <th class="text-subtitle-1 font-weight-semibold">Role</th>
            </tr>
        </thead>
        <tbody>
        <tr></tr>
            <tr v-for="item in getUsers" :key="item.id">
                <td class="text-subtitle-1">{{ item.id }}</td>
                <td class="text-subtitle-1">{{ item.name }}</td>
                <td class="text-subtitle-1">{{ item.email }}</td>
                <td class="text-subtitle-1">{{ item.role_name }}</td>

                <td>
                    <div class="d-flex align-center">
                        <v-tooltip text="Edit">
                            <template v-slot:activator="{ props }">
                                <v-btn icon flat @click="editItem(item)" v-bind="props"
                                    ><PencilIcon stroke-width="1.5" size="20" class="text-primary"
                                /></v-btn>
                            </template>
                        </v-tooltip>
                        <v-tooltip text="Reset User Password">
                          <template v-slot:activator="{ props }">
                            <v-btn icon flat @click="resetPassword(item)" v-bind="props"
                            ><MailIcon stroke-width="1.5" size="20" class="text-error"
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
