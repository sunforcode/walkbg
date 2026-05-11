package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.model.RouteMapData
import org.example.route.repository.RouteMapDataRepository
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 路线地图数据控制器
 */
@RestController
@RequestMapping("/api/route-map-data")
@Tag(name = "路线地图数据管理", description = "路线地图数据相关的API接口")
@Validated
class RouteMapDataController(
    private val routeMapDataRepository: RouteMapDataRepository
) {

    /**
     * 创建路线地图数据
     */
    @PostMapping
    @Operation(summary = "创建路线地图数据", description = "创建新的路线地图数据记录")
    fun createRouteMapData(
        @RequestBody @Valid routeMapData: RouteMapData
    ): ResponseEntity<ApiResponse<RouteMapData>> {
        return try {
            val savedMapData = routeMapDataRepository.save(routeMapData)
            ResponseUtil.success(savedMapData, "创建地图数据成功")
        } catch (e: Exception) {
            ResponseUtil.error("创建地图数据失败: ${e.message}")
        }
    }

    /**
     * 根据ID获取路线地图数据
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取路线地图数据", description = "根据ID获取路线地图数据")
    fun getRouteMapData(
        @Parameter(description = "地图数据ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<RouteMapData?>> {
        return try {
            val mapData = routeMapDataRepository.findById(id).orElse(null)
            if (mapData == null) {
                ResponseUtil.error("地图数据不存在")
            } else {
                ResponseUtil.success(mapData, "获取地图数据成功")
            }
        } catch (e: Exception) {
            ResponseUtil.error("获取地图数据失败: ${e.message}")
        }
    }

    /**
     * 获取所有路线地图数据
     */
    @GetMapping
    @Operation(summary = "获取所有路线地图数据", description = "获取所有路线地图数据列表")
    fun getAllRouteMapData(): ResponseEntity<ApiResponse<List<RouteMapData>>> {
        return try {
            val mapDataList = routeMapDataRepository.findAll()
            ResponseUtil.success(mapDataList, "获取地图数据列表成功")
        } catch (e: Exception) {
            ResponseUtil.error("获取地图数据列表失败: ${e.message}")
        }
    }

    /**
     * 删除路线地图数据
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除路线地图数据", description = "根据 ID 删除路线地图数据")
    fun deleteRouteMapData(
        @Parameter(description = "地图数据 ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            if (routeMapDataRepository.existsById(id)) {
                routeMapDataRepository.deleteById(id)
                ResponseUtil.success(null, "删除地图数据成功")
            } else {
                ResponseUtil.error("地图数据不存在")
            }
        } catch (e: Exception) {
            ResponseUtil.error("删除地图数据失败: ${e.message}")
        }
    }

}
