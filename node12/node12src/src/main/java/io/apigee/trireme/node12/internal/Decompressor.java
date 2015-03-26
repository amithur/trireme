/**
 * Copyright 2015 Apigee Corporation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.apigee.trireme.node12.internal;

import io.apigee.trireme.core.NodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Decompressor
    extends ZlibWriter
{
    private static final Logger log = LoggerFactory.getLogger(Compressor.class);

    private final Inflater inflater;

    public Decompressor(int mode, ByteBuffer dictionary)
        throws NodeException
    {
        switch (mode) {
        case INFLATE:
            inflater = new Inflater();
            break;
        case DEFLATERAW:
            inflater = new Inflater(true);
            break;
        default:
            throw new NodeException("Invalid mode " + mode);
        }

        if (dictionary != null) {
            if (dictionary.hasArray()) {
                inflater.setDictionary(dictionary.array(),
                                       dictionary.arrayOffset() + dictionary.position(),
                                       dictionary.remaining());
            } else {
                byte[] dict = new byte[dictionary.remaining()];
                dictionary.get(dict);
                inflater.setDictionary(dict);
            }
        }
    }


    @Override
    public void setParams(int level, int strategy)
    {
        // Nothing to do
    }

    @Override
    public void reset()
    {
        inflater.reset();
    }

    @Override
    public void write(int flush, ByteBuffer in, ByteBuffer out)
        throws DataFormatException
    {
        if (log.isDebugEnabled()) {
            log.debug("Deflating {} into {} flush = {}", in, out, flush);
        }

        addInput(in);

        byte[] buf;
        int off;
        int len;

        if (out.hasArray()) {
            buf = out.array();
            off = out.arrayOffset() + out.position();
            len = out.remaining();
        } else {
            buf = new byte[out.remaining()];
            out.duplicate().get(buf);
            off = 0;
            len = out.remaining();
        }

        // TODO for Java 7, pass "flush" flag!
        long oldPos = inflater.getBytesRead();
        int numWritten  = inflater.inflate(buf, off, len);
        long numRead = inflater.getBytesRead() - oldPos;

        in.position(in.position() + (int)numRead);
        out.position(out.position() + numWritten);
    }

    private void addInput(ByteBuffer in)
    {
        byte[] buf;
        int off;
        int len;

        if (in == null) {
            buf = null;
            off = len = 0;
        } else if (in.hasArray()) {
            buf = in.array();
            off = in.arrayOffset() + in.position();
            len = in.remaining();
        } else {
            buf = new byte[in.remaining()];
            in.duplicate().get(buf);
            off = 0;
            len = in.remaining();
        }

        inflater.setInput(buf, off, len);
    }

    @Override
    public void close()
    {
        inflater.end();
    }
}