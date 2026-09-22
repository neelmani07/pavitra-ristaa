package com.pavitraristaa.dbmigration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Applies the Flyway migrations to the database and exits. A non-zero exit code means the migration failed, so a
 * deployment can stop before it starts the API.
 *
 * <p>Configuration comes from the environment: {@code DB_URL}, {@code DB_USERNAME}, {@code DB_PASSWORD}. Local
 * defaults apply only when {@code SPRING_PROFILES_ACTIVE} is unset or {@code local}; in any other environment a
 * missing variable fails the job instead of migrating the wrong database.
 */
public final class DbMigrationApplication {

    private static final String LOCAL_URL = "jdbc:postgresql://localhost:5432/pavitra_ristaa";
    private static final String LOCAL_USERNAME = "pavitra_ristaa";

    private DbMigrationApplication() {
    }

    public static void main(String[] args) {
        boolean local = isLocalProfile(System.getenv("SPRING_PROFILES_ACTIVE"));
        String url = setting("DB_URL", local ? LOCAL_URL : null);
        String username = setting("DB_USERNAME", local ? LOCAL_USERNAME : null);
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .load();
        MigrateResult result = flyway.migrate();

        MigrationInfo current = flyway.info().current();
        System.out.printf("Database is at version %s; applied %d migration(s).%n",
                current == null ? "none" : current.getVersion(), result.migrationsExecuted);
    }

    static boolean isLocalProfile(String activeProfiles) {
        return activeProfiles == null || activeProfiles.isBlank() || activeProfiles.trim().equals("local");
    }

    private static String setting(String name, String localDefault) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (localDefault != null) {
            return localDefault;
        }
        throw new IllegalStateException(name + " must be set when SPRING_PROFILES_ACTIVE is not 'local'");
    }
}
