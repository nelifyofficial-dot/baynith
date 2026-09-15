package com.example.data.model

data class CastMember(
    val id: String,
    val name: String,
    val role: String,
    val profileUrl: String? = null,
    val character: String = role,
    val biography: String = "",
    val birthDate: String = "",
    val placeOfBirth: String = "",
    val knownFor: String = ""
)

object CastRepository {
    private val wakandaCast = listOf(
        CastMember(
            id = "w1",
            name = "Letitia Wright",
            role = "Shuri / Black Panther",
            profileUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
            biography = "Letitia Wright is a Guyanese-British actress. She achieved global fame portraying Shuri in the Marvel Cinematic Universe's Black Panther and Black Panther: Wakanda Forever, winning an NAACP Image Award.",
            birthDate = "October 31, 1993",
            placeOfBirth = "Georgetown, Guyana",
            knownFor = "Black Panther, Wakanda Forever, Death on the Nile, Small Axe"
        ),
        CastMember(
            id = "w2",
            name = "Lupita Nyong'o",
            role = "Nakia",
            profileUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&auto=format&fit=crop&q=80",
            biography = "Lupita Nyong'o is a Kenyan-Mexican actress. She is the recipient of an Academy Award for Best Supporting Actress for 12 Years a Slave and has starred as Nakia in Black Panther and Jordan Peele's Us.",
            birthDate = "March 1, 1983",
            placeOfBirth = "Mexico City, Mexico",
            knownFor = "12 Years a Slave, Black Panther, Us, A Quiet Place: Day One"
        ),
        CastMember(
            id = "w3",
            name = "Danai Gurira",
            role = "General Okoye",
            profileUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
            biography = "Danai Gurira is a Zimbabwean-American actress and playwright. She is best known for her roles as Michonne Hawthorne on The Walking Dead and Okoye in the Marvel Cinematic Universe.",
            birthDate = "February 14, 1978",
            placeOfBirth = "Grinnell, Iowa, USA",
            knownFor = "The Walking Dead, Black Panther, Avengers: Endgame, Eclipsed"
        ),
        CastMember(
            id = "w4",
            name = "Chadwick Boseman",
            role = "T'Challa / King of Wakanda",
            profileUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
            biography = "Chadwick Aaron Boseman was an iconic American actor. In his two-decade career, Boseman portrayed cultural icons like Jackie Robinson and James Brown before immortalizing T'Challa in Black Panther.",
            birthDate = "November 29, 1976",
            placeOfBirth = "Anderson, South Carolina, USA",
            knownFor = "Black Panther, 42, Get on Up, Ma Rainey's Black Bottom"
        ),
        CastMember(
            id = "w5",
            name = "Angela Bassett",
            role = "Queen Ramonda",
            profileUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400&auto=format&fit=crop&q=80",
            biography = "Angela Bassett is an acclaimed American actress. With a storied career spanning four decades, she won two Golden Globe Awards and was nominated for two Academy Awards.",
            birthDate = "August 16, 1958",
            placeOfBirth = "New York City, New York, USA",
            knownFor = "What's Love Got to Do with It, Wakanda Forever, 9-1-1, Malcolm X"
        ),
        CastMember(
            id = "w6",
            name = "Tenoch Huerta",
            role = "Namor / Ku'ku'lkán",
            profileUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
            biography = "Tenoch Huerta Mejía is a Mexican actor who has appeared in numerous Latin American and international films, including Narcos: Mexico and portraying Namor in Black Panther: Wakanda Forever.",
            birthDate = "January 29, 1981",
            placeOfBirth = "Ecatepec, Mexico",
            knownFor = "Narcos: Mexico, Black Panther: Wakanda Forever, The Forever Purge"
        ),
        CastMember(
            id = "w7",
            name = "Dominique Thorne",
            role = "Riri Williams / Ironheart",
            profileUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&auto=format&fit=crop&q=80",
            biography = "Dominique Thorne is an American actress known for her appearances in Beale Street Could Talk, Judas and the Black Messiah, and introducing Riri Williams in Wakanda Forever.",
            birthDate = "November 11, 1997",
            placeOfBirth = "New York City, New York, USA",
            knownFor = "Judas and the Black Messiah, Ironheart, Wakanda Forever"
        )
    )

    private val blackPantherCast = wakandaCast.take(5)

