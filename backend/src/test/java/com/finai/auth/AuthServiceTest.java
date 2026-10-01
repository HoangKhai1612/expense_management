package com.finai.auth;

import com.finai.common.ApiException;
import com.finai.security.JwtService;
import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.User;
import com.finai.user.UserRepository;
import com.finai.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final Long USER_ROLE_ID = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        Role role = new Role(Role.USER, "Standard user");
        ReflectionTestUtils.setField(role, "id", USER_ROLE_ID);
        when(roleRepository.findById(USER_ROLE_ID)).thenReturn(Optional.of(role));
        when(roleRepository.findByName(Role.USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.issueToken(any(), anyString(), anyString())).thenReturn("token-abc");
        when(jwtService.getExpiresInSeconds()).thenReturn(43200L);
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("New.User@Example.COM ", " New.User ", "Password1", "  ", "+84 123 456");
    }

    @Test
    @DisplayName("registration normalises the email and username before storing them")
    void normalisesIdentifiersOnRegister() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        service.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new.user@example.com");
        assertThat(captor.getValue().getUsername()).isEqualTo("new.user");
    }

    @Test
    @DisplayName("registration stores a hash and never the plaintext password")
    void neverStoresPlaintextPassword() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed")
                .isNotEqualTo("Password1");
        verify(passwordEncoder).encode("Password1");
    }

    @Test
    @DisplayName("a blank full name is stored as null rather than empty whitespace")
    void blankFullNameBecomesNull() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isNull();
    }

    @Test
    @DisplayName("a duplicate email is a 409 and no account is written")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("new.user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("EMAIL_ALREADY_EXISTS"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("a duplicate username is a 409 and no account is written")
    void rejectsDuplicateUsername() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("new.user")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("USERNAME_ALREADY_EXISTS"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login accepts either the email or the username")
    void logsInWithEitherIdentifier() {
        User user = userFixture(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByUsernameIgnoreCase("new.user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("Password1"), anyString())).thenReturn(true);

        assertThat(service.login(new LoginRequest("New.User@Example.com ", "Password1")).accessToken())
                .isEqualTo("token-abc");
        assertThat(service.login(new LoginRequest(" New.User ", "Password1")).accessToken())
                .isEqualTo("token-abc");
    }

    @Test
    @DisplayName("a wrong password is rejected as invalid credentials")
    void rejectsWrongPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(userFixture(UserStatus.ACTIVE)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("new.user@example.com", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_CREDENTIALS"));
    }

    /**
     * A missing account must be indistinguishable from a wrong password, so the
     * service still runs a hash comparison before rejecting.
     */
    @Test
    @DisplayName("an unknown account is rejected as invalid credentials, not as not found")
    void unknownAccountIsIndistinguishableFromWrongPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "whatever")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_CREDENTIALS"));
        verify(passwordEncoder).matches(eq("whatever"), anyString());
    }

    @Test
    @DisplayName("a locked account is refused with 403 ACCOUNT_LOCKED")
    void refusesLockedAccount() {
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(userFixture(UserStatus.LOCKED)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("new.user@example.com", "Password1")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getCode()).isEqualTo("ACCOUNT_LOCKED");
                    assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403);
                });
    }

    @Test
    @DisplayName("a deactivated account is refused with 403 ACCOUNT_DEACTIVATED")
    void refusesDeactivatedAccount() {
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(userFixture(UserStatus.DEACTIVATED)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("new.user@example.com", "Password1")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCOUNT_DEACTIVATED"));
    }

    @Test
    @DisplayName("a successful login records the login timestamp")
    void recordsLoginTimestamp() {
        User user = userFixture(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThat(user.getLastLoginAt()).isNull();
        service.login(new LoginRequest("new.user@example.com", "Password1"));
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    private User userFixture(UserStatus status) {
        User user = new User("new.user@example.com", "new.user", "hashed", "New User", null, USER_ROLE_ID);
        user.changeStatus(status);
        ReflectionTestUtils.setField(user, "id", 5L);
        return user;
    }
}
