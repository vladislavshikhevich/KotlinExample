package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    var flag = false
    return mutableListOf<T>().apply {
        this@dropLastUntil.reversed().forEach() { element ->
            if (flag) {
                add(element)
            }
            if (predicate(element)) {
                flag = true
            }
        }
        reverse()
    }
}

fun String.format(): String = if (this.startsWith('+')) {
    replace("[^+\\d]".toRegex(), "")
} else {
    this
}.also { it.trim() }