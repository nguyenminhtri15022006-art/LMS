package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.AdminCreateAdminRequest;
import com.lms.identityservice.dto.request.UserRoleUpdateRequest;
import com.lms.identityservice.dto.request.UserUpdatePasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;
import com.lms.identityservice.dto.response.UserSummaryResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.AdminUserService;
import com.lms.identityservice.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    StorageService storageService;

    UserServiceImpl userService;
    AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder,
                refreshTokenRepository,
                storageService);

        adminUserService = new AdminUserServiceImpl(
                userRepository,
                refreshTokenRepository,
                userMapper,
                passwordEncoder);

        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication("user-1", "student@example.com", "ROLE_STUDENT", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyinfoReturnsCurrentAuthenticatedUser() {
        User user = User.builder()
                .userId("user-1")
                .username("student")
                .email("student@example.com")
                .fullname("Student One")
                .role(RoleType.STUDENT)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .username("student")
                .email("student@example.com")
                .build());

        UserFullResponse response = userService.getMyinfo();

        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getUsername()).isEqualTo("student");
        verify(userRepository).findById("user-1");
    }

    @Test
    void updateMyinfoSavesNewProfileFields() {
        User user = User.builder()
                .userId("user-1")
                .fullname("Old Name")
                .avatarUrl("old-avatar.png")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            User target = invocation.getArgument(0);
            UserUpdateRequest request = invocation.getArgument(1);
            target.setFullname(request.getFullname());
            target.setAvatarUrl(request.getAvatarUrl());
            return null;
        }).when(userMapper).updateUser(eq(user), any(UserUpdateRequest.class));
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .fullname("New Name")
                .avatarUrl("new-avatar.png")
                .build());

        UserFullResponse response = userService.updateInfo(UserUpdateRequest.builder()
                .fullname("New Name")
                .avatarUrl("new-avatar.png")
                .discardedAvatarUrls(List.of("discarded.png", "new-avatar.png", "discarded.png"))
                .build());

        assertThat(response.getFullname()).isEqualTo("New Name");
        verify(storageService).deleteFile("old-avatar.png");
        verify(storageService).deleteAvatarFileForUser("discarded.png", "user-1");
        verify(storageService, never()).deleteAvatarFileForUser("new-avatar.png", "user-1");
        verify(userRepository).save(user);
    }

    @Test
    void updatePasswordSuccessWithCorrectOldPassword() {
        User user = User.builder()
                .userId("user-1")
                .password("old-hash")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword1", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .build());

        UserFullResponse response = userService.updatePassword(UserUpdatePasswordRequest.builder()
                .oldPassword("oldPassword1")
                .newPassword("newPassword1")
                .confirmPassword("newPassword1")
                .build());

        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId("user-1");
    }

    @Test
    void updatePasswordThrowsWhenOldPasswordWrong() {
        User user = User.builder()
                .userId("user-1")
                .password("old-hash")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "old-hash")).thenReturn(false);

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> userService.updatePassword(UserUpdatePasswordRequest.builder()
                        .oldPassword("wrongOldPassword")
                        .newPassword("newPassword1")
                        .confirmPassword("newPassword1")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_INCORRECT));

        verify(passwordEncoder, never()).encode(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void updateRoleChangesUserRole() {
        User user = User.builder()
                .userId("user-1")
                .role(RoleType.STUDENT)
                .roleSelected(false)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .role("INSTRUCTOR")
                .roleSelected(true)
                .build());

        UserFullResponse response = userService.updateRole(UserRoleUpdateRequest.builder()
                .role("INSTRUCTOR")
                .build());

        assertThat(user.getRole()).isEqualTo(RoleType.INSTRUCTOR);
        assertThat(user.getRoleSelected()).isTrue();
        assertThat(response.getRole()).isEqualTo("INSTRUCTOR");
        verify(userRepository).save(user);
    }

    @Test
    void updateRoleUserCannotEscalateToAdminThrowsForbidden() {
        User user = User.builder()
                .userId("user-1")
                .role(RoleType.STUDENT)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> userService.updateRole(UserRoleUpdateRequest.builder()
                        .role("ROLE_ADMIN")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        verify(userRepository, never()).save(any());
    }

    @Test
    void adminGetUsersPaginationAndFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder()
                .userId("user-2")
                .username("student02")
                .email("student02@example.com")
                .role(RoleType.STUDENT)
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findUsersByFilters(eq(RoleType.STUDENT), eq("student02"), eq(pageable)))
                .thenReturn(userPage);
        when(userMapper.toUserSummaryResponse(user)).thenReturn(UserSummaryResponse.builder()
                .userId("user-2")
                .username("student02")
                .build());

        Page<UserSummaryResponse> result = adminUserService.getAllUser(pageable, "STUDENT", "student02");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("student02");
    }

    @Test
    void adminToggleActiveUpdatesStatus() {
        User user = User.builder()
                .userId("user-2")
                .role(RoleType.STUDENT)
                .active(true)
                .build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-2")
                .active(false)
                .build());

        UserFullResponse response = adminUserService.toggleActive("user-2");

        assertThat(user.getActive()).isFalse();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId("user-2");
    }

    @Test
    void getPublicProfileReturnsPublicDataAndHidesPrivateFields() {
        User user = User.builder()
                .userId("user-99")
                .username("john_doe")
                .password("secret_hashed_password")
                .email("john@example.com")
                .phone("0987654321")
                .fullname("John Doe")
                .avatarUrl("https://storage.example.com/avatar.png")
                .bio("Software Engineer & Teacher")
                .role(RoleType.INSTRUCTOR)
                .active(true)
                .verified(true)
                .build();

        when(userRepository.findById("user-99")).thenReturn(Optional.of(user));

        UserPublicProfileResponse response = userService.getPublicProfile("user-99");

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-99");
        assertThat(response.getFullname()).isEqualTo("John Doe");
        assertThat(response.getAvatarUrl()).isEqualTo("https://storage.example.com/avatar.png");
        assertThat(response.getBio()).isEqualTo("Software Engineer & Teacher");
        assertThat(response.getRole()).isEqualTo("INSTRUCTOR");

        // Verify class UserPublicProfileResponse does not expose sensitive fields (password, email, phone, etc.)
        assertThat(UserPublicProfileResponse.class.getDeclaredFields())
                .extracting("name")
                .containsExactlyInAnyOrder("userId", "fullname", "avatarUrl", "bio", "role");
    }

    @Test
    void getPublicProfileThrowsWhenUserNotFound() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> userService.getPublicProfile("non-existent"))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void createAdminSuccess() {
        AdminCreateAdminRequest request = AdminCreateAdminRequest.builder()
                .username("newadmin")
                .password("Admin@123456")
                .fullname("New Admin")
                .email("newadmin@example.com")
                .phone("0912345678")
                .build();

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userRepository.existsByEmail("newadmin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@123456")).thenReturn("encoded_admin_pass");
        when(userMapper.toUserFullResponse(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return UserFullResponse.builder()
                    .userId(saved.getUserId())
                    .username(saved.getUsername())
                    .email(saved.getEmail())
                    .fullname(saved.getFullname())
                    .role(saved.getRole() != null ? saved.getRole().name() : null)
                    .active(saved.getActive())
                    .verified(saved.getVerified())
                    .build();
        });

        UserFullResponse response = adminUserService.createAdmin(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newadmin");
        assertThat(response.getEmail()).isEqualTo("newadmin@example.com");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getVerified()).isTrue();

        verify(userRepository).save(argThat(u ->
                "newadmin".equals(u.getUsername()) &&
                "encoded_admin_pass".equals(u.getPassword()) &&
                RoleType.ADMIN.equals(u.getRole()) &&
                Boolean.TRUE.equals(u.getActive()) &&
                Boolean.TRUE.equals(u.getVerified()) &&
                Boolean.TRUE.equals(u.getRoleSelected()) &&
                AuthProvider.LOCAL.equals(u.getAuthProvider())
        ));
    }

    @Test
    void createAdminThrowsWhenUsernameExists() {
        AdminCreateAdminRequest request = AdminCreateAdminRequest.builder()
                .username("existing_admin")
                .email("admin@example.com")
                .build();

        when(userRepository.existsByUsername("existing_admin")).thenReturn(true);

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> adminUserService.createAdmin(request))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_EXISTED));

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdminThrowsWhenEmailExists() {
        AdminCreateAdminRequest request = AdminCreateAdminRequest.builder()
                .username("newadmin")
                .email("existing@example.com")
                .build();

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> adminUserService.createAdmin(request))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_EXISTED));

        verify(userRepository, never()).save(any());
    }
}

