package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting


object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email = email, password = password)
        .also { user ->
            if(map[user.login] != null) throw IllegalArgumentException("A user with this email already exists")
            else map[user.login] = user }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User = User.makeUser(fullName, phone = rawPhone)
        .also { user ->
            if (map[user.login] != null) throw java.lang.IllegalArgumentException("A user with this phone already exists")
            else {
                map[user.login] = user
            }
        }

    fun loginUser(login: String, password: String): String? =
        map[User.formatLogin(login)]?.let {
            if (it.checkPassword(password)) it.userInfo

            else null
        }

    fun requestAccessCode(login: String) {
        val phone = User.formatLogin(login)
        val user = map[phone]
        user?.apply {
            val newAccessCode = generateAccessCode()
            changePassword(accessCode!!, newAccessCode)
            accessCode = newAccessCode
            sendAccessCodeToUser(this.login, accessCode!!)
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val userList: MutableList<User> = mutableListOf()
        for (string in list) {
            val stringList = string.split(""";""")

            val user = User.makeUser(
                fullName = stringList[0].trim(),
                email = if (stringList[1].isNullOrBlank()) null else stringList[1],
                password = stringList[2].trim(),
                phone = stringList[3].trim()
            )

            userList.add(user)
            map.put(user.login, user)
        }
        return userList
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }
}