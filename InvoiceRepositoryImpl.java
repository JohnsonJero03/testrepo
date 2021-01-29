package uk.org.oners.oneps.db.repository.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.org.oners.oneps.constants.*;
import uk.org.oners.oneps.controller.Authorizer;
import uk.org.oners.oneps.db.model.*;
import uk.org.oners.oneps.db.repository.CompanyRepository;
import uk.org.oners.oneps.db.repository.InvoiceRepositoryCustom;
import uk.org.oners.oneps.restmodel.*;
import uk.org.oners.oneps.restmodel.filter.InvoiceFilter;
import uk.org.oners.oneps.util.BeanValidator;

import javax.crypto.SealedObject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public class InvoiceRepositoryImpl<T> implements InvoiceRepositoryCustom {

    @Autowired
    private EntityManager em;

//    @Autowired
//    private InvoiceSearchDTO invoiceSearchDTO;

    @Autowired
    CompanyRepository companyRepository;

    public  CriteriaQuery commonCriteria(InvoiceFilter invoiceFilter , boolean isReport){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(InvoiceSearchDTO.class);

        Root<Invoice> root = criteriaQuery.from(Invoice.class);
        Join<Invoice,Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany);
        Join<Invoice,Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany);
        Join<Invoice,InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings);
        Join<InvoiceSourceMapping,Contract> invoiceSourceMappingContractJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.contract);
        Join<Contract,Company> contractCompanyJoin = invoiceSourceMappingContractJoin.join(Contract_.agencyCompany);
        Join<Invoice,CurrencyTaxPercentage> taxPercentage = root.join(Invoice_.currencyTaxPercentage);
        Join<CurrencyTaxPercentage,Currency> currencyCode = taxPercentage.join(CurrencyTaxPercentage_.currency);


        return cq;
    }
    public  List<T> getByFilter(InvoiceFilter invoiceFilter, boolean isReport,boolean isFurtherProcess ) {

        if(isFurtherProcess == false)
        {

            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(InvoiceSearchDTO.class);

            Root<Invoice> root = criteriaQuery.from(Invoice.class);
            Join<Invoice,Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany);
            Join<Invoice,Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany);
            Join<Invoice,InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings);
            Join<InvoiceSourceMapping,Contract> invoiceSourceMappingContractJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.contract);
            Join<Contract,Company> contractCompanyJoin = invoiceSourceMappingContractJoin.join(Contract_.agencyCompany);
            Join<Invoice,CurrencyTaxPercentage> taxPercentage = root.join(Invoice_.currencyTaxPercentage);
            Join<CurrencyTaxPercentage,Currency> currencyCode = taxPercentage.join(CurrencyTaxPercentage_.currency);

            CriteriaQuery query = checkForFurtherProcess(invoiceFilter,criteriaBuilder,root,invoiceFromCompanyJoin,invoiceToCompanyJoin,invoiceInvoiceSourceMappingJoin,contractCompanyJoin,currencyCode,isReport,isFurtherProcess);

            criteriaQuery.multiselect(root.get(Invoice_.id).alias("invoiceId"),invoiceFromCompanyJoin.get(Company_.name).alias("fromCompanyName"),
                    invoiceToCompanyJoin.get(Company_.name).alias("Tocompany"),contractCompanyJoin.get(Company_.name).alias("agencyCompany"),
                    currencyCode.get(Currency_.currencyCode).alias("CurrencyCode"),root.get(Invoice_.totalAmount).alias("totalAmount"),
                    root.get(Invoice_.invoiceGenerationType).alias("invoiceGenerationType"),root.get(Invoice_.payByDate).alias("dueDate"),
                    root.get(Invoice_.createdOn).alias("CreatedOn"),root.get(Invoice_.status).alias("Status"));

            List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, isReport);

            predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
            predicates.add(criteriaBuilder.isTrue(invoiceFromCompanyJoin.get(Company_.isActive)));
            predicates.add(criteriaBuilder.isTrue(invoiceToCompanyJoin.get(Company_.isActive)));

            if (!predicates.isEmpty())
                criteriaQuery.where(predicates.toArray(new Predicate[0]));

            criteriaQuery.orderBy(getOrderByListByIdAndCreatedOn(criteriaBuilder, root));

            TypedQuery<T> query = em.createQuery(criteriaQuery);
            if (invoiceFilter.isPagination())
                query.setFirstResult(invoiceFilter.getOffSet()).setMaxResults(invoiceFilter.getPageSize());
            return query.getResultList();
        }
        else
        {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(Invoice.class);
            Root<Invoice> root = criteriaQuery.from(Invoice.class);
            Join<Invoice, Company> fromCompanyList = root.join(Invoice_.fromCompany, JoinType.LEFT);
            Join<Invoice, Company> toComanyList = root.join(Invoice_.toCompany, JoinType.LEFT);

            List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, fromCompanyList, toComanyList, isReport);
            predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
            predicates.add(criteriaBuilder.isTrue(fromCompanyList.get(Company_.isActive)));
            predicates.add(criteriaBuilder.isTrue(toComanyList.get(Company_.isActive)));
            if (!predicates.isEmpty())
                criteriaQuery.where(predicates.toArray(new Predicate[0]));

            criteriaQuery.orderBy(getOrderByListByIdAndCreatedOn(criteriaBuilder, root));

            TypedQuery<T> query = em.createQuery(criteriaQuery);
            if (invoiceFilter.isPagination())
                query.setFirstResult(invoiceFilter.getOffSet()).setMaxResults(invoiceFilter.getPageSize());
            return  query.getResultList();


            /////////////////////////////////////
//            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
//            CriteriaQuery<Invoice> criteriaQuery = criteriaBuilder.createQuery(Invoice.class);
//
//            Root<Invoice> root = criteriaQuery.from(Invoice.class);
//            Join<Invoice,Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany);
//            Join<Invoice,Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany);
//            Join<Invoice,InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings);
//            Join<InvoiceSourceMapping,Contract> invoiceSourceMappingContractJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.contract);
//            Join<Contract,Company> contractCompanyJoin = invoiceSourceMappingContractJoin.join(Contract_.agencyCompany);
//            Join<Invoice,CurrencyTaxPercentage> taxPercentage = root.join(Invoice_.currencyTaxPercentage);
//            Join<CurrencyTaxPercentage,Currency> currencyCode = taxPercentage.join(CurrencyTaxPercentage_.currency);
//
//            List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, isReport);
//
//            predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
//            predicates.add(criteriaBuilder.isTrue(invoiceFromCompanyJoin.get(Company_.isActive)));
//            predicates.add(criteriaBuilder.isTrue(invoiceToCompanyJoin.get(Company_.isActive)));
//
//            if (!predicates.isEmpty())
//
//                criteriaQuery.where(predicates.toArray(new Predicate[0]));
//
//            criteriaQuery.orderBy(getOrderByListByIdAndCreatedOn(criteriaBuilder, root));
//
//            TypedQuery<Invoice> query = em.createQuery(criteriaQuery);
//            if (invoiceFilter.isPagination())
//                query.setFirstResult(invoiceFilter.getOffSet()).setMaxResults(invoiceFilter.getPageSize());
//            return (List<T>) query.getResultList();
        }







