package android.ai.authenticationapp.auth.data.remote

import android.ai.authenticationapp.auth.data.remote.dto.CurrentUserResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Data layer: Retrofit API interface for authentication endpoints (DummyJSON).
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): LoginResponseDto

    @GET("auth/me")
    suspend fun getCurrentUser(): CurrentUserResponseDto

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto,
    ): RefreshTokenResponseDto
}
