package com.example.vapestoreapp.utils

import android.content.Context
import com.example.vapestoreapp.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExcelImporter {

    suspend fun importFromExcel(context: Context): List<Product> = withContext(Dispatchers.IO) {
        println("🔄 Использую хардкод-данные вместо Excel")

        val products = mutableListOf<Product>()

        try {
            // ==================== ЖИДКОСТИ ====================

            // 🍬 PODONKI CRITICAL 50 mg
            addLiquidProducts(
                products = products,
                brand = "🍬PODONKI CRITICAL 50 mg🍬",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "АПЕЛЬСИНОВОЕ ДРАЖЕ" to "4617586894280",
                    "МАНГО АПЕЛЬСИН" to "4617586894372",
                    "КИСЛЫЙ МАЛИНОВЫЙ ЛИМОНАД" to "4617586894297",
                    "ЯБЛОЧНЫЙ ХОЛЛС" to "4617586894273",
                    "КИСЛЫЙ КИВИ" to "4617586894334",
                    "ЛЕДЯНОЙ АНАНАС" to "4617586894242"
                )
            )

            // 🤪 PODONKI SOUR 50mg
            addLiquidProducts(
                products = products,
                brand = "🤪PODONKI SOUR 50 mg🤪",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "МАЛИНОВЫЙ ЛИМОНАД" to "4617586897618",
                    "ЖВАЧКА ЯБЛОКО" to "4617586897533",
                    "ПЕРСИКОВОЕ ЖЕЛЕ С ЛИМОНОМ" to "4617586897649",
                    "ГОЛУБИКА ЕЖЕВИКА" to "4617586897694"
                )
            )

            // 🌋 PODONKI & MALAYSIAN ARCADE 50mg
            addLiquidProducts(
                products = products,
                brand = "🌋PODONKI & MALAYSIAN ARCADE 50 mg🌋",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "КЛУБНИКА АНАНАСОВЫЕ КОЛЬЦА" to "4609106003418"
                )
            )

            // 🧭 PODONKI PODGON 50mg
            addLiquidProducts(
                products = products,
                brand = "🧭PODONKI PODGON 50 mg🧭",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ГРЕЙПФРУТОВЫЙ ЛИМОНАД" to "4695898782815",
                    "ПЕРСИКОВЫЙ ЙОГУРТ" to "4615897152426",
                    "БРУСНИКА В САХАРЕ" to "4615897152433",
                    "ЗЕЛЕНЫЙ МАНГО" to "4695898782761",
                    "СМОРОДИНА МАЛИНА" to "4695898782709",
                    "ДЫНЯ БАНАН" to "4695898782716"
                )
            )

            // ⭐️ PODONKI × LIGHT 50 mg
            addLiquidProducts(
                products = products,
                brand = "⭐️PODONKI × LIGHT 50 mg⭐️",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ЛИМОННЫЙ МАРМЕЛАД С ПОСЫПКОЙ" to "4695898782587"
                )
            )

            // 🤯 PODONKI × ISTERIKA 50 mg
            addLiquidProducts(
                products = products,
                brand = "🤯PODONKI × ISTERIKA 50 mg🤯",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ГРУШЕВЫЙ ЛИМОНАД" to "9780445341524",
                    "АПЕЛЬСИНОВЫЙ ФИЗЗ" to "9780445311237"
                )
            )

            // 🤩 PODONKI × LASTHAP 60mg
            addLiquidProducts(
                products = products,
                brand = "🤩PODONKI × LASTHAP 60 mg🤩",
                strength = "60",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ВАФЕЛЬНЫЙ СТАКАНЧИК С КЛУБНИЧНЫМ СИРОПОМ" to "9750501312652"
                )
            )

            // 🚔 RICK N MORTY BAD TRIP 50mg
            addLiquidProducts(
                products = products,
                brand = "🚔RICK N MORTY BAD TRIP 50 mg🚔",
                strength = "50",
                purchasePrice = 9.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ГРЕЙПФРУТОВЫЙ ШВЕПС" to "9780201315561",
                    "БАНАН КОКОС" to "9780201325065",
                    "КОЛА ЛАЙМ" to "9780256386523",
                    "МАНГО, ПЕРСИК, ЛИЧИ" to "9780201975109",
                    "КЛУБНИКА ВИШНЯ" to "9780256384994",
                    "ТАРХУН" to "9780401373194",
                    "ТРОПИЧЕСКИЙ МАНГО" to "9780990356882",
                    "ЛИМОННЫЙ ЛОЛЛИПОП" to "9780258328576",
                    "ПЕРСИКОВЫЙ ЛИМОНАД" to "9780258376928",
                    "ЯБЛОКО ЛАЙМ" to "9780990342557",
                    "КЛУБНИЧНЫЙ МОХИТО" to "9780201334562",
                    "МАРМЕЛАДНЫЕ ЧЕРВЯЧКИ ЧЕРНИКА МАЛИНА" to "9780990399032",
                    "ВИШНЕВАЯ БОМБА" to "9780990345121",
                    "МАЛИНА СМОРОДИНА" to "9780201307887",
                    "ВИНОГРАДНЫЙ ЧУПА-ЧУПС" to "9780990346111",
                    "ФРУКТОВЫЙ СКИТЛС" to "9780990356486",
                    "ЧЕРНИЧНАЯ ФАЕТА" to "9780990353669"
                )
            )

            // 🧊 RICK N MORTY НА ЗАМЕРЗОНЕ 20mg
            addLiquidProducts(
                products = products,
                brand = "🧊RICK N MORTY НА ЗАМЕРЗОНЕ 20 mg🧊",
                strength = "20",
                purchasePrice = 9.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ЯБЛОЧНЫЙ ЛЕДЕНЕЦ" to "4617586898721",
                    "СКИТЛС ЛИМОН ЛАЙМ" to "4617586898769",
                    "СПРАЙТ АРБУЗ ЛАЙМ" to "4617586898844",
                    "ГОЛУБИКА, МАЛИНА, МАРМЕЛАД" to "4617586898899",
                    "СМОРОДИНА ЛАЙМ" to "4617586898714",
                    "ЛЕСНЫЕ ЯГОДЫ" to "4617586898837",
                    "ЯГОДНЫЙ ВЗРЫВ" to "4617586898707",
                    "ЯГОДНЫЙ ПУНШ" to "4617586898974",
                    "ВИШНЕВЫЙ ЛИМОНАД" to "4617586898813",
                    "МАЛИНОВАЯ ШИПУЧКА" to "4617586898875",
                    "КРАСНЫЙ ВИНОГРАД" to "4617586898745",
                    "ЧЕРНИЧНО-МАЛИНОВЫЕ ЧЕРВЯЧКИ" to "4617586898868"
                )
            )

            // 🐈 CATSWILL EXTRA 50mg
            addLiquidProducts(
                products = products,
                brand = "🐈CATSWILL EXTRA 50 mg🐈",
                strength = "50",
                purchasePrice = 9.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "КЛУБНИКА, ЛЁД, ЛИЧИ" to "4690628372533",
                    "ВИШНЯ МЯТА БАБЛГАМ" to "4690628372243",
                    "ЧЕРНЫЙ ВИНОГРАД АПЕЛЬСИН" to "4690628372151",
                    "ВИНОГРАД, СМОРОДИНА, СКИТЛС" to "4690628372175",
                    "КИСЛЫЕ ЛЕНТОЧКИ КЛУБНИКА КИВИ" to "4690628372397",
                    "СКИТЛС МАЛИНА КЛУБНИКА" to "4690628372410",
                    "ЧЕРНИКА МАЛИНА" to "4690628372304",
                    "ЕЖЕВИКА, ВИНОГРАД, МЕНТОС" to "4690628372113",
                    "ВИНОГРАД ДЫНЯ" to "4690628372106",
                    "КИСЛЫЙ АНАНАС ЛИМОН" to "4690628372328",
                    "ХУББА-БУББА КЛУБНИКА, ЧЕРНИКА, АРБУЗ" to "4690628372700",
                    "КИСЛЫЙ ВИНОГРАД КЛУБНИКА" to "4690628372595",
                    "МОРОЗНАЯ ЧЕРНИКА ЛИМОН" to "4690628372526"
                )
            )

            // 🐈 CATSWILL 50mg
            addLiquidProducts(
                products = products,
                brand = "🐈CATSWILL 50 mg🐈",
                strength = "50",
                purchasePrice = 9.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ЧЕРНИЧНО-АРБУЗНАЯ ХУББА-БУББА" to "9780201723403",
                    "КЛУБНИКА КИВИ" to "9780201400274",
                    "АЛОЭ ВИНОГРАД" to "9780201400120",
                    "ВИШНЯ, ПЕРСИК, МЯТА" to "9780201723380",
                    "ЯБЛОЧНО-ЛИМОННЫЕ КОЛЕЧКИ" to "4600873461104",
                    "ГАЗИРОВКА С ЧЕРНОЙ СМОРОДИНОЙ И КЛЮКВОЙ" to "4600873461012",
                    "ФАНТА С ЧЕРНИКОЙ" to "4600873461029",
                    "ДОЛЬКИ С ПЕРСИКОМ И КЛУБНИКОЙ" to "4600873461067",
                    "ЛЕДЕНЕЦ С ВИНОГРАДОМ, КИВИ И ЯБЛОКОМ" to "4600873461135",
                    "ВИНОГРАД С ЛАЙМОМ" to "4600873461111"
                )
            )

            // ☠️ САМОУБИЙЦА V2 DANGER 80mg
            addLiquidProducts(
                products = products,
                brand = "☠️САМОУБИЙЦА V2 DANGER 80 mg☠️",
                strength = "80",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "ЗЕЛЁНЫЙ СКИТЛС" to "4685601319360",
                    "АНАНАС, МАЛИНА, БАНАН" to "4685601311562",
                    "СМОРОДИНА, ЛИЧИ, ЛЁД" to "4685601329161",
                    "ВИШНЁВЫЙ ЙОГУРТ" to "4685601314440",
                    "НЕКТАРИН, МАНГО, АРБУЗ" to "4685601328096",
                    "ЯБЛОКО КЛУБНИКА" to "4685601330259",
                    "МАЛИНА, КЛЮКВА, ЛЁД" to "4685601326016"
                )
            )

            // 🔥 ИНДИВИDUALL 50mg
            addLiquidProducts(
                products = products,
                brand = "🔥ИНДИВИDUALL 50 mg🔥",
                strength = "50",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                flavors = listOf(
                    "МОРОЖЕНОЕ ЗЕМЛЯНИКА МАЛИНА" to "7930114215386",
                    "МАЛИНОВЫЙ ЛИМОНАД" to "7930114252725",
                    "МАРМЕЛАДНЫЕ МИШКИ" to "7930114215164",
                    "КИСЛЫЕ ЦИТРУСОВЫЕ ДОЛЬКИ" to "7930114252886"
                )
            )

            // ==================== РАСХОДНИКИ ====================

            // ⚙️ Испаритель aegis boost 0.2 (50-58W)
            products.add(
                Product(
                    brand = "⚙️Испаритель aegis boost 0.2 (50-58W)⚙️",
                    flavor = "Испаритель aegis boost 0.2 (50-58W)",
                    barcode = "6978271531360",
                    purchasePrice = 6.4,
                    retailPrice = 13.0,
                    stock = 0,
                    category = "consumable",
                    strength = "",
                    specification = "0.2"
                )
            )

            // ⚙️ Картридж xros 0.4 3ml
            products.add(
                Product(
                    brand = "⚙️Картридж xros 0.4 3ml⚙️",
                    flavor = "Картридж xros 0.4 3ml",
                    barcode = "6943498697458",
                    purchasePrice = 6.4,
                    retailPrice = 13.0,
                    stock = 0,
                    category = "consumable",
                    strength = "",
                    specification = "0.4"
                )
            )

            // ⚙️ Картридж xros 0.6 2ml
            products.add(
                Product(
                    brand = "⚙️Картридж xros 0.6 2ml⚙️",
                    flavor = "Картридж xros 0.6 2ml",
                    barcode = "6943498697434",
                    purchasePrice = 6.4,
                    retailPrice = 13.0,
                    stock = 0,
                    category = "consumable",
                    strength = "",
                    specification = "0.6"
                )
            )

            // ⚙️ Картридж xros 0.6 3ml
            products.add(
                Product(
                    brand = "⚙️Картридж xros 0.6 3ml⚙️",
                    flavor = "Картридж xros 0.6 3ml",
                    barcode = "6943498659067",
                    purchasePrice = 6.4,
                    retailPrice = 13.0,
                    stock = 0,
                    category = "consumable",
                    strength = "",
                    specification = "0.6"
                )
            )

            // ⚙️ Картридж voopoo vmate 0.7 3ml
            products.add(
                Product(
                    brand = "⚙️Картридж voopoo vmate 0.7 3ml⚙️",
                    flavor = "Картридж voopoo vmate 0.7 3ml",
                    barcode = "6941291559744",
                    purchasePrice = 6.4,
                    retailPrice = 13.0,
                    stock = 0,
                    category = "consumable",
                    strength = "",
                    specification = "0.7"
                )
            )

            // ==================== ВЕЙПЫ ====================

            // 🔋 XROS 5 MINI (цвета)
            addVapeProducts(
                products = products,
                brand = "🔋XROS 5 MINI🔋",
                purchasePrice = 40.0,
                retailPrice = 65.0,
                colors = listOf(
                    "PURPLE" to "6943498636129",
                    "PASTEL CRYSTAL" to "6943498636075"
                )
            )

            // 🔋 XROS 5 LEATHER
            products.add(
                Product(
                    brand = "🔋XROS 5 LEATHER🔋",
                    flavor = "XROS 5 LEATHER",
                    barcode = null,
                    purchasePrice = 40.0,
                    retailPrice = 100.0,
                    stock = 0,
                    category = "vape",
                    strength = "",
                    specification = "LEATHER"
                )
            )

            println("🎉 Хардкод-импорт завершен. Добавлено ${products.size} товаров")

        } catch (e: Exception) {
            println("🔥 Ошибка в хардкод-импорте: ${e.message}")
            e.printStackTrace()

            // Если что-то пошло не так, вернем хотя бы минимум
            return@withContext createFallbackProducts()
        }

        return@withContext products
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

    private fun addLiquidProducts(
        products: MutableList<Product>,
        brand: String, // Уже содержит эмодзи
        strength: String,
        purchasePrice: Double,
        retailPrice: Double,
        flavors: List<Pair<String, String>>
    ) {
        flavors.forEachIndexed {index, (flavor, barcode) ->
            val cleanFlavor = flavor.replace("\\(\\d+\\)".toRegex(), "")
                .replace("\\d+mg".toRegex(), "")
                .trim()

            products.add(
                Product(
                    brand = brand, // Используем бренд как есть с эмодзи
                    flavor = cleanFlavor,
                    barcode = barcode,
                    purchasePrice = purchasePrice,
                    retailPrice = retailPrice,
                    stock = 0,
                    category = "liquid",
                    strength = strength,
                    specification = "",
                    orderIndex = index + 1
                )
            )
        }
    }

    private fun addVapeProducts(
        products: MutableList<Product>,
        brand: String, // Уже содержит эмодзи
        purchasePrice: Double,
        retailPrice: Double,
        colors: List<Pair<String, String>>
    ) {
        colors.forEachIndexed {index, (color, barcode) ->
            val cleanColor = color.replace("\\(\\d+\\)".toRegex(), "").trim()

            products.add(
                Product(
                    brand = brand, // Используем бренд как есть с эмодзи
                    flavor = "${brand.replace("🔋", "").trim()} $cleanColor",
                    barcode = barcode,
                    purchasePrice = purchasePrice,
                    retailPrice = retailPrice,
                    stock = 0,
                    category = "vape",
                    strength = "",
                    specification = cleanColor,
                    orderIndex = index + 1
                )
            )
        }
    }

    private fun createFallbackProducts(): List<Product> {
        println("📋 Создаю резервные товары...")

        return listOf(
            Product(
                brand = "🍬PODONKI CRITICAL 50 mg🍬",
                flavor = "АПЕЛЬСИНОВОЕ ДРАЖЕ",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                stock = 5,
                category = "liquid",
                strength = "50"
            ),
            Product(
                brand = "🤪PODONKI SOUR 50 mg🤪",
                flavor = "МАЛИНОВЫЙ ЛИМОНАД",
                purchasePrice = 8.0,
                retailPrice = 15.0,
                stock = 3,
                category = "liquid",
                strength = "50"
            ),
            Product(
                brand = "⚙️Испаритель aegis boost 0.2 (50-58W)⚙️",
                flavor = "Испаритель aegis boost 0.2 (50-58W)",
                purchasePrice = 6.4,
                retailPrice = 13.0,
                stock = 10,
                category = "consumable",
                strength = "",
                specification = "0.2"
            ),
            Product(
                brand = "🔋XROS 5 MINI🔋",
                flavor = "XROS 5 MINI PURPLE",
                purchasePrice = 40.0,
                retailPrice = 65.0,
                stock = 2,
                category = "vape",
                strength = "",
                specification = "PURPLE"
            )
        )
    }
}