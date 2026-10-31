package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Q&A·문의·후기 작성자의 표시 이름을 조회한다.
 *
 * <p>{@link UserReaderPort}의 {@link User} 조회는 활성 회원만 돌려주므로 빠진 작성자는 탈퇴 회원이며
 * {@link #WITHDRAWN}으로 표시한다. 서비스 생성자에 의존성을 더하지 않도록 호출자의 포트를 인자로 받는다.
 */
public final class MemberNames {

    /** 탈퇴 시 익명화하는 회원 이름이며, 조회되지 않는 작성자의 표시 이름으로도 쓴다. */
    public static final String WITHDRAWN = "탈퇴회원";

    private MemberNames() {}

    /** 활성 회원의 이름을 돌려주고, 탈퇴 회원은 {@link #WITHDRAWN}을 돌려준다. */
    public static String of(UserReaderPort userReader, Long userId) {
        return userReader.findById(userId).map(User::getName).orElse(WITHDRAWN);
    }

    /** 활성 회원의 이름을 id별로 모은다. 탈퇴 회원은 빠지므로 호출자가 {@link #WITHDRAWN}으로 대체한다. */
    public static Map<Long, String> of(UserReaderPort userReader, Collection<Long> userIds) {
        return byId(userReader.findAllById(userIds));
    }

    /** 관리자 과거 이력 표시용으로 익명화된 탈퇴 회원의 이름도 포함한다. */
    public static Map<Long, String> ofForAdminHistory(UserReaderPort userReader, Collection<Long> userIds) {
        return byId(userReader.findAllByIdForAdminHistory(userIds));
    }

    private static Map<Long, String> byId(List<User> users) {
        return users.stream().collect(toMap(User::getId, User::getName));
    }
}
