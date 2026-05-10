<template>
  <el-dialog
    :model-value="visible"
    :title="editing ? '编辑群组' : '创建群组'"
    width="500px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="群组名称" required>
        <el-input v-model="form.name" placeholder="输入群组名称" />
      </el-form-item>
      <el-form-item label="群组描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="输入群组描述" />
      </el-form-item>
      <el-form-item label="选择Agent" required>
        <div class="agent-selector">
          <el-checkbox-group v-model="form.agentIds">
            <div v-for="agent in allAgents" :key="agent.id" class="agent-checkbox-item">
              <el-checkbox :value="agent.id">
                <span class="agent-checkbox-label">
                  🤖 {{ agent.name }}
                  <el-tag size="small" :type="getAgentTagType(agent.status)">
                    {{ getAgentStatusText(agent.status) }}
                  </el-tag>
                </span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="allAgents.length === 0" description="暂无Agent，请先创建" :image-size="40" />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('save')">
        {{ editing ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { AgentBrief, GroupForm } from '@/types/models'

defineProps<{
  visible: boolean
  editing: boolean
  saving: boolean
  form: GroupForm
  allAgents: AgentBrief[]
}>()

defineEmits<{
  save: []
  'update:visible': [value: boolean]
}>()

function getAgentTagType(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'success',
    EXECUTING: 'primary',
    STOPPED: 'info',
    ERROR: 'danger'
  }
  return map[status] || 'info'
}

function getAgentStatusText(status: string): string {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    EXECUTING: '执行中',
    STOPPED: '已停止',
    ERROR: '错误'
  }
  return map[status] || status
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.agent-selector {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  padding: 8px;
  background: $bg-surface;
}

.agent-checkbox-item {
  padding: 6px 0;
}

.agent-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
