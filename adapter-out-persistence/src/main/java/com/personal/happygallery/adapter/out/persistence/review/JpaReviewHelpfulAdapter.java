package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort;
import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewHelpfulAdapter implements ReviewHelpfulPort {

    private final ReviewHelpfulVoteRepository repository;

    JpaReviewHelpfulAdapter(ReviewHelpfulVoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveIfAbsent(ReviewHelpfulVote vote) {
        repository.insertIfAbsent(vote.getReviewId(), vote.getUserId(), vote.getCreatedAt());
    }

    @Override
    public void delete(Long reviewId, Long userId) {
        repository.deleteByReviewIdAndUserId(reviewId, userId);
    }

    @Override
    public long countByReviewId(Long reviewId) {
        return repository.countByReviewId(reviewId);
    }

    @Override
    public List<ReviewHelpfulCountView> countByReviewIds(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return List.of();
        }
        return repository.countRows(reviewIds);
    }

    @Override
    public List<Long> findHelpfulReviewIds(Long userId, List<Long> reviewIds) {
        return reviewIds.isEmpty() ? List.of() : repository.findHelpfulReviewIds(userId, reviewIds);
    }
}
