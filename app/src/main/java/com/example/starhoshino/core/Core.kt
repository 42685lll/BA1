package com.example.starhoshino.core

object KnowledgeBase {

    private val memory = mutableListOf<String>()

    fun remember(text: String) {
        memory.add(text)
        if (memory.size > 100) memory.removeAt(0)
    }

    fun reply(input: String): String {
        remember("老师：$input")
        return "唔…老师刚刚说：$input，大叔在听哦。"
    }

    fun recent(): List<String> = memory.toList()
}
