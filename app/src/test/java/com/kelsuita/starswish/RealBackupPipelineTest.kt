package com.kelsuita.starswish

import com.kelsuita.starswish.sync.SnapshotBalanceReader
import com.kelsuita.starswish.sync.XiaoxingBackupDecryptor
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Manual pipeline check against a real 4.0.8 backup; skipped when no sample is configured. */
class RealBackupPipelineTest {
    @Test fun readsBalancesFromRealBackup() {
        val archive = File(System.getenv("STARWISH_BACKUP") ?: return)
        assumeTrue(archive.isFile)
        val database = File.createTempFile("snapshot", ".mdb")
        try {
            XiaoxingBackupDecryptor.decrypt(archive, database)
            val balances = runBlocking { SnapshotBalanceReader.read(database) }
            balances.forEach { println("BALANCE ${it.name}\t${it.currency}\t${it.cents}") }
            assertTrue(balances.size >= 20)
            assertEquals(15937L, balances.single { it.name == "微信钱包" }.cents)
            assertEquals(600000L, balances.single { it.name == "成都农商银行" }.cents)
            assertEquals(21051L, balances.single { it.name == "招商银行万事网联卡" }.cents)
            assertEquals(1, balances.count { it.name == "微信钱包" })
        } finally {
            database.delete()
        }
    }
}
