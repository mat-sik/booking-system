CREATE KEYSPACE IF NOT EXISTS migration
            WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS migration.schema_migrations
(
    fileName    text,
    executed_at timestamp,
    checksum    text,
    PRIMARY KEY ((fileName))
);