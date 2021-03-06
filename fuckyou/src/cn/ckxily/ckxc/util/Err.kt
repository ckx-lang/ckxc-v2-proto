package cn.ckxily.ckxc.util

import java.lang.System.exit

fun unrecoverableError(desc: String): Nothing {
	println("Error: $desc")
	@Suppress("UNREACHABLE_CODE", "CAST_NEVER_SUCCEEDS")
	return exit(1) as Nothing
}

fun assertionFailed(desc: String): Nothing {
	println("Assertion failed: $desc")
	@Suppress("UNREACHABLE_CODE", "CAST_NEVER_SUCCEEDS")
	return exit(-1) as Nothing
}
