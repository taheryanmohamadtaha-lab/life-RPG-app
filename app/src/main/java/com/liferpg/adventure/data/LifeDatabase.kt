package com.liferpg.adventure.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        PlayerEntity::class, QuestEntity::class, CompletionEntity::class, LevelHistoryEntity::class,
        AchievementUnlockEntity::class, CustomAchievementEntity::class, SkillRankEntity::class,
        OwnedItemEntity::class, RewardEntity::class, PurchaseEntity::class, CategoryEntity::class,
        RestDayEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao
    abstract fun categoryDao(): CategoryDao
    abstract fun completionDao(): CompletionDao
    abstract fun progressDao(): ProgressDao
    abstract fun rewardDao(): RewardDao

    companion object {
        /**
         * Schema upgrades go here. NEVER use fallbackToDestructiveMigration: user progress must survive updates.
         * Example for version 2:
         *   val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE ...") } }
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun build(context: Context): LifeDatabase =
            Room.databaseBuilder(context.applicationContext, LifeDatabase::class.java, "life_rpg.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
