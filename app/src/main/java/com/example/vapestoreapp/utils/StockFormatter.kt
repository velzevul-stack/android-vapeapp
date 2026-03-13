package com.example.vapestoreapp.utils

import com.example.vapestoreapp.data.Product

object StockFormatter {

    /**
     * @param reservedByProduct Map: productId -> зарезервированное количество
     */
    fun formatStockForCabinet(
        products: List<Product>,
        reservedByProduct: Map<Int, Int> = emptyMap()
    ): String {
        // Вычисляем доступное количество с учетом резервов
        // Резервы не списывают товар со склада, только скрывают из прайса
        val productsWithAvailableStock = products.map { product ->
            val reserved = reservedByProduct[product.id] ?: 0
            val availableStock = product.stock - reserved
            product to availableStock
        }.filter { (_, availableStock) -> availableStock > 0 }

        if (productsWithAvailableStock.isEmpty()) {
            return """
                📦⚡️Доставка от 5 до 20 минут⚡️📦
                ❗️ТОЛЬКО НАЛИЧКА❗️
                
                
                📦 Склад пуст
                Добавьте товары через экран 'Принять'
            """.trimIndent()
        }

        val stringBuilder = StringBuilder()

        // Заголовок с правильными отступами
        stringBuilder.append("📦⚡️Доставка от 5 до 20 минут⚡️📦\n")
        stringBuilder.append("❗️ТОЛЬКО НАЛИЧКА❗️\n\n\n")

        // Группируем по категориям - ВАЖНО: порядок категорий фиксирован!
        val liquids = productsWithAvailableStock
            .filter { (product, _) -> product.category == "liquid" }
            .sortedBy { (product, _) -> product.orderIndex } // Сортируем по orderIndex внутри категории

        val disposables = productsWithAvailableStock
            .filter { (product, _) -> product.category == "disposable" }
            .sortedBy { (product, _) -> product.orderIndex }

        val consumables = productsWithAvailableStock
            .filter { (product, _) -> product.category == "consumable" }
            .sortedBy { (product, _) -> product.orderIndex }

        val vapes = productsWithAvailableStock
            .filter { (product, _) -> product.category == "vape" }
            .sortedBy { (product, _) -> product.orderIndex }

        val snus = productsWithAvailableStock
            .filter { (product, _) -> product.category == "snus" }
            .sortedBy { (product, _) -> product.orderIndex }

        // ЖИДКОСТИ (всегда первые)
        if (liquids.isNotEmpty()) {
            formatLiquidsSection(liquids, stringBuilder, reservedByProduct)
            if (disposables.isNotEmpty() || consumables.isNotEmpty() || vapes.isNotEmpty() || snus.isNotEmpty()) {
                stringBuilder.append("\n——————————————————\n")
            }
        }

        // ОДНОРАЗКИ (вторые)
        if (disposables.isNotEmpty()) {
            formatDisposablesSection(disposables, stringBuilder, reservedByProduct)
            if (consumables.isNotEmpty() || vapes.isNotEmpty() || snus.isNotEmpty()) {
                stringBuilder.append("\n——————————————————\n")
            }
        }

        // РАСХОДНИКИ (третьи)
        if (consumables.isNotEmpty()) {
            formatConsumablesSection(consumables, stringBuilder, reservedByProduct)
            if (vapes.isNotEmpty() || snus.isNotEmpty()) {
                stringBuilder.append("\n——————————————————\n")
            }
        }

        // ВЕЙПЫ (четвертые)
        if (vapes.isNotEmpty()) {
            formatVapesSection(vapes, stringBuilder, reservedByProduct)
            if (snus.isNotEmpty()) {
                stringBuilder.append("\n——————————————————\n")
            }
        }

        // СНЮС (последние)
        if (snus.isNotEmpty()) {
            formatSnusSection(snus, stringBuilder, reservedByProduct)
        }

        // ИТОГО - считаем доступное количество с учетом резервов
        val totalItems = productsWithAvailableStock.sumOf { (_, availableStock) -> availableStock }
        stringBuilder.append("\n\n📊 ИТОГО: $totalItems шт.\n")

        return stringBuilder.toString()
    }

    private fun getCategoryOrder(category: String): Int {
        return when (category) {
            "liquid" -> 1
            "disposable" -> 2
            "consumable" -> 3
            "vape" -> 4
            "snus" -> 5
            else -> 6
        }
    }

    private fun formatLiquidsSection(
        liquids: List<Pair<Product, Int>>,
        stringBuilder: StringBuilder,
        reservedByProduct: Map<Int, Int>
    ) {
        // Группируем по брендам
        val groupedByBrand = liquids.groupBy { (product, _) -> product.brand }
        val brands = groupedByBrand.keys.toList()

        for ((index, brand) in brands.withIndex()) {
            val products = groupedByBrand[brand] ?: emptyList()

            if (products.isNotEmpty()) {
                val firstProduct = products.first().first
                val retailPrice = firstProduct.retailPrice

                // Заголовок бренда - ФИКС тут!
                val priceText = if (retailPrice > 0) " (${retailPrice.toInt()} BYN)" else ""
                stringBuilder.append("$brand:$priceText\n")

                // Сортируем по вкусам
                products.sortedBy { (product, _) -> product.flavor }.forEach { (product, availableStock) ->
                    val quantityText = if (availableStock > 1) " ($availableStock)" else ""
                    // Убираем mg из вкуса, если есть
                    val cleanFlavor = product.flavor
                        .replace("\\d+mg".toRegex(), "")
                        .replace("\\(.*?\\)".toRegex(), "") // Убираем (цифры)
                        .trim()
                    stringBuilder.append("• $cleanFlavor$quantityText\n")
                }

                // Два отступа между брендами
                if (index < brands.size - 1) {
                    stringBuilder.append("\n\n")
                }
            }
        }
    }

