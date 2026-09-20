<script setup lang="ts">
import { onMounted, ref } from "vue";
import { loadIcon } from "./icons";

const props = defineProps<{
    appId: number;
}>();

const source = ref<string>();

onMounted(async () => {
    try {
        source.value = await loadIcon(props.appId);
    } catch {
        // The package may have disappeared since the list was loaded.
    }
});
</script>

<template>
    <img :src="source" alt="" width="42" height="42" />
</template>
