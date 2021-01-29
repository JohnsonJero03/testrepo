package uk.org.oners.oneps.db.repository;

import uk.org.oners.oneps.db.model.User;
import uk.org.oners.oneps.restmodel.*;
import uk.org.oners.oneps.restmodel.filter.InvoiceFilter;

import java.util.List;

public interface InvoiceRepositoryCustom<T> {

    List<T> getByFilter(InvoiceFilter invoiceFilter, boolean isReport,boolean isFurtherProcess);

//    List<Invoice> getInvoiceByFilter(InvoiceFilter invoiceFilter,boolean isReport);
    List<InvoiceStatusCount> getByInvoiceCount(InvoiceFilter invoiceFilter);
    List<InvoiceSummary> findByFilter(InvoiceFilter invoiceFilter,  boolean isReport);
    List<InvoiceSalesLedger> filter(InvoiceFilter invoiceFilter, boolean isReport);
    Long filterCount(InvoiceFilter filter, boolean isReport);
    List<AgencyCommissionReport> agencyCommissionReport(InvoiceFilter invoiceFilter,boolean isReport);
    List<IntermediaryReportDTO> intermediaryReport(InvoiceFilter invoiceFilter, User user);
    List<WeeklyPaymentReport> weeklyPaymentReport(InvoiceFilter invoiceFilter);
    List<ClientCreditReportDTO> clientCreditStatementReport(InvoiceFilter invoiceFilter, boolean isReport);
}
