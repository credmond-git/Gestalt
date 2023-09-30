package org.github.gestalt.config.kotlin.decoder

import org.github.gestalt.config.decoder.DecoderContext
import org.github.gestalt.config.decoder.DecoderRegistry
import org.github.gestalt.config.entity.ValidationLevel
import org.github.gestalt.config.exceptions.GestaltException
import org.github.gestalt.config.kotlin.reflect.kTypeCaptureOf
import org.github.gestalt.config.lexer.SentenceLexer
import org.github.gestalt.config.node.ConfigNodeService
import org.github.gestalt.config.node.LeafNode
import org.github.gestalt.config.path.mapper.DotNotationPathMapper
import org.github.gestalt.config.path.mapper.StandardPathMapper
import org.github.gestalt.config.reflect.TypeCapture
import org.github.gestalt.config.utils.ValidateOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

internal class IntegerDecoderTest {
    var configNodeService: ConfigNodeService? = null
    var lexer: SentenceLexer? = null
    var decoderService: DecoderRegistry? = null

    @BeforeEach
    fun setup() {
        configNodeService = Mockito.mock(ConfigNodeService::class.java)
        lexer = Mockito.mock(SentenceLexer::class.java)
        decoderService = DecoderRegistry(
            listOf(IntegerDecoder()), configNodeService, lexer, listOf(
                StandardPathMapper(),
                DotNotationPathMapper()
            )
        )
    }

    @Test
    fun name() {
        val decoder = IntegerDecoder()
        Assertions.assertEquals("kInt", decoder.name())
    }

    @Test
    fun matches() {
        val decoder = IntegerDecoder()
        Assertions.assertTrue(decoder.matches(kTypeCaptureOf<Int>()))
        Assertions.assertFalse(decoder.matches(object : TypeCapture<Int?>() {}))
        Assertions.assertFalse(decoder.matches(TypeCapture.of(Int::class.java)))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<String>()))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<Date>()))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<List<Byte>>()))
    }

    @Test
    @Throws(GestaltException::class)
    fun decode() {
        val integerDecoder = IntegerDecoder()
        val validate: ValidateOf<Int> = integerDecoder.decode(
            "db.port", LeafNode("124"),
            TypeCapture.of(
                Int::class.java
            ),
            DecoderContext(decoderService, null),
        )
        Assertions.assertTrue(validate.hasResults())
        Assertions.assertFalse(validate.hasErrors())
        Assertions.assertEquals(124, validate.results())
        Assertions.assertEquals(0, validate.errors.size)
    }

    @Test
    @Throws(GestaltException::class)
    fun notAnInteger() {
        val integerDecoder = IntegerDecoder()
        val validate: ValidateOf<Int> = integerDecoder.decode(
            "db.port", LeafNode("12s4"),
            TypeCapture.of(
                Int::class.java
            ),
            DecoderContext(decoderService, null),
        )
        Assertions.assertFalse(validate.hasResults())
        Assertions.assertTrue(validate.hasErrors())
        Assertions.assertNull(validate.results())
        Assertions.assertNotNull(validate.errors)
        Assertions.assertEquals(ValidationLevel.ERROR, validate.errors[0].level())
        Assertions.assertEquals(
            "Unable to parse a number on Path: db.port, from node: LeafNode{value='12s4'} " +
                "attempting to decode kInt",
            validate.errors[0].description()
        )
    }

    @Test
    @Throws(GestaltException::class)
    fun notAIntegerTooLarge() {
        val decoder = IntegerDecoder()
        val validate: ValidateOf<Int> = decoder.decode(
            "db.port",
            LeafNode("12345678901234567890123456789012345678901234567890123456789"),
            TypeCapture.of(Int::class.java),
            DecoderContext(decoderService, null),
        )
        Assertions.assertFalse(validate.hasResults())
        Assertions.assertTrue(validate.hasErrors())
        Assertions.assertNull(validate.results())
        Assertions.assertNotNull(validate.errors)
        Assertions.assertEquals(ValidationLevel.ERROR, validate.errors[0].level())
        Assertions.assertEquals(
            "Unable to decode a number on path: db.port, from node: " +
                "LeafNode{value='12345678901234567890123456789012345678901234567890123456789'} attempting to decode kInt",
            validate.errors[0].description()
        )
    }
}
