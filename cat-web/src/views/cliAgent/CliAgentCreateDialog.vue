<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="创建CLI Agent" width="600px" :close-on-click-modal="false">
    <el-form :model="createForm" label-width="100px">
      <el-form-item label="选择模板" required>
        <el-select v-model="createForm.templateId" style="width: 100%" @change="onTemplateChange" placeholder="请选择CLI模板">
          <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
            <span>{{ t.name }}</span>
            <span style="color: #999; margin-left: 10px;">{{ t.description }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="Agent名称" required>
        <el-input v-model="createForm.name" placeholder="请输入Agent名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="createForm.description" type="textarea" rows="2" placeholder="请输入描述" />
      </el-form-item>

      <el-divider content-position="left">启动配置</el-divider>

      <el-form-item label="可执行路径">
        <el-input v-model="createForm.executablePath" placeholder="留空使用模板默认路径" />
      </el-form-item>
      <el-form-item label="配置文件">
        <el-input v-model="createForm.configPath" placeholder="可选，如 C:\Users\xxx\.claude\settings.json" />
        <div class="form-hint">指定配置文件路径，启动时将添加 --settings 参数</div>
      </el-form-item>
      <el-form-item label="启动参数">
        <el-select v-model="createForm.args" multiple filterable allow-create style="width: 100%" placeholder="添加启动参数">
        </el-select>
      </el-form-item>
      <el-form-item label="工作目录">
        <el-input v-model="createForm.workingDir" placeholder="CLI进程的工作目录" />
      </el-form-item>

      <el-divider content-position="left">环境变量</el-divider>

      <el-form-item label="">
        <div class="env-vars-editor">
          <div v-for="(item, index) in envVarList" :key="index" class="env-var-item">
            <el-input v-model="item.key" placeholder="变量名" style="width: 150px;" />
            <el-input v-model="item.value" placeholder="变量值" style="flex: 1;" show-password />
            <el-button type="danger" size="small" @click="envVarList.splice(index, 1)">删除</el-button>
          </div>
          <el-button type="primary" size="small" @click="envVarList.push({key: '', value: ''})">+ 添加环境变量</el-button>
        </div>
      </el-form-item>

      <el-divider content-position="left">能力配置</el-divider>

      <el-form-item label="能力类型">
        <el-select v-model="createForm.capabilityType" placeholder="选择能力类型" style="width: 200px;">
          <el-option v-for="ct in capabilityTypes" :key="ct.code" :label="ct.description" :value="ct.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="领域标签">
        <el-select v-model="createForm.domainTags" multiple filterable allow-create style="width: 100%" placeholder="输入领域标签，如java, python">
        </el-select>
      </el-form-item>
      <el-form-item label="熟练度">
        <el-rate v-model="createForm.proficiencyLevel" :max="5" show-score />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createAgent } from '@/api/cliAgent'

interface Template {
  id: string
  name: string
  description: string
  cliType: string
  requiredEnvVars: string[]
}

interface CapabilityType {
  code: string
  name: string
  description: string
}

const props = defineProps<{
  visible: boolean
  templates: Template[]
  capabilityTypes: CapabilityType[]
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  created: []
}>()

const creating = ref(false)
const envVarList = ref<{key: string, value: string}[]>([])

const createForm = reactive({
  templateId: '',
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
  if (val) resetCreateForm()
})

function onTemplateChange(templateId: string) {
  const template = props.templates.find(t => t.id === templateId)
  if (template) {
    envVarList.value = template.requiredEnvVars?.map(v => ({key: v, value: ''})) || []
  }
}

function resetCreateForm() {
  createForm.templateId = ''
  createForm.name = ''
  createForm.description = ''
  createForm.executablePath = ''
  createForm.configPath = ''
  createForm.args = []
  createForm.workingDir = ''
  createForm.capabilityType = ''
  createForm.domainTags = []
  createForm.proficiencyLevel = 3
  envVarList.value = []
}

async function handleCreate() {
  if (!createForm.templateId) {
    ElMessage.warning('请选择模板')
    return
  }
  if (!createForm.name) {
    ElMessage.warning('请输入Agent名称')
    return
  }

  const envVars: Record<string, string> = {}
  envVarList.value.forEach(item => {
    if (item.key && item.value) {
      envVars[item.key] = item.value
    }
  })

  creating.value = true
  try {
    const payload: any = {
      name: createForm.name,
      description: createForm.description,
      templateId: createForm.templateId
    }

    if (createForm.executablePath) payload.executablePath = createForm.executablePath
    if (createForm.configPath) payload.configPath = createForm.configPath
    if (createForm.args.length > 0) payload.args = createForm.args
    if (createForm.workingDir) payload.workingDir = createForm.workingDir
    if (Object.keys(envVars).length > 0) payload.envVars = envVars

    if (createForm.capabilityType) {
      payload.capabilities = [{
        type: createForm.capabilityType,
        domainTags: createForm.domainTags,
        proficiencyLevel: createForm.proficiencyLevel
      }]
    }

    await createAgent(payload)
    ElMessage.success('创建成功')
    emit('update:visible', false)
    emit('created')
  } catch (error: any) {
    console.error('创建Agent失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>
