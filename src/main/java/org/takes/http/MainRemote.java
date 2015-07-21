/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;

/**
 * Front remote control.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.23
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.DoNotUseThreads")
@EqualsAndHashCode(of = "app")
public final class MainRemote {

    /**
     * Application with {@code main()} method.
     */
    private final transient Class<?> app;

    /**
     * Ctor.
     * @param type Class with main method
     */
    public MainRemote(final Class<?> type) {
        this.app = type;
    }

    /**
     * Execute this script against a running front.
     * @param script Script to run
     * @throws Exception If fails
     */
    public void exec(final MainRemote.Script script) throws Exception {
        final File file = File.createTempFile("takes-", ".txt");
        file.delete();
        final Method method = this.app.getDeclaredMethod(
            "main", String[].class
        );
        final String[] args = new String[1];
        args[0] = String.format("--port=%s", file.getAbsoluteFile());
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        method.invoke(null, (Object) args);
                    } catch (final InvocationTargetException ex) {
                        throw new IllegalStateException(ex);
                    } catch (final IllegalAccessException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        );
        thread.start();
        while (!file.exists()) {
            TimeUnit.MILLISECONDS.sleep(1L);
        }
        final int port;
        final InputStream input = new FileInputStream(file);
        try {
            // @checkstyle MagicNumber (1 line)
            final byte[] buf = new byte[10];
            input.read(buf);
            port = Integer.parseInt(new String(buf).trim());
        } finally {
            input.close();
        }
        try {
            script.exec(
                URI.create(
                    String.format(
                        "http://localhost:%d",
                        port
                    )
                )
            );
        } finally {
            file.delete();
            thread.interrupt();
        }
    }

    /**
     * Script to execute.
     */
    public interface Script {
        /**
         * Execute it against this URI.
         * @param home URI of the running front
         * @throws IOException If fails
         */
        void exec(URI home) throws IOException;
    }

}