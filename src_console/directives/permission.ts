import type { Directive } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export const permission: Directive = {
    mounted(el, binding) {
        const { value } = binding
        const permissionStore = usePermissionStore()

        if (value && value instanceof Array && value.length > 0) {
            const hasPermission = permissionStore.hasPermission(value)

            if (!hasPermission) {
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error(`need roles! Like v-permission="['admin','editor']"`)
        }
    }
}
