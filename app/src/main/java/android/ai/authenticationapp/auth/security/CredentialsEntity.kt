package android.ai.authenticationapp.auth.security

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import java.time.Instant

/**
 * Data layer: Persistence entity model for credentials, used for JSON serialization.
 */
data class CredentialsEntity(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochMilli: Long,
)

/**
 * Maps domain [Credentials] to persistence [CredentialsEntity].
 */
@SuppressLint("NewApi")
fun Credentials.toEntity(): CredentialsEntity {
    return CredentialsEntity(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAtEpochMilli = accessTokenExpiresAt.toEpochMilli(),
    )
}

/**
 * Maps persistence [CredentialsEntity] to domain [Credentials].
 */

@RequiresApi(Build.VERSION_CODES.O)
fun CredentialsEntity.toDomain(): Credentials {
    return Credentials(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = Instant.ofEpochMilli(accessTokenExpiresAtEpochMilli),
    )
}

/**
 * Utility helper for serializing and deserializing [CredentialsEntity] to/from JSON safely using Gson.
 */
object CredentialsSerializer {
    private val gson = Gson()

    fun serialize(entity: CredentialsEntity): String {
        return gson.toJson(entity)
    }

    fun deserialize(json: String?): CredentialsEntity? {
        if (json.isNullOrBlank()) return null
        return try {
            gson.fromJson(json, CredentialsEntity::class.java)
        } catch (e: Exception) {
            // Handle invalid/malformed stored JSON safely by returning null rather than crashing
            null
        }
    }
}
