package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    val lastIndex: Int
    this.apply {
        lastIndex = indexOf(last(predicate))
        return subList(0, lastIndex)
    }

}