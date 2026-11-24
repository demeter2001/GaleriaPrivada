package com.gallery.privatevault.core.database.converter

import androidx.room.TypeConverter
import com.gallery.privatevault.core.model.FolderSortOrder
import com.gallery.privatevault.core.model.NavigationStyle
import com.gallery.privatevault.core.model.RuleType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun fromFolderSortOrder(value: FolderSortOrder): String = value.name

    @TypeConverter
    fun toFolderSortOrder(value: String): FolderSortOrder = FolderSortOrder.valueOf(value)

    @TypeConverter
    fun fromNavigationStyle(value: NavigationStyle): String = value.name

    @TypeConverter
    fun toNavigationStyle(value: String): NavigationStyle = NavigationStyle.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }
}
