DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'syk-inn-api-instance')
        THEN
            ALTER USER "syk-inn-api-instance" IN DATABASE "syk-inn-api" SET pgaudit.log TO 'none';
        END IF;
    END
$$;

