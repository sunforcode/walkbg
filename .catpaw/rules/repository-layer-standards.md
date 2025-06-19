---
ruleType: Constraint
description: Repository层数据访问标准和JPA使用规范
keywords: [Repository, JPA, 数据访问, 查询方法, Spring Data]
priority: HIGH
---

<!--
=== Repository层职责 ===
Repository层负责数据访问，封装数据库操作细节。
使用Spring Data JPA提供标准化的数据访问接口。
-->

# Repository层开发标准

## 🎯 强制性约束
```
MUST_EXTEND: JpaRepository<Entity, ID>
MUST_FOLLOW: Spring Data JPA命名约定
NEVER_IMPLEMENT: 基础CRUD方法 (由JPA自动提供)
QUERY_NAMING: 遵循findBy/existsBy/countBy命名规范
CUSTOM_QUERY: 复杂查询使用@Query注解
```

## 💻 标准Repository模式
```kotlin
// ✅ 标准Repository接口
interface RouteRepository : JpaRepository<Route, String> {

    // ✅ 标准查询方法命名
    fun findByStatus(status: Int): List<Route>
    fun findByStatusAndRegion(status: Int, region: String): List<Route>
    fun findByCreatedBy(createdBy: String): List<Route>
    fun findByDifficultyBetween(minDifficulty: Int, maxDifficulty: Int): List<Route>

    // ✅ 分页查询
    fun findByStatus(status: Int, pageable: Pageable): Page<Route>
    fun findByRegionContaining(region: String, pageable: Pageable): Page<Route>

    // ✅ 存在性检查
    fun existsByName(name: String): Boolean
    fun existsByIdAndCreatedBy(id: String, createdBy: String): Boolean

    // ✅ 计数查询
    fun countByStatus(status: Int): Long
    fun countByCreatedBy(createdBy: String): Long

    // ✅ 自定义查询
    @Query("SELECT r FROM Route r WHERE r.name LIKE %:keyword% OR r.description LIKE %:keyword%")
    fun searchByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<Route>

    @Query("SELECT r FROM Route r JOIN r.waypoints w WHERE w.latitude BETWEEN :minLat AND :maxLat")
    fun findByLatitudeRange(
        @Param("minLat") minLatitude: Double,
        @Param("maxLat") maxLatitude: Double
    ): List<Route>

    // ✅ 原生SQL查询（复杂统计）
    @Query(value = "SELECT COUNT(*) FROM routes r WHERE r.created_at >= :startDate", nativeQuery = true)
    fun countRoutesCreatedAfter(@Param("startDate") startDate: Instant): Long
}
```

## 🔍 查询方法命名规范
```
// ✅ 标准命名模式
findBy{Property}                    // 根据属性查询
findBy{Property}And{Property}       // 多条件AND查询
findBy{Property}Or{Property}        // 多条件OR查询
findBy{Property}Between             // 范围查询
findBy{Property}LessThan           // 小于查询
findBy{Property}GreaterThan        // 大于查询
findBy{Property}Like               // 模糊查询
findBy{Property}Containing         // 包含查询
findBy{Property}StartingWith       // 开头匹配
findBy{Property}EndingWith         // 结尾匹配
findBy{Property}In                 // IN查询
findBy{Property}NotNull            // 非空查询
findBy{Property}IsNull             // 空值查询

existsBy{Property}                 // 存在性检查
countBy{Property}                  // 计数查询
deleteBy{Property}                 // 删除查询
```

## 🔄 分页和排序
```kotlin
// ✅ 分页查询
interface RouteRepository : JpaRepository<Route, String> {
    fun findByStatus(status: Int, pageable: Pageable): Page<Route>
    fun findByRegion(region: String, pageable: Pageable): Page<Route>
}

// ✅ 使用示例
val pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending())
val routes = routeRepository.findByStatus(1, pageable)
```

## 🚫 禁止的做法
```kotlin
// ❌ 禁止：自定义命名不遵循规范
interface RouteRepository : JpaRepository<Route, String> {
    fun getRouteByName(name: String): Route?  // 应该用findByName
    fun searchRoutes(keyword: String): List<Route>  // 应该用findByNameContaining
    fun getAllActiveRoutes(): List<Route>  // 应该用findByStatus
}

// ❌ 禁止：实现基础CRUD方法
interface RouteRepository : JpaRepository<Route, String> {
    fun save(route: Route): Route  // JPA已提供，不需要重复定义
    fun findById(id: String): Optional<Route>  // JPA已提供
}

// ❌ 禁止：复杂逻辑在Repository中
interface RouteRepository : JpaRepository<Route, String> {
    // 业务逻辑应该在Service层
    fun findActiveRoutesForUser(userId: String): List<Route> {
        // 复杂的业务逻辑...
    }
}
```

## 📋 Repository开发检查清单
- [ ] 是否继承了JpaRepository？
- [ ] 查询方法命名是否遵循Spring Data规范？
- [ ] 是否避免了重复定义基础CRUD方法？
- [ ] 复杂查询是否使用了@Query注解？
- [ ] 分页查询是否使用了Pageable参数？
- [ ] 是否避免了在Repository中包含业务逻辑？

## 🎯 性能优化建议
```kotlin
// ✅ 使用投影减少数据传输
interface RouteProjection {
    val id: String
    val name: String
    val status: Int
}

interface RouteRepository : JpaRepository<Route, String> {
    fun findByStatus(status: Int): List<RouteProjection>
}

// ✅ 使用@EntityGraph优化关联查询
@EntityGraph(attributePaths = ["waypoints", "tags"])
fun findWithDetailsById(id: String): Optional<Route>

// ✅ 批量操作
@Modifying
@Query("UPDATE Route r SET r.status = :status WHERE r.id IN :ids")
fun updateStatusByIds(@Param("status") status: Int, @Param("ids") ids: List<String>)
```

<!--
=== JPA查询优化提示 ===
1. 避免N+1查询问题，使用@EntityGraph或JOIN FETCH
2. 大数据量查询使用分页
3. 统计查询使用COUNT而不是查询后计算
4. 复杂查询考虑使用原生SQL
5. 频繁查询的字段建立数据库索引
-->
