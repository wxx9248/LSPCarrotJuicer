package top.wxx9248.lspcarrotjuicer.xposed

import android.app.Activity
import android.os.Bundle
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class XposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/XposedEntry/handleLoadPackage"
        if (lpParam.packageName != TARGET_PACKAGE_NAME) {
            return
        }
        Log.i(logTag, "Matched target package")

        val moduleClassLoader = javaClass.classLoader!!
        val targetClassLoader = lpParam.classLoader

        hook(targetClassLoader)
        hookNative()
    }

    private fun hook(targetClassLoader: ClassLoader) {
        val logTag = "LCJ/XposedEntry/hook"

        XposedHelpers.findAndHookMethod(
            TARGET_ENTRY_ACTIVITY_CLASS_NAME,
            targetClassLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    Log.d(logTag, "Before target onCreate()")
                    val targetActivity = param?.thisObject as Activity
                    showSettingsDialog(targetActivity)
                }
            }
        )

        Log.d(logTag, "Hook completed")
    }

    private fun hookNative() {
        val logTag = "LCJ/XposedEntry/hookNative"

        try {
            System.loadLibrary(NATIVE_LIBRARY_NAME)
            Log.i(logTag, "Loaded native library lib$NATIVE_LIBRARY_NAME.so")
        } catch (e: Throwable) {
            Log.e(logTag, "Cannot load native library lib$NATIVE_LIBRARY_NAME.so", e)
            throw e
        }

        Log.d(logTag, "Loaded native hook library")
    }
}
