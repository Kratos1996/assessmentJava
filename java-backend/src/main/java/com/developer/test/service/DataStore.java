package com.developer.test.service;

import com.developer.test.exception.NotFoundException;
import com.developer.test.model.Task;
import com.developer.test.model.User;
import com.developer.test.dto.StatsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Stores users and tasks through Hibernate/MySQL by default and falls back to a JSON snapshot when requested.
 * It also owns ID generation, seeded data, and snapshot loading or saving.
 * Keeping that work here makes the rest of the app easier to understand.
 */
@Service
public class DataStore {
    private static final Logger log = LoggerFactory.getLogger(DataStore.class);
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger nextUserId = new AtomicInteger(1);
    private final AtomicInteger nextTaskId = new AtomicInteger(1);
    private final ObjectMapper objectMapper;
    private final Path storageFile;
    private final boolean useFileStorage;
    private boolean mysqlMode;
    private final HibernateStore hibernateStore;
    private final PasswordService passwordService;
    private final String mysqlUrl;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final String mysqlDriverClassName;

    public DataStore(ObjectMapper objectMapper,
                     String storagePath) {
        this.objectMapper = objectMapper;
        this.storageFile = Paths.get(storagePath);
        this.useFileStorage = true;
        this.mysqlMode = false;
        this.hibernateStore = null;
        this.passwordService = new PasswordService();
        this.mysqlUrl = "";
        this.mysqlUsername = "";
        this.mysqlPassword = "";
        this.mysqlDriverClassName = MYSQL_DRIVER;
        initialize();
    }

    @Autowired
    public DataStore(ObjectMapper objectMapper,
                     @Value("${app.datastore.file:target/datastore.json}") String storagePath,
                     @Value("${app.datastore.use-file-storage:false}") boolean useFileStorage,
                     ObjectProvider<HibernateStore> hibernateStoreProvider,
                     PasswordService passwordService,
                     @Value("${app.mysql.url:}") String mysqlUrl,
                     @Value("${app.mysql.username:}") String mysqlUsername,
                     @Value("${app.mysql.password:}") String mysqlPassword,
                     @Value("${app.mysql.driver-class-name:com.mysql.cj.jdbc.Driver}") String mysqlDriverClassName) {
        this.objectMapper = objectMapper;
        this.storageFile = Paths.get(storagePath);
        this.useFileStorage = useFileStorage;
        this.mysqlMode = !useFileStorage && StringUtils.hasText(mysqlUrl);
        this.hibernateStore = hibernateStoreProvider.getIfAvailable();
        this.passwordService = passwordService;
        this.mysqlUrl = mysqlUrl;
        this.mysqlUsername = mysqlUsername;
        this.mysqlPassword = mysqlPassword;
        this.mysqlDriverClassName = StringUtils.hasText(mysqlDriverClassName) ? mysqlDriverClassName : MYSQL_DRIVER;
        initialize();
    }

    public List<User> getUsers() {
        if (mysqlMode) {
            return hibernateStore.getUsers();
        }

        return users.values().stream()
                .sorted((left, right) -> Integer.compare(left.getId(), right.getId()))
                .collect(Collectors.toList());
    }

    public User getUserById(int id) {
        if (mysqlMode) {
            return hibernateStore.getUserById(id);
        }

        return users.get(id);
    }

    public Task getTaskById(int id) {
        if (mysqlMode) {
            return hibernateStore.getTaskById(id);
        }

        return tasks.get(id);
    }

    public boolean userExists(int id) {
        if (mysqlMode) {
            return hibernateStore.getUserById(id) != null;
        }

        return users.containsKey(id);
    }

    public boolean taskExists(int id) {
        if (mysqlMode) {
            return hibernateStore.getTaskById(id) != null;
        }

        return tasks.containsKey(id);
    }

    public synchronized User createUser(String name, String email, String role) {
        if (mysqlMode) {
            return hibernateStore.createUser(name, email, role, null, null);
        }

        // We assign IDs atomically so each new record gets a stable unique number.
        int id = nextUserId.getAndIncrement();
        User user = new User(id, name, email, role);
        users.put(id, user);
        persistSnapshot();
        return user;
    }

    public synchronized User createUser(String name, String email, String role, String passwordHash, String passwordSalt) {
        if (mysqlMode) {
            return hibernateStore.createUser(name, email, role, passwordHash, passwordSalt);
        }

        int id = nextUserId.getAndIncrement();
        User user = new User(id, name, email, role, passwordHash, passwordSalt);
        users.put(id, user);
        persistSnapshot();
        return user;
    }

