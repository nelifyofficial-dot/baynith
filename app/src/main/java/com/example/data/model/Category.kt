package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class Category(
    val id: String = "",
    val name: String = "",
    val slug: String? = null,
    val order: Int = 0
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Category {
            val data = doc.data ?: emptyMap<String, Any>()
            val orderVal = when (val o = data["order"]) {
                is Number -> o.toInt()
                is String -> o.toIntOrNull() ?: 0
                else -> 0
            }
            return Category(
                id = doc.id,
                name = data["name"]?.toString() ?: doc.id,
                slug = data["slug"]?.toString(),
                order = orderVal
            )
        }
    }
}
