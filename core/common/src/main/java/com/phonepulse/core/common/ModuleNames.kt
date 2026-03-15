package com.phonepulse.core.common

/**
 * Mapping for module ids to display names.
 * Uses unicode escapes to avoid file encoding issues on different systems.
 */
object ModuleNames {
    private val names = mapOf(
        "battery" to "\uD83D\uDD0B \u0411\u0430\u0442\u0430\u0440\u0435\u044F",
        "display" to "\uD83D\uDCF1 \u042D\u043A\u0440\u0430\u043D",
        "audio" to "\uD83D\uDD0A \u0410\u0443\u0434\u0438\u043E",
        "camera" to "\uD83D\uDCF7 \u041A\u0430\u043C\u0435\u0440\u044B",
        "sensors" to "\uD83E\uDDED \u0414\u0430\u0442\u0447\u0438\u043A\u0438",
        "connectivity" to "\uD83D\uDCE1 \u0421\u0432\u044F\u0437\u044C",
        "storage" to "\uD83D\uDCBE \u041F\u0430\u043C\u044F\u0442\u044C",
        "controls" to "\uD83C\uDF9B \u0423\u043F\u0440\u0430\u0432\u043B\u0435\u043D\u0438\u0435",
        "wifi_speed" to "\uD83D\uDCF6 Wi-Fi"
    )

    fun get(moduleName: String): String = names[moduleName] ?: moduleName
}
