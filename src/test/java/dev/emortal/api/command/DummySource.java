package dev.emortal.api.command;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

interface DummySource {
    static Default create(String name) {
        return new Default(name);
    }

    String name();

    void sendMessage(String message);

    final class Default implements DummySource {

        private final String name;

        private final CountDownLatch latch = new CountDownLatch(1);
        private final Queue<String> messages = new ArrayDeque<>();

        private Default(String name) {
            this.name = name;
        }

        public void await() {
            try {
                this.latch.await();
            } catch (InterruptedException ignored) {
                // don't care if it's interrupted
            }
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public void sendMessage(String message) {
            this.messages.offer(message);
            this.latch.countDown();
        }

        public String nextMessage() {
            return this.messages.poll();
        }
    }
}
