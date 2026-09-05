package com.imobiliario.aluno.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PerfilAluno::class, DisciplinaCache::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun perfilAlunoDao(): PerfilAlunoDao
    abstract fun disciplinaDao(): DisciplinaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Migração 1 → 2: adicionou a tabela `disciplinas_cache`.
         * Nenhuma coluna existente foi alterada — apenas CREATE TABLE.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `disciplinas_cache` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `codigo_aluno` TEXT NOT NULL,
                        `disciplina_id` INTEGER NOT NULL,
                        `nome_disciplina` TEXT NOT NULL,
                        `professor` TEXT NOT NULL,
                        `notas_json` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_disciplinas_cache_codigo_aluno` " +
                    "ON `disciplinas_cache` (`codigo_aluno`)"
                )
            }
        }

        /**
         * Migração 2 → 3: adicionou a coluna `classeNome` e `turmaId` em
         * `perfil_aluno` com valores default, sem perda de dados existentes.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `perfil_aluno` ADD COLUMN `classeNome` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `perfil_aluno` ADD COLUMN `turmaId` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meufilho_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
