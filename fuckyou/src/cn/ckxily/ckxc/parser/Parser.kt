package cn.ckxily.ckxc.parser

import cn.ckxily.ckxc.ast.decl.*
import cn.ckxily.ckxc.ast.type.*
import cn.ckxily.ckxc.lex.Token
import cn.ckxily.ckxc.lex.TokenType
import cn.ckxily.ckxc.sema.Sema

class ParserStateMachine(val tokens: List<Token>, val sema: Sema = Sema(), var currentTokenIndex: Int = 0) {
	fun ParseTransUnit(): TransUnitDecl {
		while (currentToken().tokenType != TokenType.EOI) {
			when (currentToken().tokenType) {
				TokenType.Class -> parseClassDecl()
				TokenType.Enum -> parseEnumDecl()
				TokenType.Vi8, TokenType.Vi16, TokenType.Vi32, TokenType.Vi64 -> parseTopLevelVarDecl()
				else -> error("Token ${currentToken()} not allowed at top level of program")
			}
		}
		return sema.topLevelDeclContext as TransUnitDecl
	}

	private fun parseTopLevelVarDecl(): VarDecl {
		val type = parseType()
		expect(TokenType.Id)
		val name = currentToken().value as String
		nextToken()
		val varDecl = sema.actOnVarDecl(sema.currentScope, 0, name, type)
		return varDecl
	}

	private fun parseType(): Type {
		val ret = parseBuiltinType() ?: parseCustomType()
		return parsePostSpecifiers(ret)
	}

	private fun parsePostSpecifiers(ret: Type): Type {
		var modifiedType = ret
		while (true) {
			when (currentToken().tokenType) {
				TokenType.Const -> modifiedType.specifiers.isConst = true
				TokenType.Volatile -> modifiedType.specifiers.isVolatile = true
				TokenType.Mul -> modifiedType = PointerType(modifiedType, getNoSpecifier())
				TokenType.Amp -> modifiedType = ReferenceType(modifiedType, getNoSpecifier())
				else -> return modifiedType
			}
			nextToken()
		}
	}

	@Suppress("UNREACHABLE_CODE")
	private fun parseCustomType(): Type {
		error("parseCustomType not implemented")
		return null!!
	}

	private fun parseBuiltinType(): Type? {
		val type =  when (currentToken().tokenType) {
			TokenType.Vi8 -> BuiltinType(BuiltinTypeId.Int8, getNoSpecifier())
			TokenType.Vi16 -> BuiltinType(BuiltinTypeId.Int16, getNoSpecifier())
			TokenType.Vi32 -> BuiltinType(BuiltinTypeId.Int32, getNoSpecifier())
			TokenType.Vi64 -> BuiltinType(BuiltinTypeId.Int64, getNoSpecifier())
			TokenType.Vr32 -> BuiltinType(BuiltinTypeId.Float, getNoSpecifier())
			else -> return null
		}
		nextToken()
		return type
	}

	private fun parseEnumDecl(): EnumDecl {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	private fun parseClassDecl(): ClassDecl {
		assert(currentToken().tokenType == TokenType.Class)
		nextToken()

		expect(TokenType.Id)
		val name = currentToken().value!! as String

		val classDecl = sema.actOnClass(sema.currentScope, 0, name)
		parseClassFields(classDecl)
		return classDecl
	}

	private fun parseClassFields(classDecl: ClassDecl) {
		expectAndConsume(TokenType.LeftBrace)
		sema.actOnTagStartDefinition(classDecl)
		while (currentToken().tokenType != TokenType.RightBrace) {
			ParseDecl()
		}
		sema.actOnTagFinishDefinition()
	}

	private fun ParseDecl(): Decl {
		return null!!
	}

	private fun expect(tokenType: TokenType) {
		if (currentToken().tokenType != tokenType) {
			error("Expected ${tokenType.str} got ${currentToken()}")
		}
	}

	private fun expectAndConsume(tokenType: TokenType) {
		expect(tokenType)
		nextToken()
	}

	private fun currentToken() = tokens[currentTokenIndex]
	private fun peekOneToken() = tokens[currentTokenIndex+1]
	private fun nextToken() = currentTokenIndex++
}

class Parser {

}