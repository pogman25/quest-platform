package ru.pogman.questplatform.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordDigest(val salt: ByteArray, val hash: ByteArray, val iterations: Int)

class PasswordHasher(private val iterations: Int = 600_000) {
    fun create(password: String): PasswordDigest {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return PasswordDigest(salt, derive(password, salt, iterations), iterations)
    }

    fun verify(password: String, digest: PasswordDigest): Boolean =
        MessageDigest.isEqual(digest.hash, derive(password, digest.salt, digest.iterations))

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}
