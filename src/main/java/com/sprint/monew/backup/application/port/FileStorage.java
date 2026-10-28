package com.sprint.monew.backup.application.port;

import java.util.List;
import java.util.Optional;

public interface FileStorage {

    void put(String key, byte[] content);

    Optional<byte[]> get(String key);

    List<String> list(String prefix);
}
