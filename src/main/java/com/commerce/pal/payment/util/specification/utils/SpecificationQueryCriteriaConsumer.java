package com.commerce.pal.payment.util.specification.utils;

import lombok.extern.java.Log;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public class SpecificationQueryCriteriaConsumer implements Consumer<SearchCriteria> {
    private Predicate predicate;
    private CriteriaBuilder builder;
    private Root r;

    public SpecificationQueryCriteriaConsumer(Predicate predicate, CriteriaBuilder builder, Root r) {
        super();
        this.predicate = predicate;
        this.builder = builder;
        this.r = r;
    }

    @Override
    public void accept(SearchCriteria param) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (param.getOperation().equalsIgnoreCase(">")) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(r.get(param.getKey()), param.getValue().toString()));
            } else if (param.getOperation().equalsIgnoreCase(">D")) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(r.get(param.getKey()), dateFormat.parse(param.getValue().toString())));
            } else if (param.getOperation().equalsIgnoreCase("<D")) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(r.get(param.getKey()), dateFormat.parse(param.getValue().toString())));
            }else if (param.getOperation().equalsIgnoreCase("<")) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(r.get(param.getKey()), param.getValue().toString()));
            } else if (param.getOperation().equalsIgnoreCase(":")) {
                if (r.get(param.getKey()).getJavaType() == String.class) {
                    predicate = builder.and(predicate, builder.like(r.get(param.getKey()), "%" + param.getValue() + "%"));
                } else {
                    predicate = builder.and(predicate, builder.equal(r.get(param.getKey()), param.getValue()));
                }
            } else if (param.getOperation().equalsIgnoreCase("IN")) {
                if (r.get(param.getKey()).getJavaType() == String.class) {
                    String str[] = param.getValue().toString().split(",");
                    List<String> list = Arrays.asList(str);
                    Expression<String> inExpression = r.get(param.getKey());
                    Predicate inPredicate = inExpression.in(list);
                    list.forEach(unique -> {
                        predicate = builder.or(predicate, builder.equal(r.get(param.getKey()), unique));
                    });
                    //predicate = builder.and(predicate, builder.in(inPredicate));
                    /*
                    if (list != null && !list.isEmpty()) {
                        predicate = builder.and(predicate,builder.equal(r.get(param.getKey()), param.getValue().toString()));
                    }*/
                } else {

                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "FILTER KEY ERROR : " + r.get(param.getKey()));
        }
    }

    public Predicate getPredicate() {
        return predicate;
    }
}
