<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { DeviceSession } from '@canteen-smile/contracts'
import {
  getDeviceSessions,
  logoutDeviceSession,
  logoutOtherDeviceSessions,
} from '../api/sessionApi'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const loading = ref(false)
const items = ref<DeviceSession[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

/** 加载当前账号真实有效设备会话。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await getDeviceSessions(pageNo.value, pageSize.value)
    items.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

/** 应用编码中文展示。 */
function appName(code: DeviceSession['appCode']): string {
  return code === 'TENANT_ADMIN' ? '租户管理端' : '租户用户端'
}

/** 服务端时间中文展示。 */
function time(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const logoutFlight = useSingleFlight(async (session: DeviceSession) => {
  await logoutDeviceSession(session.sessionId, session.version)
  feedback.success('指定设备已下线')
  await load()
})

const logoutOthersFlight = useSingleFlight(async () => {
  await logoutOtherDeviceSessions()
  feedback.success('其他全部设备已下线')
  await load()
})

onMounted(load)
</script>

<template>
  <section class="session-page">
    <header>
      <div><p>账号安全</p><h2>登录设备</h2><span>管理当前账号在两个租户端的有效会话。</span></div>
      <el-button type="danger" plain :loading="logoutOthersFlight.pending.value" @click="logoutOthersFlight.run()">下线其他全部设备</el-button>
    </header>
    <div class="panel" v-loading="loading">
      <el-table :data="items" empty-text="暂无有效设备会话">
        <el-table-column label="设备" min-width="200"><template #default="{ row }"><strong>{{ row.deviceName || row.deviceType }}</strong><small>{{ appName(row.appCode) }}</small></template></el-table-column>
        <el-table-column label="登录 IP" min-width="140" prop="loginIpAddress" />
        <el-table-column label="登录时间" min-width="180"><template #default="{ row }">{{ time(row.loginTime) }}</template></el-table-column>
        <el-table-column label="最近活动" min-width="180"><template #default="{ row }">{{ time(row.lastActiveTime) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.current ? 'success' : 'info'">{{ row.current ? '当前设备' : '在线' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="danger" :loading="logoutFlight.pending.value" @click="logoutFlight.run(row)">下线</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="pageNo" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="sizes, prev, pager, next, total" @change="load" /></div>
    </div>
  </section>
</template>

<style scoped>
.session-page header { margin-bottom: 22px; display:flex; justify-content:space-between; align-items:flex-end; gap:24px; }
header p { margin:0 0 8px; color:#317151; font-size:11px; font-weight:700; letter-spacing:.14em; }
h2 { margin:0; font-size:34px; } header span, small { color:#718078; }
.panel { overflow:hidden; border:1px solid #dce5df; border-radius:16px; background:#fff; }
.panel strong,.panel small { display:block; }.panel small { margin-top:4px; }
.pagination { padding:18px 22px; display:flex; justify-content:flex-end; border-top:1px solid #e8eeea; }
@media(max-width:760px){.session-page header{align-items:flex-start;flex-direction:column}}
</style>
