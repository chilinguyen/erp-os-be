package com.chilleric.franchise_sys.repository.code;

import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.chilleric.franchise_sys.repository.AbstractSystemRepo;

@Repository
public class CodeRepositoryImpl extends AbstractSystemRepo implements CodeRepository {
    @Override
    public Optional<Code> getCodesByCode(String userId, String code) {
        try {
            ObjectId user_id = new ObjectId(userId);
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(user_id).and("code").is(code));
            return replaceFindOne(query, Code.class);
        } catch (IllegalArgumentException e) {
            APP_LOGGER.error("wrong type user id");
            return Optional.empty();
        }
    }

    @Override
    public void insertAndUpdateCode(Code code) {
        systemDBTemplate.save(code, "codes");
    }

    @Override
    public Optional<Code> getCodesByType(String userId, String type) {
        try {
            ObjectId user_id = new ObjectId(userId);
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(user_id).and("type").is(type));
            return replaceFindOne(query, Code.class);
        } catch (IllegalArgumentException e) {
            APP_LOGGER.error("wrong type user id");
            return Optional.empty();
        }
    }
}
