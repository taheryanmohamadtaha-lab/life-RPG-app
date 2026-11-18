package com.liferpg.adventure.domain

data class CategoryDef(
    val id: String, val fa: String, val en: String, val emoji: String, val color: Long,
    val primary: Stat, val secondary: Stat,
)

data class SkillDef(
    val id: String, val categoryId: String, val tier: Int,
    val fa: String, val en: String, val descFa: String, val descEn: String, val emoji: String,
    val parentId: String?, val parentRank: Int,
) {
    val xpPercentPerRank: Int get() = tier
    fun costOfNextRank(): Int = tier
    fun bonusStat(): Stat? {
        val c = Catalog.category(categoryId) ?: return null
        return when (tier) { 2 -> c.primary; 3 -> c.secondary; else -> null }
    }
    companion object { const val MAX_RANK = 3 }
}

data class ItemDef(
    val id: String, val slot: ItemSlot, val rarity: Rarity,
    val fa: String, val en: String, val emoji: String, val descFa: String, val descEn: String,
    val price: Int, val currency: Currency,
    val stats: Map<Stat, Int> = emptyMap(), val xpPercent: Int = 0, val coinPercent: Int = 0,
    val consumableShield: Boolean = false,
)

data class RegionDef(
    val id: String, val fa: String, val en: String, val emoji: String, val descFa: String, val descEn: String,
    val categoryIds: List<String>, val unlockLevel: Int, val unlockCompletions: Int, val masteryTarget: Int,
    val x: Float, val y: Float, val color: Long,
)

enum class Metric { TOTAL, BEST_STREAK, LEVEL, MONTH, HARD_PLUS, CATEGORY }

data class AchievementMetrics(
    val total: Int, val bestStreak: Int, val level: Int, val monthCount: Int, val hardPlus: Int,
    val perCategory: Map<String, Int>,
)

data class AchievementDef(
    val id: String, val fa: String, val en: String, val descFa: String, val descEn: String, val emoji: String,
    val metric: Metric, val categoryId: String?, val target: Int, val gold: Int, val gems: Int,
) {
    fun value(m: AchievementMetrics): Int = when (metric) {
        Metric.TOTAL -> m.total
        Metric.BEST_STREAK -> m.bestStreak
        Metric.LEVEL -> m.level
        Metric.MONTH -> m.monthCount
        Metric.HARD_PLUS -> m.hardPlus
        Metric.CATEGORY -> m.perCategory[categoryId] ?: 0
    }
    fun isReached(m: AchievementMetrics) = value(m) >= target
}

data class ChallengeTemplate(
    val id: String, val fa: String, val en: String, val categoryId: String, val difficulty: Difficulty,
)

data class AvatarDef(val id: Int, val fa: String, val en: String, val hair: Long, val cloak: Long, val eye: Long)

data class PetDef(val id: Int, val fa: String, val en: String, val body: Long, val belly: Long, val accent: Long, val glow: Long)

object Catalog {
    const val FITNESS = "fitness"
    const val LEARNING = "learning"
    const val CAREER = "career"
    const val FINANCE = "finance"
    const val CREATIVE = "creative"
    const val SOCIAL = "social"
    const val MINDFUL = "mindful"

    val categories = listOf(
        CategoryDef(FITNESS, "تناسب و سلامت", "Fitness & Health", "💪", 0xFFFF7A59, Stat.STRENGTH, Stat.VITALITY),
        CategoryDef(LEARNING, "هوش و یادگیری", "Intelligence & Learning", "📚", 0xFF39A0FF, Stat.INTELLIGENCE, Stat.FOCUS),
        CategoryDef(CAREER, "کار و بهره‌وری", "Career & Productivity", "💼", 0xFFFFC857, Stat.DISCIPLINE, Stat.FOCUS),
        CategoryDef(FINANCE, "نظم مالی", "Financial Discipline", "💰", 0xFF2ED8A3, Stat.DISCIPLINE, Stat.WISDOM),
        CategoryDef(CREATIVE, "خلاقیت و هنر", "Creativity & Art", "🎨", 0xFFE879F9, Stat.CREATIVITY, Stat.INTELLIGENCE),
        CategoryDef(SOCIAL, "خانواده و اجتماع", "Social & Family", "🤝", 0xFFFF8FB1, Stat.WISDOM, Stat.VITALITY),
        CategoryDef(MINDFUL, "آرامش و رشد فردی", "Mindfulness & Growth", "🧘", 0xFF9D7CFF, Stat.WISDOM, Stat.FOCUS),
    )
    fun category(id: String): CategoryDef? = categories.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- skills
    private fun skill(id: String, cat: String, tier: Int, fa: String, en: String, dfa: String, den: String, emoji: String) =
        SkillDef(id, cat, tier, fa, en, dfa, den, emoji, parentId = if (tier == 1) null else "${cat}_${tier - 1}", parentRank = if (tier == 2) 1 else if (tier == 3) 2 else 0)

