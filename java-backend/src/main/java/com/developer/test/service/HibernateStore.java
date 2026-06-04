package com.developer.test.service;

import com.developer.test.dto.StatsResponse;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.Task;
import com.developer.test.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.datastore.use-file-storage", havingValue = "false", matchIfMissing = true)
public class HibernateStore {
    private final SessionFactory sessionFactory;
    private final PasswordService passwordService;

    public HibernateStore(SessionFactory sessionFactory, PasswordService passwordService) {
        this.sessionFactory = sessionFactory;
        this.passwordService = passwordService;
    }

    @PostConstruct
    public void initializeSchemaAndSeed() {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            if (session.get(User.class, 1) == null) {
                String adminSalt = passwordService.generateSalt();
                session.save(new User(1, "Ishant Sharma", "ishant.sharma1947@gmail.com", "Senior Android Developer",
                        passwordService.hashPassword("admin123", adminSalt), adminSalt));
            }
            if (session.get(User.class, 2) == null) {
                session.save(new User(2, "Vladyslav Viskunov", "vladyslav.viskunov@example.com", "manager"));
            }
            if (session.get(User.class, 3) == null) {
                session.save(new User(3, "Priya Patel", "priya@example.com", "engineer"));
            }

            if (session.get(Task.class, 1) == null) {
                session.save(new Task(1, "Implement authentication", "pending", 1));
            }
            if (session.get(Task.class, 2) == null) {
                session.save(new Task(2, "Design user interface", "in-progress", 2));
            }
            if (session.get(Task.class, 3) == null) {
                session.save(new Task(3, "Review code changes", "completed", 3));
            }

            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
            }
            throw new IllegalStateException("Unable to initialize Hibernate datastore", ex);
        }
    }

    public List<User> getUsers() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from User order by id", User.class).list();
        }
    }

    public User getUserById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, id);
        }
    }

    public User getUserByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from User where lower(email) = :email", User.class)
                    .setParameter("email", email == null ? null : email.toLowerCase())
                    .uniqueResult();
        }
    }

    public User createUser(String name, String email, String role, String passwordHash, String passwordSalt) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = new User(nextUserId(session), name, email, role, passwordHash, passwordSalt);
            session.save(user);
            transaction.commit();
            return user;
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to create user", ex);
        }
    }

    public boolean userExists(int id) {
        return getUserById(id) != null;
    }

    public Task getTaskById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Task.class, id);
        }
    }

    public boolean taskExists(int id) {
        return getTaskById(id) != null;
    }

    public Task createTask(String title, String status, int userId) {
        if (!userExists(userId)) {
            throw new NotFoundException("User with id " + userId + " does not exist");
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Task task = new Task(nextTaskId(session), title, status, userId);
            session.save(task);
            transaction.commit();
            return task;
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to create task", ex);
        }
    }

    public Task updateTask(int id, String title, String status, Integer userId) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Task task = session.get(Task.class, id);
            if (task == null) {
                throw new NotFoundException("Task with id " + id + " was not found");
            }
            if (title != null) {
                task.setTitle(title);
            }
            if (status != null) {
                task.setStatus(status);
            }
            if (userId != null) {
                if (!userExists(userId)) {
                    throw new NotFoundException("User with id " + userId + " does not exist");
                }
                task.setUserId(userId);
            }
            session.update(task);
            transaction.commit();
            return task;
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw ex;
        }
    }

    public List<Task> getTasks(String status, String userId) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Task where 1=1");
            if (StringUtils.hasText(status)) {
                hql.append(" and status = :status");
            }
            if (StringUtils.hasText(userId)) {
                hql.append(" and userId = :userId");
            }
            hql.append(" order by id");

            org.hibernate.query.Query<Task> query = session.createQuery(hql.toString(), Task.class);
            if (StringUtils.hasText(status)) {
                query.setParameter("status", status.trim());
            }
            if (StringUtils.hasText(userId)) {
                query.setParameter("userId", Integer.parseInt(userId.trim()));
            }
            return query.list();
        }
    }

    public StatsResponse getStats() {
        StatsResponse stats = new StatsResponse();
        stats.getUsers().setTotal(countUsers());
        stats.getTasks().setTotal(countTasks());
        for (Task task : getTasks(null, null)) {
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
                default:
                    break;
            }
        }
        return stats;
    }

    public int countUsers() {
        try (Session session = sessionFactory.openSession()) {
            Number result = (Number) session.createQuery("select count(u) from User u").uniqueResult();
            return result == null ? 0 : result.intValue();
        }
    }

    public int countTasks() {
        try (Session session = sessionFactory.openSession()) {
            Number result = (Number) session.createQuery("select count(t) from Task t").uniqueResult();
            return result == null ? 0 : result.intValue();
        }
    }

    private int nextUserId(Session session) {
        Number result = (Number) session.createQuery("select coalesce(max(u.id), 0) from User u").uniqueResult();
        return result == null ? 1 : result.intValue() + 1;
    }

    private int nextTaskId(Session session) {
        Number result = (Number) session.createQuery("select coalesce(max(t.id), 0) from Task t").uniqueResult();
        return result == null ? 1 : result.intValue() + 1;
    }
}
