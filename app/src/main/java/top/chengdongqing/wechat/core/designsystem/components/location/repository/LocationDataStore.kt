package top.chengdongqing.wechat.core.designsystem.components.location.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.locationDataStore by preferencesDataStore(name = "location")

object PreferencesKeys {
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
}