    val skills: List<SkillDef> = listOf(
        skill("${FITNESS}_1", FITNESS, 1, "تمرین بدنی", "Body Training", "حرکت روزانه، حتی کوچک، پایهٔ هر پیشرفتی است.", "Daily movement, even a little, builds every foundation.", "🏃"),
        skill("${FITNESS}_2", FITNESS, 2, "استقامت", "Endurance", "پایداری در تمرین‌های منظم.", "Staying steady with regular practice.", "🔥"),
        skill("${FITNESS}_3", FITNESS, 3, "بدن پولادین", "Iron Body", "اوج سلامت و نشاط در بازی.", "The peak of in-game vitality.", "🏆"),
        skill("${LEARNING}_1", LEARNING, 1, "ذهن کنجکاو", "Curious Mind", "هر روز یک چیز تازه یاد بگیر.", "Learn one new thing every day.", "🔍"),
        skill("${LEARNING}_2", LEARNING, 2, "مطالعهٔ عمیق", "Deep Reading", "تمرکز طولانی روی یک موضوع.", "Long, focused study sessions.", "📖"),
        skill("${LEARNING}_3", LEARNING, 3, "دانشور", "Scholar", "تسلط واقعی بر دانش.", "True mastery of knowledge.", "🎓"),
        skill("${CAREER}_1", CAREER, 1, "برنامه‌ریز", "Planner", "کارها را روشن و قابل‌انجام کن.", "Make work clear and doable.", "🗂️"),
        skill("${CAREER}_2", CAREER, 2, "کار عمیق", "Deep Work", "بدون حواس‌پرتی کار کن.", "Work without distraction.", "⏳"),
        skill("${CAREER}_3", CAREER, 3, "استاد ساخت", "Master Builder", "ساختن چیزهای ماندگار.", "Build things that last.", "🏗️"),
        skill("${FINANCE}_1", FINANCE, 1, "بودجه‌بند", "Budgeter", "ثبت و آگاهی از هزینه‌ها.", "Track and understand spending.", "🧾"),
        skill("${FINANCE}_2", FINANCE, 2, "پس‌انداز", "Saver", "عادت پس‌انداز منظم.", "A steady saving habit.", "🐷"),
        skill("${FINANCE}_3", FINANCE, 3, "نگهبان ثروت", "Wealth Keeper", "تصمیم‌های مالی آرام و سنجیده.", "Calm, thoughtful money decisions.", "🏦"),
        skill("${CREATIVE}_1", CREATIVE, 1, "جرقه", "Spark", "ایده‌ها را یادداشت کن.", "Capture your ideas.", "✨"),
        skill("${CREATIVE}_2", CREATIVE, 2, "هنرورزی", "Craftsmanship", "تمرین مداوم یک هنر.", "Practise a craft regularly.", "🖌️"),
        skill("${CREATIVE}_3", CREATIVE, 3, "چشم‌انداز", "Visionary", "ایده‌ها را به اثر تبدیل کن.", "Turn ideas into finished work.", "🌈"),
        skill("${SOCIAL}_1", SOCIAL, 1, "سخن مهربان", "Kind Word", "یک پیام یا کلمهٔ محبت‌آمیز.", "A kind message or word.", "💬"),
        skill("${SOCIAL}_2", SOCIAL, 2, "پیوند", "Bonding", "وقت با کیفیت برای عزیزان.", "Quality time with loved ones.", "🫂"),
        skill("${SOCIAL}_3", SOCIAL, 3, "قلب قبیله", "Heart of the Guild", "تکیه‌گاه اطرافیان بودن.", "Being a pillar for others.", "❤️‍🔥"),
        skill("${MINDFUL}_1", MINDFUL, 1, "نفس آرام", "Calm Breath", "چند نفس عمیق، چند لحظه مکث.", "A few deep breaths, a moment of pause.", "🌬️"),
        skill("${MINDFUL}_2", MINDFUL, 2, "تأمل", "Reflection", "نوشتن و مرور روزانه.", "Daily journaling and review.", "🪞"),
        skill("${MINDFUL}_3", MINDFUL, 3, "آرامش درون", "Inner Peace", "ثبات در هر شرایط.", "Steadiness in any weather.", "☮️"),
    )
    fun skill(id: String): SkillDef? = skills.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- items
    private fun item(
        id: String, slot: ItemSlot, rarity: Rarity, fa: String, en: String, emoji: String, dfa: String, den: String,
        price: Int, currency: Currency = Currency.GOLD, stats: Map<Stat, Int> = emptyMap(), xp: Int = 0, coin: Int = 0,
        shield: Boolean = false,
    ) = ItemDef(id, slot, rarity, fa, en, emoji, dfa, den, price, currency, stats, xp, coin, shield)

