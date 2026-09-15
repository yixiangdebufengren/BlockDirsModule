package com.bytedance.shadowhook;

/**
 * shadowhook 原生库导出 JNI_OnLoad 会 FindClass 这个类，
 * 若 APK 中不存在该类，FindClass 返回 null，导致 JNI_OnLoad 返回 JNI_ERR，
 * 进而 System.loadLibrary("blockdirs") 抛 UnsatisfiedLinkError。
 *
 * 这里提供一个空壳类，仅用于让 shadowhook 的 JNI_OnLoad 能正常 FindClass，
 * 使 libshadowhook.so 被 libblockdirs.so 依赖加载时 JNI_OnLoad 正常返回。
 */
public class ShadowHook {
}
