/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',  // 静态导出配置，用于Docker部署
  trailingSlash: true,
  images: {
    unoptimized: true
  },
  reactStrictMode: true,
  // 生产环境优化配置
  productionBrowserSourceMaps: false, // 生产环境不生成source map以减小包体积
  compress: true, // 启用Gzip压缩
  poweredByHeader: false, // 移除X-Powered-By头

  // 构建优化
  swcMinify: true, // 使用SWC进行代码压缩，比Terser更快

  // 实验性功能配置（稳定版本）
  experimental: {
    optimizePackageImports: ['lucide-react', '@radix-ui/react-dialog'], // 包导入优化
  }
}

module.exports = nextConfig
