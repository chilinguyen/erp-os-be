package com.chilleric.franchise_sys.repository.abstract_repository;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CrmRepository<T>
  extends AbstractRepository
  implements RepositoryInterface<T> {
  @Autowired
  @Qualifier("mongo_crm_template")
  protected MongoTemplate crmDBTemplate;

  public Class<T> g() throws RuntimeException {
    ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();

    return (Class<T>) superclass.getActualTypeArguments()[0];
  }

  public Optional<List<T>> getListOrEntity(
    Map<String, String> allParams,
    String keySort,
    int page,
    int pageSize,
    String sortField
  ) {
    Query query = generateQueryMongoDB(
      allParams,
      g(),
      keySort,
      sortField,
      page,
      pageSize
    );
    return findFunction(query, g(), crmDBTemplate);
  }

  public Optional<List<T>> getListByAttribute(String value, String attribute) {
    Query query = generateQueryMongoDB(
      Map.ofEntries(Map.entry(attribute, value)),
      g(),
      "",
      "",
      0,
      0
    );
    List<T> list = findFunction(query, g(), crmDBTemplate).get();
    if (list.size() > 0) {
      return Optional.of(list);
    }
    return Optional.empty();
  }

  public Optional<T> getEntityByAttribute(String value, String attribute) {
    Query query = generateQueryMongoDB(
      Map.ofEntries(Map.entry(attribute, value)),
      g(),
      "",
      "",
      0,
      0
    );
    List<T> list = findFunction(query, g(), crmDBTemplate).get();
    if (list.size() > 0) {
      return Optional.of(list.get(0));
    }
    return Optional.empty();
  }

  public void insertAndUpdate(T entity) {
    insertUpdateFunction(entity, crmDBTemplate);
  }

  public long getTotalPage(Map<String, String> allParams) {
    return getTotalPageFunction(allParams, crmDBTemplate, g());
  }

  public void deleteById(String id) {
    deleteById(id, crmDBTemplate, g());
  }
}
