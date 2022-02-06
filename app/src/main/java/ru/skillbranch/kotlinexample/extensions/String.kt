package ru.skillbranch.kotlinexample.extensions

fun String.format(): String = if (this.startsWith('+')) {
    replace("[^+\\d]".toRegex(), "")
} else {
    this
}.also { it.trim() }