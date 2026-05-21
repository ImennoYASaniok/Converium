package com.shared.exception

class AlreadyExistsException(message: String) : BusinessException(
    httpStatus = 409,
    errorCode = "ALREADY_EXISTS",
    message = message
)