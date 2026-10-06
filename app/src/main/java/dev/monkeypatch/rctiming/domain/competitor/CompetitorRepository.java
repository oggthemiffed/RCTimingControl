package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.CompetitorsRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;

@Repository
public class CompetitorRepository extends JooqRepository<Competitor, CompetitorsRecord> {

    public CompetitorRepository(DSLContext dsl) {
        super(dsl, COMPETITORS, COMPETITORS.ID);
    }

    public Optional<Competitor> findByExternalSourceAndExternalId(String externalSource, String externalId) {
        return findOne(COMPETITORS.EXTERNAL_SOURCE.eq(externalSource).and(COMPETITORS.EXTERNAL_ID.eq(externalId)));
    }

    /** Competitors from any source with this BRCA number. */
    public List<Competitor> findByBrcaNumber(String brcaNumber) {
        return findWhere(COMPETITORS.BRCA_NUMBER.eq(brcaNumber));
    }

    /** Competitors from any source with no BRCA number and this display name, ignoring case. */
    public List<Competitor> findWithoutBrcaNumberByName(String displayName) {
        return findWhere(COMPETITORS.BRCA_NUMBER.isNull()
                .and(DSL.lower(DSL.trim(COMPETITORS.DISPLAY_NAME)).eq(displayName.trim().toLowerCase(Locale.ROOT))));
    }

    /**
     * Competitors from any source whose name is the same as this one, ignoring case and spacing, so
     * "alex  rowe" finds "Alex Rowe", and capitals are folded for every letter, accented ones included.
     * Accents themselves are kept. The rule a typed walk-in name is checked against (#123).
     * <p>
     * The names are compared in Java, not SQL: the database's own {@code lower()} only folds ASCII and
     * {@code replace()} only removes a literal space, so SQL would miss accents and tabs, and the rule
     * must not depend on the database vendor. A club has hundreds of competitors, and this runs once
     * per typed walk-in.
     */
    public List<Competitor> findByNormalizedName(String displayName) {
        String target = normalizeName(displayName);
        return findAll().stream()
                .filter(c -> c.getDisplayName() != null && normalizeName(c.getDisplayName()).equals(target))
                .toList();
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    static String normalizeName(String displayName) {
        return WHITESPACE.matcher(displayName).replaceAll("").toLowerCase(Locale.ROOT);
    }

    @Override
    protected Competitor toEntity(CompetitorsRecord r) {
        Competitor c = new Competitor();
        c.setId(r.getId());
        c.setDisplayName(r.getDisplayName());
        c.setExternalSource(r.getExternalSource());
        c.setExternalId(r.getExternalId());
        c.setBrcaNumber(r.getBrcaNumber());
        c.setHomeClub(r.getHomeClub());
        c.setSpokenName(r.getSpokenName());
        c.setCreatedAt(r.getCreatedAt());
        c.setUpdatedAt(r.getUpdatedAt());
        return c;
    }

    @Override
    protected void toRecord(Competitor c, CompetitorsRecord r) {
        r.setDisplayName(c.getDisplayName());
        r.setExternalSource(c.getExternalSource());
        r.setExternalId(c.getExternalId());
        r.setBrcaNumber(c.getBrcaNumber());
        r.setHomeClub(c.getHomeClub());
        r.setSpokenName(c.getSpokenName());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
    }

    @Override
    protected Long idOf(Competitor c) {
        return c.getId();
    }

    @Override
    protected void setId(Competitor c, Long id) {
        c.setId(id);
    }
}
