package no.nav.tsm.core.db

import io.ktor.server.config.ApplicationConfig
import java.sql.Connection
import java.sql.DriverManager


fun connectToPostgres(config: ApplicationConfig): Connection {
    val url = config.property("postgres.url").getString()
    val user = config.property("postgres.username").getString()
    val password = config.property("postgres.password").getString()

    runFlywayMigrations(url, user, password)
    // fikse envs
    // koble til postgres
    // returnere connection til DI

    /**
     * String url = "jdbc:postgresql://localhost/test";
     * Properties props = new Properties();
     * props.setProperty("user", "fred");
     * props.setProperty("password", "secret");
     * props.setProperty("ssl", "true");
     * Connection conn = DriverManager.getConnection(url, props);
     *
     * String url = "jdbc:postgresql://localhost/test?user=fred&password=secret&ssl=true";
     * Connection conn = DriverManager.getConnection(url);
     */

    return DriverManager.getConnection("jdbc:postgresql://localhost:7969/syk-inn-api?user=postgres&password=password")
}
