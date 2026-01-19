const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

export function resolveFileUrl(url?: string) {
    if (!url) return ''
    if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
        return url
    }
    if (!apiBaseUrl) return url
    const base = apiBaseUrl.endsWith('/') ? apiBaseUrl.slice(0, -1) : apiBaseUrl
    return url.startsWith('/') ? `${base}${url}` : `${base}/${url}`
}
