package top.chengdongqing.wechat.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun primaryTabNavigation() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE
    ) {
        pressHome()
        startActivityAndWait()
        device.findObject(androidx.test.uiautomator.By.text("通讯录")).click()
        device.waitForIdle()
        device.findObject(androidx.test.uiautomator.By.text("发现")).click()
        device.waitForIdle()
        device.findObject(androidx.test.uiautomator.By.text("我")).click()
        device.waitForIdle()
        device.findObject(androidx.test.uiautomator.By.text("微信")).click()
        device.waitForIdle()
    }

    private companion object {
        const val TARGET_PACKAGE = "top.chengdongqing.wechat"
    }
}
