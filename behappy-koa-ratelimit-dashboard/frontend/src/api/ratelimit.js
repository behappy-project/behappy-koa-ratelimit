import request from '@/utils/request'

export default {
  loadRateLimitSwitch(params) {
    return request({
      url: '/api/rate-limit/switch',
      method: 'get',
      params,
    })
  },
  delRateLimitSwitch(params) {
    return request({
      url: '/api/rate-limit/switch',
      method: 'delete',
      data: params,
    })
  },
  saveOrUpdateRateLimitSwitch(params) {
    return request({
      url: '/api/rate-limit/switch',
      method: 'post',
      data: params,
    })
  },

  loadRateLimitConfig(params) {
    return request({
      url: '/api/rate-limit/config',
      method: 'get',
      params,
    })
  },
  delRateLimitConfig(params) {
    return request({
      url: '/api/rate-limit/config',
      method: 'delete',
      data: params,
    })
  },
  saveOrUpdateRateLimitConfig(params) {
    return request({
      url: '/api/rate-limit/config',
      method: 'post',
      data: params,
    })
  }
}
