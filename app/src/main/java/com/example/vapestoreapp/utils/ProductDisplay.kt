package com.example.vapestoreapp.utils

import com.example.vapestoreapp.data.Product

fun Product.displayName(): String {
    val brandPart = brand.trim()
    val flavorPart = flavor.trim()
    val specPart = specification.trim()

    return when (category) {
        "vape" -> {
            if (specPart.isNotBlank()) "$brandPart - $specPart" else brandPart
        }
        else -> {
            when {
                flavorPart.isNotBlank() && !flavorPart.equals(brandPart, ignoreCase = true) -> "$brandPart - $flavorPart"
                specPart.isNotBlank() -> "$brandPart - $specPart"
                else -> brandPart
            }
        }
    }
}

fun Product.displaySubtitle(): String? {
    val brandPart = brand.trim()
    val flavorPart = flavor.trim()
    val specPart = specification.trim()

    val subtitle = when (category) {
        "vape" -> specPart
        else -> flavorPart.ifBlank { specPart }
    }.trim()

    return subtitle
        .takeIf { it.isNotBlank() }
        ?.takeIf { !it.equals(brandPart, ignoreCase = true) }
}

