export type ChatRole = 'USER' | 'ASSISTANT';

export interface ChatSession {
  id: string;
  dentistId: string;
  clinicId: string | null;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: ChatRole;
  content: string;
  citations: string[] | null;
  inputTokens: number;
  outputTokens: number;
  createdAt: string;
}

export interface CreateSessionRequest {
  title?: string;
  clinicId?: string;
}

export interface SendMessageRequest {
  content: string;
}

export interface IaUserStats {
  username: string;
  clinicName: string | null;
  messages: number;
  inputTokens: number;
  outputTokens: number;
  billedCostUsd: number;
}

export interface IaStatsResponse {
  totalSessions: number;
  totalMessages: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalRawCostUsd: number;
  totalBilledCostUsd: number;
  rows: IaUserStats[];
}