    private val equalizerCast = listOf(
        CastMember(
            id = "eq1",
            name = "Denzel Washington",
            role = "Robert McCall",
            profileUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
            biography = "Denzel Hayes Washington Jr. is a legendary two-time Academy Award-winning American actor, director, and producer widely regarded as one of the greatest actors of the 21st century.",
            birthDate = "December 28, 1954",
            placeOfBirth = "Mount Vernon, New York, USA",
            knownFor = "The Equalizer, Training Day, Glory, Malcolm X, Fences"
        ),
        CastMember(
            id = "eq2",
            name = "Dakota Fanning",
            role = "Emma Collins",
            profileUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
            biography = "Hannah Dakota Fanning is an American actress who rose to prominence as a child actress in I Am Sam and Man on Fire before starring in The Alienist and The Equalizer 3.",
            birthDate = "February 23, 1994",
            placeOfBirth = "Conyers, Georgia, USA",
            knownFor = "The Equalizer 3, Man on Fire, War of the Worlds, Once Upon a Time in Hollywood"
        ),
        CastMember(
            id = "eq3",
            name = "Marton Csokas",
            role = "Nicolai Itchenko / Teddy",
            profileUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
            biography = "Marton Csokas is a New Zealand film and television actor, known for playing villains in Hollywood blockbusters such as The Equalizer, The Bourne Supremacy, and Kingdom of Heaven.",
            birthDate = "June 30, 1966",
            placeOfBirth = "Invercargill, New Zealand",
            knownFor = "The Equalizer, Kingdom of Heaven, The Lord of the Rings, xXx"
        ),
        CastMember(
            id = "eq4",
            name = "Chloë Grace Moretz",
            role = "Alina / Teri",
            profileUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80",
            biography = "Chloë Grace Moretz is an American actress who began acting as a child in The Amityville Horror and Kick-Ass, going on to star in The Equalizer, Carrie, and Hugo.",
            birthDate = "February 10, 1997",
            placeOfBirth = "Atlanta, Georgia, USA",
            knownFor = "Kick-Ass, The Equalizer, Hugo, Let Me In, Suspiria"
        )
    )

    private val gladiatorCast = listOf(
        CastMember(
            id = "gl1",
            name = "Russell Crowe",
            role = "Maximus Decimus Meridius",
            profileUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
            biography = "Russell Ira Crowe is an Academy Award-winning New Zealand-born actor and musician known for Gladiator, A Beautiful Mind, Master and Commander, and The Insider.",
            birthDate = "April 7, 1964",
            placeOfBirth = "Wellington, New Zealand",
            knownFor = "Gladiator, A Beautiful Mind, L.A. Confidential, The Pope's Exorcist"
        ),
        CastMember(
            id = "gl2",
            name = "Joaquin Phoenix",
            role = "Commodus",
            profileUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
            biography = "Joaquin Rafael Phoenix is an American actor known for his dark and unconventional characters. He won an Academy Award for Joker and received nominations for Gladiator and Her.",
            birthDate = "October 28, 1974",
            placeOfBirth = "San Juan, Puerto Rico",
            knownFor = "Gladiator, Joker, Her, The Master, Walk the Line"
        ),
        CastMember(
            id = "gl3",
            name = "Connie Nielsen",
            role = "Lucilla",
            profileUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
            biography = "Connie Inge-Lise Nielsen is a Danish actress celebrated for her starring role as Lucilla in Gladiator and Queen Hippolyta in Wonder Woman and Justice League.",
            birthDate = "July 3, 1965",
            placeOfBirth = "Frederikshavn, Denmark",
            knownFor = "Gladiator, Wonder Woman, Justice League, The Devil's Advocate"
        ),
        CastMember(
            id = "gl4",
            name = "Pedro Pascal",
            role = "Marcus Acacius",
            profileUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
            biography = "Pedro Pascal is a Chilean-American actor who gained international acclaim in Game of Thrones, Narcos, The Mandalorian, The Last of Us, and Gladiator II.",
            birthDate = "April 2, 1975",
            placeOfBirth = "Santiago, Chile",
            knownFor = "The Last of Us, The Mandalorian, Game of Thrones, Narcos, Gladiator II"
        )
    )

