package com.commerce.pal.payment.util.specification;


import com.commerce.pal.payment.model.payment.OrderItem;
import com.commerce.pal.payment.util.specification.utils.SearchCriteria;
import com.commerce.pal.payment.util.specification.utils.SpecificationQueryCriteriaConsumer;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SpecificationsDao {
    @PersistenceContext
    private EntityManager entityManager;

    public List<OrderItem> getOrderItems(final List<SearchCriteria> params) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<OrderItem> query = builder.createQuery(OrderItem.class);
        final Root r = query.from(OrderItem.class);

        Predicate predicate = builder.conjunction();
        SpecificationQueryCriteriaConsumer searchOrderItems = new SpecificationQueryCriteriaConsumer(predicate, builder, r);
        params.stream().forEach(searchOrderItems);
        predicate = searchOrderItems.getPredicate();
        query.where(predicate);

        return entityManager.createQuery(query).getResultList();
    }
}
