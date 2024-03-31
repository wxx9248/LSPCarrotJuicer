package top.wxx9248.lspcarrotjuicer.xposed

import android.app.Activity
import android.content.res.XModuleResources
import android.os.Bundle
import android.util.Log
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.wxx9248.lspcarrotjuicer.R

class Hook : IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {
    private companion object {
        var modulePath: String? = null
        var resources: Resources? = null
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val logTag = "LCJ/Hook/initZygote"
        modulePath = startupParam.modulePath
        Log.d(logTag, "Module path: $modulePath")
    }

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/Hook/handleLoadPackage"
        if (lpParam.packageName != TARGET_PACKAGE_NAME) {
            return
        }
        Log.i(logTag, "Matched target package")

        hook(lpParam)
        hookNative(lpParam)
    }

    override fun handleInitPackageResources(resParam: XC_InitPackageResources.InitPackageResourcesParam) {
        val logTag = "LCJ/Hook/handleInitPackageResources"
        if (resParam.packageName != TARGET_PACKAGE_NAME) {
            return
        }
        Log.i(logTag, "Matched target package")

        val moduleResources = XModuleResources.createInstance(modulePath!!, resParam.res)
        resources = Resources(
            resParam.res.addResource(moduleResources, R.string.app_name),
            resParam.res.addResource(moduleResources, R.string.settings_dialog_input_host_prompt),
            resParam.res.addResource(moduleResources, R.string.settings_dialog_input_port_prompt),
        )
        Log.i(logTag, "All resources injected")
    }

    private fun hook(lpParam: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/Hook/hook"

        XposedHelpers.findAndHookMethod(
            TARGET_ENTRY_ACTIVITY_CLASS_NAME,
            lpParam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    Log.d(logTag, "Before target onCreate()")
                    val targetActivity = param?.thisObject as Activity;
                    showSettingsDialog(targetActivity, resources!!);
                }
            }
        )

        Log.d(logTag, "Hook completed")
    }

    private fun hookNative(lpParam: XC_LoadPackage.LoadPackageParam) {
        val logTag = "LCJ/Hook/hookNative"

        try {
            System.loadLibrary(NATIVE_LIBRARY_NAME);
            Log.i(logTag, "Loaded native library lib$NATIVE_LIBRARY_NAME.so")
        } catch (e: Throwable) {
            Log.e(logTag, "Cannot load native library lib$NATIVE_LIBRARY_NAME.so", e)
            throw e;
        }

        Log.d(logTag, "Loaded native hook library")
    }
}