    val items: List<ItemDef> = listOf(
        // weapons
        item("wooden_sword", ItemSlot.WEAPON, Rarity.COMMON, "شمشیر چوبی", "Wooden Sword", "🗡️", "اولین سلاح هر قهرمان.", "Every hero's first weapon.", 120, stats = mapOf(Stat.STRENGTH to 1)),
        item("iron_blade", ItemSlot.WEAPON, Rarity.UNCOMMON, "تیغهٔ آهنین", "Iron Blade", "⚔️", "محکم و قابل اعتماد.", "Sturdy and dependable.", 350, stats = mapOf(Stat.STRENGTH to 2)),
        item("scholar_staff", ItemSlot.WEAPON, Rarity.RARE, "عصای دانشور", "Scholar's Staff", "🪄", "درخشان از دانش.", "Glowing with knowledge.", 800, stats = mapOf(Stat.INTELLIGENCE to 3)),
        item("flame_saber", ItemSlot.WEAPON, Rarity.EPIC, "شمشیر شعله", "Flame Saber", "🔥", "شعله‌ای که خاموش نمی‌شود.", "A flame that never fades.", 2000, stats = mapOf(Stat.STRENGTH to 3, Stat.FOCUS to 2), xp = 2),
        item("storm_spear", ItemSlot.WEAPON, Rarity.LEGENDARY, "نیزهٔ طوفان", "Storm Spear", "⚡", "رعد را در دست داری.", "Thunder in your hand.", 30, Currency.GEMS, mapOf(Stat.STRENGTH to 4, Stat.VITALITY to 3), xp = 4),
        item("starforged_blade", ItemSlot.WEAPON, Rarity.MYTHIC, "تیغهٔ ستاره‌ساز", "Starforged Blade", "🌠", "از دل یک ستاره ساخته شده.", "Forged in the heart of a star.", 90, Currency.GEMS, mapOf(Stat.STRENGTH to 5, Stat.INTELLIGENCE to 3, Stat.DISCIPLINE to 3), xp = 6),
        // armor
        item("leather_vest", ItemSlot.ARMOR, Rarity.COMMON, "جلیقهٔ چرمی", "Leather Vest", "🦺", "سبک و راحت.", "Light and comfortable.", 120, stats = mapOf(Stat.VITALITY to 1)),
        item("chainmail", ItemSlot.ARMOR, Rarity.UNCOMMON, "زره زنجیری", "Chainmail", "⛓️", "حلقه‌های محکم.", "Tough linked rings.", 350, stats = mapOf(Stat.VITALITY to 2, Stat.DISCIPLINE to 1)),
        item("mage_robe", ItemSlot.ARMOR, Rarity.RARE, "ردای جادوگر", "Mage Robe", "🥋", "پر از رمز و راز.", "Full of mystery.", 800, stats = mapOf(Stat.INTELLIGENCE to 2, Stat.WISDOM to 2)),
        item("shadow_plate", ItemSlot.ARMOR, Rarity.EPIC, "زرهٔ سایه", "Shadow Plate", "🌑", "سایه‌ها از تو محافظت می‌کنند.", "Shadows guard you.", 2000, stats = mapOf(Stat.DISCIPLINE to 3, Stat.VITALITY to 3), xp = 2),
        item("dragon_scale", ItemSlot.ARMOR, Rarity.LEGENDARY, "زرهٔ فلس اژدها", "Dragon Scale Armor", "🐉", "فلس‌های اژدهای کهن.", "Scales of an ancient dragon.", 30, Currency.GEMS, mapOf(Stat.VITALITY to 5, Stat.DISCIPLINE to 3), xp = 4),
        item("astral_aegis", ItemSlot.ARMOR, Rarity.MYTHIC, "سپر اخترین", "Astral Aegis", "🌌", "کهکشانی در دستان تو.", "A galaxy in your hands.", 90, Currency.GEMS, mapOf(Stat.VITALITY to 5, Stat.WISDOM to 4, Stat.DISCIPLINE to 4), xp = 6),
        // helmets
        item("cloth_cap", ItemSlot.HELMET, Rarity.COMMON, "کلاه پارچه‌ای", "Cloth Cap", "🧢", "ساده و دلنشین.", "Simple and cosy.", 100, stats = mapOf(Stat.FOCUS to 1)),
        item("iron_helm", ItemSlot.HELMET, Rarity.UNCOMMON, "کلاه‌خود آهنین", "Iron Helm", "⛑️", "محکم‌ترین پشتیبان انضباط.", "A firm ally of discipline.", 300, stats = mapOf(Stat.DISCIPLINE to 2)),
        item("sage_hood", ItemSlot.HELMET, Rarity.RARE, "کلاه حکیم", "Sage Hood", "🧙", "خرد قدیمی.", "Old wisdom.", 700, stats = mapOf(Stat.WISDOM to 3)),
        item("crown_of_will", ItemSlot.HELMET, Rarity.EPIC, "تاج اراده", "Crown of Will", "👑", "تاجی برای اراده‌های استوار.", "A crown for steady wills.", 1800, stats = mapOf(Stat.DISCIPLINE to 3, Stat.FOCUS to 3), xp = 2),
        item("dragon_helm", ItemSlot.HELMET, Rarity.LEGENDARY, "کلاه‌خود اژدها", "Dragon Helm", "🐲", "نگاه اژدها.", "The gaze of a dragon.", 28, Currency.GEMS, mapOf(Stat.STRENGTH to 3, Stat.FOCUS to 3), xp = 4),
        item("dawn_halo", ItemSlot.HELMET, Rarity.MYTHIC, "هالهٔ سپیده", "Halo of Dawn", "😇", "نور آغاز هر روز.", "The light of every new day.", 85, Currency.GEMS, mapOf(Stat.WISDOM to 5, Stat.FOCUS to 4), xp = 6),
        // accessories
        item("copper_ring", ItemSlot.ACCESSORY, Rarity.COMMON, "انگشتر مسی", "Copper Ring", "💍", "ساده اما خوش‌شانس.", "Simple but lucky.", 100, stats = mapOf(Stat.CREATIVITY to 1)),
        item("focus_amulet", ItemSlot.ACCESSORY, Rarity.UNCOMMON, "طلسم تمرکز", "Focus Amulet", "🧿", "ذهن را روشن می‌کند.", "Clears the mind.", 300, stats = mapOf(Stat.FOCUS to 2)),
        item("jade_pendant", ItemSlot.ACCESSORY, Rarity.RARE, "آویز یشم", "Jade Pendant", "🟢", "آرامش سبز.", "Green calm.", 700, stats = mapOf(Stat.VITALITY to 2, Stat.WISDOM to 2), coin = 5),
        item("moon_ring", ItemSlot.ACCESSORY, Rarity.EPIC, "حلقهٔ ماه", "Moon Ring", "🌙", "نور ماه در شب‌های تاریک.", "Moonlight for dark nights.", 1800, stats = mapOf(Stat.CREATIVITY to 3, Stat.INTELLIGENCE to 2), coin = 8),
        item("phoenix_charm", ItemSlot.ACCESSORY, Rarity.LEGENDARY, "طلسم ققنوس", "Phoenix Charm", "🔶", "هر بار دوباره برمی‌خیزی.", "You rise again every time.", 26, Currency.GEMS, mapOf(Stat.VITALITY to 4, Stat.CREATIVITY to 3), coin = 10),
        item("time_locket", ItemSlot.ACCESSORY, Rarity.MYTHIC, "گردنبند زمان", "Locket of Time", "⏱️", "زمان به نفع توست.", "Time is on your side.", 80, Currency.GEMS, mapOf(Stat.FOCUS to 5, Stat.DISCIPLINE to 4), coin = 12),
        // magical items
        item("elixir_shield", ItemSlot.MAGIC, Rarity.UNCOMMON, "اکسیر سپر استریک", "Streak Shield Elixir", "🧪", "یک سپر برای حفظ استریک (حداکثر ۳). روز استراحت هم بدون مصرف سپر است.", "Adds one streak shield (max 3). Rest days never cost a shield.", 150, shield = true),
        item("rune_fortune", ItemSlot.MAGIC, Rarity.RARE, "رون ثروت", "Rune of Fortune", "🔮", "همیشه فعال: +۵٪ سکهٔ مأموریت.", "Always active: +5% quest coins.", 900, coin = 5),
        item("rune_insight", ItemSlot.MAGIC, Rarity.EPIC, "رون بینش", "Rune of Insight", "💠", "همیشه فعال: +۳٪ XP.", "Always active: +3% XP.", 25, Currency.GEMS, xp = 3),
        // collectibles
        item("dragon_egg_shard", ItemSlot.COLLECTIBLE, Rarity.UNCOMMON, "تکهٔ تخم اژدها", "Dragon Egg Shard", "🥚", "یادگاری از روزهای اول.", "A keepsake from the early days.", 200),
        item("ancient_coin", ItemSlot.COLLECTIBLE, Rarity.RARE, "سکهٔ باستانی", "Ancient Coin", "🪙", "از امپراتوری‌های گمشده.", "From lost empires.", 500),
        item("crystal_shard", ItemSlot.COLLECTIBLE, Rarity.EPIC, "تکهٔ کریستال", "Crystal Shard", "💎", "می‌درخشد.", "It glimmers.", 15, Currency.GEMS),
        item("star_map", ItemSlot.COLLECTIBLE, Rarity.LEGENDARY, "نقشهٔ ستاره‌ها", "Star Map", "🗺️", "راهی به سرزمین‌های ناشناخته.", "A way to unknown lands.", 25, Currency.GEMS),
    )
    fun item(id: String): ItemDef? = items.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- regions
    val regions = listOf(
        RegionDef("forest_knowledge", "جنگل دانش", "Forest of Knowledge", "🌲", "درختان کهن اینجا رازهای کتاب‌ها را زمزمه می‌کنند.", "Ancient trees whisper the secrets of books.", listOf(Catalog.LEARNING), 1, 0, 25, 0.24f, 0.82f, 0xFF2ED8A3),
        RegionDef("mountain_strength", "کوه قدرت", "Mountain of Strength", "⛰️", "قله‌ای که فقط با تلاش بدنی و پایداری فتح می‌شود.", "A peak conquered only by steady effort.", listOf(Catalog.FITNESS), 3, 5, 25, 0.70f, 0.70f, 0xFFFF7A59),
        RegionDef("castle_discipline", "قلعه انضباط", "Castle of Discipline", "🏰", "دیوارهایی که با عادت‌های روزانه ساخته شده‌اند.", "Walls built from daily habits.", listOf(Catalog.CAREER), 5, 12, 25, 0.38f, 0.55f, 0xFFFFC857),
        RegionDef("valley_creativity", "دره خلاقیت", "Valley of Creativity", "🎨", "رنگ‌ها و ایده‌ها در این دره جاری‌اند.", "Colours and ideas flow through this valley.", listOf(Catalog.CREATIVE), 8, 22, 25, 0.74f, 0.42f, 0xFFE879F9),
        RegionDef("kingdom_wisdom", "پادشاهی خرد", "Kingdom of Wisdom", "🕌", "قلمرو آرامش، مهربانی و بینش.", "A realm of calm, kindness and insight.", listOf(Catalog.MINDFUL, Catalog.SOCIAL), 12, 38, 40, 0.30f, 0.28f, 0xFF9D7CFF),
        RegionDef("financial_empire", "امپراتوری مالی", "Financial Empire", "🏛️", "ثروتی که با نظم و صبر ساخته می‌شود.", "Wealth built with order and patience.", listOf(Catalog.FINANCE), 16, 55, 25, 0.68f, 0.12f, 0xFF39A0FF),
    )
    fun region(id: String): RegionDef? = regions.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- achievements
    private fun ach(id: String, fa: String, en: String, dfa: String, den: String, emoji: String, metric: Metric, target: Int, gold: Int, gems: Int, cat: String? = null) =
        AchievementDef(id, fa, en, dfa, den, emoji, metric, cat, target, gold, gems)

