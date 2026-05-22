package com.shared.exception

class ValidationException(message: String) : BusinessException(
    httpStatus = 400,
    errorCode = "VALIDATION_ERROR",
    message = message
)