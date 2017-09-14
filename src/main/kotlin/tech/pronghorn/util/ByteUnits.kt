/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