    val achievements = listOf(
        ach("first_quest", "اولین مأموریت", "First Quest", "اولین مأموریتت را کامل کن.", "Complete your first quest.", "🎯", Metric.TOTAL, 1, 50, 0),
        ach("streak_7", "استریک هفت‌روزه", "Seven Day Streak", "هفت روز پشت‌سرهم فعال باش.", "Be active seven days in a row.", "🔥", Metric.BEST_STREAK, 7, 150, 1),
        ach("learning_master", "استاد یادگیری", "Learning Master", "۳۰ مأموریت یادگیری کامل کن.", "Complete 30 learning quests.", "📚", Metric.CATEGORY, 30, 300, 2, LEARNING),
        ach("fitness_champion", "قهرمان تناسب", "Fitness Champion", "۳۰ مأموریت تناسب و سلامت کامل کن.", "Complete 30 fitness quests.", "🏅", Metric.CATEGORY, 30, 300, 2, FITNESS),
        ach("productivity_master", "استاد بهره‌وری", "Productivity Master", "۳۰ مأموریت کاری کامل کن.", "Complete 30 productivity quests.", "⚙️", Metric.CATEGORY, 30, 300, 2, CAREER),
        ach("legendary_discipline", "انضباط افسانه‌ای", "Legendary Discipline", "۳۰ روز پشت‌سرهم فعال باش.", "Be active 30 days in a row.", "👑", Metric.BEST_STREAK, 30, 1000, 5),
        ach("monthly_hero", "قهرمان ماه", "Monthly Hero", "در یک ماه ۲۰ مأموریت کامل کن.", "Complete 20 quests in one month.", "🌟", Metric.MONTH, 20, 400, 2),
        ach("centurion", "صدتایی", "Centurion", "۱۰۰ مأموریت کامل کن.", "Complete 100 quests.", "💯", Metric.TOTAL, 100, 600, 3),
        ach("level_10", "سطح ده", "Level Ten", "به سطح ۱۰ برس.", "Reach level 10.", "🔟", Metric.LEVEL, 10, 300, 1),
        ach("level_25", "سطح بیست‌وپنج", "Level Twenty-Five", "به سطح ۲۵ برس.", "Reach level 25.", "🚀", Metric.LEVEL, 25, 800, 3),
        ach("hard_hitter", "غلبه بر سختی‌ها", "Hard Hitter", "۱۰ مأموریت سخت یا بالاتر کامل کن.", "Complete 10 Hard-or-higher quests.", "🗻", Metric.HARD_PLUS, 10, 250, 1),
        ach("saver", "حسابدار", "Treasurer", "۲۰ مأموریت مالی کامل کن.", "Complete 20 financial quests.", "🏦", Metric.CATEGORY, 20, 250, 1, FINANCE),
        ach("creator", "هنرمند", "Creator", "۲۰ مأموریت خلاقانه کامل کن.", "Complete 20 creative quests.", "🎭", Metric.CATEGORY, 20, 250, 1, CREATIVE),
        ach("kind_heart", "دل مهربان", "Kind Heart", "۲۰ مأموریت اجتماعی کامل کن.", "Complete 20 social quests.", "💖", Metric.CATEGORY, 20, 250, 1, SOCIAL),
        ach("mindful_soul", "جان آرام", "Mindful Soul", "۲۰ مأموریت آرامش و رشد کامل کن.", "Complete 20 mindfulness quests.", "🕊️", Metric.CATEGORY, 20, 250, 1, MINDFUL),
    )
    fun achievement(id: String): AchievementDef? = achievements.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- daily challenge templates
    val dailyChallenges = listOf(
        ChallengeTemplate("ch_walk", "۲۰ دقیقه پیاده‌روی سبک", "Take a relaxed 20-minute walk", FITNESS, Difficulty.NORMAL),
        ChallengeTemplate("ch_stretch", "۱۰ دقیقه حرکات کششی ملایم", "10 minutes of gentle stretching", FITNESS, Difficulty.EASY),
        ChallengeTemplate("ch_water", "امروز به نوشیدن آب فکر کن", "Drink water regularly today", FITNESS, Difficulty.EASY),
        ChallengeTemplate("ch_read", "۲۰ صفحه مطالعه", "Read 20 pages", LEARNING, Difficulty.NORMAL),
        ChallengeTemplate("ch_lang", "۱۵ دقیقه یادگیری یک زبان", "15 minutes of language practice", LEARNING, Difficulty.NORMAL),
        ChallengeTemplate("ch_plan", "برنامهٔ فردا را بنویس", "Write tomorrow's plan", CAREER, Difficulty.EASY),
        ChallengeTemplate("ch_focus", "یک جلسهٔ تمرکز ۲۵ دقیقه‌ای", "One focused 25-minute session", CAREER, Difficulty.NORMAL),
        ChallengeTemplate("ch_budget", "هزینه‌های امروز را ثبت کن", "Log today's expenses", FINANCE, Difficulty.EASY),
        ChallengeTemplate("ch_nobuy", "یک روز بدون خرید غیرضروری", "A day without unnecessary purchases", FINANCE, Difficulty.NORMAL),
        ChallengeTemplate("ch_draw", "۱۵ دقیقه طراحی یا نوشتن آزاد", "15 minutes of free drawing or writing", CREATIVE, Difficulty.NORMAL),
        ChallengeTemplate("ch_idea", "سه ایدهٔ تازه یادداشت کن", "Note down three fresh ideas", CREATIVE, Difficulty.EASY),
        ChallengeTemplate("ch_call", "با یک دوست یا عزیز تماس بگیر", "Call a friend or family member", SOCIAL, Difficulty.NORMAL),
        ChallengeTemplate("ch_thanks", "یک پیام قدردانی بفرست", "Send a thank-you message", SOCIAL, Difficulty.EASY),
        ChallengeTemplate("ch_breathe", "۱۰ دقیقه تنفس آرام", "10 minutes of calm breathing", MINDFUL, Difficulty.EASY),
        ChallengeTemplate("ch_gratitude", "سه چیز برای سپاسگزاری بنویس", "Write three things you're grateful for", MINDFUL, Difficulty.EASY),
    )
    val weekendChallenges = listOf(
        ChallengeTemplate("we_hike", "یک پیاده‌روی طولانی در طبیعت", "A long walk in nature", FITNESS, Difficulty.HARD),
        ChallengeTemplate("we_family", "یک وعدهٔ غذا یا گفتگوی خوب با عزیزان", "A good meal or chat with loved ones", SOCIAL, Difficulty.HARD),
        ChallengeTemplate("we_project", "یک ساعت روی پروژهٔ شخصی‌ات کار کن", "One hour on a personal project", CREATIVE, Difficulty.HARD),
        ChallengeTemplate("we_declutter", "یک گوشه از خانه را مرتب کن", "Tidy one corner of your home", CAREER, Difficulty.HARD),
    )

