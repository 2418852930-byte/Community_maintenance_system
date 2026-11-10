import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * Vite 配置
 * - 开发端口 5173
 * - /api 代理到后端 8080，避免开发阶段跨域问题
 * - /uploads 同样代理到后端 8080：这是模块二 U-15 上传接口落盘的图片访问路径，
 *   前端 <img src="/uploads/order/20260918/xxx.jpg"> 若不代理会直接 404
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 使用 @ 指向 src 目录，便于深层级组件引用
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 后端接口本身就以 /api 开头，因此不需要 rewrite
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 后端 WebMvcConfig 已把 /uploads/** 映射到磁盘上传目录
      }
    }
  }
})