    private val inceptionCast = listOf(
        CastMember(
            id = "inc1",
            name = "Leonardo DiCaprio",
            role = "Dom Cobb",
            profileUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
            biography = "Leonardo Wilhelm DiCaprio is an American actor and producer renowned for his work in biopics and period films. He is an Academy Award, BAFTA, and Golden Globe winner.",
            birthDate = "November 11, 1974",
            placeOfBirth = "Los Angeles, California, USA",
            knownFor = "Inception, Titanic, The Revenant, The Wolf of Wall Street, Killers of the Flower Moon"
        ),
        CastMember(
            id = "inc2",
            name = "Joseph Gordon-Levitt",
            role = "Arthur",
            profileUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
            biography = "Joseph Gordon-Levitt is an American actor, filmmaker, and musician. He received Golden Globe nominations for (500) Days of Summer and 50/50.",
            birthDate = "February 17, 1981",
            placeOfBirth = "Los Angeles, California, USA",
            knownFor = "Inception, The Dark Knight Rises, Looper, (500) Days of Summer"
        ),
        CastMember(
            id = "inc3",
            name = "Elliot Page",
            role = "Ariadne",
            profileUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
            biography = "Elliot Page is a Canadian actor nominated for an Academy Award for Juno. He stars in Christopher Nolan's Inception and Netflix's The Umbrella Academy.",
            birthDate = "February 21, 1987",
            placeOfBirth = "Halifax, Nova Scotia, Canada",
            knownFor = "Inception, Juno, The Umbrella Academy, X-Men: Days of Future Past"
        ),
        CastMember(
            id = "inc4",
            name = "Tom Hardy",
            role = "Eames",
            profileUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
            biography = "Edward Thomas Hardy is an English actor. After studying acting at Drama Centre London, he made his film debut in Black Hawk Down and gained acclaim in Inception, Mad Max: Fury Road, and Venom.",
            birthDate = "September 15, 1977",
            placeOfBirth = "Hammersmith, London, UK",
            knownFor = "Inception, Mad Max: Fury Road, The Dark Knight Rises, Venom, Peaky Blinders"
        ),
        CastMember(
            id = "inc5",
            name = "Cillian Murphy",
            role = "Robert Michael Fischer",
            profileUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
            biography = "Cillian Murphy is an Irish Academy Award-winning actor known for Oppenheimer, Peaky Blinders, 28 Days Later, and numerous collaborations with Christopher Nolan.",
            birthDate = "May 25, 1976",
            placeOfBirth = "Douglas, County Cork, Ireland",
            knownFor = "Oppenheimer, Peaky Blinders, Inception, Batman Begins, Dunkirk"
        )
    )

    private val defaultCast = listOf(
        CastMember(
            id = "c1",
            name = "Michael B. Jordan",
            role = "Lead Actor",
            profileUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
            biography = "Michael Bakari Jordan is an American actor, producer, and director known for his roles in Fruitvale Station, Creed, and Black Panther.",
            birthDate = "February 9, 1987",
            placeOfBirth = "Santa Ana, California, USA",
            knownFor = "Creed, Black Panther, Fruitvale Station, The Wire"
        ),
        CastMember(
            id = "c2",
            name = "Zendaya",
            role = "Co-Lead Star",
            profileUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
            biography = "Zendaya Maree Stoermer Coleman is an Emmy and Golden Globe-winning American actress and singer celebrated for Euphoria, Dune, and Spider-Man.",
            birthDate = "September 1, 1996",
            placeOfBirth = "Oakland, California, USA",
            knownFor = "Dune, Euphoria, Spider-Man: No Way Home, Challengers"
        ),
        CastMember(
            id = "c3",
            name = "Idris Elba",
            role = "Supporting Lead",
            profileUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
            biography = "Idrissa Akuna Elba is an English actor, producer, and musician known for Luther, The Wire, Beasts of No Nation, and the Marvel Cinematic Universe.",
            birthDate = "September 6, 1972",
            placeOfBirth = "London, England, UK",
            knownFor = "Luther, The Wire, Thor: Ragnarok, Beasts of No Nation, Sonic the Hedgehog 2"
        ),
        CastMember(
            id = "c4",
            name = "Florence Pugh",
            role = "Featured Cast",
            profileUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80",
            biography = "Florence Pugh is an English actress nominated for an Academy Award for Little Women. She stars in Midsommar, Black Widow, Oppenheimer, and Dune: Part Two.",
            birthDate = "January 3, 1996",
            placeOfBirth = "Oxford, England, UK",
            knownFor = "Oppenheimer, Dune: Part Two, Little Women, Midsommar, Black Widow"
        ),
        CastMember(
            id = "c5",
            name = "Pedro Pascal",
            role = "Special Appearance",
            profileUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
            biography = "Pedro Pascal is a Chilean-American actor who gained worldwide fame in Game of Thrones, Narcos, The Mandalorian, and The Last of Us.",
            birthDate = "April 2, 1975",
            placeOfBirth = "Santiago, Chile",
            knownFor = "The Last of Us, The Mandalorian, Game of Thrones, Narcos, Gladiator II"
        )
    )

    fun getCastForTitle(title: String, genres: List<String> = emptyList()): List<CastMember> {
        val lower = title.lowercase()
        return when {
            lower.contains("wakanda") || lower.contains("black panther 2") -> wakandaCast
            lower.contains("black panther") -> blackPantherCast
            lower.contains("equalizer") -> equalizerCast
            lower.contains("gladiator") -> gladiatorCast
            lower.contains("inception") -> inceptionCast
            else -> defaultCast
        }
    }
}

