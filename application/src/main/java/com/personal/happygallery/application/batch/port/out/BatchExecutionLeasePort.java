package com.personal.happygallery.application.batch.port.out;

import java.util.Optional;

public interface BatchExecutionLeasePort {
    Optional<Lease> tryAcquire(String job);

    interface Lease extends AutoCloseable {
        @Override
        void close();
    }
}
