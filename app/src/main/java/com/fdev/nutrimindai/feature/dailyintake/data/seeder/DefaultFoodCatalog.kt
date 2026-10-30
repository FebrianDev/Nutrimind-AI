package com.fdev.nutrimindai.feature.dailyintake.data.seeder

import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import java.util.UUID

object DefaultFoodCatalog {
    val foods: List<FoodEntity> = listOf(
        // --- Poultry & Eggs ---
        FoodEntity(name = "Grilled Chicken Breast", calories = 165, protein = 31.0, carbs = 0.0, fat = 3.6, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Roast Turkey Breast", calories = 135, protein = 30.0, carbs = 0.0, fat = 1.0, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Boiled Egg", calories = 78, protein = 6.3, carbs = 0.6, fat = 5.3, servingSize = 1.0, servingUnit = "egg", isSync = true),
        FoodEntity(name = "Scrambled Eggs with Olive Oil", calories = 180, protein = 13.0, carbs = 1.2, fat = 14.0, servingSize = 2.0, servingUnit = "eggs", isSync = true),
        FoodEntity(name = "Egg White Omelette", calories = 75, protein = 15.0, carbs = 1.0, fat = 0.5, servingSize = 1.0, servingUnit = "omelette", isSync = true),
        FoodEntity(name = "Baked Chicken Thigh (Skinless)", calories = 209, protein = 26.0, carbs = 0.0, fat = 11.0, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Grilled Chicken Strips", calories = 150, protein = 28.0, carbs = 1.0, fat = 3.0, servingSize = 100.0, servingUnit = "g", isSync = true),

        // --- Meat & Seafood ---
        FoodEntity(name = "Beef Sirloin Steak (Lean)", calories = 280, protein = 38.0, carbs = 0.0, fat = 14.0, servingSize = 150.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Lean Ground Beef (90/10)", calories = 215, protein = 26.0, carbs = 0.0, fat = 12.0, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Baked Atlantic Salmon", calories = 310, protein = 34.0, carbs = 0.0, fat = 19.0, servingSize = 150.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Canned Tuna in Water", calories = 130, protein = 28.0, carbs = 0.0, fat = 1.0, servingSize = 1.0, servingUnit = "can", isSync = true),
        FoodEntity(name = "Grilled Shrimp / Prawns", calories = 99, protein = 24.0, carbs = 0.2, fat = 0.3, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "White Cod Fillet", calories = 145, protein = 31.0, carbs = 0.0, fat = 1.5, servingSize = 150.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Grilled Salmon Salad", calories = 360, protein = 32.0, carbs = 8.0, fat = 22.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),

        // --- Grains, Bread & Cereals ---
        FoodEntity(name = "Cooked Brown Rice", calories = 218, protein = 4.5, carbs = 45.8, fat = 1.6, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Cooked Jasmine White Rice", calories = 205, protein = 4.2, carbs = 44.5, fat = 0.4, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Rolled Oatmeal with Water", calories = 190, protein = 6.5, carbs = 34.0, fat = 3.5, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Oatmeal with Honey & Berries", calories = 280, protein = 8.0, carbs = 54.0, fat = 4.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Whole Wheat Bread", calories = 95, protein = 4.0, carbs = 16.5, fat = 1.5, servingSize = 1.0, servingUnit = "slice", isSync = true),
        FoodEntity(name = "Sourdough Toast", calories = 110, protein = 4.5, carbs = 22.0, fat = 0.8, servingSize = 1.0, servingUnit = "slice", isSync = true),
        FoodEntity(name = "Baked Sweet Potato", calories = 135, protein = 3.0, carbs = 31.5, fat = 0.2, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Boiled Russet Potato", calories = 160, protein = 4.3, carbs = 37.0, fat = 0.2, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Cooked Quinoa", calories = 222, protein = 8.1, carbs = 39.4, fat = 3.6, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Whole Wheat Pasta (Cooked)", calories = 174, protein = 7.5, carbs = 37.0, fat = 0.8, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Avocado Toast on Sourdough", calories = 260, protein = 6.0, carbs = 26.0, fat = 15.0, servingSize = 1.0, servingUnit = "slice", isSync = true),

        // --- Dairy & Plant Alternatives ---
        FoodEntity(name = "Plain Greek Yogurt (0% Fat)", calories = 120, protein = 20.0, carbs = 7.0, fat = 0.5, servingSize = 200.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Whole Milk", calories = 149, protein = 7.7, carbs = 11.7, fat = 7.9, servingSize = 240.0, servingUnit = "ml", isSync = true),
        FoodEntity(name = "Skim Milk", calories = 83, protein = 8.3, carbs = 12.2, fat = 0.2, servingSize = 240.0, servingUnit = "ml", isSync = true),
        FoodEntity(name = "Unsweetened Almond Milk", calories = 30, protein = 1.0, carbs = 1.0, fat = 2.5, servingSize = 240.0, servingUnit = "ml", isSync = true),
        FoodEntity(name = "Low-Fat Cottage Cheese", calories = 163, protein = 28.0, carbs = 6.1, fat = 2.3, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Cheddar Cheese", calories = 113, protein = 7.0, carbs = 0.4, fat = 9.3, servingSize = 1.0, servingUnit = "slice", isSync = true),
        FoodEntity(name = "Whey Protein Isolate Shake", calories = 120, protein = 25.0, carbs = 2.0, fat = 1.0, servingSize = 1.0, servingUnit = "scoop", isSync = true),
        FoodEntity(name = "Organic Firm Tofu", calories = 83, protein = 10.0, carbs = 2.3, fat = 5.0, servingSize = 100.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Soy Tempeh", calories = 192, protein = 20.3, carbs = 7.6, fat = 10.8, servingSize = 100.0, servingUnit = "g", isSync = true),

        // --- Fruits & Berries ---
        FoodEntity(name = "Fresh Banana", calories = 105, protein = 1.3, carbs = 27.0, fat = 0.3, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Fresh Red Apple", calories = 95, protein = 0.5, carbs = 25.0, fat = 0.3, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Fresh Blueberries", calories = 84, protein = 1.1, carbs = 21.5, fat = 0.5, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Fresh Strawberries", calories = 49, protein = 1.0, carbs = 11.7, fat = 0.5, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Fresh Hass Avocado", calories = 160, protein = 2.0, carbs = 8.5, fat = 14.7, servingSize = 0.5, servingUnit = "fruit", isSync = true),
        FoodEntity(name = "Fresh Orange", calories = 62, protein = 1.2, carbs = 15.4, fat = 0.2, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Watermelon Cubes", calories = 46, protein = 0.9, carbs = 11.5, fat = 0.2, servingSize = 1.0, servingUnit = "cup", isSync = true),

        // --- Vegetables & Greens ---
        FoodEntity(name = "Steamed Green Broccoli", calories = 55, protein = 3.7, carbs = 11.2, fat = 0.6, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Fresh Baby Spinach", calories = 14, protein = 1.7, carbs = 2.2, fat = 0.2, servingSize = 2.0, servingUnit = "cups", isSync = true),
        FoodEntity(name = "Garden Salad with Olive Oil", calories = 140, protein = 2.5, carbs = 8.0, fat = 11.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Grilled Green Asparagus", calories = 27, protein = 3.0, carbs = 5.2, fat = 0.2, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Crunchy Baby Carrots", calories = 25, protein = 0.6, carbs = 5.8, fat = 0.1, servingSize = 1.0, servingUnit = "medium", isSync = true),
        FoodEntity(name = "Sliced Fresh Cucumber", calories = 15, protein = 0.7, carbs = 3.6, fat = 0.1, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Roasted Brussels Sprouts", calories = 65, protein = 4.5, carbs = 13.5, fat = 0.8, servingSize = 1.0, servingUnit = "cup", isSync = true),
        FoodEntity(name = "Steamed Green Beans", calories = 44, protein = 2.4, carbs = 9.9, fat = 0.4, servingSize = 1.0, servingUnit = "cup", isSync = true),

        // --- Nuts, Seeds & Healthy Fats ---
        FoodEntity(name = "Natural Creamy Peanut Butter", calories = 190, protein = 8.0, carbs = 7.0, fat = 16.0, servingSize = 2.0, servingUnit = "tbsp", isSync = true),
        FoodEntity(name = "Raw Almonds", calories = 164, protein = 6.0, carbs = 6.1, fat = 14.2, servingSize = 28.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Raw Walnuts", calories = 185, protein = 4.3, carbs = 3.9, fat = 18.5, servingSize = 28.0, servingUnit = "g", isSync = true),
        FoodEntity(name = "Organic Chia Seeds", calories = 117, protein = 4.0, carbs = 10.0, fat = 7.5, servingSize = 2.0, servingUnit = "tbsp", isSync = true),
        FoodEntity(name = "Extra Virgin Olive Oil", calories = 119, protein = 0.0, carbs = 0.0, fat = 13.5, servingSize = 1.0, servingUnit = "tbsp", isSync = true),
        FoodEntity(name = "Dark Chocolate (85% Cocoa)", calories = 120, protein = 2.0, carbs = 7.0, fat = 10.0, servingSize = 20.0, servingUnit = "g", isSync = true),

        // --- Prepared Meals & Bowls ---
        FoodEntity(name = "Caesar Salad with Grilled Chicken", calories = 380, protein = 32.0, carbs = 12.0, fat = 22.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Mediterranean Tuna Salad Bowl", calories = 290, protein = 30.0, carbs = 6.0, fat = 16.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Turkey Avocado Protein Wrap", calories = 420, protein = 28.0, carbs = 38.0, fat = 17.0, servingSize = 1.0, servingUnit = "wrap", isSync = true),
        FoodEntity(name = "Quinoa Vegetable Buddha Bowl", calories = 360, protein = 14.0, carbs = 54.0, fat = 11.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Hearty Lentil Vegetable Soup", calories = 230, protein = 14.0, carbs = 38.0, fat = 3.0, servingSize = 1.0, servingUnit = "bowl", isSync = true),
        FoodEntity(name = "Spaghetti Bolognese (Lean Beef)", calories = 490, protein = 32.0, carbs = 58.0, fat = 14.0, servingSize = 1.0, servingUnit = "plate", isSync = true)
    ).map { food ->
        food.copy(
            id = UUID.nameUUIDFromBytes("default_food:${food.name}".toByteArray(Charsets.UTF_8)).toString()
        )
    }
}
