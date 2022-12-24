package com.chilleric.franchise_sys.repository.settings;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.chilleric.franchise_sys.repository.AbstractSystemRepo;

@Repository
public class SettingRepositoryImpl extends AbstractSystemRepo implements SettingRepository {

    @Override
    public Optional<List<Setting>> getSettings(Map<String, String> allParams, String keySort,
            int page, int pageSize, String sortField) {
        Query query =
                generateQueryMongoDB(allParams, Setting.class, keySort, sortField, page, pageSize);
        return replaceFind(query, Setting.class);
    }

    @Override
    public void insertAndUpdate(Setting setting) {
        systemDBTemplate.save(setting, "settings");
    }

}
