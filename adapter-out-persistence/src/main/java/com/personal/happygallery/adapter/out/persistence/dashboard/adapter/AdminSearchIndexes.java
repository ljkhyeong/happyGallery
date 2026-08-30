package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.util.regex.Pattern;

record AdminSearchIndexes(String nameHmac, String phoneHmac) {

    private static final Pattern PHONE_CANDIDATE =
            Pattern.compile("^01[0-9](?:[\\s-]*[0-9]){7,8}$");
    private static final AdminSearchIndexes EMPTY = new AdminSearchIndexes(null, null);

    static AdminSearchIndexes from(String keyword, BlindIndexer blindIndexer) {
        if (keyword == null) {
            return EMPTY;
        }
        if (PHONE_CANDIDATE.matcher(keyword).matches()) {
            return new AdminSearchIndexes(
                    null,
                    blindIndexer.index(KoreanPhoneNumber.required(keyword)));
        }
        return new AdminSearchIndexes(
                blindIndexer.index(PersonalName.required(keyword)),
                null);
    }
}
