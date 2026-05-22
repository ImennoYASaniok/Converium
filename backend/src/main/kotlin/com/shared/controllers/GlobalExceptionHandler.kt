package com.shared.controllers

import com.shared.exception.BusinessException
import com.shared.exception.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Business error [{}] at {}: {}", ex.errorCode, request.requestURI, ex.message)

        return ResponseEntity
            .status(ex.httpStatus)
            .body(
                ErrorResponse(
                    status = ex.httpStatus,
                    errorCode = ex.errorCode,
                    message = ex.message ?: "Ошибка бизнес-логики",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage}"
        }

        logger.warn("Validation error at {}: {}", request.requestURI, details)

        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                errorCode = "VALIDATION_ERROR",
                message = "Ошибка валидации: $details",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Malformed JSON at {}: {}", request.requestURI, ex.message)

        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                errorCode = "MALFORMED_REQUEST",
                message = "Невозможно прочитать тело запроса. Проверьте формат JSON.",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandler(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("No handler for {} {}", ex.httpMethod, ex.requestURL)

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                errorCode = "ENDPOINT_NOT_FOUND",
                message = "Эндпоинт не существует",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Method {} not allowed at {}", ex.method, request.requestURI)

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            ErrorResponse(
                status = HttpStatus.METHOD_NOT_ALLOWED.value(),
                errorCode = "METHOD_NOT_ALLOWED",
                message = "Метод ${ex.method} не поддерживается",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument at {}: {}", request.requestURI, ex.message)

        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = 400,
                errorCode = "VALIDATION_ERROR",
                message = ex.message ?: "Некорректный аргумент",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val rootCause = NestedExceptionUtils.getMostSpecificCause(ex)
        logger.error("Data integrity violation at {}: {}", request.requestURI, rootCause.message, ex)

        val userMessage = "Данные не прошли ограничение базы данных"

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                errorCode = "DATA_CONFLICT",
                message = userMessage,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error at {}", request.requestURI, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                errorCode = "INTERNAL_ERROR",
                message = "Внутренняя ошибка сервера",
                path = request.requestURI
            )
        )
    }
}
