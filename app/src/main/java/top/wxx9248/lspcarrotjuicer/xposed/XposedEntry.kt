package top.wxx9248.lspcarrotjuicer.xposed

import android.app.Activity
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.wxx9248.lspcarrotjuicer.controller.ConfigController
import top.wxx9248.lspcarrotjuicer.utils.ServiceManager

class XposedEntry(
    xposedInterface: XposedInterface,
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(xposedInterface, param) {
    
    private var isServiceStarted = false
    private var xposedService: XposedService? = null

    init {
        Log.i("LCJ/XposedEntry", "Modern Xposed module initialized for process: ${param.processName}")
        
        // Register service listener for framework communication
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                xposedService = service
                Log.i("LCJ/XposedEntry", "XposedService connected: ${service.frameworkName} v${service.frameworkVersion}")
            }
            
            override fun onServiceDied(service: XposedService) {
                if (xposedService == service) {
                    xposedService = null
                    Log.w("LCJ/XposedEntry", "XposedService died")
                }
            }
        })
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val logTag = "LCJ/XposedEntry/onPackageLoaded"
        
        if (param.packageName != TARGET_PACKAGE_NAME) {
            return
        }
        Log.i(logTag, "Matched target package: ${param.packageName}")
        Log.i(logTag, "Xposed API initialized: ${frameworkName} v${frameworkVersion}")

        hookActivityLifecycle(param.classLoader)
        hookNativeLibrary()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun hookActivityLifecycle(targetClassLoader: ClassLoader) {
        val logTag = "LCJ/XposedEntry/hookActivityLifecycle"

        try {
            val targetClass = targetClassLoader.loadClass(TARGET_ENTRY_ACTIVITY_CLASS_NAME)
            
            // Hook onCreate method
            val onCreateMethod = targetClass.getDeclaredMethod("onCreate", Bundle::class.java)
            hook(onCreateMethod, ActivityOnCreateHooker::class.java)

            // Hook onDestroy method
            val onDestroyMethod = targetClass.getDeclaredMethod("onDestroy")
            hook(onDestroyMethod, ActivityOnDestroyHooker::class.java)

            Log.d(logTag, "Activity lifecycle hooks completed")
            
        } catch (e: ClassNotFoundException) {
            Log.e(logTag, "Target class not found: $TARGET_ENTRY_ACTIVITY_CLASS_NAME", e)
        } catch (e: NoSuchMethodException) {
            Log.e(logTag, "Target method not found", e)
        } catch (e: Exception) {
            Log.e(logTag, "Failed to hook activity lifecycle", e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startPacketForwardingService(context: Activity) {
        val logTag = "LCJ/XposedEntry/startPacketForwardingService"

        GlobalScope.launch {
            try {
                Log.i(logTag, "Starting packet forwarding service")

                // Load configuration
                val configController = ConfigController(context)
                val config = configController.getCurrentConfig()

                if (!config.enabled) {
                    Log.i(logTag, "Service is disabled in configuration, skipping startup")
                    return@launch
                }

                if (config.serverUrl.isEmpty()) {
                    Log.w(logTag, "No server URL configured, starting service anyway")
                }

                // Start the service
                ServiceManager.startService(context, config)
                Log.i(logTag, "Packet forwarding service start request sent")

            } catch (e: Exception) {
                Log.e(logTag, "Failed to start packet forwarding service", e)
            }
        }
    }

    private fun hookNativeLibrary() {
        val logTag = "LCJ/XposedEntry/hookNativeLibrary"

        try {
            System.loadLibrary(NATIVE_LIBRARY_NAME)
            Log.i(logTag, "Successfully loaded native library: lib$NATIVE_LIBRARY_NAME.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(logTag, "Failed to load native library: lib$NATIVE_LIBRARY_NAME.so", e)
            throw e
        } catch (e: Exception) {
            Log.e(logTag, "Unexpected error loading native library", e)
            throw e
        }

        Log.i(logTag, "Native library loaded successfully")
    }

    // Hooker class for onCreate method
    class ActivityOnCreateHooker : XposedInterface.Hooker {
        companion object {
            private var isServiceStarted = false
            
            @JvmStatic
            fun after(callback: XposedInterface.AfterHookCallback) {
                Log.d("LCJ/XposedEntry/onCreate", "Target activity onCreate() called")
                val targetActivity = callback.thisObject as Activity

                if (!isServiceStarted) {
                    // We can't easily create an XposedEntry instance here since it requires constructor params
                    // Instead, let's use a static method approach
                    startPacketForwardingServiceStatic(targetActivity)
                    isServiceStarted = true
                }
            }
        }
    }

    // Hooker class for onDestroy method
    class ActivityOnDestroyHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: XposedInterface.BeforeHookCallback) {
                Log.d("LCJ/XposedEntry/onDestroy", "Target activity onDestroy() called")
                // Optionally stop service when app closes
                // For now, let the service continue running
            }
        }
    }

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @JvmStatic
        private fun startPacketForwardingServiceStatic(context: Activity) {
            val logTag = "LCJ/XposedEntry/startPacketForwardingServiceStatic"

            GlobalScope.launch {
                try {
                    Log.i(logTag, "Starting packet forwarding service")

                    // Load configuration
                    val configController = ConfigController(context)
                    val config = configController.getCurrentConfig()

                    if (!config.enabled) {
                        Log.i(logTag, "Service is disabled in configuration, skipping startup")
                        return@launch
                    }

                    if (config.serverUrl.isEmpty()) {
                        Log.w(logTag, "No server URL configured, starting service anyway")
                    }

                    // Start the service
                    ServiceManager.startService(context, config)
                    Log.i(logTag, "Packet forwarding service start request sent")

                } catch (e: Exception) {
                    Log.e(logTag, "Failed to start packet forwarding service", e)
                }
            }
        }
    }
}