    public User getUserByEmail(String email) {
        if (mysqlMode) {
            return hibernateStore.getUserByEmail(email);
        }

        return users.values().stream()
                .filter(user -> user.getEmail() != null && user.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public synchronized Task createTask(String title, String status, int userId) {
        if (mysqlMode) {
            return hibernateStore.createTask(title, status, userId);
        }

        // Task creation stays here so the in-memory store remains the single source of truth.
        if (!userExists(userId)) {
            throw new NotFoundException("User with id " + userId + " does not exist");
        }

        int id = nextTaskId.getAndIncrement();
        Task task = new Task(id, title, status, userId);
        tasks.put(id, task);
        persistSnapshot();
        return task;
    }

    public synchronized Task updateTask(int id, String title, String status, Integer userId) {
        if (mysqlMode) {
            return hibernateStore.updateTask(id, title, status, userId);
        }

        Task existing = tasks.get(id);
        if (existing == null) {
            throw new NotFoundException("Task with id " + id + " was not found");
        }

        // Partial updates only change the fields the caller actually sent.
        if (title != null) {
            existing.setTitle(title);
        }
        if (status != null) {
            existing.setStatus(status);
        }
        if (userId != null) {
            if (!userExists(userId)) {
                throw new NotFoundException("User with id " + userId + " does not exist");
            }
            existing.setUserId(userId);
        }

        tasks.put(id, existing);
        persistSnapshot();
        return existing;
    }

    public List<Task> getTasks(String status, String userId) {
        if (mysqlMode) {
            return hibernateStore.getTasks(status, userId);
        }

        String normalizedStatus = status == null ? null : status.trim();
        String normalizedUserId = userId == null ? null : userId.trim();

        return tasks.values().stream()
                .sorted((left, right) -> Integer.compare(left.getId(), right.getId()))
                .filter(task -> {
                    boolean matchStatus = normalizedStatus == null || normalizedStatus.isEmpty() || task.getStatus().equals(normalizedStatus);
                    boolean matchUserId = normalizedUserId == null || normalizedUserId.isEmpty() ||
                            task.getUserId() == Integer.parseInt(normalizedUserId);
                    return matchStatus && matchUserId;
                })
                .collect(Collectors.toList());
    }

    public StatsResponse getStats() {
        if (mysqlMode) {
            return hibernateStore.getStats();
        }

        StatsResponse stats = new StatsResponse();
        stats.getUsers().setTotal(users.size());
        stats.getTasks().setTotal(tasks.size());
        
        for (Task task : tasks.values()) {
            switch (task.getStatus()) {
                case "pending":
                    stats.getTasks().setPending(stats.getTasks().getPending() + 1);
                    break;
                case "in-progress":
                    stats.getTasks().setInProgress(stats.getTasks().getInProgress() + 1);
                    break;
                case "completed":
                    stats.getTasks().setCompleted(stats.getTasks().getCompleted() + 1);
                    break;
            }
        }
        
        return stats;
    }

    public int getUserCount() {
        if (mysqlMode) {
            return hibernateStore.countUsers();
        }

        return users.size();
    }

    public int getTaskCount() {
        if (mysqlMode) {
            return hibernateStore.countTasks();
        }

        return tasks.size();
    }

    public String getStorageFilePath() {
        return storageFile.toString();
    }

    public boolean isMysqlMode() {
        return mysqlMode;
    }

    public String getStorageSourceLabel() {
        if (mysqlMode) {
            return "MySQL database";
        }
        return "File snapshot";
    }

    public String getStorageSourceDetail() {
        if (mysqlMode) {
            return StringUtils.hasText(mysqlUrl) ? "Connected to " + mysqlUrl : "Connected to MySQL";
        }
        return "Persistence file: " + storageFile;
    }

    public boolean isPersistenceReady() {
        if (mysqlMode) {
            return StringUtils.hasText(mysqlUrl);
        }

        Path parent = storageFile.getParent();
        return Files.isReadable(storageFile) || (parent != null && Files.isWritable(parent));
    }

    private void initialize() {
        if (useFileStorage) {
            initializeFileMode();
            return;
        }

        initializeMysql();
        if (mysqlMode) {
            return;
        }

        initializeFileMode();
    }

    private void initializeFileMode() {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(storageFile)) {
                loadSnapshot();
                return;
            }
        } catch (IOException ex) {
            log.warn("Falling back to seeded data because persistence initialization failed for {}", storageFile, ex);
        }

        seedDefaults();
        persistSnapshot();
    }

