package top.chengdongqing.wechat.core.common.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.security.MessageDigest
import java.security.SecureRandom

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val isEnabled: Boolean
        get() = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.matches(PIN_REGEX)) { "PIN must contain exactly four digits" }
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
        preferences.edit {
            putString(KEY_SALT, salt.encode())
                .putString(KEY_HASH, hash(pin, salt).encode())
        }
    }

    fun verify(pin: String): Boolean {
        if (isTemporarilyLocked) return false
        if (!pin.matches(PIN_REGEX)) return false
        val salt = preferences.getString(KEY_SALT, null)?.decode() ?: return false
        val expected = preferences.getString(KEY_HASH, null)?.decode() ?: return false
        val matches = MessageDigest.isEqual(expected, hash(pin, salt))
        if (matches) {
            preferences.edit { remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL) }
        } else {
            val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            preferences.edit {
                if (attempts >= MAX_ATTEMPTS) {
                    putInt(KEY_FAILED_ATTEMPTS, 0)
                    putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + LOCKOUT_MILLIS)
                } else {
                    putInt(KEY_FAILED_ATTEMPTS, attempts)
                }
            }
        }
        return matches
    }

    val isTemporarilyLocked: Boolean
        get() = preferences.getLong(KEY_LOCKED_UNTIL, 0L) > System.currentTimeMillis()

    fun clear() {
        preferences.edit { clear() }
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        var value = salt + pin.toByteArray(Charsets.UTF_8)
        repeat(HASH_ROUNDS) {
            value = MessageDigest.getInstance("SHA-256").digest(value)
        }
        return value
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode(): ByteArray? = runCatching {
        Base64.decode(this, Base64.NO_WRAP)
    }.getOrNull()

    private companion object {
        const val PREFERENCES_NAME = "app_lock"
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
        const val SALT_SIZE = 16
        const val HASH_ROUNDS = 10_000
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L
        val PIN_REGEX = Regex("\\d{4}")
    }
}
