package org.example.config

import org.example.model.*
import org.example.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.util.*

@Configuration
class DataInitializer {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    @Value("\${app.data-initialization.enabled:false}")
    private var dataInitializationEnabled: Boolean = false

    @Bean
    @Profile("!test") // 不在测试环境运行
    fun initData(
        routeRepository: RouteRepository,
        emEquipmentListRepository: EMEquipmentListRepository,
        emEquipmentTemplateRepository: EMEquipmentTemplateRepository,
        emUserEquipmentInventoryRepository: EMUserEquipmentInventoryRepository
    ): CommandLineRunner {
        return CommandLineRunner {
            if (!dataInitializationEnabled) {
                logger.info("Data initialization disabled")
                return@CommandLineRunner
            }

            logger.info("Starting data initialization...")

            try {
                // 初始化路线数据
                initRoutes(routeRepository)

                // 初始化装备模板数据
                initEquipmentTemplates(emEquipmentTemplateRepository)

                // 初始化用户装备库数据
                initUserEquipmentInventory(emUserEquipmentInventoryRepository)

                // 初始化装备清单数据
                initEquipmentLists(emEquipmentListRepository)

                logger.info("Data initialization completed successfully")
            } catch (e: Exception) {
                logger.error("Error during data initialization", e)
            }
        }
    }