    fun dailyChallengeFor(epochDay: Long): ChallengeTemplate = dailyChallenges[Math.floorMod(epochDay, dailyChallenges.size.toLong()).toInt()]
    fun weekendChallengeFor(epochDay: Long): ChallengeTemplate = weekendChallenges[Math.floorMod(epochDay / 7, weekendChallenges.size.toLong()).toInt()]

    /** Bonus mission offered by a "bonus mission" surprise (kept tiny and positive). */
    val bonusMissionFa = "ماموریت شگفتی: یک کار کوچک و دلخواه انجام بده"
    val bonusMissionEn = "Surprise mission: do one small thing you enjoy"

    // ---------------------------------------------------------------- avatars & pets
    val avatars = listOf(
        AvatarDef(0, "جادوگر ماه", "Moon Mage", 0xFFB8C0FF, 0xFF4527A0, 0xFF7CF4FF),
        AvatarDef(1, "نگهبان جنگل", "Forest Ranger", 0xFF8D6E63, 0xFF2E7D5B, 0xFF9CFF9C),
        AvatarDef(2, "شوالیهٔ شعله", "Flame Knight", 0xFFFF7043, 0xFFB71C1C, 0xFFFFE082),
        AvatarDef(3, "حکیم دریا", "Sea Sage", 0xFF4DD0E1, 0xFF0277BD, 0xFFFFFFFF),
        AvatarDef(4, "پالادین سپیده", "Dawn Paladin", 0xFFFFE082, 0xFFF9A825, 0xFFFFFFFF),
        AvatarDef(5, "سایهٔ شب", "Night Shade", 0xFF37474F, 0xFF263238, 0xFFD18CFF),
        AvatarDef(6, "ستاره‌نواز", "Star Bard", 0xFFF48FB1, 0xFF8E24AA, 0xFFFFF59D),
        AvatarDef(7, "نگهبان کریستال", "Crystal Warden", 0xFFE0E0E0, 0xFF00897B, 0xFF80FFEA),
    )
    fun avatar(id: Int): AvatarDef = avatars.getOrElse(id) { avatars[0] }

