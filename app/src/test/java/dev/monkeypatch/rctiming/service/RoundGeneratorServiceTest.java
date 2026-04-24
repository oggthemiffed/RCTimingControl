package dev.monkeypatch.rctiming.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled("wave 0 stub — enabled in plan 03")
@ExtendWith(MockitoExtension.class)
class RoundGeneratorServiceTest {

    // private RoundGeneratorService service; // uncomment in plan 03 once production class exists

    @Test
    void heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats() {
        // TODO: plan 03 implements assertions
    }

    @Test
    void bumpUpSeeding_topNofBFinal_appendedToAFinal() {
        // TODO: plan 03 implements assertions
    }
}
