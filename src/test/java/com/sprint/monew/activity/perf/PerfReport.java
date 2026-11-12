package com.sprint.monew.activity.perf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 측정 결과를 콘솔에 출력하고 build/reports/performance/{name}.md 에도 이어 쓴다. */
final class PerfReport {

    private static final Path DIRECTORY = Path.of("build", "reports", "performance");

    private PerfReport() {}

    static void write(String name, String markdown) {
        System.out.println(markdown);
        try {
            Files.createDirectories(DIRECTORY);
            Files.writeString(
                    DIRECTORY.resolve(name + ".md"),
                    markdown + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
