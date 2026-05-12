<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="编辑CLI Agent" width="600px" :close-on-click-modal="false">
    <el-form :model="editForm" label-width="100px">
      <el-form-item label="Agent名称" required>
        <el-input v-model="editForm.name" placeholder="请输入Agent名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="editForm.description" type="textarea" rows="2" placeholder="请输入描述" />
      </el-form-item>

      <el-divider content-position="left">启动配置</el-divider>

      <el-form-item label="可执行路径">
        <el-input v-model="editForm.executablePath" placeholder="留空使用模板默认路径" />
        <div class="form-hint">当前模板: {{ agent?.templateName }}</div>
      </el-form-item>
      <el-form-item label="配置文件">
        <el-input v-model="editForm.configPath" placeholder="可选，如 C:\Users\xxx\.claude\settings.json" />
        <div class="form-hint">指定配置文件路径，启动时将添加 --settings 参数</div>
      </el-form-item>
      <el-form-item label="启动参数">
        <el-select v-model="editForm.args" multiple filterable allow-create style="width: 100%" placeholder="添加启动参数">
        </el-select>
      </el-form-item>
      <el-form-item label="工作目录">
        <el-input v-model="editForm.workingDir" placeholder="CLI进程的工作目录" />
      </el-form-item>

      <el-divider content-position="left">环境变量</el-divider>

      <el-form-item label="">
        <div class="env-vars-editor">
          <div v-for="(item, index) in editEnvVarList" :key="index" class="env-var-item">
            <el-input v-model="item.key" placeholder="变量名" style="width: 150px;" />
            <el-input v-model="item.value" placeholder="变量值" style="flex: 1;" show-password />
            <el-button type="danger" size="small" @click="editEnvVarList.splice(index, 1)">删除</el-button>
          </div>
          <el-button type="primary" size="small" @click="editEnvVarList.push({key: '', value: ''})">+ 添加环境变量</el-button>
        </div>
      </el-form-item>

      <el-divider content-position="left">能力配置</el-divider>

      <el-form-item label="能力类型">
        <el-select v-model="editForm.capabilityType" placeholder="选择能力类型" style="width: 200px;">
          <el-option v-for="ct in capabilityTypes" :key="ct.code" :label="ct.description" :value="ct.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="领域标签">
        <el-select v-model="editForm.domainTags" multiple filterable allow-create style="width: 100%" placeholder="输入领域标签">
        </el-select>
      </el-form-item>
      <el-form-item label="熟练度">
        <el-rate v-model="editForm.proficiencyLevel" :max="5" show-score />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateAgent } from '@/api/cliAgent'

interface CliAgent {
  id: string
  name: string
  description: string
  templateId: string
  templateName: string
  cliType: string
  status: string
  executablePath: string
  configPath: string
  args: string[]
  envVars: Record<string, string>
  workingDir: string
  processId: string
  capabilities: any[]
  lastStartedAt: string
}

interface CapabilityType {
  code: string
  name: string
  description: string
}

const props = defineProps<{
  visible: boolean
  agent: CliAgent | null
  templates: any[]
  capabilityTypes: CapabilityType[]
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  updated: []
}>()

const editing = ref(false)
const editEnvVarList = ref<{key: string, value: string}[]>([])

const editForm = reactive({
  name: '',
  description: '',
  executablePath: '',
  configPath: '',
  args: [] as string[],
  workingDir: '',
  capabilityType: '',
  domainTags: [] as string[],
  proficiencyLevel: 3
})

watch(() => props.visible, (val) => {
  if (val && props.agent) initEditForm()
})

function initEditForm() {
  if (!props.agent) return

  editForm.name = props.agent.name
  editForm.description = props.agent.description || ''
  editForm.executablePath = props.agent.executablePath || ''
  editForm.configPath = props.agent.configPath || ''
  editForm.args = props.agent.args || []
  editForm.workingDir = props.agent.workingDir || ''

  editEnvVarList.value = []
  if (props.agent.envVars) {
    Object.entries(props.agent.envVars).forEach(([key, value]) => {
      editEnvVarList.value.push({ key, value: value || '' })
    })
  }

  if (props.agent.capabilities && props.agent.capabilities.length > 0) {
    const cap = props.agent.capabilities[0]
    editForm.capabilityType = cap.type || ''
    editForm.domainTags = cap.domainTags || []
    editForm.proficiencyLevel = cap.proficiencyLevel || 3
  } else {
    editForm.capabilityType = ''
    editForm.domainTags = []
    editForm.proficiencyLevel = 3
  }
}

async function handleEdit() {
  if (!editForm.name) {
    ElMessage.warning('请输入Agent名称')
    return
  }

  if (!props.agent) return

  const envVars: Record<string, string> = {}
  editEnvVarList.value.forEach(item => {
    if (item.key && item.value) {
      envVars[item.key] = item.value
    }
  })

  editing.value = true
  try {
    const payload: any = {
      name: editForm.name,
      description: editForm.description
    }

    if (editForm.executablePath) payload.executablePath = editForm.executablePath
    if (editForm.configPath) payload.configPath = editForm.configPath
    if (editForm.args.length > 0) payload.args = editForm.args
    if (editForm.workingDir) payload.workingDir = editForm.workingDir
    if (Object.keys(envVars).length > 0) payload.envVars = envVars

    if (editForm.capabilityType) {
      payload.capabilities = [{
        type: editForm.capabilityType,
        domainTags: editForm.domainTags,
        proficiencyLevel: editForm.proficiencyLevel
      }]
    }

    await updateAgent(props.agent.id, payload)
    ElMessage.success('保存成功')
    emit('update:visible', false)
    emit('updated')
  } catch (error: any) {
    console.error('保存Agent失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    editing.value = false
  }
}
</script>
