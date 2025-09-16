package net.kishax.mc.velocity.auth;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthLevelChecker のユニットテスト
 */
@ExtendWith(MockitoExtension.class)
public class AuthLevelCheckerTest {

  @Mock
  private ProxyServer mockProxyServer;

  @Mock
  private Player mockPlayer;

  @Mock
  private HttpClient mockHttpClient;

  @Mock
  private HttpResponse<String> mockHttpResponse;

  @Mock
  private LuckPerms mockLuckPerms;

  private AuthLevelChecker authLevelChecker;
  private final String testApiUrl = "http://test-api.example.com";
  private final String testApiKey = "test-key-123";

  @BeforeEach
  void setUp() {
    authLevelChecker = new AuthLevelChecker(mockProxyServer, testApiUrl, testApiKey, mockLuckPerms);
  }

  @Test
  void testAuthLevelEnum() {
    // Test enum creation and string conversion
    assertEquals("a", AuthLevel.MC_UNAUTHENTICATED.getCode());
    assertEquals("b", AuthLevel.MC_AUTHENTICATED_TEMP.getCode());
    assertEquals("c", AuthLevel.MC_AUTHENTICATED_UNLINKED.getCode());
    assertEquals("d", AuthLevel.MC_AUTHENTICATED_LINKED.getCode());
    assertEquals("e", AuthLevel.MC_AUTHENTICATED_PRODUCT.getCode());

    // Test fromString conversion
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, AuthLevel.fromString("MC_UNAUTHENTICATED"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_TEMP, AuthLevel.fromString("MC_AUTHENTICATED_TEMP"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_UNLINKED, AuthLevel.fromString("MC_AUTHENTICATED_UNLINKED"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_LINKED, AuthLevel.fromString("MC_AUTHENTICATED_LINKED"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_PRODUCT, AuthLevel.fromString("MC_AUTHENTICATED_PRODUCT"));

    // Test default case
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, AuthLevel.fromString("UNKNOWN"));
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, AuthLevel.fromString(null));
  }

  @Test
  void testGetPlayerAuthLevelDefault() {
    UUID playerUuid = UUID.randomUUID();

    // Should return default for unknown player
    AuthLevel level = authLevelChecker.getPlayerAuthLevel(playerUuid);
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, level);
  }

  @Test
  void testGetPlayerProductsDefault() {
    UUID playerUuid = UUID.randomUUID();

    // Should return empty list for unknown player
    List<String> products = authLevelChecker.getPlayerProducts(playerUuid);
    assertTrue(products.isEmpty());
  }

  @Test
  void testAuthLevelResponseSerialization() {
    // Test AuthLevelResponse class
    AuthLevelChecker.AuthLevelResponse response = new AuthLevelChecker.AuthLevelResponse();
    response.setAuthLevel("MC_AUTHENTICATED_PRODUCT");
    response.setActiveProducts(List.of("Premium Access", "VIP Package"));
    response.setKishaxUserId("user123");

    assertEquals("MC_AUTHENTICATED_PRODUCT", response.getAuthLevel());
    assertEquals(2, response.getActiveProducts().size());
    assertTrue(response.getActiveProducts().contains("Premium Access"));
    assertTrue(response.getActiveProducts().contains("VIP Package"));
    assertEquals("user123", response.getKishaxUserId());
  }

  @Test
  void testAuthLevelDescriptions() {
    assertEquals("MC未認証", AuthLevel.MC_UNAUTHENTICATED.getDescription());
    assertEquals("MC認証クリア（一時権限）", AuthLevel.MC_AUTHENTICATED_TEMP.getDescription());
    assertEquals("MC認証クリア＋Kishaxアカウント未連携", AuthLevel.MC_AUTHENTICATED_UNLINKED.getDescription());
    assertEquals("MC認証クリア＋Kishaxアカウント連携済み", AuthLevel.MC_AUTHENTICATED_LINKED.getDescription());
    assertEquals("MC認証クリア＋プロダクト購入済み", AuthLevel.MC_AUTHENTICATED_PRODUCT.getDescription());
  }

  @Test
  void testFromCodeMethod() {
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, AuthLevel.fromCode("a"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_TEMP, AuthLevel.fromCode("b"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_UNLINKED, AuthLevel.fromCode("c"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_LINKED, AuthLevel.fromCode("d"));
    assertEquals(AuthLevel.MC_AUTHENTICATED_PRODUCT, AuthLevel.fromCode("e"));

    // Test invalid code
    assertThrows(IllegalArgumentException.class, () -> {
      AuthLevel.fromCode("invalid");
    });
  }

  @Test
  void testStopPeriodicCheck() {
    // Test that stopPeriodicCheck doesn't throw exceptions
    assertDoesNotThrow(() -> {
      authLevelChecker.stopPeriodicCheck();
    });
  }

  @Test
  void testPlayerMockSetup() {
    // Test mock player setup for integration testing
    UUID testUuid = UUID.randomUUID();
    when(mockPlayer.getUsername()).thenReturn("TestPlayer");
    when(mockPlayer.getUniqueId()).thenReturn(testUuid);

    assertEquals("TestPlayer", mockPlayer.getUsername());
    assertEquals(testUuid, mockPlayer.getUniqueId());
  }

  @Test
  void testApiUrlAndKeyConfiguration() {
    // Test that constructor accepts configuration
    AuthLevelChecker checker = new AuthLevelChecker(mockProxyServer, "http://api.test.com", "api-key-123",
        mockLuckPerms);
    assertNotNull(checker);

    // Verify basic functionality
    UUID testUuid = UUID.randomUUID();
    assertEquals(AuthLevel.MC_UNAUTHENTICATED, checker.getPlayerAuthLevel(testUuid));
    assertTrue(checker.getPlayerProducts(testUuid).isEmpty());
  }

  @Test
  void testEmptyProductsHandling() {
    // Test with empty active products
    AuthLevelChecker.AuthLevelResponse response = new AuthLevelChecker.AuthLevelResponse();
    response.setAuthLevel("MC_AUTHENTICATED_LINKED");
    response.setActiveProducts(List.of());
    response.setKishaxUserId("user456");

    assertEquals("MC_AUTHENTICATED_LINKED", response.getAuthLevel());
    assertTrue(response.getActiveProducts().isEmpty());
    assertEquals("user456", response.getKishaxUserId());
  }

  @Test
  void testNullHandling() {
    // Test null handling in AuthLevelResponse
    AuthLevelChecker.AuthLevelResponse response = new AuthLevelChecker.AuthLevelResponse();
    response.setAuthLevel(null);
    response.setActiveProducts(null);
    response.setKishaxUserId(null);

    assertNull(response.getAuthLevel());
    assertNull(response.getActiveProducts());
    assertNull(response.getKishaxUserId());
  }
}
