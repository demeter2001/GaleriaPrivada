package com.gallery.privatevault.core.model

enum class RuleType {
    INCLUDED,
    EXCLUDED
}

/**
 * Regla de visibilidad de carpetas por perfil.
 * REGLA FUNDAMENTAL: La exclusión SIEMPRE tiene prioridad sobre la inclusión.
 */
data class FolderRule(
    val id: Long = 0,
    val profileId: Long,
    val folderPath: String,
    val ruleType: RuleType,
    val isRecursive: Boolean = true
) {
    /**
     * Comprueba si una ruta dada coincide con esta regla.
     */
    fun matches(targetPath: String): Boolean {
        val normalizedRule = folderPath.trimEnd('/', '\\').lowercase()
        val normalizedTarget = targetPath.trimEnd('/', '\\').lowercase()

        return if (isRecursive) {
            normalizedTarget == normalizedRule || normalizedTarget.startsWith("$normalizedRule/") || normalizedTarget.startsWith("$normalizedRule\\")
        } else {
            normalizedTarget == normalizedRule
        }
    }
}
