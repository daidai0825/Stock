/**
 * 前端路由常數
 */

export const RoutePath = {
  LOGIN: '/login',
  REGISTER: '/register',
  HOME: '/home',
  WATCHLIST: '/watchlist',
  PROFILE: '/profile',
  STOCK_DETAIL: '/stocks/:stockId',
  ROOT: '/',
} as const;

export type RoutePathValue = (typeof RoutePath)[keyof typeof RoutePath];

/** 產生個股詳情頁路徑 */
export const buildStockDetailPath = (stockId: string): string => `/stocks/${stockId}`;
