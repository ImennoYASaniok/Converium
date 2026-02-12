package com.user

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "user")
data class UserProperties (
    var maxFriends: Int = 500,
    var maxBanUsers: Int = 500,
    var enableProfilePicture: Boolean = false,
    var minLenPassword: Int = 5,
    var minLenUser: Int = 5,
    var enablePasswordDigitsValidation: Boolean = true
)