    private void initializeMysql() {
        if (hibernateStore == null) {
            log.warn("SQL mode was requested but no MySQL URL was configured. Falling back to file-backed storage.");
            mysqlMode = false;
            return;
        }

        try {
            hibernateStore.initializeSchemaAndSeed();
            log.info("Using MySQL-backed datastore at {}", mysqlUrl);
        } catch (Exception ex) {
            log.warn("Unable to initialize MySQL datastore for {}. Falling back to file-backed storage.", mysqlUrl, ex);
            mysqlMode = false;
        }
    }

    private void seedDefaults() {
        // Starter data gives the app something useful on first boot and keeps health checks meaningful.
        users.clear();
        tasks.clear();

        String salt = passwordService.generateSalt();
        users.put(1, new User(1, "Ishant Sharma", "ishant.sharma1947@gmail.com", "Senior Android Developer", passwordService.hashPassword("admin123", salt), salt));
        users.put(2, new User(2, "Vladyslav Viskunov", "vladyslav.viskunov@example.com", "manager"));
        users.put(3, new User(3, "Priya Patel", "priya@example.com", "engineer"));

        tasks.put(1, new Task(1, "Implement authentication", "pending", 1));
        tasks.put(2, new Task(2, "Design user interface", "in-progress", 2));
        tasks.put(3, new Task(3, "Review code changes", "completed", 3));

        nextUserId.set(4);
        nextTaskId.set(4);
    }

    private void seedDefaults(Connection connection) throws SQLException {}

    private void loadSnapshot() throws IOException {
        DataSnapshot snapshot = objectMapper.readValue(storageFile.toFile(), DataSnapshot.class);
        users.clear();
        tasks.clear();

        if (snapshot.getUsers() != null) {
            for (User user : snapshot.getUsers()) {
                users.put(user.getId(), user);
            }
        }
        if (snapshot.getTasks() != null) {
            for (Task task : snapshot.getTasks()) {
                tasks.put(task.getId(), task);
            }
        }

        nextUserId.set(snapshot.getNextUserId() > 0 ? snapshot.getNextUserId() : computeNextUserId());
        nextTaskId.set(snapshot.getNextTaskId() > 0 ? snapshot.getNextTaskId() : computeNextTaskId());
        ensureSeedAdminCredentials();
        log.info("Loaded persistent datastore from {}", storageFile);
    }

