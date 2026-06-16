import { reactive } from 'vue'

type DialogMode = 'alert' | 'confirm' | 'prompt'

type DialogOptions = {
  title?: string
  confirmText?: string
  cancelText?: string
  placeholder?: string
  defaultValue?: string
  required?: boolean
  multiline?: boolean
}

type DialogState = {
  open: boolean
  mode: DialogMode
  title: string
  message: string
  confirmText: string
  cancelText: string
  inputPlaceholder: string
  inputRequired: boolean
  inputMultiline: boolean
  inputDefault: string
}

const state = reactive<DialogState>({
  open: false,
  mode: 'alert',
  title: '提示',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  inputPlaceholder: '',
  inputRequired: false,
  inputMultiline: false,
  inputDefault: ''
})

let resolver: ((value: unknown) => void) | null = null

const openDialog = (mode: DialogMode, message: string, options: DialogOptions = {}) => {
  if (state.open && resolver) {
    resolver(mode === 'confirm' ? false : null)
    state.open = false
  }

  return new Promise<unknown>(resolve => {
    resolver = resolve
    state.open = true
    state.mode = mode
    state.title = options.title ?? (mode === 'prompt' ? '请输入' : '提示')
    state.message = message
    state.confirmText = options.confirmText ?? '确定'
    state.cancelText = options.cancelText ?? '取消'
    state.inputPlaceholder = options.placeholder ?? ''
    state.inputRequired = options.required ?? false
    state.inputMultiline = options.multiline ?? false
    state.inputDefault = options.defaultValue ?? ''
  })
}

const closeDialog = (value: unknown) => {
  state.open = false
  const current = resolver
  resolver = null
  current?.(value)
}

export const useDialogState = () => ({
  state,
  closeDialog
})

export const useDialog = () => ({
  alert: (message: string, options?: DialogOptions) => openDialog('alert', message, options).then(() => undefined),
  confirm: (message: string, options?: DialogOptions) => openDialog('confirm', message, options).then(value => Boolean(value)),
  prompt: (message: string, options?: DialogOptions) => openDialog('prompt', message, options).then(value => (value == null ? null : String(value)))
})
