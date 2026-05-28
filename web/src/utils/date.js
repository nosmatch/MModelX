/**
 * 日期格式化工具
 *
 * 统一全平台日期/时间格式，避免各页面自行实现导致风格不一致。
 *
 * @author MModelX Team
 */

/**
 * 内部：把任意输入转换为 Date 对象，无效时返回 null
 *
 * @param {Date | string | number | null | undefined} input
 * @returns {Date | null}
 */
function toDate(input) {
  if (input === null || input === undefined || input === '') return null
  const date = input instanceof Date ? input : new Date(input)
  if (Number.isNaN(date.getTime())) return null
  return date
}

/**
 * 把 Date 对象格式化为「YYYY-MM-DD HH:mm」
 *
 * @param {Date | string | number | null | undefined} input - 日期值，可以是
 *  Date 对象、ISO 字符串、时间戳数字、null/undefined
 * @param {string} [fallback='-'] - 输入无效时返回的占位文本
 * @returns {string} 格式化后的字符串
 */
export function formatDate(input, fallback = '-') {
  const date = toDate(input)
  if (!date) return fallback

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 把 Date 对象格式化为「YYYY-MM-DD HH:mm:ss」（含秒）
 *
 * @param {Date | string | number | null | undefined} input - 日期值
 * @param {string} [fallback='-'] - 输入无效时返回的占位文本
 * @returns {string} 格式化后的字符串
 */
export function formatDateTime(input, fallback = '-') {
  const date = toDate(input)
  if (!date) return fallback

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 把 Date 对象格式化为「HH:mm:ss」
 *
 * @param {Date | string | number | null | undefined} input - 日期值
 * @param {string} [fallback=''] - 输入无效时返回的占位文本
 * @returns {string} 格式化后的字符串
 */
export function formatTime(input, fallback = '') {
  const date = toDate(input)
  if (!date) return fallback

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

/**
 * 把毫秒时间戳格式化为人类可读的耗时（如 "1.2s"、"3m 5s"、"500ms"）
 *
 * @param {number | null | undefined} ms - 毫秒数
 * @param {string} [fallback='-'] - 输入无效时返回的占位文本
 * @returns {string}
 */
export function formatElapsed(ms, fallback = '-') {
  if (ms === null || ms === undefined || Number.isNaN(Number(ms))) return fallback
  const value = Number(ms)
  if (value < 1000) return `${value}ms`
  if (value < 60000) return `${(value / 1000).toFixed(1)}s`
  const minutes = Math.floor(value / 60000)
  const seconds = Math.floor((value % 60000) / 1000)
  return `${minutes}m ${seconds}s`
}
