<template>
  <div class="role-management">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="roleList" v-loading="loading" stripe>
        <el-table-column prop="name" label="角色名称" min-width="120" />
        <el-table-column prop="code" label="角色编码" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="系统角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isSystem === 1 ? 'warning' : 'info'">
              {{ row.isSystem === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
              :disabled="row.isSystem === 1"
            >
              编辑
            </el-button>
            <el-button type="primary" link @click="handlePermissions(row)">
              权限配置
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row.isSystem === 1"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code" v-if="!isEdit">
          <el-input v-model="formData.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入描述"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="权限配置" width="600px">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="selectedPermissions"
        :props="{ label: 'name', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermissions">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface Role {
  id: string
  name: string
  code: string
  description: string
  isSystem: number
  permissions: Array<{ id: string; name: string; code: string }>
  createdAt: string
}

interface Permission {
  id: string
  name: string
  code: string
  children?: Permission[]
}

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const currentRole = ref<Role | null>(null)
const formRef = ref<FormInstance>()
const treeRef = ref()

const roleList = ref<Role[]>([])
const permissionTree = ref<Permission[]>([])
const selectedPermissions = ref<string[]>([])

const formData = reactive({
  name: '',
  code: '',
  description: ''
})

const dialogTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' }
  ]
}

const fetchRoles = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/v1/roles')
    if (response.data.code === 0) {
      roleList.value = response.data.data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const fetchPermissions = async () => {
  try {
    const response = await request.get('/api/v1/roles/permissions')
    if (response.data.code === 0) {
      permissionTree.value = response.data.data
    }
  } catch (error) {
    console.error('获取权限列表失败', error)
  }
}

const handleCreate = () => {
  isEdit.value = false
  currentRole.value = null
  Object.assign(formData, {
    name: '',
    code: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (role: Role) => {
  isEdit.value = true
  currentRole.value = role
  Object.assign(formData, {
    name: role.name,
    code: role.code,
    description: role.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && currentRole.value) {
      await request.put(`/api/v1/roles/${currentRole.value.id}`, formData)
      ElMessage.success('角色更新成功')
    } else {
      await request.post('/api/v1/roles', formData)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    fetchRoles()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (role: Role) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色"${role.name}"吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await request.delete(`/api/v1/roles/${role.id}`)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handlePermissions = async (role: Role) => {
  currentRole.value = role

  // 获取角色当前权限
  try {
    const response = await request.get(`/api/v1/roles/${role.id}/permissions`)
    if (response.data.code === 0) {
      selectedPermissions.value = response.data.data
    }
  } catch (error) {
    selectedPermissions.value = role.permissions?.map(p => p.id) || []
  }

  permissionDialogVisible.value = true
}

const submitPermissions = async () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  try {
    await request.post(`/api/v1/roles/${currentRole.value?.id}/permissions`, checkedKeys)
    ElMessage.success('权限配置成功')
    permissionDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '权限配置失败')
  }
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})
</script>

<style lang="scss" scoped>
.role-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>