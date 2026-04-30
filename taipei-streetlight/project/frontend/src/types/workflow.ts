export type WorkflowStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export type WorkflowType =
  | 'FAULT_REVIEW'
  | 'REPAIR_DISPATCH'
  | 'REPAIR_CLOSE'
  | 'REPLACEMENT_REVIEW'
  | 'ASSET_CHANGE'

export type WorkflowAction =
  | 'SUBMIT'
  | 'APPROVE'
  | 'REJECT'
  | 'RETURN'
  | 'DISPATCH'
  | 'MERGE'
  | 'COMPLETE'
  | 'CANCEL'

export type TicketType = 'FAULT_TICKET' | 'REPAIR_TICKET' | 'REPLACEMENT_ORDER' | 'ASSET_CHANGE'

export interface WorkflowInstanceResponse {
  id: number
  workflowType: WorkflowType
  ticketType: TicketType
  ticketId: number
  currentStep: string
  status: WorkflowStatus
  assignedTo: string
  assignedToName: string | null
  creatorId: string
  startedAt: string
  completedAt: string | null
  updatedAt: string
  delegatedFrom: string | null
}

export interface WorkflowStepLogResponse {
  id: number
  stepCode: string
  action: string
  actorId: string
  actorName: string | null
  originalAssigneeId: string | null
  isDelegated: boolean
  comment: string | null
  attachments: Record<string, unknown>[] | null
  actedAt: string
}

export interface WorkflowTransitionRequest {
  targetStep: string
  action: string
  comment?: string
  attachments?: Record<string, unknown>[]
}

export interface DelegateSettingResponse {
  id: number
  delegatorId: string
  delegatorName: string
  delegateId: string
  delegateName: string
  startDate: string
  endDate: string
  reason: string | null
  isActive: boolean
  createdAt: string
}

export interface DelegateSettingRequest {
  delegateId: string
  startDate: string
  endDate: string
  reason?: string
}

export interface DelegateCandidateDto {
  userId: string
  displayName: string
  deptName: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
