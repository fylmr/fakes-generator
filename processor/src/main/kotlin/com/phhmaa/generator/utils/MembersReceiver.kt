package com.phhmaa.generator.utils

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility

internal fun KSClassDeclaration.getFunctions() = getAllFunctions()
    .filter { it.getVisibility() != Visibility.PRIVATE }
    .filterAnyTypeInheritance()
    .asIterable()

internal fun KSClassDeclaration.getProperties() = getAllProperties()
    .filter { it.getVisibility() != Visibility.PRIVATE }
    .asIterable()

private fun Sequence<KSFunctionDeclaration>.filterAnyTypeInheritance() =
    filter { it.simpleName.getShortName() != "equals" }
        .filter { it.simpleName.getShortName() != "hashCode" }
        .filter { it.simpleName.getShortName() != "toString" }