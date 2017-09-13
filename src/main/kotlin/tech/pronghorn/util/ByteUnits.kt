package tech.pronghorn.util

private const val kibibyte = 1024
private const val mebibyte = kibibyte * 1024
private const val gibibyte = mebibyte * 1024L
private const val tebibyte = gibibyte * 1024L
private const val kilobyte = 1000
private const val megabyte = kilobyte * 1000
private const val gigabyte = megabyte * 1000L
private const val terabyte = gigabyte * 1000L

fun kibibytes(num: Int): Int = num * kibibyte

fun mebibytes(num: Int): Int = num * mebibyte

fun gibibytes(num: Int): Long = num * gibibyte

fun tebibytes(num: Int): Long = num * tebibyte

fun kilobytes(num: Int): Int = num * kilobyte

fun megabytes(num: Int): Int = num * megabyte

fun gigabytes(num: Int): Long = num * gigabyte

fun terabytes(num: Int): Long = num * terabyte