    val pets = listOf(
        PetDef(0, "اژدهای آبی", "Aqua Dragon", 0xFF4F8DF7, 0xFFDCEBFF, 0xFFFF8FD1, 0xFF39A0FF),
        PetDef(1, "اژدهای کریستالی", "Crystal Dragon", 0xFF7C4DFF, 0xFFD4B8FF, 0xFF5CE1E6, 0xFF9D7CFF),
        PetDef(2, "اژدهای اخگر", "Ember Dragon", 0xFFFF8A3D, 0xFFFFE0B2, 0xFFFFD54F, 0xFFFFA040),
    )
    fun pet(id: Int): PetDef = pets.getOrElse(id) { pets[0] }

    // ---------------------------------------------------------------- hero evolution
    fun heroTier(level: Int): Int = when { level < 10 -> 0; level < 25 -> 1; level < 50 -> 2; else -> 3 }
    fun petStage(level: Int): Int = when { level < 10 -> 1; level < 25 -> 2; else -> 3 }

    fun heroTitle(level: Int, lang: String): String {
        val (fa, en) = when {
            level < 5 -> "تازه‌کار" to "Novice"
            level < 10 -> "شاگرد" to "Apprentice"
            level < 20 -> "ماجراجو" to "Adventurer"
            level < 35 -> "قهرمان" to "Champion"
            level < 50 -> "سلحشور" to "Paladin"
            level < 70 -> "افسانه" to "Legend"
            else -> "اسطوره" to "Mythic"
        }
        return if (lang == "fa") fa else en
    }

    /** Pure game number, unrelated to any real health measurement. */
    fun powerScore(level: Int, stats: Map<Stat, Int>): Int = level * 20 + stats.values.sum() * 5
}
