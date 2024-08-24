/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.utils

import java.io.InputStream

/**
 * A wrapper class for an InputStream that counts the number of bytes read from it.
 */
public class CountingInputStream(private val wrappedInputStream: InputStream) : InputStream() {
    /**
     * The total number of bytes read from the InputStream.
     */
    public var totalBytesRead: Long = 0
        private set

    override fun read(): Int {
        val byte: Int = wrappedInputStream.read()
        if (byte != -1) {
            totalBytesRead++
        }
        return byte
    }

    override fun read(b: ByteArray): Int {
        val bytesRead: Int = wrappedInputStream.read(b)
        if (bytesRead != -1) {
            totalBytesRead += bytesRead
        }
        return bytesRead
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead: Int = wrappedInputStream.read(b, off, len)
        if (bytesRead != -1) {
            totalBytesRead += bytesRead
        }
        return bytesRead
    }

    override fun close() {
        wrappedInputStream.close()
    }
}
