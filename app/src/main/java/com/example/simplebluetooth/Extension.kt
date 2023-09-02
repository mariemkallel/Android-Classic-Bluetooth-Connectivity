package com.example.simplebluetooth



fun String.extractData(): List<String> {
    val regex = Regex("\\{(.*?)\\}") // Matches text enclosed in square brackets
    val matches = regex.findAll(this)
    val resultList = mutableListOf<String>()

    for (match in matches) {
        val textInsideBrackets = match.groupValues[1]
        resultList.add(textInsideBrackets)
    }

    return resultList
}
