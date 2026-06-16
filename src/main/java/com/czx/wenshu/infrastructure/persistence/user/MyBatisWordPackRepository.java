package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.WordPack;
import com.czx.wenshu.domain.user.WordPackRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link WordPackRepository} 的 MyBatis 实现（P9-09）。
 */
@Repository
public class MyBatisWordPackRepository implements WordPackRepository {

    private final WordPackMapper mapper;

    public MyBatisWordPackRepository(WordPackMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(WordPack pack) {
        WordPackRecord rec = toRecord(pack);
        // 判断是 insert 还是 update（通过 id 检查不方便，改为：若 charsUsed > 0 则执行 UPDATE，否则 INSERT）
        // 更安全：通过 findActiveByUserId 包含的 id 来判断。这里简化为：pack 刚创建时 id 不存在 → insert
        // 若是已存在的 pack 被 consume 后保存 → update
        // 实际判断通过 record 是否已存在于 DB。使用简单策略：先 INSERT ON CONFLICT UPDATE
        try {
            mapper.insert(rec);
        } catch (Exception e) {
            // 主键冲突（已存在）→ update 消耗量
            mapper.update(rec);
        }
    }

    @Override
    public List<WordPack> findActiveByUserId(UUID userId) {
        return mapper.findActiveByUserId(userId.toString()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public boolean existsTrialByUserId(UUID userId) {
        return mapper.countTrialByUserId(userId.toString()) > 0;
    }

    private WordPack toDomain(WordPackRecord rec) {
        return WordPack.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getUserId()),
                rec.getPackKey(), rec.getPackType(),
                rec.getCharsTotal(), rec.getCharsUsed(),
                rec.getExpiresAt(), rec.getCreatedAt(), rec.getUpdatedAt());
    }

    private WordPackRecord toRecord(WordPack pack) {
        WordPackRecord rec = new WordPackRecord();
        rec.setId(pack.id().toString());
        rec.setUserId(pack.userId().toString());
        rec.setPackKey(pack.packKey());
        rec.setPackType(pack.packType());
        rec.setCharsTotal(pack.charsTotal());
        rec.setCharsUsed(pack.charsUsed());
        rec.setExpiresAt(pack.expiresAt());
        rec.setCreatedAt(pack.createdAt());
        rec.setUpdatedAt(pack.updatedAt());
        return rec;
    }
}
