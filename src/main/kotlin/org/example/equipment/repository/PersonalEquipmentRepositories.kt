package org.example.equipment.repository

import org.example.equipment.model.EquipmentListMember
import org.example.equipment.model.EquipmentListMemberId
import org.example.equipment.model.EquipmentListOwnership
import org.example.equipment.model.PersonalEquipmentOwnership
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.model.UserEquipmentListRecord
import org.springframework.data.jpa.repository.JpaRepository

interface PersonalEquipmentRecordRepository : JpaRepository<PersonalEquipmentRecord, String>

interface PersonalEquipmentOwnershipRepository : JpaRepository<PersonalEquipmentOwnership, String> {
    fun findByAccountIdOrderByPersonalEquipmentIdAsc(accountId: String): List<PersonalEquipmentOwnership>
    fun findByPersonalEquipmentIdAndAccountId(personalEquipmentId: String, accountId: String): PersonalEquipmentOwnership?
    fun existsByAccountIdAndNormalizedName(accountId: String, normalizedName: String): Boolean
    fun existsByAccountIdAndNormalizedNameAndPersonalEquipmentIdNot(
        accountId: String,
        normalizedName: String,
        personalEquipmentId: String
    ): Boolean
}

interface UserEquipmentListRecordRepository : JpaRepository<UserEquipmentListRecord, String>

interface EquipmentListOwnershipRepository : JpaRepository<EquipmentListOwnership, String> {
    fun findByAccountIdOrderByEquipmentListIdAsc(accountId: String): List<EquipmentListOwnership>
    fun findByEquipmentListIdAndAccountId(equipmentListId: String, accountId: String): EquipmentListOwnership?
    fun countByAccountId(accountId: String): Long
}

interface EquipmentListMemberRepository : JpaRepository<EquipmentListMember, EquipmentListMemberId> {
    fun findByEquipmentListIdOrderByPersonalEquipmentIdAsc(equipmentListId: String): List<EquipmentListMember>
    fun findByPersonalEquipmentId(personalEquipmentId: String): List<EquipmentListMember>
    fun deleteByPersonalEquipmentId(personalEquipmentId: String): Long
    fun deleteByEquipmentListId(equipmentListId: String): Long
}
