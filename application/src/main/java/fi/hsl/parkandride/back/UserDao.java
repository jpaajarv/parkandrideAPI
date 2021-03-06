// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.back;

import org.joda.time.DateTime;

import com.querydsl.core.Tuple;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.postgresql.PostgreSQLQuery;
import com.querydsl.sql.postgresql.PostgreSQLQueryFactory;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.SimpleExpression;

import fi.hsl.parkandride.back.sql.QAppUser;
import fi.hsl.parkandride.core.back.UserRepository;
import fi.hsl.parkandride.core.domain.NotFoundException;
import fi.hsl.parkandride.core.domain.SearchResults;
import fi.hsl.parkandride.core.domain.User;
import fi.hsl.parkandride.core.domain.UserSearch;
import fi.hsl.parkandride.core.domain.UserSecret;
import fi.hsl.parkandride.core.service.TransactionalRead;
import fi.hsl.parkandride.core.service.TransactionalWrite;

public class UserDao implements UserRepository {

    public static final String USER_ID_SEQ = "user_id_seq";

    private static final SimpleExpression<Long> nextUserId = SQLExpressions.nextval(USER_ID_SEQ);

    private static final DateTimeExpression<DateTime> currentTime = DateTimeExpression.currentTimestamp(DateTime.class);

    private static final QAppUser qUser = QAppUser.appUser;

    private static final MappingProjection<User> userMapping = new MappingProjection<User>(User.class,
            qUser.id, qUser.username, qUser.role, qUser.operatorId) {

        @Override
        protected User map(Tuple row) {
            User user = new User();
            user.id = row.get(qUser.id);
            user.username = row.get(qUser.username);
            user.role = row.get(qUser.role);
            user.operatorId = row.get(qUser.operatorId);
            return user;
        }
    };


    private static final MappingProjection<UserSecret> userSecretMapping = new MappingProjection<UserSecret>(UserSecret.class,
            qUser.password, qUser.minTokenTimestamp, qUser.passwordUpdatedTimestamp, userMapping) {
        @Override
        protected UserSecret map(Tuple row) {
            UserSecret userSecret = new UserSecret();
            userSecret.password = row.get(qUser.password);
            userSecret.passwordUpdatedTimestamp = row.get(qUser.passwordUpdatedTimestamp);
            userSecret.minTokenTimestamp = row.get(qUser.minTokenTimestamp);
//            userSecret.secret = row.get(qUser.secret);
            userSecret.user = row.get(userMapping);
            return userSecret;
        }
    };


    private final PostgreSQLQueryFactory queryFactory;

    public UserDao(PostgreSQLQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }


    @Override
    @TransactionalWrite
    public long insertUser(UserSecret userSecret) {
        return insertUser(userSecret, queryFactory.query().select(nextUserId).fetchOne());
    }

    @TransactionalWrite
    public long insertUser(UserSecret userSecret, long userId) {
        SQLInsertClause insert = queryFactory.insert(qUser);
        insert.set(qUser.id, userId)
                .set(qUser.password, userSecret.password)
                .set(qUser.passwordUpdatedTimestamp, currentTime)
                .set(qUser.minTokenTimestamp, currentTime)
                .set(qUser.username, userSecret.user.username.toLowerCase())
                .set(qUser.role, userSecret.user.role)
                .set(qUser.operatorId, userSecret.user.operatorId);
        insert.execute();
        return userId;
    }

    @TransactionalRead
    @Override
    public DateTime getCurrentTime() {
        return queryFactory.query().select(currentTime).fetchOne();
    }

    @TransactionalWrite
    @Override
    public void revokeTokens(long userId, DateTime asOf) {
        if (queryFactory.update(qUser)
                .where(qUser.id.eq(userId))
                .set(qUser.minTokenTimestamp, asOf)
                .execute() != 1) {
            notFound(userId);
        }
    }

    @TransactionalWrite
    @Override
    public void updatePassword(long userId, String password) {
        if (queryFactory.update(qUser)
                .where(qUser.id.eq(userId))
                .set(qUser.password, password)
                .set(qUser.passwordUpdatedTimestamp, currentTime)
                .set(qUser.minTokenTimestamp, currentTime)
                .execute() != 1) {
            notFound(userId);
        }
    }

    @TransactionalWrite
    @Override
    public void updateUser(long userId, User user) {
        if (queryFactory.update(qUser)
                .where(qUser.id.eq(userId))
                .set(qUser.username, user.username.toLowerCase())
                .set(qUser.role, user.role)
                .execute() != 1) {
            notFound(userId);
        }
    }

    @TransactionalWrite
    @Override
    public void deleteUser(long userId) {
        SQLDeleteClause clause = queryFactory.delete(qUser).where(qUser.id.eq(userId));
        if (clause.execute() != 1) {
            notFound(userId);
        };
    }

    @TransactionalRead
    @Override
    public UserSecret getUser(String username) {
        UserSecret userSecret = queryFactory
                .from(qUser)
                .where(qUser.username.eq(username.toLowerCase()))
                .select(userSecretMapping).fetchOne();
        if (userSecret == null) {
            notFound(username);
        }
        return userSecret;
    }

    private void notFound(String username) {
        throw new NotFoundException("User by username '%s'", username);
    }

    private void notFound(long userId) {
        throw new NotFoundException("User by id '%s'", userId);
    }

    @TransactionalRead
    @Override
    public UserSecret getUser(long userId) {
        UserSecret userSecret = queryFactory.from(qUser).where(qUser.id.eq(userId)).select(userSecretMapping).fetchOne();
        if (userSecret == null) {
            notFound(userId);
        }
        return userSecret;
    }

    @Override
    @TransactionalRead
    public SearchResults<User> findUsers(UserSearch search) {
        PostgreSQLQuery<User> qry = queryFactory.from(qUser).select(userMapping);
        qry.limit(search.getLimit() + 1);
        qry.offset(search.getOffset());

        if (search.getOperatorId() != null) {
            qry.where(qUser.operatorId.eq(search.getOperatorId()));
        }

        qry.orderBy(qUser.username.asc());

        return SearchResults.of(qry.fetch(), search.getLimit());
    }
}
