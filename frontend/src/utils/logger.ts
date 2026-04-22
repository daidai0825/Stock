/**
 * 前端 Logger（code-style.md §日誌：不用 console.log，用 logger）
 *
 * 在 prod build 中，info / debug 靜默；warn / error 保留。
 * 未來可替換為 Sentry、Datadog RUM 等，呼叫端無需改動。
 *
 * 此檔案是 console 的唯一合法使用點，其他檔案禁止直接呼叫 console.*。
 */

/* eslint-disable no-console */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const isProd = import.meta.env.PROD === true;

const shouldLog = (level: LogLevel): boolean => {
  if (isProd && (level === 'debug' || level === 'info')) return false;
  return true;
};

const format = (level: LogLevel, message: string): string =>
  `[${level.toUpperCase()}] ${message}`;

export const logger = {
  debug(message: string, ...args: unknown[]): void {
    if (shouldLog('debug')) console.debug(format('debug', message), ...args);
  },
  info(message: string, ...args: unknown[]): void {
    if (shouldLog('info')) console.info(format('info', message), ...args);
  },
  warn(message: string, ...args: unknown[]): void {
    if (shouldLog('warn')) console.warn(format('warn', message), ...args);
  },
  error(message: string, ...args: unknown[]): void {
    if (shouldLog('error')) console.error(format('error', message), ...args);
  },
};
