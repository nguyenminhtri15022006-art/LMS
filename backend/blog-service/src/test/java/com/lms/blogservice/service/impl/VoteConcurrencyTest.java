package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Vote;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.VoteService;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Test cho tính năng bình chọn (Vote).
 *
 * <p>Mục tiêu: Đảm bảo khi có nhiều luồng cùng gửi yêu cầu vote đồng thời vào cùng một bài viết,
 * hệ thống không sinh trùng lặp bản ghi vote và không làm sai lệch tổng điểm số.
 *
 * <p>Lưu ý kỹ thuật quan trọng:
 * Kết quả kiểm thử trên H2 in-memory chứng minh logic kiểm soát nghiệp vụ, xử lý ngoại lệ
 * và cập nhật ở tầng ứng dụng/schema; không thay thế việc xác minh hành vi transaction,
 * isolation level và row locking trên MySQL/InnoDB trong môi trường Docker Compose.
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteConcurrencyTest {

    @Autowired
    private VoteService voteService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private MinioClient minioClient;

    private Long postId;
    private final String authorId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        voteRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

        Post post = Post.builder()
                .title("Bài viết kiểm thử concurrency")
                .slug("bai-viet-kiem-thu-concurrency-" + UUID.randomUUID().toString().substring(0, 8))
                .content("Nội dung bài viết")
                .authorId(authorId)
                .status(PostStatus.PUBLISHED)
                .viewCount(0L)
                .tags(new ArrayList<>())
                .build();

        Post savedPost = postRepository.save(post);
        postId = savedPost.getId();
    }

    @AfterEach
    void tearDown() {
        voteRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("17. concurrentVoteRequestsDoNotDuplicateVoteOrCorruptScore: Nhiều luồng đồng thời vote không sinh trùng lặp vote và bảo toàn tính toàn vẹn điểm số")
    void concurrentVoteRequestsDoNotDuplicateVoteOrCorruptScore() throws InterruptedException {
        int voterCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(voterCount);
        CountDownLatch readyLatch = new CountDownLatch(voterCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(voterCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 10 người dùng khác nhau cùng vote UPVOTE đồng thời cho bài viết
        for (int i = 0; i < voterCount; i++) {
            final String voterId = "concurrent-voter-" + i;
            executorService.submit(() -> {
                GatewayAuthentication auth = new GatewayAuthentication(
                        voterId,
                        voterId + "@example.com",
                        "ROLE_STUDENT",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                readyLatch.countDown();
                try {
                    startLatch.await();
                    VoteRequest request = VoteRequest.builder().voteType(VoteType.UPVOTE).build();
                    voteService.votePost(postId, request);
                    successCount.incrementAndGet();
                } catch (BlogException | DataIntegrityViolationException e) {
                    failedCount.incrementAndGet();
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }

        // Chờ tất cả luồng sẵn sàng
        boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(allReady).isTrue();

        // Kích hoạt đồng thời
        startLatch.countDown();

        // Chờ hoàn tất
        boolean allFinished = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(allFinished).isTrue();
        executorService.shutdown();

        // Kiểm tra tất cả 10 vote của 10 user khác nhau đều thành công
        assertThat(successCount.get()).isEqualTo(voterCount);

        // Kiểm tra database chứa đúng 10 bản ghi vote
        long upvoteCount = voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.UPVOTE);
        assertThat(upvoteCount).isEqualTo(voterCount);

        List<Vote> votes = voteRepository.findAll();
        assertThat(votes).hasSize(voterCount);

        // Giờ kiểm thử tiếp: cùng 1 user bấm vote nhiều lần liên tiếp (double click cạnh tranh)
        int doubleClickThreads = 5;
        String singleUserId = "single-voter-double-click";
        ExecutorService singleUserExecutor = Executors.newFixedThreadPool(doubleClickThreads);
        CountDownLatch singleReadyLatch = new CountDownLatch(doubleClickThreads);
        CountDownLatch singleStartLatch = new CountDownLatch(1);
        CountDownLatch singleFinishLatch = new CountDownLatch(doubleClickThreads);

        for (int i = 0; i < doubleClickThreads; i++) {
            singleUserExecutor.submit(() -> {
                GatewayAuthentication auth = new GatewayAuthentication(
                        singleUserId,
                        singleUserId + "@example.com",
                        "ROLE_STUDENT",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                singleReadyLatch.countDown();
                try {
                    singleStartLatch.await();
                    VoteRequest request = VoteRequest.builder().voteType(VoteType.UPVOTE).build();
                    voteService.votePost(postId, request);
                } catch (Exception ignored) {
                } finally {
                    SecurityContextHolder.clearContext();
                    singleFinishLatch.countDown();
                }
            });
        }

        singleReadyLatch.await(5, TimeUnit.SECONDS);
        singleStartLatch.countDown();
        singleFinishLatch.await(10, TimeUnit.SECONDS);
        singleUserExecutor.shutdown();

        // Ràng buộc Unique uq_votes_user_target đảm bảo cho user này chỉ có tối đa 1 bản ghi vote
        long userVotes = voteRepository.findAll().stream()
                .filter(v -> singleUserId.equals(v.getUserId()) && postId.equals(v.getTargetId()))
                .count();
        assertThat(userVotes).isLessThanOrEqualTo(1L);
    }
}
