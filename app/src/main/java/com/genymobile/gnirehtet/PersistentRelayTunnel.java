/*
 * Copyright (C) 2017 Genymobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.genymobile.gnirehtet;

import android.net.VpnService;
import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Expose a {@link Tunnel} that automatically handles {@link RelayTunnel} reconnections.
 */
public class PersistentRelayTunnel implements Tunnel {

    private static final String TAG = PersistentRelayTunnel.class.getSimpleName();
    private static final long BASE_RETRY_DELAY_MS = 100;
    private static final long MAX_RETRY_DELAY_MS = 1600;

    private final RelayTunnelProvider provider;
    private final AtomicBoolean stopped = new AtomicBoolean();

    public PersistentRelayTunnel(VpnService vpnService, RelayTunnelListener listener) {
        provider = new RelayTunnelProvider(vpnService, listener);
    }

    @Override
    public void send(byte[] packet, int len) throws IOException {
        int failures = 0;
        while (!stopped.get()) {
            Tunnel tunnel = null;
            try {
                tunnel = provider.getCurrentTunnel();
                tunnel.send(packet, len);
                return;
            } catch (InterruptedException e) {
                throw interruptedIOException("Send interrupted", e);
            } catch (IOException e) {
                failures++;
                logFailure("send", failures, e);
                if (tunnel != null) {
                    provider.invalidateTunnel(tunnel);
                }
                waitBeforeRetry(failures);
            }
        }
        throw new InterruptedIOException("Persistent tunnel stopped");
    }

    @Override
    public int receive(byte[] packet) throws IOException {
        int failures = 0;
        while (!stopped.get()) {
            Tunnel tunnel = null;
            try {
                tunnel = provider.getCurrentTunnel();
                int r = tunnel.receive(packet);
                if (r == -1) {
                    failures++;
                    Log.d(TAG, "Tunnel read EOF, reconnecting");
                    provider.invalidateTunnel(tunnel);
                    waitBeforeRetry(failures);
                    continue;
                }
                return r;
            } catch (InterruptedException e) {
                throw interruptedIOException("Receive interrupted", e);
            } catch (IOException e) {
                failures++;
                logFailure("receive", failures, e);
                if (tunnel != null) {
                    provider.invalidateTunnel(tunnel);
                }
                waitBeforeRetry(failures);
            }
        }
        throw new InterruptedIOException("Persistent tunnel stopped");
    }

    @Override
    public void close() {
        stopped.set(true);
        provider.invalidateTunnel();
    }

    private static InterruptedIOException interruptedIOException(String message, InterruptedException cause) {
        Thread.currentThread().interrupt();
        InterruptedIOException e = new InterruptedIOException(message);
        e.initCause(cause);
        return e;
    }

    private static long computeRetryDelayMs(int failures) {
        int exponent = Math.min(4, Math.max(0, failures - 1));
        long baseDelay = Math.min(MAX_RETRY_DELAY_MS, BASE_RETRY_DELAY_MS << exponent);
        // add up to +25% jitter to avoid synchronized retry bursts
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1L, baseDelay / 4));
        return Math.min(MAX_RETRY_DELAY_MS, baseDelay + jitter);
    }

    private void waitBeforeRetry(int failures) throws InterruptedIOException {
        if (stopped.get()) {
            return;
        }
        try {
            Thread.sleep(computeRetryDelayMs(failures));
        } catch (InterruptedException e) {
            throw interruptedIOException("Retry interrupted", e);
        }
    }

    private static void logFailure(String operation, int failures, IOException e) {
        if (failures <= 3 || failures % 10 == 0) {
            Log.e(TAG, "Cannot " + operation + " to tunnel (attempt " + failures + ')', e);
        } else {
            Log.w(TAG, "Cannot " + operation + " to tunnel (attempt " + failures + ')');
        }
    }
}
