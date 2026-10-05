package dev.monkeypatch.rctiming.domain.auth;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.RefreshTokensRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static dev.monkeypatch.rctiming.jooq.generated.tables.RefreshTokens.REFRESH_TOKENS;

@Repository
public class RefreshTokenRepository extends JooqRepository<RefreshToken, RefreshTokensRecord> {

    public RefreshTokenRepository(DSLContext dsl) {
        super(dsl, REFRESH_TOKENS, REFRESH_TOKENS.ID);
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return findOne(REFRESH_TOKENS.TOKEN_HASH.eq(tokenHash));
    }

    public List<RefreshToken> findByUserIdAndRevokedFalse(Long userId) {
        return findWhere(REFRESH_TOKENS.USER_ID.eq(userId).and(REFRESH_TOKENS.REVOKED.isFalse()));
    }

    /** Revokes every refresh token the official still holds, signing them out of every browser. */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return dsl.update(REFRESH_TOKENS)
                .set(REFRESH_TOKENS.REVOKED, true)
                .where(REFRESH_TOKENS.USER_ID.eq(userId).and(REFRESH_TOKENS.REVOKED.isFalse()))
                .execute();
    }

    @Override
    protected List<Field<?>> insertOnly() {
        return List.of(REFRESH_TOKENS.CREATED_AT);
    }

    @Override
    protected RefreshToken toEntity(RefreshTokensRecord r) {
        RefreshToken t = new RefreshToken();
        t.setId(r.getId());
        t.setUserId(r.getUserId());
        t.setTokenHash(r.getTokenHash());
        t.setExpiresAt(r.getExpiresAt());
        t.setCreatedAt(r.getCreatedAt());
        t.setRevoked(r.getRevoked());
        return t;
    }

    @Override
    protected void toRecord(RefreshToken t, RefreshTokensRecord r) {
        r.setUserId(t.getUserId());
        r.setTokenHash(t.getTokenHash());
        r.setExpiresAt(t.getExpiresAt());
        r.setCreatedAt(t.getCreatedAt());
        r.setRevoked(t.isRevoked());
    }

    @Override
    protected Long idOf(RefreshToken t) {
        return t.getId();
    }

    @Override
    protected void setId(RefreshToken t, Long id) {
        t.setId(id);
    }
}
