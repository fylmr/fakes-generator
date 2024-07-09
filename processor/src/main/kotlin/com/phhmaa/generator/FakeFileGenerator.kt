package com.phhmaa.generator

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.phhmaa.FakeReturnValue
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName

class FakeFileGenerator(
    private val logger: KSPLogger,
) {

    // todo add inherited methods/properties generation
    fun generateFakeFile(symbol: KSClassDeclaration): FileSpec {
        val packageName = symbol.packageName.asString()
        val className = symbol.toClassName().simpleName
        val generatedClassName = "Fake${className}"

        val functions = symbol.declarations.filterIsInstance<KSFunctionDeclaration>()
            .filter { it.getVisibility() != Visibility.PRIVATE }
            .asIterable()

        val generatedFunctions = functions.map { function -> generateFunction(function) }

        val fields = symbol.declarations.filterIsInstance<KSPropertyDeclaration>()
            .filter { it.getVisibility() != Visibility.PRIVATE }
            .asIterable()

        val generatedFields = fields.map { field -> generateField(field) }

        val classBuilder = TypeSpec.classBuilder(generatedClassName)
            .addOverride(symbol)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(functions.getConstructorParameters())
                    .addParameters(fields.getFieldConstructorParameters())
                    .build()
            )
            .addModifiers(symbol.modifiers.mapNotNull { it.toKModifier() })
            .addFunctions(generatedFunctions)
            .addProperties(functions.toPropSpec())
            .addProperties(generatedFields)

        return FileSpec
            .builder(packageName = packageName, fileName = generatedClassName)
            .addType(classBuilder.build())
            .build()
    }

    /**
     * Generate constructor parameters for the generated class.
     * @see toPropSpec
     */
    private fun Iterable<KSFunctionDeclaration>.getConstructorParameters() = map { function ->
        ParameterSpec
            .builder(
                name = getConstructorParameterName(function),
                type = getLambdaOf(function),
            )
            .defaultValue(function.toDefaultParameter())
            .build()
    }

    private fun Iterable<KSPropertyDeclaration>.getFieldConstructorParameters() = map { field ->
        ParameterSpec
            .builder(
                name = getConstructorParameterName(field),
                type = getLambdaOf(field),
            )
            .defaultValue(field.toDefaultParameter())
            .build()
    }

    /**
     * Generate properties = constructor parameters ([getConstructorParameters]) for the generated class.
     */
    private fun Iterable<KSFunctionDeclaration>.toPropSpec(): List<PropertySpec> = map { function ->
        PropertySpec
            .builder(
                name = getConstructorParameterName(function),
                type = getLambdaOf(function),
            )
            .initializer(getConstructorParameterName(function))
            .addModifiers(KModifier.PRIVATE)
            .build()
    }

    private fun getConstructorParameterName(function: KSDeclaration) =
        "${function.simpleName.getShortName()}Fake"

    private fun getLambdaOf(function: KSFunctionDeclaration) =
        LambdaTypeName.get(
            parameters = function.parameters.map {
                ParameterSpec(
                    it.name!!.asString(),
                    it.type.toTypeName()
                )
            },
            returnType = function.returnType!!.toTypeName()
        )

    private fun getLambdaOf(field: KSPropertyDeclaration) =
        LambdaTypeName.get(
            parameters = emptyList(),
            returnType = field.type.toTypeName(),
        )

    private fun generateFunction(function: KSFunctionDeclaration): FunSpec {
        val funSpecBuilder = FunSpec.builder(function.simpleName.asString())
            .addParameters(
                function.parameters.map { parameter ->
                    ParameterSpec.builder(
                        name = parameter.name!!.asString(),
                        type = parameter.type.toTypeName(),
                    ).build()
                }
            )
            .returns(function.returnType!!.toTypeName())
            .addStatement("return ${function.simpleName.getShortName()}Fake(${function.parameters.joinToString { it.name!!.asString() }})") // e.g. fakeCreateOrder(order)
            .addModifiers(KModifier.OVERRIDE)
        return funSpecBuilder.build()
    }

    private fun generateField(field: KSPropertyDeclaration): PropertySpec {
        val propertySpecBuilder = PropertySpec.builder(field.simpleName.asString(), field.type.toTypeName())
            .initializer("${field.simpleName.getShortName()}Fake()")
            .addModifiers(KModifier.OVERRIDE)
        return propertySpecBuilder.build()
    }

    private fun TypeSpec.Builder.addOverride(from: KSClassDeclaration): TypeSpec.Builder {
        if (from.classKind == ClassKind.INTERFACE) {
            addSuperinterface(from.toClassName())
        } else {
            superclass(from.toClassName())
        }
        return this
    }

    private fun KSFunctionDeclaration.toDefaultParameter(): CodeBlock? {
        val returnType = returnType?.resolve()
        return when {
            returnType == null -> {
                logger.error("Couldn't resolve return type for function $simpleName")
                null
            }

            annotations.any { it.shortName.getShortName() == FakeReturnValue::class.simpleName } -> {
                // Get `value` parameter from FakeReturnValue
                val value = getFakeReturnValue()
                    ?: return null
                CodeBlock.of("{ ${lambdaParams()} $value }")
            }

            else -> {
                val defaultReturnValue = getReturnValue(returnType)
                CodeBlock.of("{ ${lambdaParams()}$defaultReturnValue }")
            }
        }
    }

    private fun KSPropertyDeclaration.toDefaultParameter(): CodeBlock? {
        val returnType = type.resolve()
        return when {
            annotations.any { it.shortName.getShortName() == FakeReturnValue::class.simpleName } -> {
                // Get `value` parameter from FakeReturnValue
                val value = getFakeReturnValue()
                    ?: return null
                CodeBlock.of("{ $value }")
            }

            else -> {
                val defaultReturnValue = getReturnValue(returnType)
                CodeBlock.of("{$defaultReturnValue}")
            }
        }
    }

    /**
     * Generate lambda parameters for functions with more than one parameter.
     * For example, for this function
     * ```
     * fun updateOrder(id: String, order: Order): Int?
     * ```
     * the generated code will be `_, _ ->`.
     *
     * If the function has only one parameter, return an empty string.
     * For example, for this function
     * ```
     * fun deleteOrder(id: String)
     * ```
     * the generated code will be an empty string.
     */
    private fun KSFunctionDeclaration.lambdaParams() = "${parameters.joinToString { "_" }} -> "
        .takeIf { parameters.size > 1 }
        .orEmpty()

    private fun KSDeclaration.getFakeReturnValue() =
        annotations.find { it.shortName.getShortName() == FakeReturnValue::class.simpleName }
            ?.arguments?.find { it.name?.asString() == FakeReturnValue::value.name }
            ?.value?.toString()
            ?.removeSurrounding("\"")

    /**
     * Recursively generate instances of custom classes.
     * For example, for this class
     * ```
     * data class Order(
     *     val id: String,
     *     val name: String,
     *     val price: Double,
     *     val quantity: Int,
     * )
     * ```
     * the generated code will be
     * ```
     * Order("", "", 0.0, 0)
     * ```
     */
    private fun generateCustomReturnValue(returnType: KSType): String {
        if (returnType.isMarkedNullable) {
            return "null"
        }

        val declaration = returnType.declaration as? KSClassDeclaration
        if (declaration == null) {
            logger.error("Couldn't resolve class declaration for type $returnType")
            return ""
        }

        val properties = declaration.declarations.filterIsInstance<KSPropertyDeclaration>()
            .filter { it.getVisibility() != Visibility.PRIVATE }
            .map { property ->
                val propertyType = property.type.resolve()
                val defaultValue = getReturnValue(propertyType)
                "${property.simpleName.getShortName()} = $defaultValue"
            }
            .joinToString(", ")
        return "${declaration.simpleName.getShortName()}($properties)"
    }

    private fun getReturnValue(returnType: KSType) = when (returnType.toTypeName()) {
        STRING -> "\"\""
        INT -> "0"
        DOUBLE -> "0.0"
        BOOLEAN -> "false"
        LONG -> "0L"
        CHAR -> "' '"
        BYTE -> "0.toByte()"
        SHORT -> "0.toShort()"
        FLOAT -> "0.0f"
        UNIT -> ""
        LIST -> "emptyList()"
        SET -> "emptySet()"
        MAP -> "emptyMap()"
        ARRAY -> "emptyArray()"
        else -> generateCustomReturnValue(returnType)
    }
}