package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand;
import dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus;
import dev.monkeypatch.rctiming.forwarder.proto.LapPassing;
import dev.monkeypatch.rctiming.forwarder.proto.StatusAck;
import dev.monkeypatch.rctiming.forwarder.proto.TimingServiceGrpc;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 5 / FORWARDER-03: gRPC server-side service. Receives LapPassing stream from forwarder
 * and publishes LapPassingEvent into the Spring ApplicationEventPublisher chain (TIMING-01).
 *
 * <p>FORWARDER-03 RESEND clause: RC-4 text protocol has no RESEND mechanism.
 * ForwarderCommand proto type includes a future RESEND field for P3 binary protocol use;
 * in Phase 5 the cloud never sends RESEND commands. The stream is bidirectional in proto but
 * only LapPassing flows forwarder→cloud in Phase 5.
 */
@Service
@Transactional(readOnly = true)
public class ForwarderGrpcService extends TimingServiceGrpc.TimingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ForwarderGrpcService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final RaceRepository raceRepository;
    private final ForwarderStatusPublisher statusPublisher;

    public ForwarderGrpcService(ApplicationEventPublisher eventPublisher,
                                RaceRepository raceRepository,
                                ForwarderStatusPublisher statusPublisher) {
        this.eventPublisher = eventPublisher;
        this.raceRepository = raceRepository;
        this.statusPublisher = statusPublisher;
    }

    @Override
    public void reportStatus(ForwarderStatus request, StreamObserver<StatusAck> responseObserver) {
        if ("DECODER".equals(request.getComponent())) {
            statusPublisher.onDecoderStatus(request.getState().name());
            log.info("Decoder status reported: {}", request.getState());
        }
        responseObserver.onNext(StatusAck.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<LapPassing> streamPassings(StreamObserver<ForwarderCommand> responseObserver) {
        statusPublisher.onForwarderConnected();
        return new StreamObserver<>() {

            @Override
            public void onNext(LapPassing msg) {
                // Publish with the active race ID, or 0L when no race is running.
                // LapTimingService ignores raceId=0 (no live state). PracticeTimingService
                // ignores raceId entirely — it queries practiceSessionRepository directly.
                long raceId = raceRepository.findFirstByStatus(RaceStatus.RUNNING)
                        .map(race -> race.getId())
                        .orElse(0L);
                eventPublisher.publishEvent(
                        new LapPassingEvent(raceId, msg.getTransponderNumber(), msg.getRtcTimeMicros()));
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Forwarder stream error: {}", t.getMessage());
                statusPublisher.onForwarderDisconnected();
            }

            @Override
            public void onCompleted() {
                log.info("Forwarder stream completed normally");
                responseObserver.onCompleted();
                statusPublisher.onForwarderDisconnected();
            }
        };
    }
}
