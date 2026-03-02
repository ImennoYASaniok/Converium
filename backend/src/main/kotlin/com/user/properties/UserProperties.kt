package com.user.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "user")
data class UserProperties (
    val maxFriends: Int = 500,
    val maxBanUsers: Int = 500,
    val enableProfilePicture: Boolean = false,
    val minLenPassword: Int = 5,
    val minLenUser: Int = 5,
    val enablePasswordDigitsValidation: Boolean = true
)