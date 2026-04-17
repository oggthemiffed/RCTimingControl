package dev.monkeypatch.rctiming.domain.transponder;

import dev.monkeypatch.rctiming.api.racer.dto.CreateTransponderRequest;
import dev.monkeypatch.rctiming.api.racer.dto.TransponderDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class TransponderService {

    private final TransponderRepository transponderRepository;

    public TransponderService(TransponderRepository transponderRepository) {
        this.transponderRepository = transponderRepository;
    }

    @Transactional(readOnly = true)
    public List<TransponderDto> findForUser(Long userId) {
        return transponderRepository.findByUserId(userId).stream()
                .map(TransponderDto::from)
                .toList();
    }

    public TransponderDto create(Long userId, CreateTransponderRequest req) {
        Transponder transponder = new Transponder();
        transponder.setUserId(userId);
        transponder.setTransponderNumber(req.transponderNumber());
        transponder.setLabel(req.label());
        transponder.setCreatedAt(Instant.now());
        // DataIntegrityViolationException propagates on duplicate transponder_number
        return TransponderDto.from(transponderRepository.save(transponder));
    }

    public void delete(Long transponderId, Long userId) {
        Transponder transponder = transponderRepository.findById(transponderId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transponder not found: " + transponderId));
        transponderRepository.delete(transponder);
    }
}
