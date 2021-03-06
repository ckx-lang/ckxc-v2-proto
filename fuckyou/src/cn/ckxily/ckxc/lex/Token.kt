package cn.ckxily.ckxc.lex

enum class TokenType(val str: String) {
	Let("let"),
	Vi8("vi8"),
	Vi16("vi16"),
	Vi32("vi32"),
	Vi64("vi64"),
	Vu8("vu8"),
	Vu16("vu16"),
	Vu32("vu32"),
	Vu64("vu64"),
	Vr32("vr32"),
	Vr64("vr64"),
	Boolean("bool"),
	Class("class"),
	Enum("enum"),
	Const("const"),
	Volatile("volatile"),
	Func("func"),
	Number("Number\$"),
	Id("Identifier\$"),
	Colon(":"),
	ColonColon("::"),
	Semicolon(";"),
	Comma(","),
	Period("."),
	Lt("<"),
	Gt(">"),
	Eq("="),
	DupEq("=="),
	Arrow("->"),
	Plus("+"),
	Sub("-"),
	Asterisk("*"),
	Slash("/"),
	Not("!"),
	Neq("!="),
	Amp("&"),
	AmpAmp("&&"),
	Pipe("|"),
	PipePipe("||"),
	LeftBrace("{"),
	RightBrace("}"),
	LeftBracket("["),
	RightBracket("]"),
	LeftParen("("),
	RightParen(")"),
	EOI("EOI\$")
}

class Token(val tokenType: TokenType, val value: Any? = null) {
	override fun toString(): String {
		return "$tokenType (${value ?: tokenType.str})"
	}
}
