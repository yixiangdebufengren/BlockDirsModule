#include <jni.h>
#include <string>
#include <cstring>
#include <cstdlib>
#include <dlfcn.h>
#include <unistd.h>
#include <android/log.h>
#include <sys/stat.h>

#include "dobby.h"

#define TAG "BlockDirsNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 原始函数指针（由 Dobby 回填）
static int (*orig_mkdir)(const char *, mode_t) = nullptr;
static int (*orig_mkdirat)(int, const char *, mode_t) = nullptr;

// 判断路径是否是「外置卷根目录下的第一层目录」
// 即 /storage/XXXX-XXXX/<name> 或 /mnt/media_rw/XXXX/<name>
static bool is_top_level_on_external(const char *path) {
    if (path == nullptr) {
        return false;
    }

    const char *base = nullptr;

    if (strncmp(path, "/storage/", 9) == 0) {
        base = path + 9;
    } else if (strncmp(path, "/mnt/media_rw/", 14) == 0) {
        base = path + 14;
    } else if (strncmp(path, "/mnt/secure/", 12) == 0) {
        base = path + 12;
    } else {
        return false;
    }

    // 排除内部共享存储 /storage/emulated
    if (strncmp(base, "emulated", 8) == 0) {
        return false;
    }

    // 找到卷 UUID 后的第一个 '/'，后面是第一层目录名
    const char *slash = strchr(base, '/');
    if (slash == nullptr) {
        return false;
    }

    const char *name = slash + 1;
    if (*name == '\0') {
        return false;
    }

    // 更深层不拦
    if (strchr(name, '/') != nullptr) {
        return false;
    }

    return true;
}

// Hook 后的 mkdir
static int my_mkdir(const char *path, mode_t mode) {
    if (is_top_level_on_external(path)) {
        LOGI("BLOCKED mkdir: %s", path);
        return 0;
    }
    return orig_mkdir(path, mode);
}

// Hook 后的 mkdirat
static int my_mkdirat(int dirfd, const char *path, mode_t mode) {
    if (is_top_level_on_external(path)) {
        LOGI("BLOCKED mkdirat: %s", path);
        return 0;
    }
    return orig_mkdirat(dirfd, path, mode);
}

static void *resolve_symbol(const char *lib, const char *name) {
    void *handle = dlopen(lib, RTLD_NOW | RTLD_GLOBAL);
    if (handle == nullptr) {
        LOGE("dlopen %s failed: %s", lib, dlerror());
        return nullptr;
    }
    void *sym = dlsym(handle, name);
    if (sym == nullptr) {
        LOGE("dlsym %s@%s failed: %s", name, lib, dlerror());
    }
    return sym;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_blockdirs_MainHook_installNativeHook(JNIEnv *env, jclass clazz) {
    LOGI("installNativeHook: block ALL top-level dirs on external volumes");

    // Hook libc 的 mkdir
    void *mkdir_addr = resolve_symbol("libc.so", "mkdir");
    if (mkdir_addr != nullptr) {
        if (DobbyHook(mkdir_addr, (void *)my_mkdir, (void **)&orig_mkdir) == 0) {
            LOGI("mkdir hooked @ %p", mkdir_addr);
        } else {
            LOGE("mkdir hook failed");
        }
    }

    // Hook libc 的 mkdirat
    void *mkdirat_addr = resolve_symbol("libc.so", "mkdirat");
    if (mkdirat_addr != nullptr) {
        if (DobbyHook(mkdirat_addr, (void *)my_mkdirat, (void **)&orig_mkdirat) == 0) {
            LOGI("mkdirat hooked @ %p", mkdirat_addr);
        } else {
            LOGE("mkdirat hook failed");
        }
    }
}