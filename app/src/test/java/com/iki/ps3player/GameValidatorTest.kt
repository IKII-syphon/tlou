package com.iki.ps3player

import com.iki.ps3player.game.GameValidator
import org.junit.Assert.*
import org.junit.Test

class GameValidatorTest {
    @Test fun acceptsIsoAndPkgCaseInsensitive() {
        assertTrue(GameValidator.validate("game.iso", 100).isSuccess)
        assertTrue(GameValidator.validate("GAME.PKG", 100).isSuccess)
    }
    @Test fun rejectsUnsupportedOrMissingExtension() {
        assertTrue(GameValidator.validate("game.zip", 100).isFailure)
        assertTrue(GameValidator.validate("noext", 100).isFailure)
    }
    @Test fun rejectsEmptyFile() {
        assertTrue(GameValidator.validate("game.iso", 0).isFailure)
    }
}
