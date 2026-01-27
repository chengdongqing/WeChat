package top.chengdongqing.wechat.data.repository.location

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.locationDataStore by preferencesDataStore(name = "location")

object PreferencesKeys {
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
}