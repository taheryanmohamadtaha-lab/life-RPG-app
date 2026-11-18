package com.liferpg.adventure.domain

/** The seven virtual character attributes. They are game stats, never real health measurements. */
enum class Stat(val fa: String, val en: String, val emoji: String) {
    STRENGTH("قدرت", "Strength", "💪"),
    INTELLIGENCE("هوش", "Intelligence", "🧠"),
    DISCIPLINE("انضباط", "Discipline", "🛡️"),
    VITALITY("جان", "Vitality", "❤️"),
    CREATIVITY("خلاقیت", "Creativity", "🎨"),
    FOCUS("تمرکز", "Focus", "🎯"),
    WISDOM("خرد", "Wisdom", "🦉");

    fun label(lang: String) = if (lang == "fa") fa else en
}

enum class Difficulty(
    val fa: String,
    val en: String,
    val defaultXp: Int,
    val gems: Int,
    val statGain: Int,
) {
    EASY("آسان", "Easy", 10, 0, 1),
    NORMAL("معمولی", "Normal", 25, 0, 1),
    HARD("سخت", "Hard", 50, 0, 2),
    EPIC("حماسی", "Epic", 100, 1, 3),
    LEGENDARY("افسانه‌ای", "Legendary", 200, 3, 4);

    fun label(lang: String) = if (lang == "fa") fa else en
}

enum class QuestKind(val fa: String, val en: String, val emoji: String) {
    DAILY("روزانه", "Daily", "☀️"),
    WEEKLY("هفتگی", "Weekly", "📅"),
    MONTHLY("ماهانه", "Monthly", "🌙"),
    GOAL("هدف بلندمدت", "Long-term goal", "🏔️"),
    ONE_TIME("یک‌بار", "One-time", "⭐");

    fun label(lang: String) = if (lang == "fa") fa else en
}

enum class QuestStatus { ACTIVE, DONE, ARCHIVED }

enum class QuestOrigin { USER, CHALLENGE, EVENT }

enum class Rarity(val fa: String, val en: String) {
    COMMON("معمولی", "Common"),
    UNCOMMON("کم‌یاب", "Uncommon"),
    RARE("نادر", "Rare"),
    EPIC("حماسی", "Epic"),
    LEGENDARY("افسانه‌ای", "Legendary"),
    MYTHIC("اسطوره‌ای", "Mythic");

    fun label(lang: String) = if (lang == "fa") fa else en
}

enum class ItemSlot(val fa: String, val en: String, val emoji: String) {
    WEAPON("سلاح", "Weapon", "⚔️"),
    ARMOR("زره", "Armor", "🛡️"),
    HELMET("کلاه‌خود", "Helmet", "⛑️"),
    ACCESSORY("زیورآلات", "Accessory", "💍"),
    MAGIC("اقلام جادویی", "Magic", "🔮"),
    COLLECTIBLE("کلکسیونی", "Collectible", "🏺");

    val equippable: Boolean get() = this == WEAPON || this == ARMOR || this == HELMET || this == ACCESSORY
    fun label(lang: String) = if (lang == "fa") fa else en
}

enum class Currency { GOLD, GEMS }

/** Parse an enum from its stored name, falling back to a default (used for robust DB/backup reads). */
inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    enumValues<E>().firstOrNull { it.name == name }

/** Localised digits: Persian UI shows Persian-Indic digits. */
object Digits {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    fun localize(text: String, lang: String): String {
        if (lang != "fa") return text
        val sb = StringBuilder(text.length)
        for (c in text) sb.append(if (c in '0'..'9') FA[c - '0'] else c)
        return sb.toString()
    }
    fun localize(n: Long, lang: String) = localize(n.toString(), lang)
    fun localize(n: Int, lang: String) = localize(n.toString(), lang)
}
