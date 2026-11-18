package uz.xumoyun.core.common

/** Saqlangan nomdan enum qiymati; noma'lum nom (eski versiya, xato) bo'lsa null. */
inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()