    private fun formatConsumablesSection(
        consumables: List<Pair<Product, Int>>,
        stringBuilder: StringBuilder,
        reservedByProduct: Map<Int, Int>
    ) {
        // Группируем по брендам
        val groupedByBrand = consumables.groupBy { (product, _) -> product.brand }
        val brands = groupedByBrand.keys.toList()

        for ((index, brand) in brands.withIndex()) {
            val products = groupedByBrand[brand] ?: emptyList()

            if (products.isNotEmpty()) {
                val firstProduct = products.first().first
                val retailPrice = firstProduct.retailPrice

                // ФИКС тут тоже!
                val priceText = if (retailPrice > 0) " (${retailPrice.toInt()} BYN)" else ""
                stringBuilder.append("$brand:$priceText\n")

                // Для расходников показываем только бренд, без спецификации
                val totalStock = products.sumOf { (_, availableStock) -> availableStock }
                if (totalStock > 1) {
                    stringBuilder.append("• ($totalStock)\n")
                }

                if (index < brands.size - 1) {
                    stringBuilder.append("\n\n")
                }
            }
        }
    }

    private fun formatVapesSection(
        vapes: List<Pair<Product, Int>>,
        stringBuilder: StringBuilder,
        reservedByProduct: Map<Int, Int>
    ) {
        // Группируем по брендам
        val groupedByBrand = vapes.groupBy { (product, _) -> product.brand }
        val brands = groupedByBrand.keys.toList()

        for ((index, brand) in brands.withIndex()) {
            val products = groupedByBrand[brand] ?: emptyList()

            if (products.isNotEmpty()) {
                val firstProduct = products.first().first
                val retailPrice = firstProduct.retailPrice

                // ФИКС тут тоже!
                val priceText = if (retailPrice > 0) " (${retailPrice.toInt()} BYN)" else ""
                stringBuilder.append("$brand:$priceText\n")

                products.forEach { (product, availableStock) ->
                    val quantityText = if (availableStock > 1) " ($availableStock)" else ""
                    // Показываем только цвет/модель без дублирования
                    val cleanSpec = product.specification
                        .replace("\\d+\\.\\d+".toRegex(), "") // Убираем цифры (0.2, 0.4 и т.д.)
                        .replace("\\(.*?\\)".toRegex(), "")
                        .replace("[🔋⚙️🍬🚬🫀]".toRegex(), "")
                        .trim()

                    if (cleanSpec.isNotEmpty() && !cleanSpec.matches(Regex("\\d+"))) {
                        stringBuilder.append("• $cleanSpec$quantityText\n")
                    } else {
                        stringBuilder.append("•$quantityText\n")
                    }
                }

                if (index < brands.size - 1) {
                    stringBuilder.append("\n\n")
                }
            }
        }
    }

    private fun formatDisposablesSection(
        disposables: List<Pair<Product, Int>>,
        stringBuilder: StringBuilder,
        reservedByProduct: Map<Int, Int>
    ) {
        // Группируем по брендам
        val groupedByBrand = disposables.groupBy { (product, _) -> product.brand }
        val brands = groupedByBrand.keys.toList()

        for ((index, brand) in brands.withIndex()) {
            val products = groupedByBrand[brand] ?: emptyList()

            if (products.isNotEmpty()) {
                val firstProduct = products.first().first
                val retailPrice = firstProduct.retailPrice

                // ФИКС тут тоже!
                val priceText = if (retailPrice > 0) " (${retailPrice.toInt()} BYN)" else ""
                stringBuilder.append("$brand:$priceText\n")

                products.forEach { (product, availableStock) ->
                    val quantityText = if (availableStock > 1) " ($availableStock)" else ""
                    val cleanFlavor = product.flavor
                        .replace("\\d+mg".toRegex(), "")
                        .replace("\\(.*?\\)".toRegex(), "")
                        .trim()
                    stringBuilder.append("• $cleanFlavor$quantityText\n")
                }

                if (index < brands.size - 1) {
                    stringBuilder.append("\n\n")
                }
            }
        }
    }

    private fun formatSnusSection(
        snus: List<Pair<Product, Int>>,
        stringBuilder: StringBuilder,
        reservedByProduct: Map<Int, Int>
    ) {
        // Группируем по брендам
        val groupedByBrand = snus.groupBy { (product, _) -> product.brand }
        val brands = groupedByBrand.keys.toList()

        for ((index, brand) in brands.withIndex()) {
            val products = groupedByBrand[brand] ?: emptyList()

            if (products.isNotEmpty()) {
                val firstProduct = products.first().first
                val retailPrice = firstProduct.retailPrice

                // ФИКС тут тоже!
                val priceText = if (retailPrice > 0) " (${retailPrice.toInt()} BYN)" else ""
                stringBuilder.append("$brand:$priceText\n")

                products.forEach { (product, availableStock) ->
                    val quantityText = if (availableStock > 1) " ($availableStock)" else ""
                    val cleanFlavor = product.flavor
                        .replace("\\d+mg".toRegex(), "")
                        .replace("\\(.*?\\)".toRegex(), "")
                        .trim()
                    stringBuilder.append("• $cleanFlavor$quantityText\n")
                }

                if (index < brands.size - 1) {
                    stringBuilder.append("\n\n")
                }
            }
        }
    }
}