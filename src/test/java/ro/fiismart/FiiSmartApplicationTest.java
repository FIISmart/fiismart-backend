package ro.fiismart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FiiSmartApplicationTest {

    @Test
    void mainClass_instantiation() {
        assertDoesNotThrow(() -> new FiiSmartApplication());
    }
}
