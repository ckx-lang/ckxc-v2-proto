package cn.ckxily.ckxc.codegen

import cn.ckxily.ckxc.ast.decl.*
import cn.ckxily.ckxc.ast.expr.*
import cn.ckxily.ckxc.ast.stmt.*

interface ASTConsumer {
	fun visitTransUnitDecl(transUnitDecl: TransUnitDecl): Any?
	fun visitVarDecl(varDecl: VarDecl): Any?
	fun visitEnumDecl(enumDecl: EnumDecl): Any?
	fun visitEnumeratorDecl(enumeratorDecl: EnumeratorDecl): Any?
	fun visitClassDecl(classDecl: ClassDecl): Any?
	fun visitFuncDecl(funcDecl: FuncDecl): Any?
	fun visitCompoundStmt(compoundStmt: CompoundStmt): Any?
	fun visitDeclStmt(declStmt: DeclStmt): Any?
	fun visitExprStmt(exprStmt: ExprStmt): Any?
	fun visitDeclRefExpr(declRefExpr: DeclRefExpr): Any?
}

class BetterASTPrinter(private var indentation: Int = 0) : ASTConsumer {
	private fun indent() {
		var i = 0;
		while (i < indentation) { print("  "); ++i }
	}

	override fun visitTransUnitDecl(transUnitDecl: TransUnitDecl): Any? {
		println("TranslationUnitDecl")
		indentation++
		for (topLevelDecl in transUnitDecl.decls) topLevelDecl.accept(this)
		indentation--
		return null
	}

	override fun visitVarDecl(varDecl: VarDecl): Any? {
		indent(); println("VarDecl ${varDecl.nameStr}(${varDecl.hashCode()}) of type ${varDecl.type}")
		return null
	}

	override fun visitEnumDecl(enumDecl: EnumDecl): Any? {
		indent(); println("EnumDecl ${enumDecl.nameStr}(${enumDecl.hashCode()})")
		indentation++
		for (subDecl in enumDecl.decls) subDecl.accept(this)
		indentation--
		return null
	}

	override fun visitEnumeratorDecl(enumeratorDecl: EnumeratorDecl): Any? {
		indent(); println("${enumeratorDecl.nameStr}(${enumeratorDecl.hashCode()}) = ${enumeratorDecl.init}")
		return null
	}

	override fun visitClassDecl(classDecl: ClassDecl): Any? {
		indent(); println("ClassDecl ${classDecl.nameStr}(${classDecl.hashCode()})")
		indentation++
		for (subDecl in classDecl.decls) subDecl.accept(this)
		indentation--
		return null
	}

	override fun visitFuncDecl(funcDecl: FuncDecl): Any? {
		indent(); println("FuncDecl ${funcDecl.nameStr}(${funcDecl.hashCode()})")
		indentation++
		for (paramDecl in funcDecl.paramList) paramDecl.accept(this)
		indentation--
		indent(); println("ReturnType ${funcDecl.retType}")
		if (funcDecl.funcBody != null) {
			indent(); println("FunctionBody")
			indentation++
			funcDecl.funcBody!!.accept(this)
			indentation--
		}
		return null
	}

	override fun visitCompoundStmt(compoundStmt: CompoundStmt): Any? {
		indent(); println("CompoundStmt!")
		indentation++
		for (stmt in compoundStmt.stmtList) stmt.accept(this)
		indentation--
		return null
	}

	override fun visitDeclStmt(declStmt: DeclStmt): Any? {
		indent(); println("DeclStmt")
		indentation++
		declStmt.decl.accept(this)
		indentation--
		return null
	}

	override fun visitExprStmt(exprStmt: ExprStmt): Any? {
		indent(); println("ExprStmt")
		indentation++
		exprStmt.expr.accept(this)
		indentation--
		return null
	}

	override fun visitDeclRefExpr(declRefExpr: DeclRefExpr): Any? {
		indent(); println("DeclRefExpr")
		indentation++
		/// TODO this forms bad output format.
		declRefExpr.decl.accept(this)
		indentation--
		return null
	}
}

class ASTPrinter(private var indentation: Int = 0) : ASTConsumer {
	private fun indent() {
		var i = 0; while (i < indentation * 3) { print(" "); ++i }
	}

	override fun visitTransUnitDecl(transUnitDecl: TransUnitDecl): Any? {
		println("Translation unit start!")
		indentation++
		for (topLevelDecl in transUnitDecl.decls) topLevelDecl.accept(this)
		indentation--
		println("Translation unit end!")
		return null
	}

	override fun visitVarDecl(varDecl: VarDecl): Any? {
		indent(); println("Variable declaration begin!")
		indentation++
		indent(); println("${varDecl.nameStr}(${varDecl.hashCode()}) of type ${varDecl.type}")
		indentation--
		indent(); println("Variable declaration end!")
		return null
	}

	override fun visitEnumDecl(enumDecl: EnumDecl): Any? {
		indent(); println("Enum declaration begin!")
		indent(); println("enum ${enumDecl.nameStr}(${enumDecl.hashCode()})")
		indentation++
		for (subDecl in enumDecl.decls) subDecl.accept(this)
		indentation--
		indent(); println("Enum declaration end!")
		return null
	}

	override fun visitEnumeratorDecl(enumeratorDecl: EnumeratorDecl): Any? {
		indent(); println("${enumeratorDecl.nameStr}(${enumeratorDecl.hashCode()}) = ${enumeratorDecl.init}")
		return null
	}

	override fun visitClassDecl(classDecl: ClassDecl): Any? {
		indent(); println("Class declaration begin!")
		indent(); println("class ${classDecl.nameStr}(${classDecl.hashCode()})")
		indentation++
		for (subDecl in classDecl.decls) subDecl.accept(this)
		indentation--
		indent(); println("Class declaration end!")
		return null
	}

	override fun visitFuncDecl(funcDecl: FuncDecl): Any? {
		indent(); println("Function declaration begin!")
		indent(); println("function ${funcDecl.nameStr}(${funcDecl.hashCode()})")
		indentation++
		for (paramDecl in funcDecl.paramList) paramDecl.accept(this)
		indentation--
		indent(); println("Return type is ${funcDecl.retType}")
		if (funcDecl.funcBody != null) {
			indent(); println("Function body!")
			indentation++
			funcDecl.funcBody!!.accept(this)
			indentation--
		}
		indent(); println("Function declaration end!")
		return null
	}

	override fun visitCompoundStmt(compoundStmt: CompoundStmt): Any? {
		indent(); println("Compound statement begin!")
		indentation++
		for (stmt in compoundStmt.stmtList) stmt.accept(this)
		indentation--
		indent(); println("Compound statement end!")
		return null
	}

	override fun visitDeclStmt(declStmt: DeclStmt): Any? {
		indent(); println("Declaration statement begin!")
		indentation++
		declStmt.decl.accept(this)
		indentation--
		indent(); println("Declaration statement end!")
		return null
	}

	override fun visitExprStmt(exprStmt: ExprStmt): Any? {
		indent(); println("Expression statement begin!")
		indentation++
		exprStmt.expr.accept(this)
		indentation--
		indent(); println("Expression statement end!")
		return null
	}

	override fun visitDeclRefExpr(declRefExpr: DeclRefExpr): Any? {
		indent(); println("DeclRefExpr begin!")
		indentation++
		/// TODO this forms bad output format.
		declRefExpr.decl.accept(this)
		indentation--
		indent(); println("DeclRefExpr end!")
		return null
	}
}

