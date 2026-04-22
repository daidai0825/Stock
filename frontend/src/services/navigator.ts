/**
 * 全域 router navigator 注入機制
 *
 * 背景：
 *   axios 攔截器位於非 React 樹中，無法直接呼叫 useNavigate()。
 *   過去版本以 window.location.assign('/login') 強制跳轉，會觸發 hard reload，
 *   清掉 React Router 透過 location.state 攜帶的 `from` 路徑，違反
 *   SRS F-MEMBER-XX「登入後回到原頁」流程。
 *
 * 解決方案：
 *   App.tsx 內 mount 一個 NavigatorBridge，把 useNavigate() 的結果
 *   透過 setAppNavigate 注入到本 module；攔截器以 appNavigate() 跳轉，
 *   保留 SPA 體驗並可攜帶 state（含原頁路徑）。
 *
 * 對應規範：
 *   - .claude/rules/react-typescript.md（SPA UX、無 hard reload）
 *   - .claude/rules/api-design.md（traceId 全鏈追蹤）
 *
 * Fallback：
 *   若 navigator 尚未注入（例如測試環境未 mount NavigatorBridge），
 *   會以 console.warn 提示並 no-op，避免測試噪音。
 */

import type { NavigateFunction, NavigateOptions, To } from 'react-router-dom';

let navigatorRef: NavigateFunction | null = null;

/**
 * 由 App.tsx / NavigatorBridge 在 mount 時注入。
 * 卸載時應傳入 null 清空，避免 stale closure。
 */
export const setAppNavigate = (navigate: NavigateFunction | null): void => {
  navigatorRef = navigate;
};

/**
 * 取得目前注入的 navigator（可為 null）。供測試斷言使用。
 */
export const getAppNavigate = (): NavigateFunction | null => {
  return navigatorRef;
};

/**
 * 攔截器專用 navigate 包裝。
 * 若 navigator 尚未注入則 no-op（不 throw，避免影響使用者操作）。
 */
export const appNavigate = (to: To, options?: NavigateOptions): void => {
  if (navigatorRef === null) {
    // SSR / 測試環境未注入時 fallback 提示，但不阻斷流程
    if (typeof console !== 'undefined') {
      console.warn('[navigator] appNavigate called before NavigatorBridge mounted; ignored.', { to });
    }
    return;
  }
  navigatorRef(to, options);
};
