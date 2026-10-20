package com.example.financecalc1

enum class InterfaceStyle(val id: String, val title: Words, val description: Words, val defaultPalette: String) {
    WAVE("wave", Words("Волна", "Толқын"), Words("Волнистая шапка и большая клавиатура", "Толқынды тақырып және үлкен пернетақта"), "wave"),
    ORBIT("orbit", Words("Orbit", "Orbit"), Words("Мягкие поля и круговой результат", "Жұмсақ өрістер және шеңберлі нәтиже"), "mint"),
    PRISM("prism", Words("Prism", "Prism"), Words("Компактные блоки и акцентный результат", "Ықшам блоктар және айқын нәтиже"), "graphite");
    companion object { fun from(id: String?) = entries.firstOrNull { it.id == id } ?: WAVE }
}
