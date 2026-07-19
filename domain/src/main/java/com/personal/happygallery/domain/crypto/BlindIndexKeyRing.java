package com.personal.happygallery.domain.crypto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 현재 키와 이전 키로 블라인드 인덱스를 계산하는 키링.
 *
 * <p>일반 저장은 현재 키만 사용하고, 회전 전환기의 조회만 이전 키 후보까지 확인한다.
 */
public final class BlindIndexKeyRing {

    private final String activeKeyId;
    private final Map<String, BlindIndexer> indexers;

    public BlindIndexKeyRing(String activeKeyId, Map<String, byte[]> keys) {
        if (activeKeyId == null || activeKeyId.isBlank()) {
            throw new IllegalArgumentException("활성 HMAC 키 ID는 필수입니다.");
        }
        LinkedHashMap<String, BlindIndexer> configured = new LinkedHashMap<>();
        keys.forEach((keyId, key) -> configured.put(keyId, new BlindIndexer(key)));
        if (!configured.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("활성 HMAC 키를 키링에서 찾을 수 없습니다: " + activeKeyId);
        }
        this.activeKeyId = activeKeyId;
        this.indexers = Collections.unmodifiableMap(configured);
    }

    public String index(String plaintext) {
        return indexers.get(activeKeyId).index(plaintext);
    }

    public List<String> indexCandidates(String plaintext) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(index(plaintext));
        indexers.forEach((keyId, indexer) -> {
            if (!keyId.equals(activeKeyId)) {
                candidates.add(indexer.index(plaintext));
            }
        });
        return List.copyOf(candidates);
    }

    public String indexWith(String keyId, String plaintext) {
        BlindIndexer indexer = indexers.get(keyId);
        if (indexer == null) {
            throw new IllegalArgumentException("HMAC 키를 찾을 수 없습니다: " + keyId);
        }
        return indexer.index(plaintext);
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public List<String> keyIds() {
        return List.copyOf(indexers.keySet());
    }
}
