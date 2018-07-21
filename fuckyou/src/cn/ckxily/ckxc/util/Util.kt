package cn.ckxily.ckxc.util

sealed class Either<out T, out U> {
	fun asLeft() = (this as Left<T>).obj

	fun asRight() = (this as Right<U>).obj

	fun asLeftSafe() = (this as? Left<T>)?.obj

	fun asRightSafe() = (this as? Right<U>)?.obj

	// FIXME haskell stdlib function, copied implementation
	// FIXME remove this comment after you read this
	inline fun <R> either(f: (T) -> R, g: (U) -> R) = when (this) {
		is Left -> f(obj)
		is Right -> g(obj)
	}
}

class Left<T>(var obj: T) : Either<T, Nothing>()

class Right<U>(var obj: U) : Either<Nothing, U>()

fun addressOf(any: Any) = "<@${any.hashCode().toString(16)}>"