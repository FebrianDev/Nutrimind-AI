package com.fdev.nutrimindai.core.database

import com.fdev.nutrimindai.core.database.seeder.DemoAccountSeeder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAccountSeederTest {

    @Test
    fun isDemoAccount_recognizesAlexAccounts() {
        assertTrue(DemoAccountSeeder.isDemoAccount("demo.gain@nutrimind.ai"))
        assertTrue(DemoAccountSeeder.isDemoAccount("alex@nutrimind.ai"))
        assertTrue(DemoAccountSeeder.isDemoAccount(" DEMO.GAIN@nutrimind.ai "))
        assertTrue(DemoAccountSeeder.isDemoAccount(" Alex@Nutrimind.ai "))
    }

    @Test
    fun isDemoAccount_recognizesSarahAccounts() {
        assertTrue(DemoAccountSeeder.isDemoAccount("demo.loss@nutrimind.ai"))
        assertTrue(DemoAccountSeeder.isDemoAccount("sarah@nutrimind.ai"))
        assertTrue(DemoAccountSeeder.isDemoAccount(" DEMO.LOSS@nutrimind.ai "))
        assertTrue(DemoAccountSeeder.isDemoAccount(" Sarah@Nutrimind.ai "))
    }

    @Test
    fun isDemoAccount_rejectsNormalAccounts() {
        assertFalse(DemoAccountSeeder.isDemoAccount("user@gmail.com"))
        assertFalse(DemoAccountSeeder.isDemoAccount("random@nutrimind.ai"))
        assertFalse(DemoAccountSeeder.isDemoAccount(""))
    }

    @Test
    fun isAlexAccount_distinguishesBetweenAlexAndSarah() {
        assertTrue(DemoAccountSeeder.isAlexAccount("demo.gain@nutrimind.ai"))
        assertTrue(DemoAccountSeeder.isAlexAccount("alex@nutrimind.ai"))
        assertFalse(DemoAccountSeeder.isAlexAccount("demo.loss@nutrimind.ai"))
        assertFalse(DemoAccountSeeder.isAlexAccount("sarah@nutrimind.ai"))
    }
}

