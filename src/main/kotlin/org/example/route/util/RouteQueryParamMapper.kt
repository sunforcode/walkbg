package org.example.route.util

/**
 * 路线查询参数映射工具
 * 
 * 提供统一的参数抽象，将前端的抽象参数映射到后端的具体查询条件
 */
object RouteQueryParamMapper {

    // ==================== 路线类别映射 ====================
    /**
     * 路线类别到标签的映射
     * 前端的 category 参数可以映射到对应的标签
     */
    private val categoryToTags = mapOf(
        "hiking" to listOf("徒步", "登山", "健行"),
        "cycling" to listOf("骑行", "自行车", "单车"),
        "camping" to listOf("露营", "野营", "营地"),
        "climbing" to listOf("攀岩", "登山", "攀爬"),
        "urban" to listOf("城市", "城市漫步", "城市探索"),
        "mountain" to listOf("山地", "高山", "山峰"),
        "coastal" to listOf("海滨", "海边", "沿海", "海景")
    )

    /**
     * 中文类别名称到英文 category 的映射
     */
    private val chineseCategoryToEnglish = mapOf(
        "徒步" to "hiking",
        "骑行" to "cycling",
        "露营" to "camping",
        "攀岩" to "climbing",
        "城市" to "urban",
        "山地" to "mountain",
        "海滨" to "coastal"
    )

    // ==================== 难度映射 ====================
    /**
     * 难度字符串到数字的映射
     */
    private val difficultyStringToInt = mapOf(
        "easy" to 1,
        "简单" to 1,
        "medium" to 3,
        "中等" to 3,
        "hard" to 5,
        "困难" to 5
    )

    // ==================== 路线类型映射 ====================
    /**
     * 路线类型字符串到数字的映射
     */
    private val routeTypeStringToInt = mapOf(
        "roundtrip" to 0,
        "往返" to 0,
        "loop" to 1,
        "环线" to 1,
        "oneway" to 2,
        "单程" to 2,
        "multiday" to 3,
        "多日" to 3
    )

    // ==================== 排序映射 ====================
    /**
     * 排序参数到排序字段的映射
     */
    sealed class SortOption {
        object Popular : SortOption()
        object New : SortOption()
        object Distance : SortOption()
    }

    private val sortStringToOption = mapOf(
        "popular" to SortOption.Popular,
        "热门" to SortOption.Popular,
        "new" to SortOption.New,
        "最新" to SortOption.New,
        "distance" to SortOption.Distance,
        "距离" to SortOption.Distance
    )

    // ==================== 公开方法 ====================

    /**
     * 根据 category 获取对应的标签列表
     */
    fun getTagsForCategory(category: String?): List<String> {
        if (category.isNullOrBlank()) return emptyList()
        
        // 先尝试直接使用 category 作为英文 key
        val tags = categoryToTags[category.lowercase()]
        if (tags != null) return tags
        
        // 再尝试将中文类别转换为英文 key
        val englishCategory = chineseCategoryToEnglish[category]
        if (englishCategory != null) {
            return categoryToTags[englishCategory] ?: emptyList()
        }
        
        return emptyList()
    }

    /**
     * 解析难度参数（支持字符串和数字）
     */
    fun parseDifficulty(difficulty: Any?): Int? {
        return when (difficulty) {
            is Int -> difficulty
            is String -> {
                difficultyStringToInt[difficulty.lowercase()] 
                    ?: difficulty.toIntOrNull()
            }
            else -> null
        }
    }

    /**
     * 解析路线类型参数（支持字符串和数字）
     */
    fun parseRouteType(routeType: Any?): Int? {
        return when (routeType) {
            is Int -> routeType
            is String -> {
                routeTypeStringToInt[routeType.lowercase()]
                    ?: routeType.toIntOrNull()
            }
            else -> null
        }
    }

    /**
     * 解析排序参数
     */
    fun parseSort(sort: String?): SortOption {
        if (sort.isNullOrBlank()) return SortOption.Popular
        return sortStringToOption[sort.lowercase()] ?: SortOption.Popular
    }

    /**
     * 解析标签参数字符串（逗号分隔）
     */
    fun parseTags(tagsString: String?): List<String> {
        if (tagsString.isNullOrBlank()) return emptyList()
        return tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * 获取所有支持的类别
     */
    fun getAllCategories(): List<String> {
        return categoryToTags.keys.toList()
    }

    /**
     * 获取所有支持的中文类别名称
     */
    fun getAllChineseCategories(): List<String> {
        return chineseCategoryToEnglish.keys.toList()
    }

    /**
     * 检查类别是否有效
     */
    fun isValidCategory(category: String?): Boolean {
        if (category.isNullOrBlank()) return false
        val lowerCategory = category.lowercase()
        return categoryToTags.containsKey(lowerCategory) 
            || chineseCategoryToEnglish.containsKey(category)
    }
}
