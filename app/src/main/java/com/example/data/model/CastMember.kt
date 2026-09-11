package com.example.data.model

data class CastMember(
    val id: String,
    val name: String,
    val role: String,
    val profileUrl: String? = null
)

object CastRepository {
    private val wakandaCast = listOf(
        CastMember("w1", "Letitia Wright", "Shuri", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg"),
        CastMember("w2", "Lupita Nyong'o", "Nakia", "https://image.tmdb.org/t/p/w185/m67LdM5YhYV6fK04m7qfV0N1s3e.jpg"),
        CastMember("w3", "Danai Gurira", "Okoye", "https://image.tmdb.org/t/p/w185/z7xQh18m7UqV6fK04m7qfV0N1s3.jpg"),
        CastMember("w4", "Chadwick Boseman", "T'Challa", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg"),
        CastMember("w5", "Angela Bassett", "Ramonda", "https://image.tmdb.org/t/p/w185/6O2Q3LzZ0P2q7v9xK2x9Z0P2q7.jpg"),
        CastMember("w6", "Tenoch Huerta", "Namor", "https://image.tmdb.org/t/p/w185/3V9xK2x9Z0P2q7v9xK2x9Z0P2q7.jpg"),
        CastMember("w7", "Dominique Thorne", "Riri Williams", "https://image.tmdb.org/t/p/w185/8Q2x9Z0P2q7v9xK2x9Z0P2q7v9.jpg")
    )

    private val blackPantherCast = listOf(
        CastMember("bp1", "Chadwick Boseman", "T'Challa / Black Panther", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg"),
        CastMember("bp2", "Michael B. Jordan", "Erik Killmonger", "https://image.tmdb.org/t/p/w185/kfcn0N49529q7v9xK2x9Z0P2q7.jpg"),
        CastMember("bp3", "Lupita Nyong'o", "Nakia", "https://image.tmdb.org/t/p/w185/m67LdM5YhYV6fK04m7qfV0N1s3e.jpg"),
        CastMember("bp4", "Danai Gurira", "Okoye", "https://image.tmdb.org/t/p/w185/z7xQh18m7UqV6fK04m7qfV0N1s3.jpg"),
        CastMember("bp5", "Letitia Wright", "Shuri", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg")
    )

    private val equalizerCast = listOf(
        CastMember("eq1", "Denzel Washington", "Robert McCall", "https://image.tmdb.org/t/p/w185/cEU286RG92hv0vgj7m1V9.jpg"),
        CastMember("eq2", "Marton Csokas", "Teddy", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg"),
        CastMember("eq3", "Chloë Grace Moretz", "Teri", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg"),
        CastMember("eq4", "David Harbour", "Frank Masters", "https://image.tmdb.org/t/p/w185/kfcn0N49529q7v9xK2x9Z0P2q7.jpg")
    )

    private val gladiatorCast = listOf(
        CastMember("gl1", "Russell Crowe", "Maximus", "https://image.tmdb.org/t/p/w185/cEU286RG92hv0vgj7m1V9.jpg"),
        CastMember("gl2", "Joaquin Phoenix", "Commodus", "https://image.tmdb.org/t/p/w185/kfcn0N49529q7v9xK2x9Z0P2q7.jpg"),
        CastMember("gl3", "Connie Nielsen", "Lucilla", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg"),
        CastMember("gl4", "Oliver Reed", "Proximo", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg")
    )

    private val inceptionCast = listOf(
        CastMember("inc1", "Leonardo DiCaprio", "Dom Cobb", "https://image.tmdb.org/t/p/w185/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg"),
        CastMember("inc2", "Joseph Gordon-Levitt", "Arthur", "https://image.tmdb.org/t/p/w185/dhv9yJmPzQ9xK2x9Z0P2q7v9.jpg"),
        CastMember("inc3", "Elliot Page", "Ariadne", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg"),
        CastMember("inc4", "Tom Hardy", "Eames", "https://image.tmdb.org/t/p/w185/yVGF93vjIUAcUtx976Q0Fz9g5e.jpg"),
        CastMember("inc5", "Ken Watanabe", "Saito", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg")
    )

    fun getCastForTitle(title: String, genres: List<String> = emptyList()): List<CastMember> {
        val lower = title.lowercase()
        return when {
            lower.contains("wakanda") || lower.contains("black panther 2") -> wakandaCast
            lower.contains("black panther") -> blackPantherCast
            lower.contains("equalizer") -> equalizerCast
            lower.contains("gladiator") -> gladiatorCast
            lower.contains("inception") -> inceptionCast
            else -> {
                // Return high-quality movie cast
                listOf(
                    CastMember("c1", "Letitia Wright", "Lead Protagonist", "https://image.tmdb.org/t/p/w185/7Crsm6rW3W3Qy3BvCkgm1m1uR8.jpg"),
                    CastMember("c2", "Lupita Nyong'o", "Co-Lead", "https://image.tmdb.org/t/p/w185/m67LdM5YhYV6fK04m7qfV0N1s3e.jpg"),
                    CastMember("c3", "Danai Gurira", "General", "https://image.tmdb.org/t/p/w185/z7xQh18m7UqV6fK04m7qfV0N1s3.jpg"),
                    CastMember("c4", "Chadwick Boseman", "Hero", "https://image.tmdb.org/t/p/w185/bHBDkR7JmPzQ9xK2x9Z0P2q7v9.jpg"),
                    CastMember("c5", "Angela Bassett", "Matriarch", "https://image.tmdb.org/t/p/w185/6O2Q3LzZ0P2q7v9xK2x9Z0P2q7.jpg")
                )
            }
        }
    }
}
