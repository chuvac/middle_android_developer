package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val list = mutableListOf<Int>()
    if (!this.isNullOrEmpty() && substr.isNotEmpty()){
        var index: Int = 0
        while (index in 0..this.length) {
            index = this.indexOf(substr, startIndex = index, ignoreCase = ignoreCase)
            if (index != -1) {
                list.add(index)
                index += substr.length
            }
        }
    }
    return list
}