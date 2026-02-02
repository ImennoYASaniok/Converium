package com

import com.user.User
import com.user.UserProperties
import com.user.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ApplicationRunner(
    private val userService: UserService,
    private val userProperties: UserProperties
) : CommandLineRunner {

    private val logger: Logger = LoggerFactory.getLogger(ApplicationRunner::class.java)

    override fun run(vararg args: String?) {
    }
}