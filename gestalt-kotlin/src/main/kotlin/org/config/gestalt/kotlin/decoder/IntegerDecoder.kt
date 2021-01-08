package org.config.gestalt.kotlin.decoder

import org.config.gestalt.decoder.LeafDecoder
import org.config.gestalt.decoder.Priority
import org.config.gestalt.entity.ValidationError
import org.config.gestalt.kotlin.reflect.KTypeCapture
import org.config.gestalt.node.ConfigNode
import org.config.gestalt.reflect.TypeCapture
import org.config.gestalt.utils.StringUtils
import org.config.gestalt.utils.ValidateOf

class IntegerDecoder : LeafDecoder<Int>() {
    override fun name(): String {
        return "Int"
    }

    override fun priority(): Priority {
        return Priority.HIGH
    }

    override fun matches(klass: TypeCapture<*>): Boolean {
        return if (klass is KTypeCapture<*>) {
            klass.isAssignableFrom(Int::class)
        } else {
            false
        }
    }

    override fun leafDecode(path: String?, node: ConfigNode): ValidateOf<Int> {
        val results: ValidateOf<Int>
        val value = node.value.orElse("")
        results = if (StringUtils.isInteger(value)) {
            try {
                val intVal = value.toInt()
                ValidateOf.valid(intVal)
            } catch (e: NumberFormatException) {
                ValidateOf.inValid(ValidationError.DecodingNumberFormatException(path, node, name()))
            }
        } else {
            ValidateOf.inValid(ValidationError.DecodingNumberParsing(path, node, name()))
        }
        return results
    }
}