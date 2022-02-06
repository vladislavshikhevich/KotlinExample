package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.format

object UserHolder {

    private val map = mutableMapOf<String, User>()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName = fullName, email = email, password = password).registerUser("email")

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User = User.makeUser(fullName = fullName, phone = rawPhone).registerUser("phone")


    private fun User.registerUser(loginTypeText: String): User = also {
        if (!map.containsKey(login)) {
            map[login] = this
        } else {
            throw IllegalArgumentException("A user with this $loginTypeText already exists")
        }
    }

    fun loginUser(login: String, password: String): String? = map[login.format()]?.let { user ->
        if (user.checkPassword(password)) {
            user.userInfo
        } else {
            null
        }
    }

    fun requestAccessCode(login: String) {
        map[login.format()]?.generateAccessCode()
    }

    /*fun importUsers(list: List<String>): List<User> {
        val users = mutableListOf<User>()
        list.forEach { csvUserInfo ->
            with(csvUserInfo.split(";")) {
                users.add(
                    User.makeCsvUser(
                        fullName = getOrNull(0)?.trim().orEmpty(),
                        email = getOrNull(1)?.trim()?.ifEmpty { null },
                        passwordInfo = getOrNull(2)?.trim()?.ifEmpty { null },
                        rawPhone = getOrNull(3)?.trim()?.ifEmpty { null }
                    )
                )
            }

        }
        return users
    }*/

    fun importUsers(list: List<String>): List<User> {
        val userList = arrayListOf<User>()
        list.forEach {
            val (fullName, email, access, phone) =
                it.split(";").map { it.trim().ifBlank { null } }.subList(0, 4)
            userList.add(User.makeCsvUser(
                fullName = fullName!!,
                email = email,
                rawPhone = phone,
                passwordInfo = access
            )
                .also { map[it.login] = it })
        }
        return userList
    }
}