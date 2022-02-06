package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.format
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    private var _email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() }

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().uppercase() }
            .joinToString(" ")

    val email: String?
        get() = _email

    private var _phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }
    val phone: String?
        get() = _phone

    private var _login: String? = null
    internal var login: String
        get() = _login!!
        set(value) {
            _login = value.lowercase()
        }

    private var _salt: String? = null
    val salt: String by lazy {
        _salt ?: ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    }

    lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, _email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        sendAccessCodeToUser(rawPhone, code)
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        passwordInfo: String?,
        rawPhone: String?,
    ) : this(firstName, lastName, email, rawPhone, mapOf("src" to "csv")) {
        passwordInfo?.split(":")?.let {
            _salt = it.firstOrNull()
            passwordHash = it.lastOrNull() ?: throw IllegalArgumentException("Csv file does not contain password hash")
        }
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(_email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        rawPhone?.let { checkPhone(rawPhone) }

        _phone = rawPhone
        login = _email ?: _phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $_email
            phone: $_phone
            meta: $meta
        """.trimIndent()
    }

    private fun checkPhone(phone: String?) = phone?.let {
        val formattedPhone = phone.format()
        val phoneLength = formattedPhone.length == 12
        val isFirstPlus = formattedPhone.firstOrNull() == '+'
        val isOtherDigits = formattedPhone.takeLast(11).all { c -> c.isDigit() }

        if (phoneLength && isFirstPlus && isOtherDigits) {
            true
        } else {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and " +
                    "containing 11 digits")
        }
    } ?: false

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
        } else {
            throw IllegalArgumentException("The entered password does not match the current password")
        }
    }

    private fun encrypt(password: String): String {
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567890"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
            passwordHash = encrypt(this.toString())
            accessCode = this.toString()
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("..... sending access code: $code on $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, rawPhone = phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        fun makeCsvUser(
            fullName: String,
            email: String? = null,
            passwordInfo: String? = null,
            rawPhone: String? = null,
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return User(
                firstName,
                lastName,
                email,
                passwordInfo,
                rawPhone
            )
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only first " +
                                "name and last name, current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}