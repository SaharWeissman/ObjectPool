package com.saharw.annotationprocessor

import com.google.auto.service.AutoService
import com.saharw.annotationprocessor.pooled.Pooled
import com.saharw.objectpool.core.ObjectPool
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class Processor : AbstractProcessor() {

    val TAG = "PooledProcessor"

    // required methods to generate
    private val CREATE_METHOD_NAME = "create"
    private val VALIDATE_METHOD_NAME = "validate"
    private val EXPIRE_METHOD_NAME = "expire"

    // generated args / file values
    private val FIRST_ARG_NAME = "arg1"
    private val KOTLIN_FILE_EXTENSION: String = ".kt"
    val POOLED_CLASS_PERFIX = "Pooled"

    // error msgs
    private val ERR_MSG_MISSING_METHOD = "make sure method exist, has no arguments & static"

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    /**
     * this method is used to define which annotation types this processor can handle
     */
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        println("getSupportedAnnotationTypes")
        return mutableSetOf(Pooled::class.java.name)
    }

    /**
     * specify which source version (e.g. java version) is supported by this annotation processor.
     * apparently leaving a default value is bad practice - better to specify value / state latest
     */
    override fun getSupportedSourceVersion(): SourceVersion {
        println("getSupportedSourceVersion")
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Pooled::class.java)
                ?.forEach({
                    println("$TAG: processing ${it.simpleName}")
                    generateObjectPoolClass(processingEnv, it)
                })
        return true
    }

    private fun generateObjectPoolClass(processingEnv: ProcessingEnvironment, annotatedElement: Element?) {
        if(annotatedElement != null){
            val packName = processingEnv.elementUtils.getPackageOf(annotatedElement).toString()
            val className = annotatedElement.simpleName.toString()
            val newClassName = "$POOLED_CLASS_PERFIX$className"
            val annotation = annotatedElement.getAnnotation(Pooled::class.java)

            // create the subclass initial skeleton
            val pooledType = TypeVariableName.invoke(annotatedElement.simpleName.toString(), annotatedElement.javaClass)
            val fileBuilder = FileSpec.builder(packName, newClassName)
                    .addType(TypeSpec.classBuilder(newClassName)

                            .addSuperclassConstructorParameter("%L", annotation.expirationTime)
                            .addSuperclassConstructorParameter("%L", annotation.lockedInitialCap)
                            .addSuperclassConstructorParameter("%L", annotation.unlockedInitialCap)

                            // 1. generate 'create' method
                            .addFunction(generateMethod(processingEnv, annotatedElement, CREATE_METHOD_NAME))

                            //2. generate 'validate' method
                            .addFunction(generateMethod(processingEnv, annotatedElement, VALIDATE_METHOD_NAME,
                                    mutableMapOf(Pair(Pair(FIRST_ARG_NAME, pooledType), FIRST_ARG_NAME))
                            ))

                            //2. generate 'expire' method
                            .addFunction(generateMethod(processingEnv, annotatedElement, EXPIRE_METHOD_NAME,
                                    mutableMapOf(Pair(Pair(FIRST_ARG_NAME, pooledType), FIRST_ARG_NAME))
                            ))

                            .superclass(ParameterizedTypeName.get(ClassName.bestGuess(ObjectPool::class.java.canonicalName!!), pooledType))
                            .build())


            var file = fileBuilder.build()
            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            file.writeTo(File(kaptKotlinGeneratedDir, "$newClassName$KOTLIN_FILE_EXTENSION"))
        }else {
            error("generateObjectPoolClass: annotated element is null")
        }
    }

    /**
     * TODO:
     * 1. add validation - check 'create' method exist as companion object method
     * 2. add prints for logging & throw appropriate exceptions in case method is missing
     *
     */
    private fun generateMethod(processingEnv: ProcessingEnvironment, annotatedElement: Element?, methodName: String, mirroredArgs: Map<Pair<String,TypeName>, String>? = null) : FunSpec {
        println("running generateMethod(\"$methodName\")")
        var funcSpecBuilder : FunSpec.Builder
        if(annotatedElement != null) {

            // first validate method exist
            if(validateNoArgsStaticMethodExist(processingEnv, annotatedElement, methodName)) {

                if(mirroredArgs == null || mirroredArgs.isEmpty()) {
                    println("running generateMethod(\"$methodName\"): no args")
                    funcSpecBuilder = FunSpec.builder(methodName).addModifiers(KModifier.OVERRIDE)
                            .addStatement("return ${TypeVariableName.invoke(annotatedElement.simpleName.toString(), annotatedElement.javaClass)}.$methodName()")
                }


                // check if there are args to be mirrored => declare in generated method & pass to original method
                else{
                    println("running generateMethod(\"$methodName\"): with args map of ${mirroredArgs.size}")
                    funcSpecBuilder = FunSpec.builder(methodName).addModifiers(KModifier.OVERRIDE)

                    var returnStatementSb = StringBuilder()

                    returnStatementSb.append("return ${TypeVariableName.invoke(annotatedElement.simpleName.toString(), annotatedElement.javaClass)}.$methodName(")

                    mirroredArgs.forEach { pair, argName ->
                        funcSpecBuilder.addParameter(pair.first, pair.second)
                        returnStatementSb.append("$argName,")
                    }

                    // remove last ','
                    returnStatementSb.deleteCharAt(returnStatementSb.length-1)
                    returnStatementSb.append(")")
                    funcSpecBuilder.addStatement(returnStatementSb.toString())
                }

                return funcSpecBuilder.build()
            }else {
                throw NotImplementedError("class ${annotatedElement.simpleName} is missing required method: \"$methodName\", $ERR_MSG_MISSING_METHOD")
            }
        }else {
            throw IllegalArgumentException("given annotated element is null!")
        }
    }

    private fun validateNoArgsStaticMethodExist(processingEnv: ProcessingEnvironment, annotatedElement: Element, methodName: String) : Boolean {
        //TODO: need to find out how to load class in reflection since class is not part of this project, migth require creating custom 'ClassLoader'...?
        return true
    }
}