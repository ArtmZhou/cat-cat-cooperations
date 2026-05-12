import request from '@/utils/request'

export interface KnowledgeBase {
  id: string
  name: string
  description: string
  embeddingProvider: string
  embeddingModel: string
  vectorStoreProvider: string
  chunkSize: number
  chunkOverlap: number
  maxTokens: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeBaseForm {
  name: string
  description?: string
  embeddingProvider?: string
  embeddingModel?: string
  vectorStoreProvider?: string
  chunkSize?: number
  chunkOverlap?: number
  maxTokens?: number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export function getKnowledgeBases(page = 1, pageSize = 20): Promise<PageResult<KnowledgeBase>> {
  return request.get('/knowledge-bases', { params: { page, pageSize } })
}

export function getKnowledgeBase(id: string): Promise<KnowledgeBase> {
  return request.get(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: KnowledgeBaseForm): Promise<KnowledgeBase> {
  return request.post('/knowledge-bases', data)
}

export function updateKnowledgeBase(id: string, data: Partial<KnowledgeBaseForm>): Promise<KnowledgeBase> {
  return request.put(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: string): Promise<void> {
  return request.delete(`/knowledge-bases/${id}`)
}
