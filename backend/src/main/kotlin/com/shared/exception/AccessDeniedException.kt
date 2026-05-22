package com.shared.exception

class AccessDeniedException(message: String) : BusinessException(
    httpStatus = 403,
    errorCode = "FORBIDDEN",
    message = message
)