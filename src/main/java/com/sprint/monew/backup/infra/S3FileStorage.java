package com.sprint.monew.backup.infra;

import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.backup.domain.exception.BackupStorageFailedException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public void put(String key, byte[] content) {
        try {
            s3Client.putObject(
                    request -> request.bucket(properties.bucket()).key(key).contentType("application/json"),
                    RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            throw new BackupStorageFailedException(key);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        try {
            return Optional.of(s3Client.getObjectAsBytes(
                            request -> request.bucket(properties.bucket()).key(key))
                    .asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new BackupStorageFailedException(key);
        }
    }

    @Override
    public List<String> list(String prefix) {
        return s3Client
                .listObjectsV2Paginator(
                        request -> request.bucket(properties.bucket()).prefix(prefix))
                .contents()
                .stream()
                .map(S3Object::key)
                .toList();
    }
}
