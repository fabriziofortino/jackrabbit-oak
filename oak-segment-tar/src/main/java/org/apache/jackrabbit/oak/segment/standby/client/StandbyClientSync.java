/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.segment.standby.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.jackrabbit.core.data.util.NamedThreadFactory;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.tar.GCGeneration;
import org.apache.jackrabbit.oak.segment.standby.jmx.ClientStandbyStatusMBean;
import org.apache.jackrabbit.oak.segment.standby.jmx.StandbyStatusMBean;
import org.apache.jackrabbit.oak.segment.standby.store.CommunicationObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StandbyClientSync implements ClientStandbyStatusMBean, Runnable, Closeable {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String host;
        private int port;
        private FileStore fileStore;
        private boolean secure;
        private int readTimeoutMs;
        private boolean autoClean;
        private File spoolFolder;
        private String sslKeyFile;
        private String sslKeyPassword;
        private String sslChainFile;
        private String sslSubjectPattern;

        private Builder() {}

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withFileStore(FileStore fileStore) {
            this.fileStore = fileStore;
            return this;
        }

        public Builder withSecureConnection(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder withReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder withAutoClean(boolean autoClean) {
            this.autoClean = autoClean;
            return this;
        }

        public Builder withSpoolFolder(File spoolFolder) {
            this.spoolFolder = spoolFolder;
            return this;
        }

        public Builder withSSLKeyFile(String sslKeyFile) {
            this.sslKeyFile = sslKeyFile;
            return this;
        }

        public Builder withSSLKeyPassword(String sslKeyPassword) {
            this.sslKeyPassword = sslKeyPassword;
            return this;
        }

        public Builder withSSLChainFile(String sslChainFile) {
            this.sslChainFile = sslChainFile;
            return this;
        }

        public Builder withSSLSubjectPattern(String sslSubjectPattern) {
            this.sslSubjectPattern = sslSubjectPattern;
            return this;
        }

        public StandbyClientSync build() {
            return new StandbyClientSync(this);
        }
    }

    public static final String CLIENT_ID_PROPERTY_NAME = "standbyID";

    private static final Logger log = LoggerFactory.getLogger(StandbyClientSync.class);

    private static final AtomicInteger standbyRunCounter = new AtomicInteger();

    private final String host;

    private final int port;

    private final int readTimeoutMs;

    private final boolean autoClean;

    private final CommunicationObserver observer;

    private final boolean secure;

    private final FileStore fileStore;

    private final NioEventLoopGroup group;

    private final File spoolFolder;

    private final StandbyClientSyncExecution execution;

    private final AtomicBoolean active = new AtomicBoolean(false);

    private final String sslKeyFile;

    private final String sslKeyPassword;

    private final String sslChainFile;

    private final String sslSubjectPattern;

    private int failedRequests;

    private long lastSuccessfulRequest;

    private volatile String state;

    private volatile boolean running = true;

    private long syncStartTimestamp;

    private long syncEndTimestamp;

    private static String clientId() {
        String s = System.getProperty(CLIENT_ID_PROPERTY_NAME);

        if (s == null || s.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        return s;
    }

    private StandbyClientSync(Builder builder) {
        this.state = STATUS_INITIALIZING;
        this.lastSuccessfulRequest = -1;
        this.syncStartTimestamp = -1;
        this.syncEndTimestamp = -1;
        this.failedRequests = 0;
        this.host = builder.host;
        this.port = builder.port;
        this.secure = builder.secure;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.autoClean = builder.autoClean;
        this.fileStore = builder.fileStore;
        this.observer = new CommunicationObserver(clientId());
        this.group = new NioEventLoopGroup(0, new NamedThreadFactory("standby"));
        this.execution = new StandbyClientSyncExecution(fileStore, () -> running);
        this.spoolFolder = builder.spoolFolder;
        this.sslKeyFile = builder.sslKeyFile;
        this.sslKeyPassword = builder.sslKeyPassword;
        this.sslChainFile = builder.sslChainFile;
        this.sslSubjectPattern = builder.sslSubjectPattern;
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new StandardMBean(this, ClientStandbyStatusMBean.class), new ObjectName(this.getMBeanName()));
        } catch (Exception e) {
            log.error("cannot register standby status mbean", e);
        }
    }

    public String getMBeanName() {
        return StandbyStatusMBean.JMX_NAME + ",id=\"" + this.observer.getID() + "\"";
    }

    @Override
    public void close() {
        stop();
        state = STATUS_CLOSING;
        final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        try {
            jmxServer.unregisterMBean(new ObjectName(this.getMBeanName()));
        } catch (Exception e) {
            log.error("cannot unregister standby status mbean", e);
        }
        closeGroup();
        observer.unregister();
        state = STATUS_CLOSED;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();

        try {
            Thread.currentThread().setName("standby-run-" + standbyRunCounter.incrementAndGet());

            if (!running) {
                return;
            }

            state = STATUS_STARTING;

            if (!active.compareAndSet(false, true)) {
                return;
            }

            state = STATUS_RUNNING;

            try {
                long startTimestamp = System.currentTimeMillis();

                GCGeneration genBefore = headGeneration(fileStore);

                try (StandbyClient client = StandbyClient.builder()
                     .withHost(host)
                     .withPort(port)
                     .withGroup(group)
                     .withClientId(observer.getID())
                     .withSecure(secure)
                     .withReadTimeoutMs(readTimeoutMs)
                     .withSpoolFolder(spoolFolder)
                     .withSSLKeyFile(sslKeyFile)
                     .withSSLKeyPassword(sslKeyPassword)
                     .withSSLChainFile(sslChainFile)
                     .withSSLSubjectPattern(sslSubjectPattern).build()) {
                    execution.execute(client);
                }

                fileStore.flush();

                GCGeneration genAfter = headGeneration(fileStore);

                if (autoClean && genAfter.compareWith(genBefore) > 0) {
                    log.info("New head generation detected (prevHeadGen: {} newHeadGen: {}), running cleanup.", genBefore, genAfter);
                    cleanupAndRemove();
                }

                this.failedRequests = 0;
                this.syncStartTimestamp = startTimestamp;
                this.syncEndTimestamp = System.currentTimeMillis();
                this.lastSuccessfulRequest = syncEndTimestamp / 1000;
            } catch (Exception e) {
                this.failedRequests++;
                log.error("Failed synchronizing state.", e);
            } finally {
                active.set(false);
            }
        } finally {
            Thread.currentThread().setName(name);
        }
    }

    @NotNull
    private static GCGeneration headGeneration(FileStore fileStore) {
        return fileStore.getHead().getRecordId().getSegment().getGcGeneration();
    }

    private void cleanupAndRemove() throws IOException {
        fileStore.cleanup();
    }

    @NotNull
    @Override
    public String getMode() {
        return "client: " + this.observer.getID();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        running = true;
        state = STATUS_RUNNING;
    }

    @Override
    public void stop() {
        running = false;
        state = STATUS_STOPPED;
    }

    @Override
    public String getStatus() {
        return this.state;
    }

    @Override
    public int getFailedRequests() {
        return this.failedRequests;
    }

    @Override
    public int getSecondsSinceLastSuccess() {
        if (this.lastSuccessfulRequest < 0) {
            return -1;
        }
        return (int) (System.currentTimeMillis() / 1000 - this.lastSuccessfulRequest);
    }

    @Override
    public int calcFailedRequests() {
        return this.getFailedRequests();
    }

    @Override
    public int calcSecondsSinceLastSuccess() {
        return this.getSecondsSinceLastSuccess();
    }

    @Override
    public void cleanup() {
        try {
            cleanupAndRemove();
        } catch (IOException e) {
            log.error("Error while cleaning up", e);
        }
    }

    @Override
    public long getSyncStartTimestamp() {
        return syncStartTimestamp;
    }

    @Override
    public long getSyncEndTimestamp() {
        return syncEndTimestamp;
    }

    private void closeGroup() {
        if (group == null) {
            return;
        }
        if (group.shutdownGracefully(2, 15, TimeUnit.SECONDS).awaitUninterruptibly(20, TimeUnit.SECONDS)) {
            log.debug("Group shut down");
        } else {
            log.debug("Group shutdown timed out");
        }
    }

}
