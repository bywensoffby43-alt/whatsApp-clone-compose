package io.getstream.whatsappclone.account

/**
 * Implement this interface when Supabase Auth and the HTTPS backend are ready.
 * Never put Supabase service_role or MongoDB credentials in the Android app.
 */
interface AccountBackend {
    suspend fun requestPhoneNumberChange(newPhoneE164: String): Result<Unit>
    suspend fun confirmPhoneNumberChange(newPhoneE164: String, otp: String): Result<Unit>
    suspend fun deleteMyAccount(): Result<Unit>
    suspend fun signOutEverywhere(): Result<Unit>
}

/** Safe default: destructive/identity actions stay unavailable until a real backend is injected. */
class BackendNotConfigured : AccountBackend {
    private fun unavailable() = Result.failure<Unit>(IllegalStateException("Account backend is not configured yet."))
    override suspend fun requestPhoneNumberChange(newPhoneE164: String) = unavailable()
    override suspend fun confirmPhoneNumberChange(newPhoneE164: String, otp: String) = unavailable()
    override suspend fun deleteMyAccount() = unavailable()
    override suspend fun signOutEverywhere() = unavailable()
}
