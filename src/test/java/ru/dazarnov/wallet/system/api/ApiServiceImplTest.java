package ru.dazarnov.wallet.system.api;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import ru.dazarnov.wallet.TestClass;
import ru.dazarnov.wallet.config.ApiConfig;
import ru.dazarnov.wallet.converter.AccountConverter;
import ru.dazarnov.wallet.converter.AccountConverterImpl;
import ru.dazarnov.wallet.converter.OperationConverter;
import ru.dazarnov.wallet.converter.OperationConverterImpl;
import ru.dazarnov.wallet.dao.account.AccountDao;
import ru.dazarnov.wallet.dao.operation.OperationDao;
import ru.dazarnov.wallet.domain.Account;
import ru.dazarnov.wallet.domain.Operation;
import ru.dazarnov.wallet.exception.AccountDaoException;
import ru.dazarnov.wallet.exception.OperationDaoException;
import ru.dazarnov.wallet.rest.serialization.DeserializationJsonMapper;
import ru.dazarnov.wallet.rest.serialization.DeserializationMapper;
import ru.dazarnov.wallet.rest.serialization.SerializationJsonMapper;
import ru.dazarnov.wallet.rest.serialization.SerializationMapper;
import ru.dazarnov.wallet.service.account.AccountService;
import ru.dazarnov.wallet.service.account.AccountServiceImpl;
import ru.dazarnov.wallet.service.operation.OperationService;
import ru.dazarnov.wallet.service.operation.OperationServiceImpl;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.dazarnov.wallet.rest.util.UtilMessage.*;

class ApiServiceImplTest extends TestClass {

    private static ApiService apiService;

    private static AccountDao accountDao = new AccountDao() {

        private final Map<Long, Account> accounts = new HashMap<>();

        @Override
        public void save(Account account) {
            accounts.put(account.getId(), account);
        }

        @Override
        public Optional<Account> findById(long id) {
            return Optional.ofNullable(accounts.get(id));
        }
    };

    private static OperationDao operationDao = new OperationDao() {

        private final Map<Long, Operation> operations = new HashMap<>();

        @Override
        public Optional<Operation> findById(long id) {
            return Optional.ofNullable(operations.get(id));
        }

        @Override
        public void save(Operation operation) {
            operations.put(operation.getId(), operation);
        }
    };

    private HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @BeforeAll
    static void setUp() {
        ApiConfig apiConfig = new ApiConfig();

        SerializationMapper serializationMapper = new SerializationJsonMapper();
        DeserializationMapper deserializationMapper = new DeserializationJsonMapper();

        OperationConverter operationConverter = new OperationConverterImpl();
        AccountConverter accountConverter = new AccountConverterImpl(operationConverter);

        AccountService accountService = new AccountServiceImpl(accountDao, accountConverter);

        OperationService operationService = new OperationServiceImpl(operationDao, accountService, operationConverter);

        apiService = new ApiServiceImpl(apiConfig, serializationMapper, deserializationMapper, operationService, accountService);
        apiService.init();
    }

    @AfterAll
    static void tearDown() {
        apiService.shutdown();
    }

    @Test
    void testShowAccount0() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/account/show/1"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.statusCode());
        assertEquals(NOT_FOUND_MESSAGE, response.body());
    }

    @Test
    void testShowAccount1() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/account/show/foo"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.statusCode());
        assertEquals(ERROR_MESSAGE, response.body());
    }

    @Test
    void testShowAccount2() throws IOException, InterruptedException, JSONException, AccountDaoException {

        Account account = new Account("Oleg", BigDecimal.valueOf(100), Set.of());
        account.setId(1L);
        accountDao.save(account);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/account/show/1"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_OK, response.statusCode());
        JSONAssert.assertEquals(loadFileAsString("show_account_0.json"), response.body(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testShowOperation0() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/show/1"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.statusCode());
        assertEquals(NOT_FOUND_MESSAGE, response.body());
    }

    @Test
    void testShowOperation1() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/show/foo"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.statusCode());
        assertEquals(ERROR_MESSAGE, response.body());
    }

    @Test
    void testShowOperation2() throws IOException, InterruptedException, JSONException, OperationDaoException {

        Account oleg = new Account("Oleg", BigDecimal.valueOf(100), Set.of());
        oleg.setId(1L);

        Account german = new Account("German", BigDecimal.valueOf(100), Set.of());
        german.setId(2L);

        Operation operation = new Operation(BigDecimal.valueOf(100), oleg, german);
        operation.setId(1L);

        operationDao.save(operation);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/show/1"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_OK, response.statusCode());
        JSONAssert.assertEquals(loadFileAsString("show_operation_0.json"), response.body(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testCreateAccount0() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/account/create"))
                .POST(HttpRequest.BodyPublishers.ofString(loadFileAsString("create_account_0.json")))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_CREATED, response.statusCode());
        assertEquals(SUCCESS_MESSAGE, response.body());
    }

    @Test
    void testCreateAccount1() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/account/create"))
                .POST(HttpRequest.BodyPublishers.ofString("foo"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.statusCode());
        assertEquals(ERROR_MESSAGE, response.body());
    }

    @Test
    void testCreateOperation0() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/send"))
                .POST(HttpRequest.BodyPublishers.ofString(loadFileAsString("create_operation_0.json")))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.statusCode());
        assertEquals(ERROR_MESSAGE, response.body());
    }

    @Test
    void testCreateOperation1() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/send"))
                .POST(HttpRequest.BodyPublishers.ofString("foo"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.statusCode());
        assertEquals(ERROR_MESSAGE, response.body());
    }

    @Test
    void testCreateOperation2() throws IOException, InterruptedException, AccountDaoException {

        Account oleg = new Account("Oleg", BigDecimal.valueOf(100), Set.of());
        oleg.setId(1L);

        Account german = new Account("German", BigDecimal.valueOf(100), Set.of());
        german.setId(2L);

        accountDao.save(oleg);
        accountDao.save(german);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/operation/send"))
                .POST(HttpRequest.BodyPublishers.ofString(loadFileAsString("create_operation_0.json")))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpServletResponse.SC_CREATED, response.statusCode());
        assertEquals(SUCCESS_MESSAGE, response.body());
    }
}
