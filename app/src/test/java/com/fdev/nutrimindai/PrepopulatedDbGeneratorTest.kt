package com.fdev.nutrimindai

import com.fdev.nutrimindai.core.database.DB_VERSION
import com.fdev.nutrimindai.feature.dailyintake.data.seeder.DefaultFoodCatalog
import kotlinx.serialization.json.*
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class PrepopulatedDbGeneratorTest {

    @Test
    fun generatePrepopulatedDb() {
        val projectDir = File(".").absoluteFile
        val rootAppDir = if (File(projectDir, "app").exists()) File(projectDir, "app") else projectDir
        val schemaFile = File(rootAppDir, "schemas/com.fdev.nutrimindai.core.database.NutriMindDatabase/$DB_VERSION.json")
        val assetsDbDir = File(rootAppDir, "src/main/assets/database")
        assetsDbDir.mkdirs()
        val dbFile = File(assetsDbDir, "nutrimind.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val json = Json { ignoreUnknownKeys = true }
        val rootElement = json.parseToJsonElement(schemaFile.readText()).jsonObject
        val databaseObj = rootElement["database"]!!.jsonObject
        val entities = databaseObj["entities"]!!.jsonArray
        val setupQueries = databaseObj["setupQueries"]!!.jsonArray

        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            conn.autoCommit = false
            val statement = conn.createStatement()
            statement.execute("PRAGMA user_version = $DB_VERSION")

            // 1. Create all entity tables and indices
            for (element in entities) {
                val entity = element.jsonObject
                val tableName = entity["tableName"]!!.jsonPrimitive.content
                val createSql = entity["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", tableName)
                statement.execute(createSql)

                val indices = entity["indices"]?.jsonArray
                if (indices != null) {
                    for (idxElement in indices) {
                        val indexObj = idxElement.jsonObject
                        val indexSql = indexObj["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", tableName)
                        statement.execute(indexSql)
                    }
                }
            }

            // 2. Run setupQueries (room_master_table and identity_hash)
            for (element in setupQueries) {
                val sql = element.jsonPrimitive.content
                statement.execute(sql)
            }

            // 3. Prepopulate food_table with DefaultFoodCatalog
            val insertFoodSql = """
                INSERT INTO food_table (id, name, calories, protein, carbs, fat, servingSize, servingUnit, isSync)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val prepStatement = conn.prepareStatement(insertFoodSql)
            for (food in DefaultFoodCatalog.foods) {
                prepStatement.setString(1, food.id)
                prepStatement.setString(2, food.name)
                prepStatement.setInt(3, food.calories)
                prepStatement.setDouble(4, food.protein)
                prepStatement.setDouble(5, food.carbs)
                prepStatement.setDouble(6, food.fat)
                prepStatement.setDouble(7, food.servingSize)
                prepStatement.setString(8, food.servingUnit)
                prepStatement.setInt(9, if (food.isSync) 1 else 0)
                prepStatement.addBatch()
            }
            prepStatement.executeBatch()

            conn.commit()
        }

        println("Prepopulated database successfully generated at: ${dbFile.absolutePath} (size: ${dbFile.length()} bytes)")
        assert(dbFile.exists() && dbFile.length() > 0)
    }
}
