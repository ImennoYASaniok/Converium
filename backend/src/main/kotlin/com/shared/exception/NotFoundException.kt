package com.shared.exception

class NotFoundException(
    val id: Long,
    val resourceType: String,
    message: String = "$resourceType не найден (id=$id)"
) : BusinessException(
    httpStatus = 404,
    errorCode = "NOT_FOUND",
    message = message
)