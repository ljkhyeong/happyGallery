package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.AdminPassQueryMapper;
import com.personal.happygallery.application.search.port.out.AdminPassQueryPort;
import com.personal.happygallery.application.search.port.out.AdminPassQueryResult;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class MyBatisAdminPassQueryAdapter implements AdminPassQueryPort {

    private static final Pattern FORMATTED_PASS_ID = Pattern.compile(
            "^PASS-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final AdminPassQueryMapper mapper;
    private final BlindIndexer blindIndexer;

    MyBatisAdminPassQueryAdapter(AdminPassQueryMapper mapper, BlindIndexer blindIndexer) {
        this.mapper = mapper;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public List<AdminPassQueryResult> search(String keyword, LocalDateTime now, int offset, int size) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_PASS_ID);
        AdminSearchIndexes indexes =
                AdminSearchIndexes.from(searchKeyword.keyword(), blindIndexer);
        return mapper.search(
                searchKeyword.keyword(),
                indexes.nameHmac(),
                indexes.phoneHmac(),
                searchKeyword.exactId(),
                now,
                offset,
                size);
    }

    @Override
    public long count(String keyword) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_PASS_ID);
        AdminSearchIndexes indexes =
                AdminSearchIndexes.from(searchKeyword.keyword(), blindIndexer);
        return mapper.count(
                searchKeyword.keyword(),
                indexes.nameHmac(),
                indexes.phoneHmac(),
                searchKeyword.exactId());
    }

    @Override
    public Optional<AdminPassQueryResult> findById(Long passId, LocalDateTime now) {
        return mapper.findById(passId, now);
    }
}
