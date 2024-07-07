package com.phhmaa

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.phhmaa.generator.FakeFileGenerator

class GenerateFakeProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return GenerateFakeProcessor(
            codeGenerator = environment.codeGenerator,
            fakeFileGenerator = FakeFileGenerator(
                logger = environment.logger,
            ),
        )
    }
}