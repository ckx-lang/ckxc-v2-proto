package cn.ckxily.ckxc.sema

import cn.ckxily.ckxc.ast.decl.*
import cn.ckxily.ckxc.ast.expr.*
import cn.ckxily.ckxc.ast.stmt.DeclStmt
import cn.ckxily.ckxc.ast.stmt.ExprStmt
import cn.ckxily.ckxc.ast.type.*
import cn.ckxily.ckxc.ast.type.getNoSpecifier
import cn.ckxily.ckxc.parser.QualifiedName
import cn.ckxily.ckxc.util.*
import java.util.*

class Scope(val parent: Scope? = null,
						var depth: Int,
						private var decls: MutableList<Decl> = ArrayList()) {
	init {
		depth = if (parent == null) 0 else parent.depth + 1
	}

	fun addDecl(decl: Decl) {
		decls.add(decl)
	}

	fun removeDecl(decl: Decl) {
		decls.remove(decl)
	}

	fun lookupVarDeclLocally(name: String) =
			lookupLocally(name).filter { decl -> postSelect(decl, LookupKind.LookupVarDecl) }

	fun lookupFuncLocally(name: String) =
			lookupLocally(name).filter { decl -> postSelect(decl, LookupKind.LookupFunc) }

	fun lookupClassLocally(name: String) =
			lookupLocally(name).filter { decl -> postSelect(decl, LookupKind.LookupClass) }

	fun lookupEnumLocally(name: String) =
			lookupLocally(name).filter { decl -> postSelect(decl, LookupKind.LookupEnum) }

	fun lookupASTContextLocally(name: String) =
			lookupLocally(name).filter { decl -> postSelect(decl, LookupKind.LookupASTContext) }

	fun lookupLocally(name: String) = decls.filter { decl -> decl.nameStr?.equals(name) ?: false }

	enum class LookupKind { LookupEverything, LookupVarDecl, LookupClass, LookupEnum, LookupASTContext, LookupFunc }

	private fun lookup(name: String, lookupKind: LookupKind): List<Decl> = dlLookup(this, name, lookupKind)

	private fun lookup(qualifiedName: QualifiedName, lookupKind: LookupKind): List<Decl> {
		var basicDecl = dlLookup(this, qualifiedName.nameChains.first(), LookupKind.LookupASTContext)
		var i = 1
		while (i < qualifiedName.nameChains.size - 1) {
			if (basicDecl.size != 1) {
				unrecoverableError("懒癌发作不想写错误信息了!")
			}

			if (basicDecl.first() !is DeclContext) {
				unrecoverableError("$basicDecl is not a DeclContext")
			}

			basicDecl = (basicDecl.first() as DeclContext).lookupLocalDecl(qualifiedName.nameChains[i])
			i += 1
		}

		return LinkedList(basicDecl).filter { decl -> postSelect(decl, lookupKind) }
	}

	private fun postSelect(decl: Decl, lookupKind: LookupKind): Boolean = when (lookupKind) {
		LookupKind.LookupEverything -> true
		LookupKind.LookupVarDecl -> decl.declKind == DeclKind.VarDecl
		LookupKind.LookupClass -> decl.declKind == DeclKind.ClassDecl
		LookupKind.LookupEnum -> decl.declKind == DeclKind.EnumDecl
		LookupKind.LookupASTContext -> decl.declKind == DeclKind.ClassDecl || decl.declKind == DeclKind.EnumDecl
		LookupKind.LookupFunc -> decl.declKind == DeclKind.FuncDecl
	}

	fun lookup(maybeQualifiedName: Either<String, QualifiedName>, lookupKind: LookupKind): List<Decl> =
			maybeQualifiedName.either({lookup(it, lookupKind)}, {lookup(it, lookupKind)})
}

tailrec fun dlLookup(scope: Scope, name: String, lookupKind: Scope.LookupKind): List<Decl> {
	val localResult = when(lookupKind) {
		Scope.LookupKind.LookupVarDecl -> scope.lookupVarDeclLocally(name)
		Scope.LookupKind.LookupFunc -> scope.lookupFuncLocally(name)
		Scope.LookupKind.LookupClass -> scope.lookupClassLocally(name)
		Scope.LookupKind.LookupEnum -> scope.lookupEnumLocally(name)
		Scope.LookupKind.LookupASTContext -> scope.lookupASTContextLocally(name)
		Scope.LookupKind.LookupEverything -> scope.lookupLocally(name)
	}

	if (localResult.isEmpty() && scope.parent != null) {
		return dlLookup(scope.parent, name, lookupKind)
	}
	return localResult
}

