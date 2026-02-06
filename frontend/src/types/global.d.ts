type ApiErrorLike = {
  message?: string
  response?: {
    data?: {
      message?: string
      msg?: string
    }
  }
}
