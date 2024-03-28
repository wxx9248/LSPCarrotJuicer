package top.wxx9248.lspcarrotjuicer.xposed

import android.util.Log

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hook: IXposedHookLoadPackage {
    private companion object {
        const val TARGET_PACKAGE_NAME = "jp.co.cygames.umamusume"
        const val NATIVE_LIBRARY_NAME = "lspcarrotjuicer"
    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/Hook/handleLoadPackage"
        if (param.packageName != TARGET_PACKAGE_NAME) {
            return
        }
        Log.i(logTag, "Hit target package: executing hook procedure")
        hook(param)
    }

    private fun hook(param: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/Hook/hook"

        try {
            System.loadLibrary(NATIVE_LIBRARY_NAME);
            Log.i(logTag, "Loaded native library lib$NATIVE_LIBRARY_NAME.so")
        } catch (e: Throwable) {
            Log.e(logTag, "Cannot load native library lib$NATIVE_LIBRARY_NAME.so", e)
            throw e;
        }
    }
}
