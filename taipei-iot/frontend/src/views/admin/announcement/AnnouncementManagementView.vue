<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/authStore'
import { useDeptStore } from '@/stores/deptStore'
import {
  listAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
} from '@/api/announcement'
import { getDeptOptions } from '@/api/dept'
import type { AnnouncementResponse, AnnouncementRequest } from '@/types/announcement'
import type { DeptOptionVO } from '@/types/dept'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()
const authStore = useAuthStore()
const deptStore = useDeptStore()

// ── 權限判斷 ──
const isAdmin = computed(() => {
  const roles = authStore.userInfo?.roles ?? []
  return roles.includes('ADMIN') || roles.includes('SUPER_ADMIN')
})

const isDeptAdmin = computed(() => {
  const roles = authStore.userInfo?.roles ?? []
  return roles.includes('DEPT_ADMIN')
})

const userDeptId = computed(() => authStore.userInfo?.deptId)

// ── 列表 ──
const loading = ref(false)
const tableData = ref<AnnouncementResponse[]>([])
const pagination = reactive({ page: 0, size: 10, total: 0 })
const statusFilter = ref('ALL')
const keyword = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await listAnnouncements({
      admin: true,
      statusFilter: statusFilter.value,
      keyword: keyword.value || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    if (res.errorCode === '00000') {
      tableData.value = res.body.content
      pagination.total = res.body.totalElements
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.page = page - 1
  fetchList()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 0
  fetchList()
}

function handleSearch() {
  pagination.page = 0
  fetchList()
}

// ── Dialog ──
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const deptOptions = ref<DeptOptionVO[]>([])

const form = reactive<AnnouncementRequest & { publishMode: 'now' | 'schedule'; neverExpire: boolean }>({
  title: '',
  content: '',
  status: 'DRAFT',
  scope: 'ALL',
  targetDeptIds: [],
  pinned: false,
  publishAt: null,
  expireAt: null,
  publishMode: 'now',
  neverExpire: false,
})

const expireManuallyEdited = ref(false)

function resetForm() {
  form.title = ''
  form.content = ''
  form.status = 'DRAFT'
  form.scope = 'ALL'
  form.targetDeptIds = []
  form.pinned = false
  form.publishAt = null
  form.expireAt = null
  form.publishMode = 'now'
  form.neverExpire = false
  expireManuallyEdited.value = false
}

function openCreate() {
  resetForm()
  dialogMode.value = 'create'
  editingId.value = null

  // DEPT_ADMIN：自動填入自己部門
  if (isDeptAdmin.value && userDeptId.value) {
    form.scope = 'DEPT'
    form.targetDeptIds = [Number(userDeptId.value)]
  }

  dialogVisible.value = true
}

function openEdit(row: AnnouncementResponse) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.title = row.title
  form.content = row.content
  form.status = row.status
  form.scope = row.scope
  form.targetDeptIds = row.targetDeptIds ?? []
  form.pinned = row.pinned
  form.publishAt = row.publishAt
  form.expireAt = row.expireAt
  form.publishMode = 'schedule'
  form.neverExpire = row.expireAt === null
  expireManuallyEdited.value = true

  // DEPT_ADMIN：強制自己部門
  if (isDeptAdmin.value && userDeptId.value) {
    form.scope = 'DEPT'
    form.targetDeptIds = [Number(userDeptId.value)]
  }

  dialogVisible.value = true
}

// 自動計算 expire_at（publish_at + 30 天）
function toDateTimeString(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

watch(() => form.publishAt, (newVal) => {
  if (!expireManuallyEdited.value && !form.neverExpire && newVal) {
    const d = new Date(newVal)
    d.setDate(d.getDate() + 30)
    form.expireAt = toDateTimeString(d)
  }
})

watch(() => form.neverExpire, (val) => {
  if (val) {
    form.expireAt = null
  } else if (form.publishAt) {
    expireManuallyEdited.value = false
    const d = new Date(form.publishAt)
    d.setDate(d.getDate() + 30)
    form.expireAt = toDateTimeString(d)
  }
})

function onExpireAtManualChange() {
  expireManuallyEdited.value = true
}

async function handleSave() {
  const payload: AnnouncementRequest = {
    title: form.title,
    content: form.content,
    status: form.status,
    scope: form.scope,
    targetDeptIds: form.scope === 'DEPT' ? form.targetDeptIds : [],
    pinned: form.pinned,
    publishAt: form.publishMode === 'now' ? null : form.publishAt,
    expireAt: form.neverExpire ? null : form.expireAt,
  }

  try {
    if (dialogMode.value === 'create') {
      const res = await createAnnouncement(payload)
      if (res.errorCode === '00000') {
        ElMessage.success(t('announcement.msg.created'))
        dialogVisible.value = false
        fetchList()
      }
    } else if (editingId.value) {
      const res = await updateAnnouncement(editingId.value, payload)
      if (res.errorCode === '00000') {
        ElMessage.success(t('announcement.msg.updated'))
        dialogVisible.value = false
        fetchList()
      }
    }
  } catch {
    ElMessage.error(t('common.error'))
  }
}

async function handleDelete(row: AnnouncementResponse) {
  try {
    await ElMessageBox.confirm(
      t('announcement.msg.deleteConfirm'),
      t('common.confirm'),
      { type: 'warning' },
    )
    const res = await deleteAnnouncement(row.id)
    if (res.errorCode === '00000') {
      ElMessage.success(t('announcement.msg.deleted'))
      fetchList()
    }
  } catch {
    // cancelled
  }
}

// ── 狀態顯示 ──
function getStatusType(row: AnnouncementResponse): string {
  if (row.status === 'DRAFT') return 'info'
  if (row.expireAt && new Date(row.expireAt) < new Date()) return 'danger'
  return 'success'
}

function getStatusLabel(row: AnnouncementResponse): string {
  if (row.status === 'DRAFT') return t('announcement.status.draft')
  if (row.expireAt && new Date(row.expireAt) < new Date()) return t('announcement.status.expired')
  return t('announcement.status.published')
}

function getScopeLabel(row: AnnouncementResponse): string {
  if (row.scope === 'ALL') return t('announcement.scope.all')
  return row.targetDeptNames?.join('、') || t('announcement.scope.dept')
}

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getExpireLabel(row: AnnouncementResponse): string {
  if (!row.expireAt) return t('announcement.neverExpire')
  return formatDateTime(row.expireAt)
}

// ── 初始化 ──
onMounted(async () => {
  fetchList()
  // 載入部門選項
  try {
    const res = await getDeptOptions()
    if (res.errorCode === '00000') {
      deptOptions.value = res.body
    }
  } catch {
    // fallback
  }
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>{{ t('announcement.admin.title') }}</h2>
        <p class="page-subtitle">{{ t('announcement.admin.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        + {{ t('announcement.admin.create') }}
      </el-button>
    </div>

    <!-- 篩選 -->
    <div class="filter-bar">
      <el-select v-model="statusFilter" style="width: 140px" @change="handleSearch">
        <el-option :label="t('announcement.filter.all')" value="ALL" />
        <el-option :label="t('announcement.status.draft')" value="DRAFT" />
        <el-option :label="t('announcement.status.published')" value="PUBLISHED" />
        <el-option :label="t('announcement.status.expired')" value="EXPIRED" />
      </el-select>
      <el-input
        v-model="keyword"
        :placeholder="t('announcement.filter.searchTitle')"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <!-- 列表 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column :label="t('announcement.field.title')" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column :label="t('announcement.field.scope')" width="140">
        <template #default="{ row }">{{ getScopeLabel(row) }}</template>
      </el-table-column>
      <el-table-column :label="t('announcement.field.publishAt')" width="130">
        <template #default="{ row }">{{ formatDateTime(row.publishAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('announcement.field.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row)" size="small">{{ getStatusLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('announcement.field.pinned')" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.pinned">✓</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('announcement.field.expireAt')" width="130">
        <template #default="{ row }">{{ getExpireLabel(row) }}</template>
      </el-table-column>
      <el-table-column :label="t('announcement.field.actions')" width="100" fixed="right">
        <template #default="{ row }">
          <template v-if="row.editable !== false">
            <el-button type="primary" link size="small" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分頁 -->
    <div class="pagination-bar">
      <span>{{ t('announcement.admin.total', { count: pagination.total }) }}</span>
      <el-pagination
        :current-page="pagination.page + 1"
        :page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新增/編輯 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('announcement.admin.create') : t('announcement.admin.edit')"
      width="600px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item :label="t('announcement.field.title')" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item :label="t('announcement.field.content')" required>
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>

        <el-form-item :label="t('announcement.field.scope')">
          <template v-if="isAdmin">
            <el-radio-group v-model="form.scope">
              <el-radio value="ALL">{{ t('announcement.scope.all') }}</el-radio>
              <el-radio value="DEPT">{{ t('announcement.scope.dept') }}</el-radio>
            </el-radio-group>
            <el-select
              v-if="form.scope === 'DEPT'"
              v-model="form.targetDeptIds"
              multiple
              :placeholder="t('announcement.field.selectDepts')"
              style="width: 100%; margin-top: 8px"
            >
              <el-option
                v-for="opt in deptOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else>
            <el-tag>{{ deptStore.getDeptName(userDeptId!) }}</el-tag>
          </template>
        </el-form-item>

        <el-form-item :label="t('announcement.field.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">{{ t('announcement.status.draft') }}</el-radio>
            <el-radio value="PUBLISHED">{{ t('announcement.status.published') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('announcement.field.publishAt')">
          <el-radio-group v-model="form.publishMode">
            <el-radio value="now">{{ t('announcement.publishMode.now') }}</el-radio>
            <el-radio value="schedule">{{ t('announcement.publishMode.schedule') }}</el-radio>
          </el-radio-group>
          <el-date-picker
            v-if="form.publishMode === 'schedule'"
            v-model="form.publishAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%; margin-top: 8px"
          />
        </el-form-item>

        <el-form-item :label="t('announcement.field.pinned')">
          <el-checkbox v-model="form.pinned">{{ t('announcement.field.pinnedLabel') }}</el-checkbox>
        </el-form-item>

        <el-form-item :label="t('announcement.field.expireAt')">
          <div style="display: flex; align-items: center; gap: 12px; width: 100%">
            <el-date-picker
              v-if="!form.neverExpire"
              v-model="form.expireAt"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="flex: 1"
              @change="onExpireAtManualChange"
            />
            <el-checkbox v-model="form.neverExpire">{{ t('announcement.neverExpire') }}</el-checkbox>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-muted);
}
</style>
