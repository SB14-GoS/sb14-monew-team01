package com.sprint.monew.backup.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.monew.backup.domain.exception.BackupStorageFailedException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

class S3FileStorageTest {

    private static final String BUCKET = "test-backup-bucket";
    private static final String KEY = "articles/2026-10-28.json";

    private S3Client s3Client;
    private S3FileStorage storage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        storage = new S3FileStorage(s3Client, new S3Properties("ap-northeast-2", BUCKET, "test-log-bucket"));
    }

    @Test
    @DisplayName("저장하면 백업 버킷에 키와 JSON 타입으로 올린다")
    @SuppressWarnings("unchecked")
    void putUploadsToBackupBucket() {
        storage.put(KEY, "[]".getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        request.getValue().accept(builder);
        PutObjectRequest built = builder.build();
        assertThat(built.bucket()).isEqualTo(BUCKET);
        assertThat(built.key()).isEqualTo(KEY);
        assertThat(built.contentType()).isEqualTo("application/json");
    }

    @Test
    @DisplayName("저장 중 S3 오류는 BackupStorageFailedException으로 바꾼다")
    @SuppressWarnings("unchecked")
    void putWrapsSdkException() {
        given(s3Client.putObject(any(Consumer.class), any(RequestBody.class)))
                .willThrow(SdkClientException.create("down"));

        assertThatThrownBy(() -> storage.put(KEY, new byte[0])).isInstanceOf(BackupStorageFailedException.class);
    }

    @Test
    @DisplayName("파일이 있으면 내용을 그대로 돌려준다")
    @SuppressWarnings("unchecked")
    void getReturnsContent() {
        byte[] content = "[{\"title\":\"기사\"}]".getBytes(StandardCharsets.UTF_8);
        given(s3Client.getObjectAsBytes(any(Consumer.class)))
                .willReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));

        assertThat(storage.get(KEY)).hasValue(content);

        ArgumentCaptor<Consumer<GetObjectRequest.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        verify(s3Client).getObjectAsBytes(request.capture());
        GetObjectRequest.Builder builder = GetObjectRequest.builder();
        request.getValue().accept(builder);
        assertThat(builder.build().bucket()).isEqualTo(BUCKET);
        assertThat(builder.build().key()).isEqualTo(KEY);
    }

    @Test
    @DisplayName("파일이 없으면 빈 값을 돌려준다")
    @SuppressWarnings("unchecked")
    void getReturnsEmptyWhenMissing() {
        given(s3Client.getObjectAsBytes(any(Consumer.class)))
                .willThrow(NoSuchKeyException.builder().message("missing").build());

        assertThat(storage.get(KEY)).isEmpty();
    }

    @Test
    @DisplayName("조회 중 S3 오류는 BackupStorageFailedException으로 바꾼다")
    @SuppressWarnings("unchecked")
    void getWrapsSdkException() {
        given(s3Client.getObjectAsBytes(any(Consumer.class))).willThrow(SdkClientException.create("down"));

        assertThatThrownBy(() -> storage.get(KEY)).isInstanceOf(BackupStorageFailedException.class);
    }

    @Test
    @DisplayName("prefix 아래 파일 키 목록을 돌려준다")
    @SuppressWarnings("unchecked")
    void listReturnsKeys() {
        ListObjectsV2Iterable pages = mock(ListObjectsV2Iterable.class);
        List<S3Object> objects = List.of(
                S3Object.builder().key("articles/2026-10-27.json").build(),
                S3Object.builder().key("articles/2026-10-28.json").build());
        given(pages.contents()).willReturn(objects::iterator);
        given(s3Client.listObjectsV2Paginator(any(Consumer.class))).willReturn(pages);

        assertThat(storage.list("articles/")).containsExactly("articles/2026-10-27.json", "articles/2026-10-28.json");

        ArgumentCaptor<Consumer<ListObjectsV2Request.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        verify(s3Client).listObjectsV2Paginator(request.capture());
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder();
        request.getValue().accept(builder);
        assertThat(builder.build().prefix()).isEqualTo("articles/");
    }

    @Test
    @DisplayName("목록 조회 중 S3 오류는 BackupStorageFailedException으로 바꾼다")
    @SuppressWarnings("unchecked")
    void listWrapsSdkException() {
        given(s3Client.listObjectsV2Paginator(any(Consumer.class))).willThrow(SdkClientException.create("down"));

        assertThatThrownBy(() -> storage.list("articles/")).isInstanceOf(BackupStorageFailedException.class);
    }
}
