export interface AgentBrief {
  id: string
  name: string
  status: string
}

export interface ChatGroup {
  id: string
  name: string
  description: string
  agentIds: string[]
  knowledgeBaseIds: string[]
  agents: AgentBrief[]
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  groupId: string
  senderType: 'user' | 'agent' | 'system'
  senderAgentId?: string
  senderName: string
  content: string
  mentionedAgentIds: string[]
  broadcast: boolean
  createdAt: string
}

export interface CliAgent {
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

export interface GroupForm {
  name: string
  description: string
  agentIds: string[]
  knowledgeBaseIds?: string[]
}

export interface GroupMessagePayload {
  content: string
  mentionedAgentIds?: string[] | null
}