//
//            /*Commented for JPA-Projection
//        Root<Invoice> root = criteriaQuery.frogetByFilterm(Invoice.class);
//        Join<Invoice, Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany, JoinType.LEFT);
//        Join<Invoice, Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany, JoinType.LEFT);*/
//
//        List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, isReport);
//
//        predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
//        predicates.add(criteriaBuilder.isTrue(invoiceFromCompanyJoin.get(Company_.isActive)));
//        predicates.add(criteriaBuilder.isTrue(invoiceToCompanyJoin.get(Company_.isActive)));
//
//        if (!predicates.isEmpty())
//
//            criteriaQuery.where(predicates.toArray(new Predicate[0]));
//
//        criteriaQuery.orderBy(getOrderByListByIdAndCreatedOn(criteriaBuilder, root));
//
//        TypedQuery<InvoiceSearchDTO> query = em.createQuery(criteriaQuery);
//        if (invoiceFilter.isPagination())
//            query.setFirstResult(invoiceFilter.getOffSet()).setMaxResults(invoiceFilter.getPageSize());
//        return (List<T>) query.getResultList();
//
////        return result;
    }


    public CriteriaQuery checkForFurtherProcess(,InvoiceFilter invoiceFilter, CriteriaBuilder builder,
                                                Root<Invoice> root, Join<Invoice, Company> invoiceFromCompanyJoin,
                                                Join<Invoice, Company> invoiceToCompanyJoin,Join<Invoice,InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin, Join<Contract,Company> contractCompanyJoin,Join<CurrencyTaxPercentage,Currency> currencyCode,boolean isReport, boolean isFurtherProccess)
    {
        if(isFurtherProccess == false)
        {

            criteriaQuery.multiselect(root.get(Invoice_.id).alias("invoiceId"),invoiceFromCompanyJoin.get(Company_.name).alias("fromCompanyName"),
                    invoiceToCompanyJoin.get(Company_.name).alias("Tocompany"),contractCompanyJoin.get(Company_.name).alias("agencyCompany"),
                    currencyCode.get(Currency_.currencyCode).alias("CurrencyCode"),root.get(Invoice_.totalAmount).alias("totalAmount"),
                    root.get(Invoice_.invoiceGenerationType).alias("invoiceGenerationType"),root.get(Invoice_.payByDate).alias("dueDate"),
                    root.get(Invoice_.createdOn).alias("CreatedOn"),root.get(Invoice_.status).alias("Status"));
        }
    }



    /////

   /*public List<Invoice> getInvoiceByFilter(InvoiceFilter invoiceFilter, boolean isReport) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Invoice> criteriaQuery = criteriaBuilder.createQuery(Invoice.class);
        Root<Invoice> root = criteriaQuery.from(Invoice.class);
        Join<Invoice, Company> fromCompanyList = root.join(Invoice_.fromCompany, JoinType.LEFT);
        Join<Invoice, Company> toComanyList = root.join(Invoice_.toCompany, JoinType.LEFT);

        List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, fromCompanyList, toComanyList, isReport);
        predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
        predicates.add(criteriaBuilder.isTrue(fromCompanyList.get(Company_.isActive)));
        predicates.add(criteriaBuilder.isTrue(toComanyList.get(Company_.isActive)));
        if (!predicates.isEmpty())
            criteriaQuery.where(predicates.toArray(new Predicate[0]));

        criteriaQuery.orderBy(getOrderByListByIdAndCreatedOn(criteriaBuilder, root));

        TypedQuery<Invoice> query = em.createQuery(criteriaQuery);
        if (invoiceFilter.isPagination())
            query.setFirstResult(invoiceFilter.getOffSet()).setMaxResults(invoiceFilter.getPageSize());
        return query.getResultList();

//        return result;
    }*/

    /////

    public List<Invoice> getFilter(InvoiceFilter invoiceFilter, boolean isReport) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Invoice> criteriaQuery = criteriaBuilder.createQuery(Invoice.class);

        Root<Invoice> root = criteriaQuery.from(Invoice.class);
        Join<Invoice, Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany, JoinType.LEFT);
        Join<Invoice, Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany, JoinType.LEFT);
        Join<Invoice, InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings, JoinType.LEFT);
        Join<InvoiceSourceMapping, Contract> invoiceContractJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.contract, JoinType.LEFT);
        Join<InvoiceSourceMapping, TimeSheet> invoiceTimesheetJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.timeSheet, JoinType.LEFT);

        Join<Contract, Company> contractCompanyJoin = invoiceContractJoin.join(Contract_.agencyCompany, JoinType.LEFT);
        List<Predicate> predicates = getPredicates(invoiceFilter, criteriaBuilder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, isReport);

        predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
        predicates.add(criteriaBuilder.isTrue(invoiceFromCompanyJoin.get(Company_.isActive)));
        predicates.add(criteriaBuilder.isTrue(invoiceToCompanyJoin.get(Company_.isActive)));

        if (!predicates.isEmpty())
            criteriaQuery.where(predicates.toArray(new Predicate[0]));

        criteriaQuery.orderBy(getOrderByListByCompanyName(invoiceFilter, criteriaBuilder, contractCompanyJoin, invoiceFromCompanyJoin, invoiceToCompanyJoin, invoiceTimesheetJoin));

        TypedQuery<Invoice> query = em.createQuery(criteriaQuery);
        return query.getResultList();
    }

    private List<Order> getOrderByListByIdAndCreatedOn(CriteriaBuilder criteriaBuilder, Root<Invoice> root) {
        List<Order> orderByList = new ArrayList<>();
        orderByList.add(criteriaBuilder.desc(root.get(Invoice_.createdOn)));
        orderByList.add(criteriaBuilder.desc(root.get(Invoice_.id)));
        return orderByList;
    }

    private List<Order> getOrderByListByCompanyName(InvoiceFilter invoiceFilter, CriteriaBuilder criteriaBuilder, Join<Contract, Company> contractCompanyJoin, Join<Invoice, Company> invoiceFromCompanyJoin,
                                                    Join<Invoice, Company> invoiceToCompanyJoin, Join<InvoiceSourceMapping, TimeSheet> invoiceTimesheetJoin) {
        List<Order> orderByList = new ArrayList<>();

        if (invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_AGENCY_TO_CLIENT.getId())
                || invoiceFilter.getInvoiceTypes().contains(InvoiceType.AGENCY_TO_CLIENT.getId())) {
            orderByList.add(criteriaBuilder.asc(invoiceFromCompanyJoin.get(Company_.name)));
            orderByList.add(criteriaBuilder.asc(invoiceToCompanyJoin.get(Company_.name)));
            orderByList.add(criteriaBuilder.asc(contractCompanyJoin.get(Company_.name)));
        } else if (invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_CLIENT.getId()) ||
                invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_ONEPS_TO_CLIENT.getId())) {
            orderByList.add(criteriaBuilder.asc(contractCompanyJoin.get(Company_.name)));
            orderByList.add(criteriaBuilder.asc(invoiceToCompanyJoin.get(Company_.name)));
        } else if (invoiceFilter.getInvoiceTypes().contains(InvoiceType.CONTRACTOR_TO_AGENCY.getId()) &&
                invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_AGENCY.getId()) &&
                invoiceFilter.getInvoiceTypes().contains(InvoiceType.AGENCY_TO_ONEPS.getId())) {
            orderByList.add(criteriaBuilder.asc(invoiceTimesheetJoin.get(TimeSheet_.id)));
        } else {
            orderByList.add(criteriaBuilder.asc(invoiceToCompanyJoin.get(Company_.name)));
        }
        return orderByList;
    }

    private List<Predicate> getPredicates(InvoiceFilter invoiceFilter, CriteriaBuilder builder,
                                          Root<Invoice> root, Join<Invoice, Company> invoiceFromCompanyJoin,
                                          Join<Invoice, Company> invoiceToCompanyJoin, boolean isReport) {
        List<Predicate> predicates = new ArrayList<>();
        if (invoiceFilter.getIsUnpaidInvoiceView() != null && invoiceFilter.getIsUnpaidInvoiceView()) {
            Expression<Integer> expression = root.get(Invoice_.status);
            predicates.add(expression.in(getUnpaidInvoice()));
        } else {
            if (invoiceFilter.getStatus() != 0) {
                predicates.add(builder.equal(root.get(Invoice_.status), invoiceFilter.getStatus()));
            }
        }

        if (invoiceFilter.getIsManualInvoiceView() != null) {
            if (invoiceFilter.getIsManualInvoiceView()) {
                Expression<Integer> expression = root.get(Invoice_.invoiceType);
                predicates.add(expression.in(getManualInvoiceType()));
            } else if (!invoiceFilter.getIsManualInvoiceView()) {
                Expression<Integer> expression = root.get(Invoice_.invoiceType);
                predicates.add(expression.in(getInvoiceType()));
            }
        }

        if (BeanValidator.isValidString(invoiceFilter.getReference()))
            predicates.add(builder.like(root.get(Invoice_.id), BeanValidator.getSearchPattern(invoiceFilter.getReference())));

        if (BeanValidator.isValidString(invoiceFilter.getFromCompanyName()))
            predicates.add(builder.like(invoiceFromCompanyJoin.get(Company_.name),
                    BeanValidator.getSearchPattern(invoiceFilter.getFromCompanyName())));

        if (BeanValidator.isValidString(invoiceFilter.getToCompanyName()))
            predicates.add(builder.like(invoiceToCompanyJoin.get(Company_.name),
                    BeanValidator.getSearchPattern(invoiceFilter.getToCompanyName())));

        if (BeanValidator.isValidString(invoiceFilter.getClientId()))
            predicates.add(builder.like(invoiceToCompanyJoin.get(Company_.id),
                    BeanValidator.getSearchPattern(invoiceFilter.getClientId())));

        if (invoiceFilter.getDueDate() != null)
            predicates.add(builder.equal(root.get(Invoice_.payByDate), BeanValidator.getDateWithoutTime(invoiceFilter.getDueDate())));

        if (BeanValidator.isValidString(invoiceFilter.getClientCompanyName())) {
            Predicate toCompany = builder.like(invoiceToCompanyJoin.get(Company_.name),
                    BeanValidator.getSearchPattern(invoiceFilter.getClientCompanyName()));
            Predicate fromCompany = builder.like(invoiceFromCompanyJoin.get(Company_.name),
                    BeanValidator.getSearchPattern(invoiceFilter.getClientCompanyName()));
            Predicate fromToCompany = builder.or(toCompany, fromCompany);
            predicates.add((builder.and(fromToCompany, root.get(Invoice_.invoiceType).in(getCreditLimitInvoiceType()))));

        }

        if (invoiceFilter.getGeneratedFor() != null) {
            Join<Invoice, InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings, JoinType.LEFT);
            Join<InvoiceSourceMapping, TimeSheet> timeSheetJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.timeSheet, JoinType.LEFT);
            Join<InvoiceSourceMapping, Expenses> expensesJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.expenses, JoinType.LEFT);
            Predicate timeSheet = builder.like(timeSheetJoin.get(TimeSheet_.id),
                    BeanValidator.getSearchPattern(invoiceFilter.getGeneratedFor()));
            Predicate expenses = builder.like(expensesJoin.get(Expenses_.id),
                    BeanValidator.getSearchPattern(invoiceFilter.getGeneratedFor()));

            predicates.add(builder.or(timeSheet, expenses));
        }

        if (invoiceFilter.getGeneratedDate() != null)
            predicates.add(builder.greaterThanOrEqualTo(root.get(Invoice_.createdOn), BeanValidator.getDateWithoutTime(invoiceFilter.getGeneratedDate())));

        if (invoiceFilter.getOverDueIn() != null) {
            if (invoiceFilter.getOverDueIn() >= 1 && invoiceFilter.getOverDueIn() <= 5)
                applyCashFlow(predicates, invoiceFilter, builder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin);
        } else {
            if (BeanValidator.isValidString(invoiceFilter.getAgencyCompanyName())) {
                Join<Invoice, Invoice> parentInvoiceJoin = root.join(Invoice_.parentInvoice, JoinType.LEFT);
                Join<Invoice, Company> agencyCompanyJoin = parentInvoiceJoin.join(Invoice_.fromCompany, JoinType.LEFT);
                Predicate agencyInFromCompany = builder.like(invoiceFromCompanyJoin.get(Company_.name),
                        BeanValidator.getSearchPattern(invoiceFilter.getAgencyCompanyName()));
                Predicate agencyInvoiceCompany = builder.like(agencyCompanyJoin.get(Company_.name),
                        BeanValidator.getSearchPattern(invoiceFilter.getAgencyCompanyName()));
                Predicate agencyInToCompany = builder.like(invoiceToCompanyJoin.get(Company_.name),
                        BeanValidator.getSearchPattern(invoiceFilter.getAgencyCompanyName()));
                predicates.add(builder.or(agencyInFromCompany, agencyInToCompany, agencyInvoiceCompany));
            }

            if (BeanValidator.isValidBigDecimal((invoiceFilter.getAmount()))) {
                Predicate totalAmount = builder.equal(root.get(Invoice_.totalAmount),
                        invoiceFilter.getAmount());
                Predicate invoiceAmount = builder.equal(root.get(Invoice_.invoiceAmount),
                        invoiceFilter.getAmount());

                predicates.add(builder.or(totalAmount, invoiceAmount));
            }

            //For Aged Sales Ledger by Agency report, user restrictions already applied. So skip this apply user restrictions
            if (!(invoiceFilter.getInvoiceTypes() != null && invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_CLIENT.getId()) &&
                    invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_ONEPS_TO_CLIENT.getId()) &&
                    Authorizer.isAgency())) {
                applyUserRestrictions(predicates, builder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin);
            }

            if (invoiceFilter.getInvoiceTypes() != null && (invoiceFilter.getInvoiceTypes().contains(InvoiceType.CONTRACTOR_TO_AGENCY.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.AGENCY_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_AGENCY.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.AGENCY_TO_ONEPS.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_AGENCY_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_ONEPS_TO_AGENCY.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.EXPENSES_ONEPS_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.EXPENSES_AGENCY_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_AGENCY_TO_ONEPS.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.MANUAL_ONEPS_TO_CLIENT.getId())
                    || invoiceFilter.getInvoiceTypes().contains(InvoiceType.CREDIT_NOTE.getId()))) {
                if (invoiceFilter.getInvoiceTypes().contains(InvoiceType.CONTRACTOR_TO_AGENCY.getId()) ||
                        invoiceFilter.getInvoiceTypes().contains(InvoiceType.ONEPS_TO_AGENCY.getId())) {
                    Predicate toCompanyInfo = builder.and((root.get(Invoice_.invoiceType).in(invoiceFilter.getInvoiceTypes())),
                            builder.equal(invoiceToCompanyJoin.get(Company_.type), CompanyType.AGENCY.getId()));
                    predicates.add((builder.or(toCompanyInfo, root.get(Invoice_.invoiceType).in(invoiceFilter.getInvoiceTypes()))));
                } else if (invoiceFilter.getInvoiceTypes().contains(InvoiceType.AGENCY_TO_CLIENT.getId())) {
                    Predicate fromCompanyInfo = builder.and(root.get(Invoice_.invoiceType).in(invoiceFilter.getInvoiceTypes()),
                            builder.equal(invoiceFromCompanyJoin.get(Company_.type), CompanyType.AGENCY.getId()));
                    predicates.add((builder.or(fromCompanyInfo, root.get(Invoice_.invoiceType).in(invoiceFilter.getInvoiceTypes()))));
                }
                predicates.add(root.get(Invoice_.invoiceType).in(invoiceFilter.getInvoiceTypes()));
            }

            predicates = getLastMonthInvoices(invoiceFilter, builder, predicates, root, isReport);
        }

        return predicates;
    }

    public List<InvoiceStatusCount> getByInvoiceCount(InvoiceFilter invoiceFilter) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<InvoiceStatusCount> criteriaQuery = builder.createQuery(InvoiceStatusCount.class);
        Root<Invoice> root = criteriaQuery.from(Invoice.class);

        Join<Invoice, Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany, JoinType.LEFT);
        Join<Invoice, Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany, JoinType.LEFT);

        List<Predicate> predicates = getPredicates(invoiceFilter, builder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, true);

        if (!predicates.isEmpty())
            criteriaQuery.where(predicates.toArray(new Predicate[0]));

        criteriaQuery.select(builder.construct(InvoiceStatusCount.class,
                root.get(Invoice_.status), builder.countDistinct(root.get(Invoice_.id))));

        criteriaQuery.groupBy(root.get(Invoice_.status));

        TypedQuery<InvoiceStatusCount> query = em.createQuery(criteriaQuery);

        return query.getResultList();
    }

    private String getWeeklyPaymentQuery() {

        return " select a.id AS invoiceReference,date_format(a.created_on, '%Y-%m-%d') as invoiceDate,a.from_company_name AS invoiceFrom,a.to_company_name AS invoiceTo, " +
                "      case company.name " +
                "      when contract.id is null then CONCAT(company.name,'(',contract.job_title,')') " +
                "      when contract.id is not null then '-' " +
                "      End as placement, invoiceSource.source_reference_id AS generatedFor, " +
                "      case a.invoice_type " +
                "      when  1 Then -(a.invoice_amount) " +
                "      when  5 Then a.invoice_amount " +
                "      when  3 Then  -(a.invoice_amount) End as invoiceAmount, " +
                "      case a.invoice_type " +
                "      when  1 Then -(a.vat_amount) " +
                "      when  5 Then a.vat_amount " +
                "      when  3 Then  -(a.vat_amount) End as vatAmount, " +
                "      case a.invoice_type " +
                "      when  1 Then -(a.total_amount) " +
                "      when  5 Then a.total_amount " +
                "      when  3 Then  -(a.total_amount) End as totalAmount, " +
                "      currency.currency_symbol AS currencySymbol, " +
                "      case a.status " +
                "      when 1 Then 'Initiated' " +
                "      when 2 Then 'Paid' " +
                "      when 3 Then 'Overdue' " +
                "      End as status, timeSheet.end_date AS invoiceDueDate, " +
                "      case a.status  " +
                "      when '1' then '-' " +
                "      when '2' then date_format(a.date_updated, '%Y-%m-%d') " +
                "      when '3' then '-' " +
                "      End as invoicePaidDate, " +
                "      CONCAT(user.first_name,' ',user.last_name) AS contractorName, " +
                "      timeSheet.end_date AS weekEnding,timeSheet.total_units AS hoursOrDays " +
                "      from invoice_source_mapping as invoiceSource " +
                "      join invoice a on a.id = invoiceSource.invoice_id " +
                "      join contract contract on contract.id = invoiceSource.contract_id " +
                "      join user user on user.id = contract.contractor_id " +
                "      join currency_tax_percentage currencyTaxPercentage on currencyTaxPercentage.id = a.currency_tax_percentage_id " +
                "      join currency currency on currencyTaxPercentage.currency_id = currency.id " +
                "      join time_sheet timeSheet on timeSheet.id = invoiceSource.source_reference_id " +
                "      join company company on company.id = contract.client_company_id " +
                "      join company agencyCompany1 on agencyCompany1.id = contract.agency_company_id " +
                "      where ((a.invoice_type = 1 and a.status != 2) or (a.invoice_type = 5 and a.status != 2) or (a.invoice_type = 3 and a.status != 2)) ";
    }

    public List<WeeklyPaymentReport> weeklyPaymentReport(InvoiceFilter invoiceFilter) {
        String query = getWeeklyPaymentQuery();
        if (BeanValidator.isValidDate(invoiceFilter.getFromDate()) && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            query += " AND date_format(a.created_on, '%Y-%m-%d') Between ?3 AND ?4 ";
        } else {
            if (BeanValidator.isValidDate(invoiceFilter.getFromDate()))
                query += " AND date_format(a.created_on, '%Y-%m-%d') >= ?5 ";

            if (BeanValidator.isValidDate(invoiceFilter.getToDate()))
                query += " AND date_format(a.created_on, '%Y-%m-%d') <= ?6 ";
        }

        if (!BeanValidator.isValidDate(invoiceFilter.getFromDate()) && !BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            query += " AND date_format(a.created_on, '%Y-%m-%d') Between ?7 AND ?8 ";
        }

        if (Authorizer.isAdmin() && BeanValidator.isValidString(invoiceFilter.getAgencyId()))
            query += " AND agencyCompany1.id = ?1 ";

        query += " AND a.is_active = 1 " +
                " AND contract.is_active = 1 " +
//               " AND user.is_active = 1 " +
                " AND timeSheet.is_active = 1 ";
//               " AND company.is_active = 1 " +
//               " AND agencyCompany1.is_active = 1"

        query += " Order by invoiceDate desc, invoiceReference desc ";
        Query nativeQuery = em.createNativeQuery(query, "WeeklyPaymentMapping");
        if (BeanValidator.isValidString(invoiceFilter.getAgencyId())) {
            nativeQuery.setParameter(1, invoiceFilter.getAgencyId());
        }

        if (BeanValidator.isValidDate(invoiceFilter.getFromDate()) && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            nativeQuery.setParameter(3, invoiceFilter.getFromDate());
            nativeQuery.setParameter(4, invoiceFilter.getToDate());
        } else {
            if (BeanValidator.isValidDate(invoiceFilter.getFromDate()))
                nativeQuery.setParameter(5, invoiceFilter.getFromDate());

            if (BeanValidator.isValidDate(invoiceFilter.getToDate()))
                nativeQuery.setParameter(6, invoiceFilter.getToDate());
        }
        if (!BeanValidator.isValidDate(invoiceFilter.getFromDate()) && !BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            nativeQuery.setParameter(7, BeanValidator.getMonthStartDate(new Date()));
            nativeQuery.setParameter(8, new Date());
        }
        return nativeQuery.getResultList();
    }

    public List<AgencyCommissionReport> agencyCommissionReport(InvoiceFilter invoiceFilter, boolean isReport) {
        Contract contract;
        TimeSheet timeSheet;
        Expenses expenses;
        boolean isFurtherProcess = true;
        List<Invoice> invoices = (List<Invoice>) getByFilter(invoiceFilter, isReport,isFurtherProcess );

        List<AgencyCommissionReport> agencyCommissionReportList = new ArrayList<>();

        for (Invoice invoice : invoices) {

            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED) {

                contract = invoice.getInvoiceSourceMappings().getContract();
                timeSheet = invoice.getInvoiceSourceMappings().getTimeSheet();
                expenses = invoice.getInvoiceSourceMappings().getExpenses();
            } else {
                contract = null;
                timeSheet = null;
                expenses = null;
            }

            contract = contract == null || !contract.isActive() ? null : contract;
            timeSheet = timeSheet == null || !timeSheet.isActive() ? null : timeSheet;
            expenses = expenses == null || !expenses.isActive() ? null : expenses;

            String generatedFor = "-";
            if (timeSheet == null && expenses != null)
                generatedFor = expenses.getId();
            else if (timeSheet != null)
                generatedFor = timeSheet.getId();
            else if (timeSheet == null && expenses == null)
                generatedFor = invoice.getDescription();

            String timesheetFrequency = "-";
            String rateType = "-";
            BigDecimal commission = new BigDecimal(0);
            String commissionAndCommissionType = "-";
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            String endDate = "-";
            String contractorName = "-";
            String agencyConsultant = "-";

            List<AgencyCommission> agencyCommissionList = new ArrayList<>();

            if (contract != null) {
                timesheetFrequency = contract.getTimeSheetFrequency().getDescription();
                agencyConsultant = contract.getAgencyConsultant() != null ? contract.getAgencyConsultant().getFirstName() + " " + contract.getAgencyConsultant().getLastName() : "-";
            }

            if (timeSheet != null) {
                endDate = dateFormat.format(timeSheet.getEndDate());
                contractorName = timeSheet.getContract().getContractor().getFirstName() + " " + timeSheet.getContract().getContractor().getLastName();

                List<ContractorRateTotal> contractorRateTotalList = new ArrayList<>();

                for (TimeSheetDetail timeSheetDetail : timeSheet.getTimeSheetDetails()) {
                    ContractorRateTotal contractorRateTotal = contractorRateTotalList.stream().filter(contractorRateTotalItem -> contractorRateTotalItem
                            .getContractorRateId().equals(timeSheetDetail.getContractRate().getId())).findAny().orElse(null);
                    if (contractorRateTotal == null) {
                        contractorRateTotal = new ContractorRateTotal(timeSheetDetail.getContractRate().getId(), timeSheetDetail.getContractRate().getRate(), timeSheetDetail.getUnits());
                        contractorRateTotalList.add(contractorRateTotal);
                    } else {
                        contractorRateTotal.setTotalUnits(contractorRateTotal.getTotalUnits().add(timeSheetDetail.getUnits()));
                    }
                }

                List<ContractRate> contractRateList = contract.getContractRates();
                for (ContractorRateTotal contractorRateTotal : contractorRateTotalList) {
                    ContractRate contractRate = contractRateList.stream().filter(contractorRateItem -> contractorRateItem
                            .getId().equals(contractorRateTotal.getContractorRateId())).findAny().orElse(null);
                    rateType = contractRate.getRateTypeObject().getDescription();
                    AgencyChargeType agencyChargeType = contractRate.getAgencyChargeTypeObject();
                    commission = contractRate.getAgencyCharge();

                    if (agencyChargeType == AgencyChargeType.PERCENTAGE)
                        commissionAndCommissionType = commission + " " + "%";
                    else if (agencyChargeType == AgencyChargeType.AMOUNT)
                        commissionAndCommissionType = invoice.getCurrencyTaxPercentage().getCurrency().getCurrencySymbol() + " " + commission;

                    AgencyCommission agencyCommission
                            = new AgencyCommission(contractRate.getRate(), contractRate.getAgencyCharge(), commissionAndCommissionType, contractorRateTotal, contractRate.getAgencyChargeTypeObject(), contractRate.getRateTypeObject());
                    agencyCommissionList.add(agencyCommission);
                }
            }

            String clientCompanyNameAndJobTitle = "-";

            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED)
                clientCompanyNameAndJobTitle = (contract != null ? contract.getClientCompany().getName() + "(" +
                        contract.getJobTitle() + ")" : "-");
            else
                switch (invoice.getInvoiceType()) {
                    case MANUAL_AGENCY_TO_CLIENT:
                        clientCompanyNameAndJobTitle = invoice.getToCompany().getName();
                        break;
                    case MANUAL_AGENCY_TO_ONEPS:
                        clientCompanyNameAndJobTitle = getClientNameFromManualContractorToAgencyInvoice(invoice).get(0);
                        break;
                }

            SimpleDateFormat formatDate = new SimpleDateFormat("dd-MMM-yyyy");
            AgencyCommissionReport agencyCommissionReport = new AgencyCommissionReport(invoice.getFromCompanyName(), invoice.getToCompanyName(), agencyConsultant,
                    formatDate.format(invoice.getCreatedOn()), timesheetFrequency, clientCompanyNameAndJobTitle, generatedFor, endDate, contractorName,
                    invoice.getCurrencyTaxPercentage().getCurrency().getCurrencySymbol(), rateType, agencyCommissionList);
            agencyCommissionReportList.add(agencyCommissionReport);
        }

        return agencyCommissionReportList;
    }

    private String getIntermediateReportQuery() {

        return " SELECT a.from_company_id AS fromCompanyId, c.first_name AS workerForeName, c.last_name AS workerSurName, " +
                " SUM(a.total_amount) AS workerAmount, " +
                "  b.company_address_line_one AS workerAddressOne, " +
                "  b.company_address_line_two AS workerAddressTwo, b.company_address_line_three AS workerAddressThree, " +
                "  b.company_address_line_four AS workerAddressFour, b.company_address_post_code AS workerPostCode, " +
                "  e.start_date   AS workerStartDateOfEngagement, " +
                "  CASE WHEN e.end_date <= ?3 THEN e.end_date ELSE NULL END AS workerEndDateOfEngagement, " +
                "  h.currency_code AS workerCurrency, " +
                "  CASE WHEN a.from_company_vat_registration_number IS NULL THEN 'N' ELSE 'Y' END AS workerVat, " +
                "  a.from_company_registration_number AS workerPartyPaid, b.company_address_line_one AS workerAddressLineOne, " +
                "  b.company_address_line_two AS workerAddressLineTwo, b.company_address_line_three AS workerAddressLineThree, " +
                "  b.company_address_line_four AS workerAddressLineFour, b.company_address_post_code AS workerPartyPaidPostCode, " +
                "  ContractorVatInfo.fromCompReg AS workerRegistrationNumber, a.from_company_name AS workerCompanyName " +
                "  FROM invoice a " +
                "  INNER JOIN (select inv.id as invId, " +
                "         min(invStatus.date_created) as invPaidDate " +
                "         from invoice as inv " +
                "         join invoice_status as invStatus on invStatus.invoice_id = inv.id " +
                "         where inv.status = 2 and invStatus.status = 2 " +
                "         and inv.to_company_id = ?1 " +
                "         and inv.is_active = 1 " +
                "         and date_format(invStatus.date_created,'%Y-%m-%d') BETWEEN ?2 and ?3 " +
                "         group by inv.id)  invPaidStatus on invPaidStatus.invId = a.id " +
                "  JOIN company b ON a.from_company_id = b.id " +
                "  JOIN user c ON c.company_id = b.id " +
                "  JOIN invoice_source_mapping d ON d.invoice_id = a.id " +
                "  JOIN contract e ON e.id = d.contract_id " +
                "  JOIN currency_tax_percentage g ON a.currency_tax_percentage_id = g.id " +
                "  JOIN currency h ON h.id = g.currency_id " +
                "  LEFT JOIN (SELECT inv.from_company_id AS fromCompanyId, inv.from_company_registration_number AS fromCompReg, " +
                "        MIN(inv.created_on) AS invoiceCreatedOnDate " +
                "        FROM contract c " +
                "        JOIN invoice_source_mapping ism ON ism.contract_id = c.id " +
                "        JOIN invoice inv ON inv.id = ism.invoice_id " +
                "        WHERE inv.invoice_type = '1' " +
                "        AND date_format(inv.created_on, '%Y-%m-%d') BETWEEN ?2 and ?3 " +
                "        and inv.is_active = 1 " +
                "        GROUP BY fromCompanyId, fromCompReg " +
                "        ORDER BY invoiceCreatedOnDate) " +
                " ContractorVatInfo ON ContractorVatInfo.fromCompanyId = a.from_company_id AND ContractorVatInfo.fromCompReg = a.from_company_registration_number " +
                " WHERE a.invoice_type = 1 AND a.status = 2 " +
                " AND a.to_company_id = ?1 ";
    }

    public List<IntermediaryReportDTO> intermediaryReport(InvoiceFilter invoiceFilter, User user) {
        String query = getIntermediateReportQuery();
        query = applyDateFilter(query, invoiceFilter);

        query += "  AND a.is_active =1 " +
//              "   AND b.is_active =1 " +
//                "   AND c.is_active = 1 " +
                "   AND e.is_active = 1 ";

        query += "  GROUP BY a.from_company_id, workerForeName, workerSurName, workerAddressOne, workerAddressTwo, workerAddressThree, " +
                "  workerAddressFour, workerPostCode,  workerCurrency, workerVat, workerPartyPaid, workerAddressLineOne, " +
                "  workerAddressLineTwo, workerAddressLineThree, workerAddressLineFour, workerPartyPaidPostCode, workerRegistrationNumber, workerCompanyName, " +
                "  workerStartDateOfEngagement, workerEndDateOfEngagement ";
        Query nativeQuery = em.createNativeQuery(query, "ReportMapping");

        if (BeanValidator.isValidString(invoiceFilter.getAgencyId())) {
            nativeQuery.setParameter(1, invoiceFilter.getAgencyId());
        } else if (Authorizer.isAgency()) {
            nativeQuery.setParameter(1, Authorizer.getLoggedInUserCompanyId());
        }

        if (BeanValidator.isValidDate(invoiceFilter.getFromDate()) && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            nativeQuery.setParameter(2, invoiceFilter.getFromDate());
            nativeQuery.setParameter(3, invoiceFilter.getToDate());
        }
        return nativeQuery.getResultList();
    }

    public List<InvoiceSummary> findByFilter(InvoiceFilter invoiceFilter, boolean isReport) {
        Contract contract;
        TimeSheet timeSheet;
        Expenses expenses;

        boolean isFurtherProcess = true;

        if (Authorizer.isAgency() && !BeanValidator.isValidString(invoiceFilter.getAgencyId())) {
            invoiceFilter.setAgencyId(Authorizer.getLoggedInUserCompanyId());
        }
        if (Authorizer.isAgency() && !BeanValidator.isValidString(invoiceFilter.getAgencyCompanyName())) {
            invoiceFilter.setAgencyCompanyName(companyRepository.findById(Authorizer.getLoggedInUserCompanyId()).get().getName());
        }

        List<Invoice> invoices = (List<Invoice>) getByFilter(invoiceFilter, isReport,isFurtherProcess);

        List<InvoiceSummary> invoiceSummaryList = new ArrayList<>();

        //TODO: The pattern should be fetched from application.properties
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED) {
                contract = invoice.getInvoiceSourceMappings().getContract();
                timeSheet = invoice.getInvoiceSourceMappings().getTimeSheet();
                expenses = invoice.getInvoiceSourceMappings().getExpenses();
            } else {
                contract = null;
                timeSheet = null;
                expenses = null;
            }

            contract = contract == null || !contract.isActive() ? null : contract;
            timeSheet = timeSheet == null || !timeSheet.isActive() ? null : timeSheet;
            expenses = expenses == null || !expenses.isActive() ? null : expenses;

            String generatedFor = "-";
            if (timeSheet == null && expenses != null)
                generatedFor = expenses.getId();
            else if (timeSheet != null)
                generatedFor = timeSheet.getId();
            else if (timeSheet == null && expenses == null)
                generatedFor = invoice.getDescription();

            BigDecimal totalUnits = (timeSheet != null ? timeSheet.getTotalUnits() : new BigDecimal(0));

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            String endDate = (timeSheet != null ? dateFormat.format(timeSheet.getEndDate()) : "-");

            String invoicePaid = (invoice.getStatus().equals(InvoiceFormStatus.PAID) ? dateFormatWithTime.format(invoice.getDateUpdated()) : "-");
            String clientCompanyNameAndJobTitle = "-";

            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED)
                clientCompanyNameAndJobTitle = (contract != null ? contract.getClientCompany().getName() + "(" +
                        contract.getJobTitle() + ")" : "-");
            else
                switch (invoice.getInvoiceType()) {
                    case MANUAL_CONTRACTOR_TO_AGENCY:
                    case MANUAL_AGENCY_TO_ONEPS:
                        clientCompanyNameAndJobTitle = getClientNameFromManualContractorToAgencyInvoice(invoice).get(0);
                        break;
                    case MANUAL_AGENCY_TO_CLIENT:
                    case MANUAL_ONEPS_TO_CLIENT:
                        clientCompanyNameAndJobTitle = invoice.getToCompany().getName();
                        break;
                    case MANUAL_ONEPS_TO_AGENCY:
                        clientCompanyNameAndJobTitle = invoice.getParentInvoice() != null ? invoice.getParentInvoice().getToCompany().getName() : "-";
                        break;
                }

            String contractorName = (timeSheet != null ? timeSheet.getContract().getContractor().getFirstName() + " " + timeSheet.getContract().getContractor().getLastName() : "-");
            //TODO: The pattern should be fetched from application.properties

            String agencyConsultant = (contract != null && contract.getAgencyConsultant() != null ? contract.getAgencyConsultant().getFirstName() + " " + contract.getAgencyConsultant().getLastName() : "-");

            String agencyCompanyName = (contract != null ? contract.getAgencyCompany().getName() : "-");

            String clientCompanyName = (contract != null ? contract.getClientCompany().getName() : "-");

            String timeSheetFrequency = (contract != null ? contract.getTimeSheetFrequency().getDescription() : "-");

            InvoiceSummary invoiceSummary = new InvoiceSummary(invoice.getId(), dateFormat.format(invoice.getCreatedOn()),
                    invoice.getFromCompanyName(), invoice.getToCompanyName(),
                    clientCompanyNameAndJobTitle, generatedFor,
                    invoice.getInvoiceAmount(), invoice.getVatAmount(), invoice.getTotalAmount(),
                    invoice.getPayByDate() != null ? dateFormat.format(invoice.getPayByDate()) : "-",
                    invoice.getStatus().getDescription(), invoicePaid,
                    contractorName, endDate, totalUnits, invoice.getCurrencyTaxPercentage().getCurrency().getCurrencySymbol(),
                    agencyConsultant, invoice.getInvoiceType().getId(), agencyCompanyName, clientCompanyName, timeSheetFrequency);
            invoiceSummaryList.add(invoiceSummary);
        }
        return invoiceSummaryList;
    }

    private List<String> getClientNameFromManualContractorToAgencyInvoice(Invoice invoice) {
        String query = " SELECT company.name AS clientName FROM invoice levelOne ";

        if (invoice.getInvoiceType() == InvoiceType.MANUAL_CONTRACTOR_TO_AGENCY) {
            query = query + " INNER JOIN invoice levelTwo ON levelTwo.parent_id = levelOne.id "
                    + " INNER JOIN invoice levelThree ON levelThree.parent_id = levelTwo.id "
                    + " INNER JOIN company company ON company.id = levelThree.to_company_id ";
        } else {
            query += " INNER JOIN invoice levelTwo ON levelTwo.parent_id = levelOne.id "
                    + " INNER JOIN company company ON company.id = levelTwo.to_company_id ";
        }
        query += " WHERE levelOne.id = ?1 ";

        Query nativeQuery = em.createNativeQuery(query);
        nativeQuery.setParameter(1, invoice.getId());
        return nativeQuery.getResultList();
    }

    public List<InvoiceSalesLedger> filter(InvoiceFilter invoiceFilter, boolean isReport) {
        Contract contract;
        TimeSheet timeSheet;

        if (Authorizer.isAgency() && !BeanValidator.isValidString(invoiceFilter.getAgencyId())) {
            invoiceFilter.setAgencyId(Authorizer.getLoggedInUserCompanyId());
        }
        if (Authorizer.isAgency() && !BeanValidator.isValidString(invoiceFilter.getAgencyCompanyName())) {
            invoiceFilter.setAgencyCompanyName(companyRepository.findById(Authorizer.getLoggedInUserCompanyId()).get().getName());
        }

        List<Invoice> invoices = getFilter(invoiceFilter, isReport);

        List<InvoiceSalesLedger> invoiceSummaryList = new ArrayList<>();

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED) {
                contract = invoice.getInvoiceSourceMappings().getContract();
                timeSheet = invoice.getInvoiceSourceMappings().getTimeSheet();
            } else {
                contract = null;
                timeSheet = null;
            }

            contract = contract == null || !contract.isActive() ? null : contract;
            timeSheet = timeSheet == null || !timeSheet.isActive() ? null : timeSheet;

            String clientCompanyNameAndJobTitle = "-";
            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED)
                clientCompanyNameAndJobTitle = (contract != null ? contract.getClientCompany().getName() + "(" +
                        contract.getJobTitle() + ")" : "-");
            else if (invoice.getInvoiceType() == InvoiceType.MANUAL_ONEPS_TO_CLIENT)
                clientCompanyNameAndJobTitle = invoice.getToCompany().getName();

            String generatedFor = "-";
            if (timeSheet != null)
                generatedFor = timeSheet.getId();
            else
                generatedFor = invoice.getDescription();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

            String agencyCompanyName = "-";
            if (contract != null && (invoice.getInvoiceType() == InvoiceType.AGENCY_TO_CLIENT ||
                    invoice.getInvoiceType() == InvoiceType.ONEPS_TO_CLIENT ||
                    invoice.getInvoiceType() == InvoiceType.ONEPS_TO_AGENCY))
                agencyCompanyName = contract.getAgencyCompany().getName();
            else if (invoice.getInvoiceType() == InvoiceType.MANUAL_AGENCY_TO_CLIENT)
                agencyCompanyName = invoice.getFromCompanyName();
            else if (invoice.getInvoiceType() == InvoiceType.MANUAL_ONEPS_TO_AGENCY)
                agencyCompanyName = invoice.getToCompanyName();

            InvoiceSalesLedger invoiceSummary = new InvoiceSalesLedger(invoice.getId(), invoice.getCreatedOn(),
                    invoice.getFromCompanyName(), invoice.getToCompanyName(), clientCompanyNameAndJobTitle,
                    generatedFor, invoice.getInvoiceAmount(), invoice.getVatAmount(), invoice.getTotalAmount(),
                    invoice.getPayByDate() != null ? dateFormat.format(invoice.getPayByDate()) : "-",
                    invoice.getStatus().getDescription(), invoice.getCurrencyTaxPercentage().getCurrency().getCurrencySymbol(),
                    agencyCompanyName, invoice.getInvoiceType().getId());

            invoiceSummaryList.add(invoiceSummary);
        }

        return invoiceSummaryList;
    }

    private void applyUserRestrictions(List<Predicate> predicates, CriteriaBuilder builder, Root<Invoice> root, Join<Invoice, Company> invoiceFromCompanyJoin, Join<Invoice, Company> invoiceToCompanyJoin) {
        if (Authorizer.isNot1PSUser()) {
            Predicate fromCompany = builder.equal(invoiceFromCompanyJoin.get(Company_.id), Authorizer.getLoggedInUserCompanyId());
            Predicate toCompany = builder.equal(invoiceToCompanyJoin.get(Company_.id), Authorizer.getLoggedInUserCompanyId());
            Predicate company = builder.or(fromCompany, toCompany);

            if (!Authorizer.isAgency()) {
                Join<Invoice, InvoiceSourceMapping> invoiceInvoiceSourceMappingJoin = root.join(Invoice_.invoiceSourceMappings, JoinType.LEFT);
                Join<InvoiceSourceMapping, Contract> invoiceContractJoin = invoiceInvoiceSourceMappingJoin.join(InvoiceSourceMapping_.contract, JoinType.LEFT);

                if (Authorizer.isContractor()) {
                    Join<Contract, User> contractUserJoin = invoiceContractJoin.join(Contract_.contractor, JoinType.LEFT);
                    Predicate contractor = builder.equal(contractUserJoin.get(User_.id), Authorizer.getLoggedInUserId());
                    predicates.add(contractor);
                } else if (Authorizer.isInvoiceContactManager() || Authorizer.isRecruitingManager() || Authorizer.isClientFullApprover() || Authorizer.isClientAdmin()) {
                    Join<Invoice, InvoiceAdhocPermClientPermission> clientPermissionJoin = root.join(Invoice_.invoiceAdhocPermClientPermission, JoinType.LEFT);
                    Join<InvoiceAdhocPermClientPermission, User> clientUserPermissionJoin = clientPermissionJoin.join(InvoiceAdhocPermClientPermission_.user, JoinType.LEFT);
                    if (Authorizer.isInvoiceContactManager()) {
                        Join<Contract, Company> contractClientCompany = invoiceContractJoin.join(Contract_.clientCompany, JoinType.LEFT);
                        Join<Company, User> contractClientCompanyUser = contractClientCompany.join(Company_.users, JoinType.LEFT);
                        Join<User, Role> contractClientCompanyUserRole = contractClientCompanyUser.join(User_.roles, JoinType.LEFT);
                        Predicate contractClientCompanyInvoiceContact = builder.and(builder.equal(contractClientCompanyUser.get(User_.id), Authorizer.getLoggedInUserId()),
                                builder.equal(contractClientCompanyUserRole.get(Role_.role), RoleType.ROLE_INVOICE_CONTACT_MANAGER));
                        Predicate invoiceContactManager = builder.or(contractClientCompanyInvoiceContact,
                                builder.equal(clientUserPermissionJoin.get(User_.id), Authorizer.getLoggedInUserId()));
                        predicates.add(invoiceContactManager);
                    } else if (Authorizer.isRecruitingManager()) {
                        Join<Contract, User> contractUserJoin = invoiceContractJoin.join(Contract_.recruitingManager, JoinType.LEFT);
                        Predicate recruitingManager = builder.or(builder.equal(contractUserJoin.get(User_.id), Authorizer.getLoggedInUserId()),
                                builder.equal(clientUserPermissionJoin.get(User_.id), Authorizer.getLoggedInUserId()));
                        predicates.add(recruitingManager);
                    } else if (Authorizer.isClientFullApprover()) {
                        Join<Contract, User> contractRecruitingManager = invoiceContractJoin.join(Contract_.recruitingManager, JoinType.LEFT);
                        Join<Contract, User> contractInvoiceManager = invoiceContractJoin.join(Contract_.invoiceContactManager, JoinType.LEFT);
                        Predicate clientFullApprover = builder.or(builder.equal(contractRecruitingManager.get(User_.id), Authorizer.getLoggedInUserId()),
                                builder.equal(contractInvoiceManager.get(User_.id), Authorizer.getLoggedInUserId()),
                                builder.equal(clientUserPermissionJoin.get(User_.id), Authorizer.getLoggedInUserId()));
                        predicates.add(clientFullApprover);
                    } else if (Authorizer.isClientAdmin()) {
                        Join<Contract, User> contractRecruitingManager = invoiceContractJoin.join(Contract_.recruitingManager, JoinType.LEFT);
                        Predicate clientFullApprover = builder.or(builder.equal(contractRecruitingManager.get(User_.id), Authorizer.getLoggedInUserId()),
                                builder.equal(clientUserPermissionJoin.get(User_.id), Authorizer.getLoggedInUserId()));
                        predicates.add(clientFullApprover);
                    }
                }
            }
            predicates.add(company);
        }
    }

    private void applyCashFlow(List<Predicate> predicates, InvoiceFilter invoiceFilter, CriteriaBuilder builder,
                               Root<Invoice> root, Join<Invoice, Company> invoiceFromCompanyJoin, Join<Invoice, Company> invoiceToCompanyJoin) {
        if (invoiceFilter.getOverDueIn() == OverDueType.LESS_THAN_ONE_WEEK.getId())
            predicates.add(builder.between(root.get(Invoice_.payByDate), BeanValidator.getDateByGivenWeek(0),
                    BeanValidator.getDateByGivenWeek(1)));
        else if (invoiceFilter.getOverDueIn() == OverDueType.ONE_TO_TWO_WEEK.getId())
            predicates.add(builder.between(root.get(Invoice_.payByDate), BeanValidator.getDateByGivenWeek(1),
                    BeanValidator.getDateByGivenWeek(2)));
        else if (invoiceFilter.getOverDueIn() == OverDueType.TWO_TO_THREE_WEEK.getId())
            predicates.add(builder.between(root.get(Invoice_.payByDate), BeanValidator.getDateByGivenWeek(2),
                    BeanValidator.getDateByGivenWeek(3)));
        else if (invoiceFilter.getOverDueIn() == OverDueType.THREE_TO_FOUR_WEEK.getId())
            predicates.add(builder.between(root.get(Invoice_.payByDate), BeanValidator.getDateByGivenWeek(3),
                    BeanValidator.getDateByGivenWeek(4)));
        else
            predicates.add(builder.greaterThanOrEqualTo(root.get(Invoice_.payByDate), BeanValidator.getDateByGivenWeek(4)));

        predicates.add(builder.equal(root.get(Invoice_.status), InvoiceFormStatus.INITIATED.getId()));

        if (invoiceFilter.getCashFlowType() == CashFlowType.CASH_IN.getId())
            predicates.add(builder.equal(invoiceFromCompanyJoin.get(Company_.id.getName()), Authorizer.getLoggedInUserCompanyId()));
        else if (invoiceFilter.getCashFlowType() == CashFlowType.CASH_OUT.getId())
            predicates.add(builder.equal(invoiceToCompanyJoin.get(Company_.id.getName()), Authorizer.getLoggedInUserCompanyId()));
    }

    private List<Predicate> getLastMonthInvoices(InvoiceFilter invoiceFilter, CriteriaBuilder builder, List<Predicate> predicates, Root<Invoice> root, boolean isReport) {
        if (BeanValidator.isValidDate(invoiceFilter.getFromDate()) && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            Predicate fromDate = builder.greaterThanOrEqualTo(root.get(Invoice_.createdOn), BeanValidator.getDateWithoutTime(invoiceFilter.getFromDate()));
            Predicate toDate = builder.lessThanOrEqualTo(root.get(Invoice_.createdOn), BeanValidator.getDateWithoutTime(invoiceFilter.getToDate()));
            predicates.add(builder.and(fromDate, toDate));
        } else if (!Authorizer.isAgency() && BeanValidator.isValidDate(invoiceFilter.getFromDate())) {
            predicates.add(builder.between(root.get(Invoice_.createdOn), BeanValidator.getDateWithoutTime(invoiceFilter.getFromDate()), BeanValidator.getDateByAddingByThirty(invoiceFilter.getFromDate())));
        } else if (!Authorizer.isAgency() && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            predicates.add(builder.between(root.get(Invoice_.createdOn), BeanValidator.getDateBySubtractByThirty(invoiceFilter.getToDate()), BeanValidator.getDateWithoutTime(invoiceFilter.getToDate())));
        } else if (Authorizer.isAgency() && BeanValidator.isValidDate(invoiceFilter.getFromDate())) {
            predicates.add(builder.between(root.get(Invoice_.createdOn), BeanValidator.getDateWithoutTime(invoiceFilter.getFromDate()), BeanValidator.getDateByAddingByThreeMonth(invoiceFilter.getFromDate())));
        } else if (Authorizer.isAgency() && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            predicates.add(builder.between(root.get(Invoice_.createdOn), BeanValidator.getDateBySubtractingByThreeMonth(invoiceFilter.getToDate()), BeanValidator.getDateWithoutTime(invoiceFilter.getToDate())));
        } else if (isReport) {
            if (!BeanValidator.isValidDate(invoiceFilter.getFromDate()) && !BeanValidator.isValidDate((invoiceFilter.getToDate())) && !BeanValidator.isValidString(invoiceFilter.getAgencyCompanyName()) && !BeanValidator.isValidString(invoiceFilter.getFromCompanyName())
                    && !BeanValidator.isValidString(invoiceFilter.getToCompanyName())) {
                Predicate fromDate = builder.greaterThanOrEqualTo(root.get(Invoice_.createdOn), BeanValidator.getMonthStartDate(new Date()));
                Predicate toDate = builder.lessThanOrEqualTo(root.get(Invoice_.createdOn), new Date());
                predicates.add(builder.and(fromDate, toDate));
            } else if (!BeanValidator.isValidDate(invoiceFilter.getFromDate()) && !BeanValidator.isValidDate((invoiceFilter.getToDate()))
                    && (BeanValidator.isValidString(invoiceFilter.getAgencyCompanyName()) || BeanValidator.isValidString(invoiceFilter.getFromCompanyName())
                    || BeanValidator.isValidString(invoiceFilter.getToCompanyName()))) {
                Predicate fromDate = builder.greaterThanOrEqualTo(root.get(Invoice_.createdOn), BeanValidator.getMonthStartDate(new Date()));
                Predicate toDate = builder.lessThanOrEqualTo(root.get(Invoice_.createdOn), new Date());
                predicates.add(builder.and(fromDate, toDate));
            }
        }
        return predicates;
    }

    private List<Integer> getInvoiceType() {
        return Arrays.asList(InvoiceType.CONTRACTOR_TO_AGENCY.getId(), InvoiceType.AGENCY_TO_CLIENT.getId(),
                InvoiceType.ONEPS_TO_AGENCY.getId(), InvoiceType.CONTRACTOR_TO_CLIENT.getId(), InvoiceType.AGENCY_TO_ONEPS.getId(),
                InvoiceType.ONEPS_TO_CLIENT.getId(), InvoiceType.EXPENSES_ONEPS_TO_CLIENT.getId(), InvoiceType.EXPENSES_AGENCY_TO_CLIENT.getId());
    }

    private List<Integer> getManualInvoiceType() {
        return Arrays.asList(InvoiceType.MANUAL_AGENCY_TO_CLIENT.getId(), InvoiceType.MANUAL_ONEPS_TO_CLIENT.getId(),
                InvoiceType.MANUAL_ONEPS_TO_AGENCY.getId(), InvoiceType.MANUAL_AGENCY_TO_ONEPS.getId(),
                InvoiceType.CREDIT_NOTE.getId(), InvoiceType.MANUAL_CONTRACTOR_TO_AGENCY.getId());
    }

    private List<Integer> getUnpaidInvoice() {
        return Arrays.asList(InvoiceFormStatus.INITIATED.getId(), InvoiceFormStatus.OVERDUE.getId());
    }

    private List<Integer> getCreditLimitInvoiceType() {
        return Arrays.asList(InvoiceType.MANUAL_ONEPS_TO_CLIENT.getId(), InvoiceType.CREDIT_NOTE.getId(), InvoiceType.ONEPS_TO_CLIENT.getId(), InvoiceType.EXPENSES_ONEPS_TO_CLIENT.getId(), InvoiceType.AGENCY_TO_CLIENT.getId());
    }

    private boolean getInvoiceTypesToShownNegativeValues(Invoice invoice) {
        return invoice.getInvoiceType().getId() == InvoiceType.CONTRACTOR_TO_AGENCY.getId() ||
                invoice.getInvoiceType().getId() == InvoiceType.ONEPS_TO_AGENCY.getId();
    }

    private String applyDateFilter(String query, InvoiceFilter invoiceFilter) {
        if (BeanValidator.isValidDate(invoiceFilter.getFromDate()) && BeanValidator.isValidDate(invoiceFilter.getToDate())) {
            query += " AND date_format(a.created_on,'%Y-%m-%d') Between ?2 AND ?3 ";
        }
        return query;
    }

    public Long filterCount(InvoiceFilter filter, boolean isReport) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Invoice> root = query.from(Invoice.class);

        Join<Invoice, Company> invoiceFromCompanyJoin = root.join(Invoice_.fromCompany, JoinType.LEFT);
        Join<Invoice, Company> invoiceToCompanyJoin = root.join(Invoice_.toCompany, JoinType.LEFT);

        List<Predicate> predicates = getPredicates(filter, criteriaBuilder, root, invoiceFromCompanyJoin, invoiceToCompanyJoin, isReport);

        predicates.add(criteriaBuilder.isTrue(root.get(Invoice_.isActive)));
        predicates.add(criteriaBuilder.isTrue(invoiceFromCompanyJoin.get(Company_.isActive)));
        predicates.add(criteriaBuilder.isTrue(invoiceToCompanyJoin.get(Company_.isActive)));

        query = query.select(criteriaBuilder.count(root).alias("count"));

        if (!predicates.isEmpty())
            query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<Long> countQuery = em.createQuery(query);
        return countQuery.getSingleResult();
    }

    public List<ClientCreditReportDTO> clientCreditStatementReport(InvoiceFilter invoiceFilter, boolean isReport) {
        Contract contract;
        TimeSheet timeSheet;
        Expenses expenses;
        boolean isFurtherProcess = true;
        List<Invoice> invoices = (List<Invoice>) getByFilter(invoiceFilter, isReport,isFurtherProcess);

        List<ClientCreditReportDTO> clientCreditReport = new ArrayList<>();

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED) {
                contract = invoice.getInvoiceSourceMappings().getContract();
                timeSheet = invoice.getInvoiceSourceMappings().getTimeSheet();
                expenses = invoice.getInvoiceSourceMappings().getExpenses();
            } else {
                contract = null;
                timeSheet = null;
                expenses = null;
            }

            contract = contract == null || !contract.isActive() ? null : contract;
            timeSheet = timeSheet == null || !timeSheet.isActive() ? null : timeSheet;
            expenses = expenses == null || !expenses.isActive() ? null : expenses;

            String generatedFor = "-";
            if (timeSheet == null && expenses != null)
                generatedFor = expenses.getId();
            else if (timeSheet != null)
                generatedFor = timeSheet.getId();
            else if (timeSheet == null && expenses == null)
                generatedFor = invoice.getDescription();

            String clientCompanyNameAndJobTitle = "-";

            if (invoice.getInvoiceGenerationType() == InvoiceGenerationType.AUTOMATED)
                clientCompanyNameAndJobTitle = (contract != null ? contract.getClientCompany().getName() + "(" +
                        contract.getJobTitle() + ")" : "-");
            else
                switch (invoice.getInvoiceType()) {
                    case MANUAL_CONTRACTOR_TO_AGENCY:
                    case MANUAL_AGENCY_TO_ONEPS:
                        clientCompanyNameAndJobTitle = getClientNameFromManualContractorToAgencyInvoice(invoice).get(0);
                        break;
                    case MANUAL_AGENCY_TO_CLIENT:
                    case MANUAL_ONEPS_TO_CLIENT:
                        clientCompanyNameAndJobTitle = invoice.getToCompany().getName();
                        break;
                    case MANUAL_ONEPS_TO_AGENCY:
                        clientCompanyNameAndJobTitle = invoice.getParentInvoice() != null ? invoice.getParentInvoice().getToCompany().getName() : "-";
                        break;
                }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            ClientCreditReportDTO clientCreditReportDTO = new ClientCreditReportDTO(invoice.getId(), dateFormat.format(invoice.getCreatedOn()), clientCompanyNameAndJobTitle,
                    generatedFor,invoice.getInvoiceAmount(), invoice.getVatAmount(), invoice.getTotalAmount(), invoice.getStatus().getDescription(),
                    invoice.getPayByDate() != null ? dateFormat.format(invoice.getPayByDate()) : "-",invoice.getCurrencyTaxPercentage().getCurrency().getCurrencySymbol());
            clientCreditReport.add(clientCreditReportDTO);
        }
        return clientCreditReport;
    }
}