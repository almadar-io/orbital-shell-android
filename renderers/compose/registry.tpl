package generated.patterns.core

import generated.patterns.PatternRenderer

object CorePatternRegistry {
    private val registry: Map<String, PatternRenderer> = mapOf(
{{entries}}
    )

    fun get(patternName: String): PatternRenderer {
        val inner: PatternRenderer = registry[patternName] ?: { props, modifier, onEvent ->
            FallbackRenderer(patternName, props, modifier, onEvent)
        }
        return { props, modifier, onEvent ->
            inner(props, modifier.verificationContainer(VerificationIds.pattern(patternName)).almadarNodeEnter(props), onEvent)
        }
    }
}
