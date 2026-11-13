package com.kelsuita.starswish.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal data class SnapshotAccountBalance(val name: String, val currency: Int, val cents: Long, val deleted: Boolean)

internal object SnapshotBalanceReader {
    private data class Property(val name: String, val type: Int, val offset: Int)
    private data class Entity(val id: Int, val name: String, val properties: List<Property>)
    private class Table(private val data: ByteArray, private val pos: Int = Constants.u32(data, 0).toInt()) {
        private val vtable = pos - Constants.i32(data, pos)
        fun field(slot: Int): Int? {
            val entry = 4 + slot * 2
            if (entry + 2 > Constants.u16(data, vtable)) return null
            val offset = Constants.u16(data, vtable + entry)
            return if (offset == 0) null else pos + offset
        }
        fun uint(slot: Int, default: Long = 0L): Long = field(slot)?.let { Constants.u32(data, it) } ?: default
        fun long(slot: Int): Long = field(slot)?.let { Constants.i64(data, it) } ?: 0L
        fun ushort(slot: Int): Int = field(slot)?.let { Constants.u16(data, it) } ?: 0
        fun string(slot: Int): String? = field(slot)?.let { p ->
            val target = p + Constants.u32(data, p).toInt()
            val size = Constants.u32(data, target).toInt()
            require(size >= 0 && target + 4 + size <= data.size)
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data, target + 4, size)).toString()
        }
        fun tables(slot: Int): List<Table> = field(slot)?.let { p ->
            val target = p + Constants.u32(data, p).toInt()
            val size = Constants.u32(data, target).toInt()
            require(size in 0..100_000 && target + 4L + size * 4L <= data.size)
            (0 until size).map { index ->
                val element = target + 4 + index * 4
                Table(data, element + Constants.u32(data, element).toInt())
            }
        }.orEmpty()
        fun bytes(slot: Int): ByteArray? = field(slot)?.let { p ->
            val target = p + Constants.u32(data, p).toInt()
            val size = Constants.u32(data, target).toInt()
            require(size in 0..Constants.MAX_FIELD_BYTES && target + 4L + size <= data.size)
            data.copyOfRange(target + 4, target + 4 + size)
        }
    }

    suspend fun read(database: File): List<SnapshotAccountBalance> = withContext(Dispatchers.IO) {
        require(database.length() in 1..Constants.MAX_DATABASE_BYTES) { "数据库大小无效" }
        val entries = LmdbSnapshot.open(database).entries()
        val entities = readSchema(entries)
        val accounts = entities.values.singleOrNull { it.name == "AccountEntity" }
            ?: error("备份中未找到唯一的 AccountEntity")
        val details = entities.values.singleOrNull { it.name == "DetailEntity" }
            ?: error("备份中未找到唯一的 DetailEntity")
        val cycles = entities.values.singleOrNull { it.name == "CycleEntity" }
        val cycleRows = cycles?.let { readRows(entries, it) }.orEmpty()
        if (cycleRows.isNotEmpty()) throw SyncError("webdav.error_cycles")
        val accountRows = readRows(entries, accounts)
        val accountIds = accountRows.associate { row ->
            val id = (row["id"] as? Number)?.toLong() ?: error("账户 ID 缺失")
            id to true
        }
        val centsById = accountIds.keys.associateWith { 0L }.toMutableMap()
        for (row in readRows(entries, details)) {
            if (row["isDelete"] == true || row["isPre"] == true) continue
            val type = (row["detailType"] as? Number)?.toInt() ?: error("明细类型缺失")
            val source = (row["accountId"] as? Number)?.toLong() ?: 0L
            val destination = (row["account2Id"] as? Number)?.toLong() ?: 0L
            val amount = amountCents(row["accountAmount"])
            when (type) {
                1, 4 -> add(centsById, source, amount)
                0 -> add(centsById, source, -amount)
                2, 3 -> {
                    add(centsById, source, -amount)
                    add(centsById, destination, amountCents(row["accountAmount2"]))
                }
                else -> error("未支持的明细类型 $type，已停止同步")
            }
        }
        accountRows.mapNotNull { row ->
            if (row["isDelete"] == true) return@mapNotNull null
            val id = (row["id"] as Number).toLong()
            val name = (row["name"] as? String)?.takeIf(String::isNotBlank)
                ?: error("账户名称缺失")
            val currency = (row["currency"] as? Number)?.toInt() ?: 0
            SnapshotAccountBalance(name, currency, centsById[id] ?: error("账户明细关联不一致"), false)
        }
    }

    private fun readSchema(entries: List<Pair<ByteArray, ByteArray>>): Map<Int, Entity> {
        val output = linkedMapOf<Int, Entity>()
        for ((key, value) in entries) {
            if (key.size != 8 || key.take(4).any { it != 0.toByte() } || key.all { it == 0.toByte() }) continue
            val table = Table(value)
            val id = table.uint(1).toInt()
            if (id.toLong() != Constants.u64be(key)) continue
            val name = table.string(3) ?: error("实体名称缺失")
            val props = table.tables(4).map { p ->
                val type = p.ushort(7)
                val offset = p.ushort(8)
                check(offset >= 4 && offset % 2 == 0) { "属性偏移无效：${p.string(6)}" }
                Property(p.string(6) ?: error("属性名称缺失"), type, offset)
            }
            output[id] = Entity(id, name, props)
        }
        check(output.isNotEmpty()) { "LMDB 模型表为空" }
        return output
    }

    private fun readRows(entries: List<Pair<ByteArray, ByteArray>>, entity: Entity): List<Map<String, Any?>> {
        val output = mutableListOf<Map<String, Any?>>()
        for ((key, value) in entries) {
            if (key.size != 8) continue
            val prefix = Constants.u32be(key)
            if ((prefix and 0xff000000L) != 0x18000000L || prefix % 4L != 0L) continue
            if ((prefix - 0x18000000L) / 4L != entity.id.toLong()) continue
            val table = Table(value)
            val row = entity.properties.associate { property ->
                val slot = (property.offset - 4) / 2
                property.name to readValue(table, value, slot, property.type)
            }
            check((row["id"] as? Number)?.toLong() == Constants.u32be(key, 4)) { "${entity.name} 记录 ID 与 LMDB 键不一致" }
            output += row
        }
        return output
    }

    private fun readValue(table: Table, bytes: ByteArray, slot: Int, type: Int): Any? = when (type) {
        1 -> table.field(slot)?.let { bytes[it].toInt() != 0 } ?: false
        2 -> table.field(slot)?.let { bytes[it].toInt() } ?: 0
        3, 4 -> table.field(slot)?.let { Constants.u16(bytes, it) } ?: 0
        5 -> table.field(slot)?.let { Constants.i32(bytes, it) } ?: 0
        6, 10, 12 -> table.field(slot)?.let { Constants.i64(bytes, it) } ?: 0L
        // ObjectBox stores a ToOne relation inline as an unsigned 64-bit target id; the
        // account/detail links rely on it, so it must be read rather than skipped.
        11 -> table.field(slot)?.let { Constants.i64(bytes, it) } ?: 0L
        7 -> table.field(slot)?.let { Float.fromBits(Constants.i32(bytes, it)) } ?: 0f
        8 -> table.field(slot)?.let { Double.fromBits(Constants.i64(bytes, it)) } ?: 0.0
        9 -> table.string(slot)
        23 -> table.bytes(slot)?.joinToString("") { "%02x".format(it) }
        // Relations and other ObjectBox-only types are irrelevant here; skipping them keeps a
        // backup readable when the schema grows a property this reader does not consume.
        else -> null
    }

    private fun amountCents(raw: Any?): Long {
        val number = when (raw) {
            is Number -> raw.toString().toBigDecimal()
            is String -> raw.toBigDecimalOrNull() ?: error("金额数据无效")
            else -> error("金额字段缺失")
        }
        return number.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
    }

    private fun add(balances: MutableMap<Long, Long>, id: Long, amount: Long) {
        if (id == 0L) return
        balances[id] = Math.addExact(balances[id] ?: error("明细指向不存在的账户 $id"), amount)
    }

    private object Constants {
        const val MAX_DATABASE_BYTES = 256L * 1024 * 1024
        const val MAX_FIELD_BYTES = 16 * 1024 * 1024
        fun u16(b: ByteArray, p: Int) = (b[p].toInt() and 255) or ((b[p + 1].toInt() and 255) shl 8)
        fun u32(b: ByteArray, p: Int) = (u16(b, p).toLong() or (u16(b, p + 2).toLong() shl 16))
        fun i32(b: ByteArray, p: Int) = u32(b, p).toInt()
        fun i64(b: ByteArray, p: Int) = ByteBuffer.wrap(b, p, 8).order(ByteOrder.LITTLE_ENDIAN).long
        fun u32be(b: ByteArray, p: Int = 0) = ((b[p].toLong() and 255) shl 24) or ((b[p + 1].toLong() and 255) shl 16) or
            ((b[p + 2].toLong() and 255) shl 8) or (b[p + 3].toLong() and 255)
        fun u64be(b: ByteArray) = b.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 255) }
    }
}
