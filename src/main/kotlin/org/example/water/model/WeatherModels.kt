package org.example.water.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 和风天气API响应基类
 */
@JsonIgnoreProperties(ignoreUnknown = true)
open class WeatherApiResponse {
    @JsonProperty("code")
    var code: String = ""
    
    @JsonProperty("updateTime")
    var updateTime: String = ""
    
    @JsonProperty("fxLink")
    var fxLink: String = ""
}

/**
 * 天气预报响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherForecastResponse : WeatherApiResponse() {
    @JsonProperty("daily")
    var daily: List<DailyForecast> = emptyList()
}

/**
 * 每日天气预报
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DailyForecast {
    @JsonProperty("fxDate")
    var fxDate: String = ""
    
    @JsonProperty("sunrise")
    var sunrise: String = ""
    
    @JsonProperty("sunset")
    var sunset: String = ""
    
    @JsonProperty("moonrise")
    var moonrise: String = ""
    
    @JsonProperty("moonset")
    var moonset: String = ""
    
    @JsonProperty("moonPhase")
    var moonPhase: String = ""
    
    @JsonProperty("tempMax")
    var tempMax: String = ""
    
    @JsonProperty("tempMin")
    var tempMin: String = ""
    
    @JsonProperty("iconDay")
    var iconDay: String = ""
    
    @JsonProperty("textDay")
    var textDay: String = ""
    
    @JsonProperty("iconNight")
    var iconNight: String = ""
    
    @JsonProperty("textNight")
    var textNight: String = ""
    
    @JsonProperty("wind360Day")
    var wind360Day: String = ""
    
    @JsonProperty("windDirDay")
    var windDirDay: String = ""
    
    @JsonProperty("windScaleDay")
    var windScaleDay: String = ""
    
    @JsonProperty("windSpeedDay")
    var windSpeedDay: String = ""
    
    @JsonProperty("wind360Night")
    var wind360Night: String = ""
    
    @JsonProperty("windDirNight")
    var windDirNight: String = ""
    
    @JsonProperty("windScaleNight")
    var windScaleNight: String = ""
    
    @JsonProperty("windSpeedNight")
    var windSpeedNight: String = ""
    
    @JsonProperty("humidity")
    var humidity: String = ""
    
    @JsonProperty("precip")
    var precip: String = ""
    
    @JsonProperty("pressure")
    var pressure: String = ""
    
    @JsonProperty("vis")
    var vis: String = ""
    
    @JsonProperty("cloud")
    var cloud: String = ""
    
    @JsonProperty("uvIndex")
    var uvIndex: String = ""
}

/**
 * 实时天气响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherNowResponse : WeatherApiResponse() {
    @JsonProperty("now")
    var now: WeatherNow = WeatherNow()
}

/**
 * 实时天气
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherNow {
    @JsonProperty("obsTime")
    var obsTime: String = ""
    
    @JsonProperty("temp")
    var temp: String = ""
    
    @JsonProperty("feelsLike")
    var feelsLike: String = ""
    
    @JsonProperty("icon")
    var icon: String = ""
    
    @JsonProperty("text")
    var text: String = ""
    
    @JsonProperty("wind360")
    var wind360: String = ""
    
    @JsonProperty("windDir")
    var windDir: String = ""
    
    @JsonProperty("windScale")
    var windScale: String = ""
    
    @JsonProperty("windSpeed")
    var windSpeed: String = ""
    
    @JsonProperty("humidity")
    var humidity: String = ""
    
    @JsonProperty("precip")
    var precip: String = ""
    
    @JsonProperty("pressure")
    var pressure: String = ""
    
    @JsonProperty("vis")
    var vis: String = ""
    
    @JsonProperty("cloud")
    var cloud: String = ""
    
    @JsonProperty("dew")
    var dew: String = ""
}

/**
 * 分钟级降水响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MinutelyPrecipitationResponse : WeatherApiResponse() {
    @JsonProperty("summary")
    var summary: String = ""
    
    @JsonProperty("minutely")
    var minutely: List<MinutelyPrecipitation> = emptyList()
}

/**
 * 分钟级降水
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MinutelyPrecipitation {
    @JsonProperty("fxTime")
    var fxTime: String = ""
    
    @JsonProperty("precip")
    var precip: String = ""
    
    @JsonProperty("type")
    var type: String = ""
}

/**
 * 天气预警响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherWarningResponse : WeatherApiResponse() {
    @JsonProperty("warning")
    var warning: List<WeatherWarning> = emptyList()
}

/**
 * 天气预警
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherWarning {
    @JsonProperty("id")
    var id: String = ""
    
    @JsonProperty("sender")
    var sender: String = ""
    
    @JsonProperty("pubTime")
    var pubTime: String = ""
    
    @JsonProperty("title")
    var title: String = ""
    
    @JsonProperty("startTime")
    var startTime: String = ""
    
    @JsonProperty("endTime")
    var endTime: String = ""
    
    @JsonProperty("status")
    var status: String = ""
    
    @JsonProperty("level")
    var level: String = ""
    
    @JsonProperty("type")
    var type: String = ""
    
    @JsonProperty("text")
    var text: String = ""
}

/**
 * 生活指数响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class IndicesResponse : WeatherApiResponse() {
    @JsonProperty("daily")
    var daily: List<DailyIndex> = emptyList()
}

/**
 * 每日生活指数
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DailyIndex {
    @JsonProperty("date")
    var date: String = ""
    
    @JsonProperty("type")
    var type: String = ""
    
    @JsonProperty("name")
    var name: String = ""
    
    @JsonProperty("level")
    var level: String = ""
    
    @JsonProperty("category")
    var category: String = ""
    
    @JsonProperty("text")
    var text: String = ""
}

/**
 * 山区天气预报（应用内使用的数据模型）
 */
data class MountainWeatherForecast(
    val daily: List<DailyMountainForecast>,
    val minutely: List<MinutelyPrecipitationInfo>? = null,
    val warnings: List<WeatherWarningInfo>? = null,
    val outdoorIndices: Map<String, OutdoorIndexInfo>? = null,
    val altitude: Int? = null,
    val updateTime: Instant
)

/**
 * 每日山区天气预报
 */
data class DailyMountainForecast(
    val date: String,
    val tempMax: Int,
    val tempMin: Int,
    val weatherDay: String,
    val weatherNight: String,
    val windDirection: String,
    val windSpeed: Int,
    val windScale: String,
    val humidity: Int,
    val precipitation: Double,
    val uvIndex: Int,
    val pressure: Int,
    val visibility: Double,
    val sunrise: String,
    val sunset: String,
    val moonPhase: String
)

/**
 * 分钟级降水信息
 */
data class MinutelyPrecipitationInfo(
    val time: String,
    val precipitation: Double,
    val type: String
)

/**
 * 天气预警信息
 */
data class WeatherWarningInfo(
    val id: String,
    val title: String,
    val type: String,
    val level: String,
    val text: String,
    val pubTime: String
)

/**
 * 户外指数信息
 */
data class OutdoorIndexInfo(
    val name: String,
    val category: String,
    val level: String,
    val description: String,
    val suggestion: String
)