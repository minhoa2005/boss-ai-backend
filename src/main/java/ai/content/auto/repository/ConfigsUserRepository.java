package ai.content.auto.repository;

import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConfigsUserRepository extends JpaRepository<ConfigsUser, Long> {

        List<ConfigsUser> findByUserAndConfigsPrimary(User user, ConfigsPrimary configsPrimary);

        /**
         * Find all user configurations by user and category
         * This joins configs_user with configs_primary to filter by category
         */
        @Query("SELECT cu FROM ConfigsUser cu " +
                        "JOIN cu.configsPrimary cp " +
                        "WHERE cu.user = :user AND cp.category = :category AND cp.active = true " +
                        "ORDER BY cp.sortOrder")
        List<ConfigsUser> findByUserAndCategory(@Param("user") User user, @Param("category") String category);

        /**
         * Find all user configurations by user ID and category
         */
        @Query("SELECT cu FROM ConfigsUser cu " +
                        "JOIN cu.configsPrimary cp " +
                        "WHERE cu.user.id = :userId AND cp.category = :category AND cp.active = true " +
                        "ORDER BY cp.sortOrder")
        List<ConfigsUser> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);

        /**
         * Find all user configurations by category (for admin access)
         * Returns all users' configurations for a specific category
         */
        @Query("SELECT cu FROM ConfigsUser cu " +
                        "JOIN cu.configsPrimary cp " +
                        "WHERE cp.category = :category AND cp.active = true " +
                        "ORDER BY cu.user.username, cp.sortOrder")
        List<ConfigsUser> findAllByCategory(@Param("category") String category);

        public ConfigsUser findConfigsUserByUserAndConfigsPrimary(User user, ConfigsPrimary configsPrimary);
}