    private fun initRoutes(routeRepository: RouteRepository) {
        if (routeRepository.count() > 0) {
            logger.info("Routes already exist, skipping initialization")
            return
        }

        logger.info("Initializing routes...")

        val routes = listOf(
            Route(
                id = UUID.randomUUID().toString(),
                name = "黄山经典徒步路线",
                description = "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。",
                region = "黄山风景区",
                distance = 15.5,
                duration = "8小时",
                difficulty = 2,
                popularity = 10,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Route(
                id = UUID.randomUUID().toString(),
                name = "莫干山徒步路线",
                description = "莫干山是浙江省湖州市德清县下辖的山区，以竹海和清新空气闻名。",
                region = "浙江省湖州市",
                distance = 10.2,
                duration = "5小时",
                difficulty = 1,
                popularity = 8,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Route(
                id = UUID.randomUUID().toString(),
                name = "泰山登山路线",
                description = "泰山是中国五岳之首，有着悠久的历史和文化底蕴。",
                region = "山东省泰安市",
                distance = 8.5,
                duration = "6小时",
                difficulty = 3,
                popularity = 15,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        // 添加季节和标签
        routes[0].apply {
            addSeason("春季")
            addSeason("秋季")
            addTag("山岳")
            addTag("森林")
        }

        routes[1].apply {
            addSeason("春季")
            addSeason("夏季")
            addSeason("秋季")
            addTag("竹林")
            addTag("山村")
        }

        routes[2].apply {
            addSeason("春季")
            addSeason("秋季")
            addTag("山岳")
            addTag("文化")
            addTag("历史")
        }

        val savedRoutes = routeRepository.saveAll(routes)
        logger.info("Initialized ${savedRoutes.size} routes")
    }

    private fun initEquipmentTemplates(templateRepository: EMEquipmentTemplateRepository) {
        if (templateRepository.count() > 0) {
            logger.info("Equipment templates already exist, skipping initialization")
            return
        }

        logger.info("Initializing equipment templates...")

        val shortHikeTemplate = EMEquipmentTemplate(
            id = UUID.randomUUID().toString(),
            name = "官方春季短途徒步装备",
            description = "适合春季1-3天短途徒步的基础装备清单",
            type = EMEquipmentListType.SHORT_HIKE,
            creatorId = "admin",
            creatorName = "系统管理员",
            isOfficial = true,
            usageCount = 1250,
            rating = 4.8,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 添加季节
        shortHikeTemplate.addSeason(EMSeasonSuitability.SPRING)
        shortHikeTemplate.addSeason(EMSeasonSuitability.AUTUMN)

        // 添加标签
        shortHikeTemplate.addTag("短途")
        shortHikeTemplate.addTag("入门")
        shortHikeTemplate.addTag("轻量化")

        // 添加装备项目
        val hikeShoes = EMTemplateEquipmentItem(
            id = UUID.randomUUID().toString(),
            name = "徒步鞋",
            category = EMEquipmentCategory.CLOTHING,
            description = "防水透气徒步鞋",
            weight = 800.0,
            weightUnit = EMWeightUnit.GRAM,
            quantity = 1,
            necessity = EMEquipmentNecessity.ESSENTIAL,
            brand = "通用",
            model = "入门级",
            notes = "选择防水透气的徒步鞋，确保舒适度和支撑性"
        )

        val backpack = EMTemplateEquipmentItem(
            id = UUID.randomUUID().toString(),
            name = "背包",
            category = EMEquipmentCategory.BACKPACK,
            description = "20-30L日用背包",
            weight = 700.0,
            weightUnit = EMWeightUnit.GRAM,
            quantity = 1,
            necessity = EMEquipmentNecessity.ESSENTIAL,
            brand = "通用",
            model = "入门级",
            notes = "选择合适大小的背包，确保能装下所有必要装备"
        )

        shortHikeTemplate.addEquipmentItem(hikeShoes)
        shortHikeTemplate.addEquipmentItem(backpack)

        // 保存模板
        val savedTemplate = templateRepository.save(shortHikeTemplate)
        logger.info("Initialized equipment template with ${savedTemplate.equipmentItems.size} items")
    }

    private fun initUserEquipmentInventory(inventoryRepository: EMUserEquipmentInventoryRepository) {
        if (inventoryRepository.count() > 0) {
            logger.info("User equipment inventories already exist, skipping initialization")
            return
        }

        logger.info("Initializing user equipment inventories...")

        val testUserId = "user123"
        val inventory = EMUserEquipmentInventory(
            id = UUID.randomUUID().toString(),
            userId = testUserId,
            lastUpdatedAt = Instant.now()
        )

        // 添加装备项目
        val hikeShoes = EMEquipmentItem(
            id = UUID.randomUUID().toString(),
            name = "徒步鞋",
            category = EMEquipmentCategory.CLOTHING,
            description = "Salomon X Ultra 3防水徒步鞋",
            weight = 800.0,
            weightUnit = EMWeightUnit.GRAM,
            quantity = 1,
            necessity = EMEquipmentNecessity.ESSENTIAL,
            brand = "Salomon",
            model = "X Ultra 3",
            price = 899.0,
            condition = EMEquipmentCondition.GOOD,
            usageCount = 12,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        inventory.addEquipmentItem(hikeShoes)

        val savedInventory = inventoryRepository.save(inventory)
        logger.info("Initialized user equipment inventory for user $testUserId with ${savedInventory.equipmentItems.size} items")
    }

    private fun initEquipmentLists(listRepository: EMEquipmentListRepository) {
        if (listRepository.count() > 0) {
            logger.info("Equipment lists already exist, skipping initialization")
            return
        }

        logger.info("Initializing equipment lists...")

        val testUserId = "user123"
        val equipmentList = EMEquipmentList(
            id = UUID.randomUUID().toString(),
            name = "我的黄山徒步装备",
            description = "计划于2023年5月前往黄山徒步的装备清单",
            type = EMEquipmentListType.SHORT_HIKE,
            routeId = null, // 可以设置为实际的路线ID
            routeName = "黄山经典徒步路线",
            tripDays = 2,
            personCount = 1,
            creatorId = testUserId,
            creatorName = "测试用户",
            isOfficial = false,
            isTemplate = false,
            status = EMEquipmentListStatus.PLANNING,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 添加季节
        equipmentList.addSeason(EMSeasonSuitability.SPRING)

        // 添加标签
        equipmentList.addTag("短途")
        equipmentList.addTag("春季")
        equipmentList.addTag("入门")

        // 添加装备项目
        val hikeShoes = EMEquipmentItem(
            id = UUID.randomUUID().toString(),
            name = "徒步鞋",
            category = EMEquipmentCategory.CLOTHING,
            description = "Salomon X Ultra 3防水徒步鞋",
            weight = 800.0,
            weightUnit = EMWeightUnit.GRAM,
            quantity = 1,
            necessity = EMEquipmentNecessity.ESSENTIAL,
            prepared = true,
            isOwned = true,
            brand = "Salomon",
            model = "X Ultra 3",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        equipmentList.addEquipmentItem(hikeShoes)

        val savedList = listRepository.save(equipmentList)
        logger.info("Initialized equipment list for user $testUserId with ${savedList.equipmentItems.size} items")
    }
}