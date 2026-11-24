package com.gallery.privatevault.core.database.dao

import androidx.room.*
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.database.entity.ProfileFolderRuleEntity
import com.gallery.privatevault.core.database.entity.ProfileVirtualFolderCrossRef
import com.gallery.privatevault.core.database.relation.ProfileWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY orderIndex ASC, id ASC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    fun getProfileWithDetailsFlow(profileId: Long): Flow<ProfileWithDetails?>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileWithDetails(profileId: Long): ProfileWithDetails?

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY orderIndex ASC")
    fun getAllProfilesWithDetailsFlow(): Flow<List<ProfileWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun countProfiles(): Int

    // Reglas de visibilidad
    @Query("SELECT * FROM profile_folder_rules WHERE profileId = :profileId")
    fun getRulesForProfileFlow(profileId: Long): Flow<List<ProfileFolderRuleEntity>>

    @Query("SELECT * FROM profile_folder_rules WHERE profileId = :profileId")
    suspend fun getRulesForProfile(profileId: Long): List<ProfileFolderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ProfileFolderRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ProfileFolderRuleEntity>)

    @Query("DELETE FROM profile_folder_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Long)

    @Query("DELETE FROM profile_folder_rules WHERE profileId = :profileId")
    suspend fun clearRulesForProfile(profileId: Long)

    // Asociación con carpetas virtuales
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileVirtualFolderRef(ref: ProfileVirtualFolderCrossRef)

    @Query("DELETE FROM profile_virtual_folder_cross_ref WHERE profileId = :profileId AND virtualFolderId = :virtualFolderId")
    suspend fun removeVirtualFolderFromProfile(profileId: Long, virtualFolderId: Long)
}
