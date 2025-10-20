/** @type {import('next').NextConfig} */
const nextConfig = {
  // 静态导出配置，用于Docker部署
  output: 'export',
  trailingSlash: true,
  images: {
    unoptimized: true
  },
  // 开发时的API重写（仅在dev模式下生效）
  async rewrites() {
    // 生产环境下由nginx处理API代理，这里只处理开发环境
    if (process.env.NODE_ENV === 'development') {
      return [
        {
          source: '/api/:path*',
          destination: 'http://localhost:9090/:path*'
        }
      ]
    }
    return []
  },
  experimental: {
    esmExternals: false
  }
}

module.exports = nextConfig
