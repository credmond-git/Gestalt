package org.github.gestalt.config.kotlin.decoder

import org.github.gestalt.config.decoder.DecoderContext
import org.github.gestalt.config.decoder.DecoderRegistry
import org.github.gestalt.config.entity.ValidationLevel
import org.github.gestalt.config.exceptions.GestaltException
import org.github.gestalt.config.kotlin.reflect.kTypeCaptureOf
import org.github.gestalt.config.lexer.PathLexer
import org.github.gestalt.config.lexer.SentenceLexer
import org.github.gestalt.config.node.ConfigNodeService
import org.github.gestalt.config.node.LeafNode
import org.github.gestalt.config.node.MapNode
import org.github.gestalt.config.path.mapper.DotNotationPathMapper
import org.github.gestalt.config.path.mapper.StandardPathMapper
import org.github.gestalt.config.reflect.TypeCapture
import org.github.gestalt.config.tag.Tags
import org.github.gestalt.config.utils.GResultOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

internal class StringAndLeafDecoderTest {
    var configNodeService: ConfigNodeService? = null
    var lexer: SentenceLexer? = null
    var decoderService: DecoderRegistry? = null

    @BeforeEach
    fun setup() {
        configNodeService = Mockito.mock(ConfigNodeService::class.java)
        lexer = Mockito.mock(SentenceLexer::class.java)
        decoderService = DecoderRegistry(
            listOf(BooleanDecoder()), configNodeService, lexer, listOf(
                StandardPathMapper(),
                DotNotationPathMapper()
            )
        )
    }

    @Test
    fun name() {
        val decoder = StringDecoder()
        Assertions.assertEquals("kString", decoder.name())
    }

    @Test
    fun canDecode() {
        val decoder = StringDecoder()
        Assertions.assertTrue(decoder.canDecode("", Tags.of(), LeafNode(""), kTypeCaptureOf<String>()))
        Assertions.assertFalse(decoder.canDecode("", Tags.of(), LeafNode(""), object : TypeCapture<String?>() {}))
        Assertions.assertFalse(decoder.canDecode("", Tags.of(), LeafNode(""), TypeCapture.of(String::class.java)))
        Assertions.assertFalse(decoder.canDecode("", Tags.of(), LeafNode(""), kTypeCaptureOf<Int>()))
        Assertions.assertFalse(decoder.canDecode("", Tags.of(), LeafNode(""), kTypeCaptureOf<Date>()))
        Assertions.assertFalse(decoder.canDecode("", Tags.of(), LeafNode(""), kTypeCaptureOf<List<Byte>>()))
    }

    @Test
    @Throws(GestaltException::class)
    fun decode() {
        val stringDecoder = StringDecoder()
        val result: GResultOf<String> = stringDecoder.decode(
            "db.user", Tags.of(),
            LeafNode("test"),
            TypeCapture.of(
                String::class.java
            ),
            DecoderContext(decoderService, null, null, PathLexer()),
        )
        Assertions.assertTrue(result.hasResults())
        Assertions.assertFalse(result.hasErrors())
        Assertions.assertEquals("test", result.results())
        Assertions.assertEquals(0, result.errors.size)
    }

    @Test
    @Throws(GestaltException::class)
    fun `invalid Leaf Node`() {
        val stringDecoder = StringDecoder()
        val result: GResultOf<String> = stringDecoder.decode(
            "db.user", Tags.of(),
            LeafNode(null),
            TypeCapture.of(
                String::class.java
            ),
            DecoderContext(decoderService, null, null, PathLexer()),
        )
        Assertions.assertFalse(result.hasResults())
        Assertions.assertTrue(result.hasErrors())
        Assertions.assertNull(result.results())
        Assertions.assertNotNull(result.errors)
        Assertions.assertEquals(ValidationLevel.MISSING_VALUE, result.errors[0].level())
        Assertions.assertEquals(
            "Leaf on path: db.user, has no value attempting to decode kString",
            result.errors[0].description()
        )
    }

    @Test
    @Throws(GestaltException::class)
    fun `decode Invalid Node`() {
        val stringDecoder = StringDecoder()
        val result: GResultOf<String> = stringDecoder.decode(
            "db.user", Tags.of(),
            MapNode(HashMap()),
            TypeCapture.of(
                String::class.java
            ),
            DecoderContext(decoderService, null, null, PathLexer()),
        )
        Assertions.assertFalse(result.hasResults())
        Assertions.assertTrue(result.hasErrors())
        Assertions.assertNull(result.results())
        Assertions.assertNotNull(result.errors)
        Assertions.assertEquals(ValidationLevel.ERROR, result.errors[0].level())
        Assertions.assertEquals(
            "Expected a leaf on path: db.user, received node type: map, attempting to decode kString",
            result.errors[0].description()
        )
    }
}
