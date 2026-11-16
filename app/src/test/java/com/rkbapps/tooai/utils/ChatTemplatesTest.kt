package com.rkbapps.tooai.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTemplatesTest {

    // Lines from the template embedded in Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm.
    private val qwen25 = """
        {%- if messages[0]['role'] == 'system' %}
            {{- '<|im_start|>system\n' + messages[0]['content'] + '<|im_end|>\n' }}
        {%- endif %}
        {%- for message in messages %}
            {{- '<|im_start|>' + message.role + '\n' + message.content + '<|im_end|>' + '\n' }}
        {%- endfor %}
    """.trimIndent()

    @Test
    fun `a ChatML template that joins content as a string needs the override`() {
        assertTrue(needsChatMlOverride(qwen25))
    }

    @Test
    fun `binary bytes around the template don't stop detection`() {
        assertTrue(needsChatMlOverride("\u0000ÿTFL3\u0001" + qwen25 + "\u0000\u0013"))
    }

    @Test
    fun `a ChatML template that checks the content type is left alone`() {
        val listAware = """
            {%- for message in messages %}
                {%- if message['content'] is string %}
                    {{- '<|im_start|>' + message['role'] + '\n' + message['content'] + '<|im_end|>\n' }}
                {%- endif %}
            {%- endfor %}
        """.trimIndent()
        assertFalse(needsChatMlOverride(listAware))
    }

    @Test
    fun `a non-ChatML template is left alone`() {
        val gemma = """
            {%- for message in messages -%}
            {{ '<start_of_turn>' + role + '\n' }}
            {%- for item in message['content'] -%}{{ item['text'] | trim }}{%- endfor -%}
            {%- endfor -%}
        """.trimIndent()
        assertFalse(needsChatMlOverride(gemma))
    }

    @Test
    fun `a file with no template is left alone`() {
        assertFalse(needsChatMlOverride("\u0000\u0001\u0002 no template here"))
    }

    @Test
    fun `the replacement template handles both content forms and is not itself flagged`() {
        assertTrue(CHATML_TEMPLATE.contains("is string"))
        assertTrue(CHATML_TEMPLATE.contains("item['text']"))
        assertFalse(needsChatMlOverride(CHATML_TEMPLATE))
    }
}
