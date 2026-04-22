/**
 * 自選股模組型別（M-WATCH）
 * 對應 SRS §1.2 M-WATCH，Wave A 僅提供型別與 mock service
 */

export interface WatchlistItem {
  watchId: string;
  stockCode: string;
  stockName: string;
  market: 'TSE' | 'OTC';
  groupTag: string;
  sortOrder: number;
  /**
   * 個股健康度指標（0-100）；Wave A 為 mock
   * 命名強制依 SRS §0：禁用「綜合評分」字眼
   */
  healthScore: number | null;
  changePercent: number | null;
  lastPrice: number | null;
  /** 資料延遲分鐘數（公開資料延遲 20 分鐘） */
  dataDelayMinutes: number;
  createdAt: string;
}

export interface AddWatchlistRequest {
  stockCode: string;
  groupTag?: string;
}

export interface RemoveWatchlistRequest {
  watchId: string;
}
