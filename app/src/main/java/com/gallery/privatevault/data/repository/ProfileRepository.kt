package com.gallery.privatevault.data.repository

import com.gallery.privatevault.core.database.dao.AppSettingsDao
import com.gallery.privatevault.core.database.dao.ProfileDao
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.database.entity.ProfileFolderRuleEntity
import com.gallery.privatevault.core.database.relation.ProfileWithDetails
import com.gallery.privatevault.core.mediastore.ProfileRuleEvaluator
import com.gallery.privatevault.core.model.FolderRule
import com.gallery.privatevault.core.model.Profile
import kotlinx.coroutines.flow.*

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val appSettingsDao: AppSettingsDao
) {

    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()

    val activeProfileId: Flow<Long> = appSettingsDao.getSettingsFlow()
        .map { it?.activeProfileId ?: 1L }
        .distinctUntilChanged()

    val activeProfileWithDetails: Flow<ProfileWithDetails?> = activeProfileId
        .flatMapLatest { id -> profileDao.getProfileWithDetailsFlow(id) }

    val activeProfileEvaluator: Flow<ProfileRuleEvaluator> = activeProfileWithDetails
        .map { details ->
            if (details != null) {
                ProfileRuleEvaluator(
                    includeAllByDefault = details.profile.includeAllByDefault,
                    rules = details.rules
                )
            } else {
                ProfileRuleEvaluator(includeAllByDefault = true, rules = emptyList())
            }
        }

    suspend fun setActiveProfile(profileId: Long) {
        appSettingsDao.updateActiveProfile(profileId)
    }

    suspend fun createProfile(
        name: String,
        iconName: String = "folder",
        isProtected: Boolean = false,
        pinHash: String? = null,
        isReadOnly: Boolean = false,
        linkedThemeId: String? = null,
        includeAllByDefault: Boolean = true
    ): Long {
        val count = profileDao.countProfiles()
        return profileDao.insertProfile(
            ProfileEntity(
                name = name,
                iconName = iconName,
                isProtected = isProtected,
                pinHash = pinHash,
                isReadOnly = isReadOnly,
                linkedThemeId = linkedThemeId,
                includeAllByDefault = includeAllByDefault,
                orderIndex = count
            )
        )
    }

    suspend fun addFolderRule(
        profileId: Long,
        folderPath: String,
        ruleType: com.gallery.privatevault.core.model.RuleType,
        isRecursive: Boolean = true
    ) {
        profileDao.insertRule(
            ProfileFolderRuleEntity(
                profileId = profileId,
                folderPath = folderPath,
                ruleType = ruleType,
                isRecursive = isRecursive
            )
        )
    }

    suspend fun removeRule(ruleId: Long) {
        profileDao.deleteRule(ruleId)
    }

    val allProfilesWithDetails: Flow<List<ProfileWithDetails>> = profileDao.getAllProfilesWithDetailsFlow()

    suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }
}
