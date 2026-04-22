/**
 * 後端 API 統一回應結構（Envelope Pattern）
 * 對應 .claude/rules/api-design.md：
 * - 統一 HTTP 200
 * - code === 0 表示成功
 * - code !== 0 表示業務錯誤（由攔截器拋出 BusinessError）
 * - 時間採 ISO 8601 含時區（GMT+8）
 */

export interface ApiErrorDetail {
  field: string;
  message: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  errors?: ApiErrorDetail[];
  timestamp: string;
  traceId: string;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

/**
 * 業務錯誤（HTTP 200 + code !== 0 統一拋出）
 * 由 services/http.ts response interceptor 產生
 */
export class BusinessError extends Error {
  public readonly code: number;
  public readonly traceId?: string;
  public readonly details?: ApiErrorDetail[];

  public constructor(code: number, message: string, traceId?: string, details?: ApiErrorDetail[]) {
    super(message);
    this.name = 'BusinessError';
    this.code = code;
    if (traceId !== undefined) {
      this.traceId = traceId;
    }
    if (details !== undefined) {
      this.details = details;
    }
  }
}
