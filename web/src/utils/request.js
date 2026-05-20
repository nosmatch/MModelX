import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 根据后端响应格式处理（兼容字符串和数字类型的 code）
    // 后端返回: code="200", message="success", data=...
    const isSuccess = res.code === '200' || res.code === 200

    if (!isSuccess) {
      // 失败时显示红色错误消息，包含具体原因
      ElMessage({
        message: res.message || '请求失败',
        type: 'error',
        duration: 5000,
        showClose: true
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 成功时不自动显示消息，让业务层决定是否显示成功提示
    return res
  },
  (error) => {
    console.error('Response error:', error)

    let errorMessage = '请求失败'

    if (error.response) {
      const { status, data } = error.response

      // 尝试获取后端返回的具体错误信息
      if (data && data.message) {
        errorMessage = data.message
      } else if (data && typeof data === 'string') {
        errorMessage = data
      }

      // 根据状态码提供更具体的错误信息
      switch (status) {
        case 401:
          errorMessage = '未授权，请登录'
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          errorMessage = data?.message || '拒绝访问：没有权限执行此操作'
          break
        case 404:
          errorMessage = '请求地址不存在：' + (error.response.config?.url || '')
          break
        case 500:
          errorMessage = data?.message || '服务器内部错误，请稍后重试'
          break
        case 503:
          errorMessage = '服务暂时不可用，请稍后重试'
          break
        default:
          if (!errorMessage || errorMessage === '请求失败') {
            errorMessage = `请求失败 (${status}): ${data?.message || '未知错误'}`
          }
      }
    } else if (error.request) {
      errorMessage = '网络连接失败，请检查网络设置'
    } else {
      errorMessage = error.message || '请求配置错误'
    }

    // 显示红色错误消息，包含具体的失败原因
    ElMessage({
      message: errorMessage,
      type: 'error',
      duration: 5000,
      showClose: true
    })

    return Promise.reject(error)
  }
)

export default request