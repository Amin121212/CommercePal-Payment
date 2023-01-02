package com.commerce.pal.payment.util.specification;


import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import com.commerce.pal.payment.model.payment.Order;
import com.commerce.pal.payment.model.payment.OrderItem;
import com.commerce.pal.payment.model.payment.Transaction;
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

    public List<Order> getOrders(final List<SearchCriteria> params) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Order> query = builder.createQuery(Order.class);
        final Root r = query.from(Order.class);

        Predicate predicate = builder.conjunction();
        SpecificationQueryCriteriaConsumer searchOrders = new SpecificationQueryCriteriaConsumer(predicate, builder, r);
        params.stream().forEach(searchOrders);
        predicate = searchOrders.getPredicate();
        query.where(predicate);
        query.orderBy((builder.desc(r.get("orderId"))));
        return entityManager.createQuery(query).getResultList();
    }


    public List<Transaction> getTransactions(final List<SearchCriteria> params, Integer page) {
        Integer transPageSize = 20;
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Transaction> query = builder.createQuery(Transaction.class);
        final Root r = query.from(Transaction.class);

        Predicate predicate = builder.conjunction();
        SpecificationQueryCriteriaConsumer searchTrans = new SpecificationQueryCriteriaConsumer(predicate, builder, r);
        params.stream().forEach(searchTrans);
        predicate = searchTrans.getPredicate();
        query.where(predicate);
        query.orderBy((builder.desc(r.get("id"))));

        /*
        https://stackoverflow.com/questions/60703308/implementing-pagination-using-entity-manager-in-spring
        https://stackoverflow.com/questions/9321916/jpa-criteriabuilder-how-to-use-in-comparison-operator
        https://stackoverflow.com/questions/51583079/criteria-api-in-with-arraylist
         */
        return entityManager.createQuery(query)
                .setMaxResults(transPageSize)
                .getResultList();
    }

    public List<MerchantWithdrawal> getMerchantWithdrawal(final List<SearchCriteria> params) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<MerchantWithdrawal> query = builder.createQuery(MerchantWithdrawal.class);
        final Root r = query.from(MerchantWithdrawal.class);

        Predicate predicate = builder.conjunction();
        SpecificationQueryCriteriaConsumer searchOrderItems = new SpecificationQueryCriteriaConsumer(predicate, builder, r);
        params.stream().forEach(searchOrderItems);
        predicate = searchOrderItems.getPredicate();
        query.where(predicate);
        return entityManager.createQuery(query).getResultList();
    }


}
