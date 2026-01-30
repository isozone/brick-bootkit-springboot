import axios from 'axios'
import { useMessage, useDialog } from 'naive-ui'
import { MESSAGE, CODE } from '@/constants/constants'

// 创建 axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 添加 token 等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response
    const message = useMessage()
    const dialog = useDialog()

    if (data.code === CODE.SUCCESS) {
      return data.data
    }

    // 错误处理
    if (data.code === CODE.UNAUTHORIZED) {
      dialog.error({
        title: '登录过期',
        content: '请重新登录',
        positiveText: '确定',
        onPositiveClick: () => {
          localStorage.removeItem('token')
          window.location.href = '/login'
        }
      })
      return Promise.reject(new Error('登录过期'))
    }

    message.error(data.message || MESSAGE.ERROR.UNKNOWN)
    return Promise.reject(new Error(data.message || MESSAGE.ERROR.UNKNOWN))
  },
  (error) => {
    const message = useMessage()
    message.error(MESSAGE.ERROR.NETWORK)
    return Promise.reject(error)
  }
)

export default request
