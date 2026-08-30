package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.Review;

public interface ReviewStorePort {

    Review save(Review review);
}
