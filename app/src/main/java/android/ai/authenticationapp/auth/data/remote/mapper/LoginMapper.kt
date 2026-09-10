package android.ai.authenticationapp.auth.data.remote.mapper

import android.ai.authenticationapp.auth.data.remote.dto.LoginResponseDto
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.User
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Data layer: Mapper extension functions for login responses.
 */
fun LoginResponseDto.toUser(): User {
    val trimmedFirstName = firstName.trim()
    val trimmedLastName = lastName.trim()
    val fullName = when {
        trimmedFirstName.isNotEmpty() && trimmedLastName.isNotEmpty() -> "$trimmedFirstName $trimmedLastName"
        trimmedFirstName.isNotEmpty() -> trimmedFirstName
        trimmedLastName.isNotEmpty() -> trimmedLastName
        else -> null
    }

    return User(
        id = id.toString(),
        email = email,
        name = fullName,
    )
}

@OptIn(ExperimentalTime::class)
fun LoginResponseDto.toCredentials(
    accessTokenExpiresAt: Instant
): Credentials {
    return Credentials(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
    )
}
