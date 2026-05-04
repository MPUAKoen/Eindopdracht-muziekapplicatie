package com.example.demo.config;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {
        repairPieceOrderColumns();

        if (userRepository.count() > 0) {
            return;
        }

        String defaultPassword = passwordEncoder.encode("Test123");
        System.out.println("Seeding realistic demo data...");

        Map<String, String[][]> instrumentPieces = Map.of(
                "Piano", new String[][]{
                        {"Moonlight Sonata", "Beethoven"},
                        {"Nocturne Op.9 No.2", "Chopin"},
                        {"Clair de Lune", "Debussy"},
                        {"Turkish March", "Mozart"},
                        {"Prelude in C Major", "Bach"},
                        {"Für Elise", "Beethoven"},
                        {"Waltz Op.64 No.2", "Chopin"},
                        {"Gymnopédie No.1", "Satie"},
                        {"Rondo Alla Turca", "Mozart"},
                        {"Ballade No.1", "Chopin"}
                },
                "Opera", new String[][]{
                        {"Nessun Dorma", "Puccini"},
                        {"La Donna è Mobile", "Verdi"},
                        {"Vissi d’arte", "Puccini"},
                        {"Casta Diva", "Bellini"},
                        {"Largo al factotum", "Rossini"},
                        {"Che gelida manina", "Puccini"},
                        {"Una furtiva lagrima", "Donizetti"},
                        {"Di Provenza il mar", "Verdi"},
                        {"Addio del passato", "Verdi"},
                        {"Dalla sua pace", "Mozart"}
                },
                "Violin", new String[][]{
                        {"Violin Concerto in D Major", "Tchaikovsky"},
                        {"The Four Seasons: Spring", "Vivaldi"},
                        {"Meditation from Thaïs", "Massenet"},
                        {"Partita No.2 in D minor", "Bach"},
                        {"Zigeunerweisen", "Sarasate"},
                        {"Carmen Fantasy", "Sarasate"},
                        {"Concerto in A minor", "Bach"},
                        {"Air on the G String", "Bach"},
                        {"Introduction and Rondo Capriccioso", "Saint-Saëns"},
                        {"Symphonie Espagnole", "Lalo"}
                },
                "Guitar", new String[][]{
                        {"Asturias", "Albéniz"},
                        {"Recuerdos de la Alhambra", "Tárrega"},
                        {"Concierto de Aranjuez", "Rodrigo"},
                        {"Romance Anónimo", "Trad."},
                        {"Gran Vals", "Tárrega"},
                        {"Capricho Árabe", "Tárrega"},
                        {"La Catedral", "Barrios"},
                        {"Etude No.1", "Villa-Lobos"},
                        {"Lágrima", "Tárrega"},
                        {"Sevilla", "Albéniz"}
                },
                "Drums", new String[][]{
                        {"Funky Drummer Groove", "James Brown"},
                        {"Tom Sawyer", "Rush"},
                        {"Wipe Out", "The Surfaris"},
                        {"Rosanna Shuffle", "Toto"},
                        {"Come Together", "The Beatles"},
                        {"Smells Like Teen Spirit", "Nirvana"},
                        {"Good Times Bad Times", "Led Zeppelin"},
                        {"YYZ", "Rush"},
                        {"Superstition Groove", "Stevie Wonder"},
                        {"Back in Black", "AC/DC"}
                },
                "Harp", new String[][]{
                        {"Clair de Lune", "Debussy"},
                        {"The Swan", "Saint-Saëns"},
                        {"Canon in D", "Pachelbel"},
                        {"Prelude in C", "Bach"},
                        {"First Arabesque", "Debussy"},
                        {"Greensleeves", "Trad."},
                        {"Entr’acte from Carmen", "Bizet"},
                        {"Nocturne", "Chopin"},
                        {"Impromptu-Caprice", "Pierné"},
                        {"The Minstrel’s Adieu to His Native Land", "Thomas"}
                },
                "Flute", new String[][]{
                        {"Sicilienne", "Fauré"},
                        {"Badinerie", "Bach"},
                        {"Dance of the Blessed Spirits", "Gluck"},
                        {"Carnival of Venice", "Genin"},
                        {"Flute Concerto", "Mozart"},
                        {"Syrinx", "Debussy"},
                        {"Tango Etude", "Piazzolla"},
                        {"Fantaisie Brillante", "Borne"},
                        {"Andante in C", "Mozart"},
                        {"Minuet", "Beethoven"}
                },
                "Cello", new String[][]{
                        {"Cello Suite No.1", "Bach"},
                        {"Swan", "Saint-Saëns"},
                        {"Concerto in E minor", "Elgar"},
                        {"Kol Nidrei", "Bruch"},
                        {"Cello Concerto No.1", "Haydn"},
                        {"Meditation", "Massenet"},
                        {"Arpeggione Sonata", "Schubert"},
                        {"Salut d’Amour", "Elgar"},
                        {"Scheherazade", "Rimsky-Korsakov"},
                        {"Requiem", "Fauré"}
                },
                "Clarinet", new String[][]{
                        {"Clarinet Concerto", "Mozart"},
                        {"Rhapsody in Blue", "Gershwin"},
                        {"Première Rhapsodie", "Debussy"},
                        {"Concerto No.1", "Weber"},
                        {"Sonata in E-flat", "Brahms"},
                        {"Solo de Concours", "Messager"},
                        {"Three Pieces", "Stravinsky"},
                        {"Introduction and Rondo", "Saint-Saëns"},
                        {"Concerto for Clarinet", "Copland"},
                        {"Capriccio", "Rossini"}
                }
        );

        Function<String, List<Piece>> generatePiecesForInstrument = instrument -> {
            List<Piece> pieces = new ArrayList<>();
            String[][] source = instrumentPieces.getOrDefault(instrument, instrumentPieces.get("Piano"));

            for (String[] entry : source) {
                Piece piece = new Piece();
                piece.setTitle(entry[0]);
                piece.setComposer(entry[1]);
                piece.setNotes("Study notes for " + entry[0]);
                pieces.add(piece);
            }

            return pieces;
        };

        for (int i = 1; i <= 2; i++) {
            User admin = new User();
            admin.setName("Admin " + i);
            admin.setEmail("admin" + i + "@email.com");
            admin.setPassword(defaultPassword);
            admin.setInstrument("Piano");
            admin.setRole("ADMIN");
            admin.setFavoritePieces(generatePiecesForInstrument.apply("Piano"));
            admin.setWishlist(generatePiecesForInstrument.apply("Piano"));
            admin.setWorkingOnPieces(generatePiecesForInstrument.apply("Piano"));
            admin.setRepertoire(generatePiecesForInstrument.apply("Piano"));
            userRepository.save(admin);
        }

        String[] teacherInstruments = {"Piano", "Opera", "Violin", "Guitar", "Drums", "Harp", "Flute", "Cello", "Clarinet", "Voice"};
        Map<String, User> teachersByInstrument = new HashMap<>();
        for (String instrument : teacherInstruments) {
            User teacher = new User();
            teacher.setName("Teacher " + instrument);
            teacher.setEmail("teacher" + instrument.toLowerCase() + "@email.com");
            teacher.setPassword(defaultPassword);
            teacher.setInstrument(instrument);
            teacher.setRole("TEACHER");
            teacher.setFavoritePieces(generatePiecesForInstrument.apply(instrument));
            teacher.setWishlist(generatePiecesForInstrument.apply(instrument));
            teacher.setWorkingOnPieces(generatePiecesForInstrument.apply(instrument));
            teacher.setRepertoire(generatePiecesForInstrument.apply(instrument));
            userRepository.save(teacher);
            teachersByInstrument.put(instrument.toLowerCase(), teacher);
        }

        String[] studentInstruments = {"Piano", "Opera", "Violin", "Guitar", "Drums", "Harp"};
        for (String instrument : studentInstruments) {
            for (int i = 1; i <= 5; i++) {
                User student = new User();
                student.setName("Student " + instrument + " " + i);
                student.setEmail("student" + instrument.toLowerCase() + i + "@email.com");
                student.setPassword(defaultPassword);
                student.setInstrument(instrument);
                student.setRole("USER");
                student.setFavoritePieces(generatePiecesForInstrument.apply(instrument));
                student.setWishlist(generatePiecesForInstrument.apply(instrument));
                student.setWorkingOnPieces(generatePiecesForInstrument.apply(instrument));
                student.setRepertoire(generatePiecesForInstrument.apply(instrument));

                User teacher = teachersByInstrument.get(instrument.toLowerCase());
                if (teacher != null) {
                    student.setTeacher(teacher);
                }

                userRepository.save(student);
            }
        }

        System.out.println("Database seeded successfully with:");
        System.out.println("  - 2 Admins");
        System.out.println("  - 10 Teachers");
        System.out.println("  - 30 Students (no Voice students, replaced by Harp)");
        System.out.println("  - 10 instrument-specific pieces per category each");
        System.out.println("  - Students linked to matching teachers");
    }

    private void repairPieceOrderColumns() {
        for (String tableName : List.of(
                "user_favorite_piece_links",
                "user_wishlist_piece_links",
                "user_working_piece_links",
                "user_repertoire_piece_links"
        )) {
            compactPieceOrder(tableName);
        }
    }

    private void compactPieceOrder(String tableName) {
        entityManager.createNativeQuery("""
                update %s
                set piece_order = -piece_order - 1
                """.formatted(tableName))
                .executeUpdate();

        entityManager.createNativeQuery("""
                update %s links
                set piece_order = ranked.new_order
                from (
                    select user_id,
                           piece_id,
                           row_number() over (partition by user_id order by piece_order desc) - 1 as new_order
                    from %s
                ) ranked
                where links.user_id = ranked.user_id
                  and links.piece_id = ranked.piece_id
                """.formatted(tableName, tableName))
                .executeUpdate();
    }
}
