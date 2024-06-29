/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.utils

import java.io.InputStream

/**
 * A wrapper class for an InputStream that counts the number of bytes read from it.
 */
class CountingInputStream(private val wrappedInputStream: InputStream) : InputStream() {
    var bytesRead: Long = 0
        private set

    override fun read(): Int {
        val byte: Int = wrappedInputStream.read()
        if (byte != -1) {
            bytesRead++
        }
        return byte
    }

    override fun read(b: ByteArray): Int {
        val numRead: Int = wrappedInputStream.read(b)
        if (numRead != -1) {
            bytesRead += numRead
        }
        return numRead
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val numRead: Int = wrappedInputStream.read(b, off, len)
        if (numRead != -1) {
            bytesRead += numRead
        }
        return numRead
    }

    override fun close() {
        wrappedInputStream.close()
    }
}
