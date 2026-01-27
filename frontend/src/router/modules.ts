/**
 * 后台管理组件自动映射
 * 使用 Vite glob import 自动扫描 views/console 目录下的所有组件
 * 无需手动维护，新增组件自动被发现
 */

// 自动扫描所有 console 组件
const modules = import.meta.glob('../views/console/**/*.vue')

// 构建组件映射表：将相对路径转换为数据库格式的路径
export const consoleComponentMap: Record<string, () => Promise<any>> = Object.fromEntries(
  Object.entries(modules).map(([path, component]) => [
    // 将 '../views/console/user/index.vue' 转换为 'views/console/user/index.vue'
    path.replace('../', ''),
    component
  ])
)

/**
 * 根据组件路径获取组件
 * @param componentPath 数据库中的 component 字段值
 * @returns 组件懒加载函数，未找到则返回 404 页面
 */
export function resolveComponent(componentPath: string | undefined): () => Promise<any> {
  if (!componentPath) {
    return () => import('@/views/console/dashboard/index.vue')
  }
  
  // 标准化路径
  const normalizedPath = componentPath.startsWith('views/') 
    ? componentPath 
    : `views/${componentPath}`
  
  return consoleComponentMap[normalizedPath] || (() => import('@/views/error/404.vue'))
}

/**
 * 布局组件（用于父级菜单）
 */
export const ParentView = () => import('@/views/console/layout/ParentView.vue')
