package uk.org.oners.oneps.restmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.org.oners.oneps.constants.InvoiceFormStatus;
import uk.org.oners.oneps.constants.InvoiceGenerationType;
import uk.org.oners.oneps.db.model.Invoice;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor

public class InvoiceSearchDTO<T> {



    private String invoiceId;

    private String fromCompany;

    private String toCompany;

    private String agencyName;

    private String currencyCode;

    private BigDecimal totalAmount;

    private int invoiceGenerationType;

    private Date dueDate;

    private Date createdOn;

    private int status;

//    private Invoice invoice;

//    public InvoiceSearchDTO(String invoiceId, String fromCompany, String toCompany, String agencyName, String currencyCode, BigDecimal totalAmount,
//                            int invoiceGenerationType, Date dueDate, Date createdOn, int status) {
//        this.invoiceId = invoiceId;
//        this.fromCompany = fromCompany;
//        this.toCompany = toCompany;
//        this.agencyName = agencyName;
//        this.currencyCode = currencyCode;
//        this.totalAmount = totalAmount;
//        this.invoiceGenerationType = invoiceGenerationType;
//        this.dueDate = dueDate;
//        this.createdOn = createdOn;
//        this.status = status;
//    }

    public InvoiceFormStatus getStatus() {
        return InvoiceFormStatus.getInvoiceStatus(this.status);
    }

    public InvoiceGenerationType getInvoiceGenerationType() {
        return InvoiceGenerationType.getInvoiceGenerationType(this.invoiceGenerationType);
    }



}
