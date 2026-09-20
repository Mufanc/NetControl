<script setup lang="ts">
import {
    NAlert,
    NButton,
    NCheckbox,
    NConfigProvider,
    NEmpty,
    NInput,
    NSpin,
    NSwitch,
    type GlobalThemeOverrides,
} from "naive-ui";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import AppIcon from "./AppIcon.vue";
import { type AppInfo, onDisconnect, request } from "./api";
import { clearIcons } from "./icons";

type Filter = "all" | "blocked";

const theme: GlobalThemeOverrides = {
    common: {
        borderRadius: "10px",
        primaryColor: "#255bc9",
        primaryColorHover: "#356bd8",
        primaryColorPressed: "#194da9",
    },
};

const apps = ref<AppInfo[]>([]);
const blocked = ref(new Set<number>());
const filter = ref<Filter>("all");
const search = ref("");
const showSystem = ref(false);
const limit = ref(60);
const active = ref(true);
const loading = ref(false);
const saving = ref(false);
const notice = ref("");

const filteredApps = computed(() => {
    const term = search.value.trim().toLowerCase();

    return apps.value.filter(
        (app) =>
            (showSystem.value || !app.system) &&
            (filter.value !== "blocked" || blocked.value.has(app.appid)) &&
            `${app.label} ${app.packages.join(" ")} ${app.appid}`
                .toLowerCase()
                .includes(term),
    );
});

const visibleApps = computed(() => filteredApps.value.slice(0, limit.value));

onDisconnect(() => {
    active.value = false;
    notice.value = "管理服务不可达。请关闭此页，从 KernelSU 模块入口重新进入。";
});

async function refresh(): Promise<void> {
    if (loading.value || !active.value) return;

    loading.value = true;

    try {
        const [appsResponse, blockedResponse] = await Promise.all([
            request("api/apps"),
            request("api/blacklist"),
        ]);

        apps.value = ((await appsResponse.json()) as AppInfo[]).sort(
            (left, right) => left.label.localeCompare(right.label),
        );
        blocked.value = new Set((await blockedResponse.json()) as number[]);
        notice.value = "";
    } catch (err) {
        if (active.value) notice.value = `读取失败：${message(err)}`;
    } finally {
        loading.value = false;
    }
}

async function setAllowed(app: AppInfo, allowed: boolean): Promise<void> {
    if (saving.value || !active.value) return;

    saving.value = true;

    try {
        await request(`api/blacklist/${app.appid}`, {
            method: allowed ? "DELETE" : "PUT",
        });

        const next = new Set(blocked.value);

        if (allowed) next.delete(app.appid);
        else next.add(app.appid);

        blocked.value = next;
        notice.value = "";
    } catch (err) {
        if (active.value) notice.value = `保存失败：${message(err)}`;
    } finally {
        saving.value = false;
    }
}

function selectFilter(next: Filter): void {
    filter.value = next;
    limit.value = 60;
}

function message(err: unknown): string {
    return err instanceof Error ? err.message : "操作失败";
}

onMounted(refresh);
onBeforeUnmount(clearIcons);
window.addEventListener("pagehide", clearIcons, { once: true });
</script>

<template>
    <n-config-provider :theme-overrides="theme">
        <main>
            <header>
                <div>
                    <h1>Net<span>Control</span></h1>
                    <p class="subtitle">应用联网权限</p>
                </div>

                <div class="signal" :class="{ off: !active }">
                    <i />
                    <span>{{ active ? "已连接" : "连接已断开" }}</span>
                </div>
            </header>

            <n-alert v-if="notice" type="error" :show-icon="false">
                {{ notice }}
            </n-alert>

            <div class="toolbar">
                <n-input
                    v-model:value="search"
                    clearable
                    placeholder="搜索应用、包名或 App ID"
                    @update:value="limit = 60"
                />
                <n-button
                    :disabled="!active"
                    :loading="loading"
                    @click="refresh"
                >
                    刷新
                </n-button>
            </div>

            <div class="filters">
                <n-button
                    size="small"
                    :type="filter === 'all' ? 'primary' : 'default'"
                    @click="selectFilter('all')"
                >
                    全部
                </n-button>
                <n-button
                    size="small"
                    :type="filter === 'blocked' ? 'primary' : 'default'"
                    @click="selectFilter('blocked')"
                >
                    已禁止
                </n-button>
                <n-checkbox
                    v-model:checked="showSystem"
                    @update:checked="limit = 60"
                >
                    显示系统应用
                </n-checkbox>
            </div>

            <div class="summary" role="status">
                {{ filteredApps.length }} 个应用 · {{ blocked.size }} 个 App ID
                已禁止
            </div>

            <n-spin :show="loading && apps.length === 0">
                <section class="list" aria-label="应用列表">
                    <article
                        v-for="app in visibleApps"
                        :key="app.appid"
                        class="app"
                    >
                        <app-icon :app-id="app.appid" />

                        <div class="info">
                            <div class="name">{{ app.label }}</div>
                            <div class="meta">
                                {{ app.packages.join("\n") }} ID {{ app.appid }}
                            </div>
                        </div>

                        <label class="toggle">
                            <span>{{
                                blocked.has(app.appid) ? "已禁止" : "允许"
                            }}</span>
                            <n-switch
                                :value="!blocked.has(app.appid)"
                                :disabled="saving || !active"
                                :aria-label="`${app.label} 允许联网`"
                                @update:value="setAllowed(app, $event)"
                            />
                        </label>
                    </article>

                    <n-empty
                        v-if="!loading && filteredApps.length === 0"
                        description="没有符合条件的应用"
                    />
                </section>
            </n-spin>

            <n-button
                v-if="filteredApps.length > limit"
                class="more"
                @click="limit += 60"
            >
                显示更多
            </n-button>

            <p class="foot">
                同一 App ID
                的应用共享策略，包括其他用户中的同一应用。更改立即作用于新建网络连接，已有连接不会被中断。
            </p>
        </main>
    </n-config-provider>
</template>
