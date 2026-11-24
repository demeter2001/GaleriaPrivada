package com.gallery.privatevault.core.mediastore

import com.gallery.privatevault.core.database.entity.ProfileFolderRuleEntity
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.Profile
import com.gallery.privatevault.core.model.RuleType

/**
 * Motor de Evaluación de Reglas de Inclusión/Exclusión para Perfiles.
 *
 * REGLA FUNDAMENTAL DE LA ESPECIFICACIÓN:
 * "La exclusión SIEMPRE gana sobre la inclusión. Si la carpeta /DCIM está incluida,
 * pero /DCIM/Capturas está excluida, el contenido de /DCIM/Capturas queda oculto
 * por completo en ese perfil (incluyendo subcarpetas internas)."
 */
class ProfileRuleEvaluator(
    private val includeAllByDefault: Boolean,
    rules: List<ProfileFolderRuleEntity>
) {
    private val excludedPrefixes: List<String>
    private val includedPrefixes: List<String>

    init {
        val excluded = mutableListOf<String>()
        val included = mutableListOf<String>()

        rules.forEach { rule ->
            val normalized = normalizePath(rule.folderPath)
            if (rule.ruleType == RuleType.EXCLUDED) {
                excluded.add(normalized)
            } else {
                included.add(normalized)
            }
        }

        this.excludedPrefixes = excluded
        this.includedPrefixes = included
    }

    /**
     * Evalúa si un archivo multimedia es visible bajo el perfil activo.
     */
    fun isMediaPermitted(item: MediaItem): Boolean {
        // Los elementos en la papelera interna se filtran fuera del flujo normal
        if (item.isTrash) return false

        val normalizedPath = normalizePath(item.path)

        // 1. REGLA PRIORITARIA: Exclusión siempre gana
        for (excluded in excludedPrefixes) {
            if (isPathMatching(normalizedPath, excluded)) {
                return false
            }
        }

        // 2. Si el perfil incluye todo el almacenamiento por defecto
        if (includeAllByDefault) {
            return true
        }

        // 3. Si el perfil solo incluye carpetas seleccionadas
        for (included in includedPrefixes) {
            if (isPathMatching(normalizedPath, included)) {
                return true
            }
        }

        return false
    }

    private fun isPathMatching(targetPath: String, rulePath: String): Boolean {
        return targetPath == rulePath || targetPath.startsWith("$rulePath/")
    }

    private fun normalizePath(path: String): String {
        return path.replace('\\', '/')
            .trim()
            .trimEnd('/')
            .lowercase()
    }

    companion object {
        fun fromProfile(profile: Profile, rules: List<ProfileFolderRuleEntity>): ProfileRuleEvaluator {
            return ProfileRuleEvaluator(
                includeAllByDefault = profile.includeAllByDefault,
                rules = rules
            )
        }
    }
}
