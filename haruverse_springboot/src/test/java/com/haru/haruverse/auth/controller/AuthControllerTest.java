package com.haru.haruverse.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.auth.dto.LoginRequest;
import com.haru.haruverse.auth.dto.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AuthController '통합' 테스트 — 전체 컨텍스트(H2·JPA·Security·JWT)를 띄우고
// 회원가입→로그인→보호 API 접근까지 실제 HTTP 흐름으로 관통 검증.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // ---------- 회원가입 ----------

    @Test
    @DisplayName("POST /api/auth/signup: 새 이메일이면 200 OK")
    void signup_success() throws Exception {
        SignupRequest req = new SignupRequest("ctrl@haru.com", "pw1234", "컨트롤러");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/signup: 이메일 중복이면 409 + 에러 메시지")
    void signup_duplicate() throws Exception {
        SignupRequest req = new SignupRequest("dup2@haru.com", "pw1234", "중복");
        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    // ---------- 로그인 (JWT) ----------

    @Test
    @DisplayName("POST /api/auth/login: 올바른 자격이면 200 + JWT 토큰 반환")
    void login_success() throws Exception {
        signup("login@haru.com", "pw1234", "로그인");

        LoginRequest login = new LoginRequest("login@haru.com", "pw1234");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@haru.com"))
                .andExpect(jsonPath("$.nickname").value("로그인"));
    }

    @Test
    @DisplayName("POST /api/auth/login: 비밀번호가 틀리면 401")
    void login_wrong_password() throws Exception {
        signup("wrong@haru.com", "correct-pw", "틀림");

        LoginRequest login = new LoginRequest("wrong@haru.com", "wrong-pw");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    // ---------- 보호 엔드포인트 (/api/members/me) ----------

    @Test
    @DisplayName("GET /api/members/me: 유효한 토큰이면 200 + 내 정보")
    void me_with_valid_token() throws Exception {
        signup("me@haru.com", "pw1234", "미");
        String token = loginAndGetToken("me@haru.com", "pw1234");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@haru.com"))
                .andExpect(jsonPath("$.nickname").value("미"));
    }

    @Test
    @DisplayName("GET /api/members/me: 토큰이 없으면 401")
    void me_without_token() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 헬퍼 ----------

    private void signup(String email, String password, String nickname) throws Exception {
        SignupRequest req = new SignupRequest(email, password, nickname);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
