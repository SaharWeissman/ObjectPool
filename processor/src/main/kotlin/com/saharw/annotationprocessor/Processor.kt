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
    val POOLED_CLASS_PERFIX = "Pooled"
    private val CREATE_METHOD_NAME = "create"

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
            val newClassName = "${POOLED_CLASS_PERFIX}_$className"
            val annotation = annotatedElement.getAnnotation(Pooled::class.java)

            // create the subclass initial skeleton
            val pooledType = TypeVariableName.invoke(annotatedElement.simpleName.toString(), annotatedElement.javaClass)
            val fileBuilder = FileSpec.builder(packName, newClassName)
                    .addType(TypeSpec.classBuilder(newClassName)

                            .addSuperclassConstructorParameter("%L", annotation.expirationTime)
                            .addSuperclassConstructorParameter("%L", annotation.lockedInitialCap)
                            .addSuperclassConstructorParameter("%L", annotation.unlockedInitialCap)

                            // generate 'create' method
                            .addFunction(generateCreateMethod(annotatedElement))
                            .superclass(ParameterizedTypeName.get(ClassName.bestGuess(ObjectPool::class.java.canonicalName!!), pooledType))
                            .build())


            var file = fileBuilder.build()
            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            file.writeTo(File(kaptKotlinGeneratedDir, "$newClassName.kt"))
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
    private fun generateCreateMethod(annotatedElement: Element?) : FunSpec {
        println("running addCreateMethod")

        if(annotatedElement != null) {
            return FunSpec.builder(CREATE_METHOD_NAME).addModifiers(KModifier.OVERRIDE)
                    .addStatement("return ${TypeVariableName.invoke(annotatedElement.simpleName.toString(), annotatedElement.javaClass)}.create()")
                    .build()
        }else {
            throw IllegalArgumentException("given annotated element is null!")
        }
    }
}