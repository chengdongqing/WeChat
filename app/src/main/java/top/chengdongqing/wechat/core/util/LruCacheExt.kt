package top.chengdongqing.wechat.core.util

import android.util.LruCache

fun <K : Any, V : Any> LruCache<K, V>.getOrPut(key: K, create: () -> V): V =
    get(key) ?: create().also { put(key, it) }