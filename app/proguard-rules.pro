# BlockDirs R8 规则

# Xposed 模块入口类保持（LSPosed 通过 meta-data 反射加载，类名不能被混淆）
-keep class top.yixiangren.blockdirs.** { *; }

# 保持 Android 组件类（Activity / alias / receiver 等，manifest 声明不能被混淆）
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# MainActivityAlias（activity-alias 隐藏桌面图标依赖，组件名不能混淆）
-keep class top.yixiangren.blockdirs.MainActivityAlias { *; }

# 保持 JNI / native 方法（native 库通过 System.loadLibrary 加载，方法名不能混淆）
-keepclasseswithmembernames class * {
    native <methods>;
}

# appcompat / material 已在其 AAR 自带 keep 规则，无需额外处理
# 保留注解（部分反射依赖）
-keepattributes *Annotation*
