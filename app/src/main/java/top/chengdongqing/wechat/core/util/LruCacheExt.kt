package top.chengdongqing.wechat.core.util

import android.util.LruCache

fun <K : Any, V : Any> LruCache<K, V>.getOrPut(key: K, create: () -> V): V =
    get(key) ?: create().also { put(key, it) }

suspend fun <K : Any, V : Any> LruCache<K, V>.getOrPutAsync(key: K, create: suspend () -> V?): V? =
    get(key) ?: create()?.also { put(key, it) }