class Sema(var topLevelDeclContext: DeclContext = TransUnitDecl(),
					 var currentDeclContext: DeclContext = topLevelDeclContext,
					 var currentScope: Scope = Scope(null, 0)) {
	private fun pushScope() {
		currentScope = Scope(currentScope, currentScope.depth+1)
	}

	private fun popScope() {
		currentScope = currentScope.parent!!
	}

	private fun checkDuplicate(scope: Scope, nameStr: String) {
		if (scope.lookupLocally(nameStr).isNotEmpty()) {
			unrecoverableError("redefinition of $nameStr")
		}
	}

	fun actOnDeclInContext(decl: Decl, declContext: DeclContext = currentDeclContext) = declContext.addDecl(decl)

	fun actOnDeclInScope(decl: Decl, scope: Scope = currentScope) = scope.addDecl(decl)

	fun actOnVarDecl(scope: Scope, name: String, type: Type): VarDecl {
		checkDuplicate(scope, name)
		return VarDecl(name, type)
	}

	fun actOnClass(scope: Scope, name: String): ClassDecl {
		checkDuplicate(scope, name)
		return ClassDecl(name)
	}

	fun actOnEnum(scope: Scope, name: String): EnumDecl {
		checkDuplicate(scope, name)
		return EnumDecl(name)
	}

	fun actOnTagStartDefinition() = pushScope()

	fun actOnTagFinishDefinition() = popScope()

	fun actOnEnumerator(scope: Scope, enumDecl: EnumDecl, name: String, init: Int?): EnumeratorDecl {
		checkDuplicate(scope, name)
		val enumerator = EnumeratorDecl(name, init?: 0)
		actOnDeclInScope(enumerator)
		actOnDeclInContext(enumerator, enumDecl)
		return enumerator
	}

	fun actOnFuncDecl(scope: Scope, name: String): FuncDecl {
		return FuncDecl(name, ArrayList(), BuiltinType(BuiltinTypeId.Int8, getNoSpecifier()), null)
	}

	@Suppress("UNUSED_PARAMETER")
	fun actOnStartParamList(scope: Scope, funcDecl: FuncDecl) = pushScope()

	@Suppress("UNUSED_PARAMETER")
	fun actOnFinishParamList(scope: Scope, funcDecl: FuncDecl) = popScope()

	fun actOnParam(scope: Scope, funcDecl: FuncDecl, name: String, type: Type): VarDecl {
		checkDuplicate(scope, name)
		val varDecl = VarDecl(name, type)
		funcDecl.paramList.add(varDecl)
		return varDecl
	}

	fun actOnStartFuncDef() {}

	fun actOnFinishFuncDef() {}

	fun actOnCompoundStmtBegin() = pushScope()

	fun actOnCompoundStmtEnd() = popScope()

	fun actOnDeclStmt(decl: Decl): DeclStmt = DeclStmt(decl)

	fun actOnDeclRefExpr(decl: VarDecl): DeclRefExpr = DeclRefExpr(decl)

	fun actOnExprStmt(expr: Expr): ExprStmt = ExprStmt(expr)

	fun actOnBinaryExpr(lhs: Expr, rhs: Expr, opCode: BinaryOpCode): Expr {
		/// TODO add support for operator overloading
		return when (opCode) {
			BinaryOpCode.Assign -> actOnAssignmentExpr(lhs, rhs, opCode)
			BinaryOpCode.LogicAnd, BinaryOpCode.LogicOr -> actOnLogicalExpr(lhs, rhs, opCode)
			else -> actOnAlgebraExpr(lhs, rhs, opCode)
		}
	}

	fun actOnInitialization(decl: VarDecl, init: Expr) {
		/// TODO add support for user-defined constructors

		if (decl.type.isReference()) {
			actOnRefInit(decl, init)
			return
		}

		if (decl.type.isBuiltin() && dehugify(init.type).isBuiltin()) {
			actOnBuiltinTypeInit(decl, init)
			return
		}
	}

	fun actOnBuiltinTypeInit(decl: VarDecl, init: Expr) {
		if (canImplicitCast(init.type, decl.type)) {
			decl.init = actOnImplicitCast(init, decl.type)
			return
		}
		unrecoverableError("cannot initialize ${decl.type} with ${init.type}")
	}

	fun actOnRefInit(decl: VarDecl, init: Expr) {
		val exprType = dehugify(init.type)
		if (init.valueCategory == ValueCategory.RValue) {
			if (!init.type.qualifiers.isConst) {
				unrecoverableError("binding rvalue to non-const lvalue reference")
			}
		}
		if (!(decl.type as ReferenceType).referenced.equalType(exprType)) {
			unrecoverableError("cannot initialize reference of type ${(decl.type as ReferenceType).referenced} " +
																"with ${exprType}")
		}
		decl.init = init
	}

	fun actOnAssignmentExpr(lhs: Expr, rhs: Expr, opCode: BinaryOpCode): Expr {
		assert(opCode == BinaryOpCode.Assign)
		if (lhs.valueCategory != ValueCategory.LValue) {
			unrecoverableError("Expected lvalue at the left hand side of assignment expression")
		}
		if (lhs.type.qualifiers.isConst) {
			unrecoverableError("Expected non-const value at left hand side of assignment expression")
		}

		val castedRhs = actOnImplicitCast(rhs, lhs.type) ?: unrecoverableError("failed to cast assignee")
		return BinaryExpr(opCode, lhs, actOnLValueToRValueDecay(castedRhs), lhs.type)
	}

	fun actOnLogicalExpr(lhs: Expr, rhs: Expr, opCode: BinaryOpCode): Expr {
		val castedLhsType =
			lhs.type as? BuiltinType ?: unrecoverableError("Non-bool types used in logical expression")
		val castedRhsType =
			rhs.type as? BuiltinType ?: unrecoverableError("Non-bool types used in logical expression")
		if (castedLhsType.builtinTypeId != BuiltinTypeId.Boolean
				|| castedRhsType.builtinTypeId != BuiltinTypeId.Boolean) {
			unrecoverableError("Non-bool types used in logical expression")
		}

		return BinaryExpr(
				opCode,
				actOnLValueToRValueDecay(lhs),
				actOnLValueToRValueDecay(rhs),
				BuiltinType(BuiltinTypeId.Boolean, getNoSpecifier()))
	}

	fun actOnAlgebraExpr(lhs: Expr, rhs: Expr, opCode: BinaryOpCode): Expr {
		val commonType =
				TypeUtility.commonBuiltinType(
						lhs.type as BuiltinType,
						rhs.type as BuiltinType) ?: unrecoverableError("No common type!")

		val castedLhs = actOnImplicitCast(lhs, commonType) ?: unrecoverableError("failed to cast lhs")
		val castedRhs = actOnImplicitCast(rhs, commonType) ?: unrecoverableError("failed to cast rhs")
		return BinaryExpr(opCode, actOnLValueToRValueDecay(castedLhs), actOnLValueToRValueDecay(castedRhs),
											if (opCode == BinaryOpCode.Less || opCode == BinaryOpCode.Greater
													|| opCode == BinaryOpCode.Equal || opCode == BinaryOpCode.NEQ
													|| opCode == BinaryOpCode.GEQ || opCode == BinaryOpCode.LEQ)
												BuiltinType(BuiltinTypeId.Boolean, getNoSpecifier())
											else commonType)
	}

	fun dehugify(type: Type) = if (type.isReference()) (type as ReferenceType).referenced else type
	fun botherIf(condi: Boolean, desc: String) = if (condi) unrecoverableError(desc) else 0

	fun canImplicitCast(fromType: Type, destType: Type, bother: Boolean = false): Boolean {
		/// TODO handle user-defined conversion stuffs

		if (fromType.fullEqual(destType)) {
			return true
		}

		val castedFromType = dehugify(fromType)
		val castedDestType = dehugify(destType)

		@Suppress("NON_EXHAUSTIVE_WHEN")
		when (castedFromType.qualifiers.compareWith(castedDestType.qualifiers)) {
			TypeQualifiers.CompareResult.Nonsense, TypeQualifiers.CompareResult.LessQualified -> {
				botherIf(bother, "反正是qualifier出问题了你看着办吧")
				return false
			}
		}

		if (castedFromType.typeId == TypeId.Builtin && castedDestType.typeId == TypeId.Builtin) {
			return canImplicitCastBuiltin(castedFromType as BuiltinType, castedDestType as BuiltinType, bother)
		}

		if (castedFromType.typeId == TypeId.Pointer && castedDestType.typeId == TypeId.Pointer) {
			return canImplicitCastPointer(castedFromType as PointerType, castedDestType as PointerType)
		}

		return false
	}

	fun canImplicitCastPointer(fromType: PointerType, destType: PointerType) = destType.pointee.isVoid()

	fun checkValueCategoryForCast(fromValueCategory: ValueCategory,
																destValueCategory: ValueCategory,
																bother: Boolean, destType: Type): Boolean {
		if (fromValueCategory == ValueCategory.RValue && destValueCategory == ValueCategory.LValue) {
			botherIf(bother, "Shouldn't use rvalue when a lvalue is required")
			return false
		}
		else if (fromValueCategory == ValueCategory.RValue
				&& destType.typeId == TypeId.Reference
				&& !(destType as ReferenceType).referenced.qualifiers.isConst) {
			botherIf(bother, "Binding rvalue to non-const lvalue reference")
			return false
		}
		return true
	}

	fun canImplicitCastBuiltin(fromType: BuiltinType, destType: BuiltinType, bother: Boolean): Boolean {
		if (fromType.isInteger() && destType.isInteger()) {
			return fromType.builtinTypeId.rank <= destType.builtinTypeId.rank
		}
		else if (fromType.isFloating() && destType.isFloating()) {
			return fromType.builtinTypeId.rank <= destType.builtinTypeId.rank
		}
		else if (fromType.isBool() && destType.isBool()) {
			return true
		}
		return false
	}

	// TODO this function looks nasty, split it into several parts in further commits.
	fun actOnImplicitCast(expr: Expr, desired: Type): Expr? {
		/// TODO add handler for user-defined cast

		var currentExpr = expr
		var exprType = expr.type
		var desiredType = desired

		if (exprType.fullEqual(desiredType)) {
			return expr
		}

		if (exprType.isReference() && desiredType.isReference()) {
			return null
		}

		if (exprType.isReference()) {
			exprType = (exprType as ReferenceType).referenced
		}

		if (exprType.isPointer() && desiredType.isPointer()) {
			return performImplicitPointerCast(expr, exprType as PointerType, desiredType as PointerType);
		}

		if (exprType.isBuiltin() && desired.isBuiltin()) {
			return performImplicitBuiltinTypeCast(expr, exprType as BuiltinType, desiredType as BuiltinType)
		}

		return null
	}

	fun performImplicitBuiltinTypeCast(expr: Expr, exprType: BuiltinType, desiredType: BuiltinType): Expr? {
		if (exprType.isInteger() && desiredType.isInteger()) {
			if (exprType.builtinTypeId.rank <= desiredType.builtinTypeId.rank) {
				return ImplicitCastExpr(CastOperation.IntegerWidenCast, expr, desiredType)
			}
		}
		else if (exprType.isFloating() && desiredType.isFloating()) {
			if (exprType.builtinTypeId.rank <= desiredType.builtinTypeId.rank) {
				return ImplicitCastExpr(CastOperation.FloatingWidenCast, expr, desiredType)
			}
		}
		return null
	}

	fun performImplicitPointerCast(expr: Expr, exprType: PointerType, desiredType: PointerType): Expr? {
		if (desiredType.pointee.isVoid()) {
			return ImplicitCastExpr(CastOperation.PointerBitwiseCast, expr, desiredType)
		}
		return null
	}

	fun actOnLValueToRValueDecay(expr: Expr): Expr {
		return if (expr.valueCategory == ValueCategory.RValue) expr else ImplicitDecayExpr(expr)
	}

	fun actOnFuncCall(id: Either<String, QualifiedName>, args: MutableList<Expr>): CallExpr {
		val funcDecls = currentScope.lookup(id, Scope.LookupKind.LookupFunc)
		if (funcDecls.isEmpty()) {
			unrecoverableError("unknown function `$id'")
		}

		for (funcDecl in funcDecls) {
			if (canPerfectFreeze(funcDecl as FuncDecl, args)) {
				return doPerfectFreeze(funcDecl as FuncDecl, args)
			}
		}

		unrecoverableError("no viable call to function $id, candicate ${funcDecls.size} functions")
	}

	private fun doPerfectFreeze(funcDecl: FuncDecl, args: List<Expr>): CallExpr {
		val convertedArgs: MutableList<Expr> = ArrayList()
		for (argParamPair in args.zip(funcDecl.paramList)) {
			convertedArgs.add(actOnLValueToRValueDecay(actOnImplicitCast(argParamPair.first, argParamPair.second.type)!!))
		}
		return CallExpr(funcDecl, convertedArgs)
	}

	private fun canPerfectFreeze(funcDecl: FuncDecl, args: List<Expr>): Boolean {
		/// TODO passing arguments to functions is not simply "copy or cast", it involves reference handling and more
		if (funcDecl.paramList.size != args.size) {
			return false
		}

		for (declTypePair in funcDecl.paramList.zip(args)) {
			if (!canImplicitCast(declTypePair.second.type, declTypePair.first.type)) {
				return false
			}
		}

		return true
	}
}
