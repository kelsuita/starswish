package com.kelsuita.starswish.sync

import java.io.File

/**
 * Read-only LMDB page walker.
 *
 * ObjectBox keeps every entity row in the single unnamed LMDB database, so a full key/value scan
 * is enough. Reading the B+tree directly avoids LMDB's JNI wrapper: its native library is only
 * shipped for a couple of ABIs, which made snapshot parsing fail on other devices. This reader
 * only inspects bytes already loaded in memory and never writes to the snapshot.
 */
internal class LmdbSnapshot private constructor(
    private val data: ByteArray,
    private val pageSize: Int,
    private val rootPage: Long,
) {
    /** All key/value pairs of the main database, in LMDB key order. */
    fun entries(): List<Pair<ByteArray, ByteArray>> {
        val output = ArrayList<Pair<ByteArray, ByteArray>>(256)
        // Explicit stack: the tree depth is small, but recursion on untrusted input is not needed.
        val pending = ArrayDeque<Long>()
        pending.addLast(rootPage)
        var visited = 0
        while (pending.isNotEmpty()) {
            val pgno = pending.removeLast()
            require(pgno in 2 until maxPage()) { "LMDB 页号超出范围：$pgno" }
            check(++visited <= maxPage().toInt()) { "LMDB 页数超出预期" }
            val pageOffset = pageOffset(pgno)
            val flags = u16(pageOffset + PAGE_HEADER_FLAGS)
            when {
                flags and P_BRANCH != 0 -> {
                    for (index in 0 until nodeCount(pageOffset)) {
                        val node = pageOffset + nodePointer(pageOffset, index)
                        pending.addLast(childPage(node))
                    }
                }
                flags and P_LEAF != 0 -> readLeaf(pageOffset, output)
                else -> error("LMDB 数据页类型不受支持：0x${flags.toString(16)}")
            }
        }
        return output
    }

    private fun maxPage() = (data.size / pageSize).toLong()

    private fun pageOffset(pgno: Long): Int {
        val offset = pgno * pageSize
        require(offset in 0..(data.size - MIN_PAGE_BYTES).toLong()) { "LMDB 页偏移超出文件范围：$pgno" }
        return offset.toInt()
    }

    private fun nodePointer(pageOffset: Int, index: Int): Int {
        val pointer = pageOffset + PAGE_HEADER_BYTES + index * 2
        require(pointer + 2 <= data.size) { "LMDB 节点指针越界" }
        return u16(pointer)
    }

    private fun nodeCount(pageOffset: Int): Int {
        val lower = u16(pageOffset + PAGE_HEADER_LOWER)
        require(lower >= PAGE_HEADER_BYTES) { "LMDB 页头长度无效：$lower" }
        return (lower - PAGE_HEADER_BYTES) / 2
    }

    private fun childPage(node: Int): Long {
        require(node + 4 <= data.size) { "LMDB 分支节点越界" }
        return u16(node).toLong() or (u16(node + 2).toLong() shl 16)
    }

    private fun readLeaf(pageOffset: Int, output: MutableList<Pair<ByteArray, ByteArray>>) {
        for (index in 0 until nodeCount(pageOffset)) {
            val node = pageOffset + nodePointer(pageOffset, index)
            require(node + NODE_HEADER_BYTES <= data.size) { "LMDB 节点越界" }
            val flags = u16(node + 4)
            require(flags and (F_DUPDATA or F_SUBDATA) == 0) { "LMDB 数据页包含重复键，当前不支持" }
            val keySize = u16(node + 6)
            val dataSize = u16(node) or (u16(node + 2) shl 16)
            require(dataSize in 0..MAX_FIELD_BYTES) { "LMDB 数据长度超出限制：$dataSize" }
            val keyStart = node + NODE_HEADER_BYTES
            require(keyStart + keySize <= data.size) { "LMDB 键越界" }
            val key = data.copyOfRange(keyStart, keyStart + keySize)
            val valueStart = keyStart + keySize
            val value = if (flags and F_BIGDATA != 0) {
                require(valueStart + 8 <= data.size) { "LMDB 溢出页指针越界" }
                overflowValue(u64(valueStart), dataSize)
            } else {
                require(valueStart + dataSize <= data.size) { "LMDB 数据越界" }
                data.copyOfRange(valueStart, valueStart + dataSize)
            }
            output += key to value
        }
    }

    private fun overflowValue(pgno: Long, dataSize: Int): ByteArray {
        require(pgno in 2 until maxPage()) { "LMDB 溢出页号超出范围：$pgno" }
        val pageOffset = pageOffset(pgno)
        val pages = u32(pageOffset + PAGE_HEADER_FLAGS)
        require(pages > 0) { "LMDB 溢出页长度无效" }
        require(pages * pageSize - PAGE_HEADER_BYTES >= dataSize) { "LMDB 溢出页长度不足" }
        val start = pageOffset + PAGE_HEADER_BYTES
        require(start + dataSize <= data.size) { "LMDB 溢出页越界" }
        return data.copyOfRange(start, start + dataSize)
    }

    private fun u16(offset: Int): Int {
        require(offset >= 0 && offset + 2 <= data.size) { "LMDB 读取越界" }
        return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun u32(offset: Int): Long = u16(offset).toLong() or (u16(offset + 2).toLong() shl 16)

    private fun u64(offset: Int): Long {
        require(offset >= 0 && offset + 8 <= data.size) { "LMDB 读取越界" }
        var value = 0L
        for (index in 7 downTo 0) value = (value shl 8) or (data[offset + index].toLong() and 0xFF)
        return value
    }

    private data class Meta(val root: Long, val txnId: Long)

    companion object {
        private const val MAGIC = 0xBEEFC0DEL
        private const val VERSION = 1L
        private const val PAGE_HEADER_BYTES = 16
        private const val PAGE_HEADER_FLAGS = 10
        private const val PAGE_HEADER_LOWER = 12
        private const val NODE_HEADER_BYTES = 8
        private const val MIN_PAGE_BYTES = 512
        private const val MAX_FIELD_BYTES = 16 * 1024 * 1024
        private const val META_MAGIC = 16
        private const val META_VERSION = 20
        /** `mm_dbs[1].md_root`: the main database, i.e. `MAIN_DBI`. */
        private const val META_MAIN_ROOT = 128
        private const val META_TXN_ID = 144
        private const val META_BYTES = 152
        /** mp_flags of a branch page. */
        private const val P_BRANCH = 0x01
        /** mp_flags of a leaf page. */
        private const val P_LEAF = 0x02
        /** Node value lives in overflow pages. */
        private const val F_BIGDATA = 0x01
        private const val F_DUPDATA = 0x02
        private const val F_SUBDATA = 0x04
        private val PAGE_SIZES = intArrayOf(4096, 8192, 16384, 32768, 65536)

        /** Loads the whole snapshot. The caller bounds the database size before this runs. */
        fun open(database: File): LmdbSnapshot {
            val bytes = database.readBytes()
            val pageSize = PAGE_SIZES.firstOrNull { size ->
                bytes.size >= size * 2 && metaAt(bytes, 0) != null && pageNumberAt(bytes, size) == 1L
            } ?: throw SyncError("webdav.error_unsupported")
            val meta = listOfNotNull(metaAt(bytes, 0), metaAt(bytes, pageSize)).maxBy { it.txnId }
            return LmdbSnapshot(bytes, pageSize, meta.root)
        }

        private fun pageNumberAt(bytes: ByteArray, offset: Int): Long {
            if (offset + 8 > bytes.size) return -1
            var value = 0L
            for (index in 7 downTo 0) value = (value shl 8) or (bytes[offset + index].toLong() and 0xFF)
            return value
        }

        private fun metaAt(bytes: ByteArray, offset: Int): Meta? {
            if (offset + META_BYTES > bytes.size) return null
            fun u16(p: Int) = (bytes[p].toInt() and 0xFF) or ((bytes[p + 1].toInt() and 0xFF) shl 8)
            fun u32(p: Int) = u16(p).toLong() or (u16(p + 2).toLong() shl 16)
            fun u64(p: Int) = pageNumberAt(bytes, p)
            if (u32(offset + META_MAGIC) != MAGIC || u32(offset + META_VERSION) != VERSION) return null
            val root = u64(offset + META_MAIN_ROOT)
            if (root < 2) return null
            return Meta(root, u64(offset + META_TXN_ID))
        }
    }
}