    private synchronized void persistSnapshot() {
        if (mysqlMode) {
            return;
        }

        try {
            DataSnapshot snapshot = new DataSnapshot();
            snapshot.setUsers(getUsers());
            snapshot.setTasks(getTasks(null, null));
            snapshot.setNextUserId(nextUserId.get());
            snapshot.setNextTaskId(nextTaskId.get());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), snapshot);
        } catch (IOException ex) {
            log.error("Unable to persist datastore snapshot to {}", storageFile, ex);
        }
    }

    private int computeNextUserId() {
        return users.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private int computeNextTaskId() {
        return tasks.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private void ensureSeedAdminCredentials() {
        User admin = getUserByEmail("ishant.sharma1947@gmail.com");
        if (admin == null) {
            String salt = passwordService.generateSalt();
            users.put(1, new User(1, "Ishant Sharma", "ishant.sharma1947@gmail.com", "Senior Android Developer",
                    passwordService.hashPassword("admin123", salt), salt));
            persistSnapshot();
            return;
        }

        if (!StringUtils.hasText(admin.getPasswordHash())) {
            String salt = passwordService.generateSalt();
            admin.setPasswordSalt(salt);
            admin.setPasswordHash(passwordService.hashPassword("admin123", salt));
            persistSnapshot();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);
    }

    private void ensureMysqlSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "name VARCHAR(255) NOT NULL, " +
                            "email VARCHAR(255) NOT NULL, " +
                            "role VARCHAR(255) NOT NULL" +
                            ")");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tasks (" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "title VARCHAR(255) NOT NULL, " +
                            "status VARCHAR(50) NOT NULL, " +
                            "user_id INT NOT NULL" +
                            ")");
        }
    }

    private boolean isTableEmpty(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (resultSet.next()) {
                return resultSet.getInt(1) == 0;
            }
            return true;
        }
    }

    private List<User> loadUsersFromMysql() {
        List<User> loadedUsers = new ArrayList<>();
        String sql = "SELECT id, name, email, role FROM users ORDER BY id";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loadedUsers.add(mapUser(resultSet));
            }
            return loadedUsers;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load users from MySQL", ex);
        }
    }

    private User loadUserByIdFromMysql(int id) {
        String sql = "SELECT id, name, email, role FROM users WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load user from MySQL", ex);
        }
    }

    private Task loadTaskByIdFromMysql(int id) {
        String sql = "SELECT id, title, status, user_id FROM tasks WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapTask(resultSet) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load task from MySQL", ex);
        }
    }

    private List<Task> loadTasksFromMysql(String status, String userId) {
        String normalizedStatus = status == null ? null : status.trim();
        String normalizedUserId = userId == null ? null : userId.trim();
        StringBuilder sql = new StringBuilder("SELECT id, title, status, user_id FROM tasks");
        List<Object> parameters = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(normalizedStatus)) {
            conditions.add("status = ?");
            parameters.add(normalizedStatus);
        }
        if (StringUtils.hasText(normalizedUserId)) {
            conditions.add("user_id = ?");
            parameters.add(Integer.parseInt(normalizedUserId));
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY id");

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < parameters.size(); index++) {
                Object parameter = parameters.get(index);
                if (parameter instanceof Integer) {
                    statement.setInt(index + 1, (Integer) parameter);
                } else {
                    statement.setString(index + 1, String.valueOf(parameter));
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Task> loadedTasks = new ArrayList<>();
                while (resultSet.next()) {
                    loadedTasks.add(mapTask(resultSet));
                }
                return loadedTasks;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load tasks from MySQL", ex);
        }
    }

    private User createUserInMysql(String name, String email, String role) {
        String sql = "INSERT INTO users (name, email, role) VALUES (?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, role);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                int id = generatedKeys.next() ? generatedKeys.getInt(1) : loadLastInsertedId(connection, "users");
                return new User(id, name, email, role);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create user in MySQL", ex);
        }
    }

    private Task createTaskInMysql(String title, String status, int userId) {
        if (!userExists(userId)) {
            throw new NotFoundException("User with id " + userId + " does not exist");
        }

        String sql = "INSERT INTO tasks (title, status, user_id) VALUES (?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, status);
            statement.setInt(3, userId);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                int id = generatedKeys.next() ? generatedKeys.getInt(1) : loadLastInsertedId(connection, "tasks");
                return new Task(id, title, status, userId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create task in MySQL", ex);
        }
    }

    private Task updateTaskInMysql(int id, String title, String status, Integer userId) {
        Task existing = loadTaskByIdFromMysql(id);
        if (existing == null) {
            throw new NotFoundException("Task with id " + id + " was not found");
        }

        String resolvedTitle = title != null ? title : existing.getTitle();
        String resolvedStatus = status != null ? status : existing.getStatus();
        int resolvedUserId = userId != null ? userId : existing.getUserId();
        if (userId != null && !userExists(userId)) {
            throw new NotFoundException("User with id " + userId + " does not exist");
        }

        String sql = "UPDATE tasks SET title = ?, status = ?, user_id = ? WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resolvedTitle);
            statement.setString(2, resolvedStatus);
            statement.setInt(3, resolvedUserId);
            statement.setInt(4, id);
            statement.executeUpdate();
            return new Task(id, resolvedTitle, resolvedStatus, resolvedUserId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update task in MySQL", ex);
        }
    }

    private StatsResponse loadStatsFromMysql() {
        StatsResponse stats = new StatsResponse();
        stats.getUsers().setTotal(countRowsFromMysql("users"));
        stats.getTasks().setTotal(countRowsFromMysql("tasks"));

        String sql = "SELECT status, COUNT(*) AS total FROM tasks GROUP BY status";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                int total = resultSet.getInt("total");
                switch (status) {
                    case "pending":
                        stats.getTasks().setPending(total);
                        break;
                    case "in-progress":
                        stats.getTasks().setInProgress(total);
                        break;
                    case "completed":
                        stats.getTasks().setCompleted(total);
                        break;
                    default:
                        break;
                }
            }
            return stats;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to compute MySQL stats", ex);
        }
    }

    private int countRowsFromMysql(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to count rows in MySQL table " + tableName, ex);
        }
    }

    private int loadLastInsertedId(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT MAX(id) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("role")
        );
    }

    private Task mapTask(ResultSet resultSet) throws SQLException {
        return new Task(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("status"),
                resultSet.getInt("user_id")
        );
    }

    /**
     * Stores a snapshot of the current users and tasks on disk.
     * The file is plain JSON, so it can be loaded again on the next start.
     * This keeps the demo state stable without needing a database.
     */
    public static class DataSnapshot {
        private List<User> users = new ArrayList<>();
        private List<Task> tasks = new ArrayList<>();
        private int nextUserId;
        private int nextTaskId;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }

        public List<Task> getTasks() {
            return tasks;
        }

        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
        }

        public int getNextUserId() {
            return nextUserId;
        }

        public void setNextUserId(int nextUserId) {
            this.nextUserId = nextUserId;
        }

        public int getNextTaskId() {
            return nextTaskId;
        }

        public void setNextTaskId(int nextTaskId) {
            this.nextTaskId = nextTaskId;
        }
    }
}
