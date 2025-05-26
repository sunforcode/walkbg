package org.example.repository

import org.example.model.EMUserEquipmentInventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EMUserEquipmentInventoryRepository : JpaRepository<EMUserEquipmentInventory, String> {
    
    fun findByUserId(userId: String): EMUserEquipmentInventory?
    
    @Query("SELECT COUNT(ei) FROM EMUserEquipmentInventory uei JOIN uei.equipmentItems ei WHERE uei.userId = :userId")
    fun countEquipmentItemsByUserId(userId: String): Long
    
    @Query("SELECT COUNT(ei) FROM EMUserEquipmentInventory uei JOIN uei.equipmentItems ei WHERE uei.userId = :userId AND ei.category = :category")
    fun countEquipmentItemsByUserIdAndCategory(userId: String, category: String): Long
}