package org.pragmatica.http.example.urlshortener.persistence;

import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.http.example.urlshortener.domain.entity.ShortenedUrl;
import org.pragmatica.http.example.urlshortener.persistence.template.ShortenedUrlTemplate;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import static org.pragmatica.db.postgres.Sql.QRY;

public interface ShortenedUrlRepository {
    default Promise<ShortenedUrl> create(ShortenedUrl shortenedUrl) {
        return QRY."INSERT INTO shortenedurl (\{template().fieldNames()}) VALUES (\{template().fieldValues(shortenedUrl)}) RETURNING *"
            .in(dbEnv())
            .mapResult(ra -> ra.asSingle(template()));
    }

    default Promise<ShortenedUrl> read(String id) {
        return QRY."SELECT * FROM shortenedurl WHERE id = \{id}"
            .in(dbEnv())
            .mapResult(ra -> ra.asSingle(template()));
    }

    default Promise<Unit> delete(String id) {
        return QRY."DELETE FROM shortenedurl WHERE id = \{id}"
            .in(dbEnv())
            .mapToUnit();
    }

    DbEnv dbEnv();

    default ShortenedUrlTemplate template() {
        return ShortenedUrlTemplate.INSTANCE;
    }
}
