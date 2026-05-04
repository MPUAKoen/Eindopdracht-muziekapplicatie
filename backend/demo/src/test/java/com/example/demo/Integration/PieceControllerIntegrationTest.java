package com.example.demo.Integration;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PieceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;

    @BeforeEach
    void setup() {
        email = "piece_" + UUID.randomUUID() + "@mail.com";

        User user = new User();
        user.setName("Piece Test User");
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole("USER");
        user.setInstrument("Piano");
        userRepository.save(user);
    }

    @Test
    void createAndDeletePiece_UsesIdentifierBasedFlow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/pieces")
                        .with(user(email).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "Arabesque No. 1",
                              "composer": "Debussy",
                              "notes": "Start under tempo",
                              "category": "working-on-pieces"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.category").value("working-on-pieces"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdPiece = objectMapper.readTree(createResponse);
        String pieceId = createdPiece.get("id").asText();

        mockMvc.perform(get("/api/pieces/" + pieceId)
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pieceId))
                .andExpect(jsonPath("$.title").value("Arabesque No. 1"));

        mockMvc.perform(delete("/api/pieces/" + pieceId)
                        .with(user(email).roles("USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pieces/" + pieceId)
                        .with(user(email).roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSeedLikePiece_WithRealJwtAuthentication_Succeeds() throws Exception {
        String jwtEmail = "seed_like_" + UUID.randomUUID() + "@mail.com";

        User user = new User();
        user.setName("Seed Like User");
        user.setEmail(jwtEmail);
        user.setPassword(passwordEncoder.encode("Test123"));
        user.setRole("USER");
        user.setInstrument("Opera");

        Piece piece = new Piece();
        piece.setTitle("Nessun Dorma");
        piece.setComposer("Puccini");
        piece.setNotes("Study notes for Nessun Dorma");
        user.getFavoritePieces().add(piece);
        userRepository.saveAndFlush(user);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "password": "Test123"
                            }
                            """.formatted(jwtEmail)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();
        Long pieceId = userRepository.findByEmail(jwtEmail)
                .orElseThrow()
                .getFavoritePieces()
                .get(0)
                .getId();

        mockMvc.perform(delete("/api/pieces/" + pieceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pieces?category=favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteFirstFavoritePiece_WhenMultiplePiecesExist_DoesNotReorderIntoDuplicateKey() throws Exception {
        Piece firstPiece = new Piece();
        firstPiece.setTitle("First Favorite");
        firstPiece.setComposer("Composer One");

        Piece secondPiece = new Piece();
        secondPiece.setTitle("Second Favorite");
        secondPiece.setComposer("Composer Two");

        User user = userRepository.findByEmail(email).orElseThrow();
        user.getFavoritePieces().add(firstPiece);
        user.getFavoritePieces().add(secondPiece);
        userRepository.saveAndFlush(user);

        Long firstPieceId = userRepository.findByEmail(email)
                .orElseThrow()
                .getFavoritePieces()
                .get(0)
                .getId();

        mockMvc.perform(delete("/api/pieces/" + firstPieceId)
                        .with(user(email).roles("USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pieces?category=favorite")
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Second Favorite"));
    }
}
