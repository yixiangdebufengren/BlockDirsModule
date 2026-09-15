cmake_minimum_required(VERSION 3.18)
project(blockdirs)

set(CMAKE_CXX_STANDARD 17)

include(FetchContent)

FetchContent_Declare(
    dobby
    GIT_REPOSITORY https://github.com/jmpews/Dobby.git
    GIT_TAG master
    GIT_SHALLOW TRUE
)

set(DOBBY_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(Plugin.SymbolResolver OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(dobby)

# ---- Patch Dobby 的 Darwin 汇编语法为 Android/Linux GNU 语法 ----
# Dobby master 的 closure_bridge_arm64.asm 使用了 Apple 汇编器的
# @PAGE / @PAGEOFF 修饰符，Android NDK 的 clang(GNU as) 不认识，
# 会导致 "invalid symbol kind for ADRP relocation" 编译错误。
# 这里在 add_subdirectory 之后、真正编译之前，把源码文件原地改写。
set(_DOBBY_ASM "${dobby_SOURCE_DIR}/source/TrampolineBridge/ClosureTrampolineBridge/arm64/closure_bridge_arm64.asm")
if(EXISTS "${_DOBBY_ASM}")
    file(READ "${_DOBBY_ASM}" _asm_content)
    # 精确替换 Darwin 语法的两行，改为 Android/Linux GNU 语法。
    # 注意：必须先处理 @PAGEOFF（更长的串），再处理 @PAGE，
    # 否则 @PAGE 会误匹配 @PAGEOFF 的前 5 个字符，残留 "OFF"。
    string(REPLACE
        "cdecl(common_closure_bridge_handler)@PAGEOFF"
        ":lo12:cdecl(common_closure_bridge_handler)"
        _asm_content "${_asm_content}")
    string(REPLACE
        "cdecl(common_closure_bridge_handler)@PAGE"
        "cdecl(common_closure_bridge_handler)"
        _asm_content "${_asm_content}")
    file(WRITE "${_DOBBY_ASM}" "${_asm_content}")
    message(STATUS "[blockdirs] patched Dobby closure_bridge_arm64.asm for Android GNU as")
endif()

add_library(blockdirs SHARED blockdirs.cpp)

target_link_libraries(blockdirs
    dobby_static
    log
    dl)