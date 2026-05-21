package com.shared.exception

abstract class BusinessException(
    val httpStatus: Int,
    val errorCode: String,
    message: String
) : RuntimeException(message)