package org.example.common.enums

/**
 * 行程状态枚举
 */
enum class TripStatus(val code: Int, val description: String) {
    PLANNING(0, "规划中"),
    IN_PROGRESS(1, "进行中"), 
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");
    
    companion object {
        fun fromCode(code: Int): TripStatus? = values().find { it.code == code }
    }
}

/**
 * 路线难度枚举
 */
enum class RouteDifficulty(val level: Int, val description: String) {
    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难"),
    EXTREME(4, "极限");
    
    companion object {
        fun fromLevel(level: Int): RouteDifficulty? = values().find { it.level == level }
    }
}

/**
 * 路线状态枚举
 */
enum class RouteStatus(val description: String) {
    PLANNING("规划中"),
    ACTIVE("活跃"),
    COMPLETED("已完成"),
    ARCHIVED("已归档")
}

/**
 * 路线类型枚举
 */
enum class RouteType(val code: Int, val description: String) {
    LOOP(1, "环线"),
    POINT_TO_POINT(2, "点对点"),
    OUT_AND_BACK(3, "往返");
    
    companion object {
        fun fromCode(code: Int): RouteType? = values().find { it.code == code }
    }
}

/**
 * 路线方向枚举
 */
enum class RouteDirection(val code: Int, val description: String) {
    CLOCKWISE(1, "顺时针"),
    COUNTERCLOCKWISE(2, "逆时针"),
    BIDIRECTIONAL(3, "双向");
    
    companion object {
        fun fromCode(code: Int): RouteDirection? = values().find { it.code == code }
    }
}

/**
 * 季节适宜性枚举
 */
enum class SeasonSuitability(val description: String) {
    SPRING("春季"),
    SUMMER("夏季"),
    AUTUMN("秋季"),
    WINTER("冬季"),
    ALL_SEASONS("四季")
}

/**
 * 天气状况枚举
 */
enum class WeatherCondition(val description: String) {
    SUNNY("晴朗"),
    CLOUDY("多云"),
    RAINY("雨天"),
    SNOWY("雪天"),
    FOGGY("雾天"),
    STORMY("雷雨");
    
    companion object {
        fun fromString(condition: String): WeatherCondition? {
            return values().find { 
                it.name.equals(condition, ignoreCase = true) || 
                it.description == condition 
            }
        }
    }
}

/**
 * 隐私设置枚举
 */
enum class PrivacySetting(val description: String) {
    PUBLIC("公开"),
    PRIVATE("私有"),
    FRIENDS_ONLY("仅好友")
}