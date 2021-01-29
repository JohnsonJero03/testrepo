package uk.org.oners.oneps.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import uk.org.oners.oneps.constants.ContentType;
import uk.org.oners.oneps.constants.InvoiceFormStatus;
import uk.org.oners.oneps.constants.InvoiceGenerationType;
import uk.org.oners.oneps.constants.InvoiceType;
import uk.org.oners.oneps.db.model.Invoice;
import uk.org.oners.oneps.db.model.User;
import uk.org.oners.oneps.db.repository.InvoiceRepository;
import uk.org.oners.oneps.exception.ValidationFailedException;
import uk.org.oners.oneps.restmodel.CommentDTO;
import uk.org.oners.oneps.restmodel.InvoiceManualSettingDTO;
import uk.org.oners.oneps.restmodel.InvoiceSearchDTO;
import uk.org.oners.oneps.restmodel.InvoiceSummary;
import uk.org.oners.oneps.restmodel.filter.InvoiceFilter;
import uk.org.oners.oneps.restmodel.filter.Report;
import uk.org.oners.oneps.service.*;
import uk.org.oners.oneps.util.BeanValidator;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusService invoiceStatusService;
    private final InvoiceService invoiceService;
    private final UserService userService;
    private final ManualInvoiceService manualInvoiceService;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    @GetMapping(value = "/{id}")
    //@PostFilter ("filterObject.owner == authentication.name")
    public ResponseEntity findInvoices(@PathVariable String id) {

        Invoice invoice = invoiceService.getInvoice(id);

        if (!invoiceService.hasPermission(invoice) && invoice.isActive())
            throw new AccessDeniedException("Invalid user");

        if (!invoice.isActive() && !Authorizer.isAdmin())
            throw new IllegalArgumentException("INVOICE UNAVAILABLE");

        HashMap map = invoiceService.getCompanyLogoForInvoices(invoice);

        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @Transactional
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity search(@RequestBody InvoiceFilter filter) {
        List<InvoiceSearchDTO> contracts = invoiceRepository.getByFilter(filter, false,true);
        filter.setTotalElements(invoiceRepository.filterCount(filter, false));
        Report<InvoiceFilter, InvoiceSearchDTO> report = new Report<>();
        report.setContent(contracts);
        report.setFilterBy(filter);

        return new ResponseEntity<>(report, HttpStatus.OK);
    }

    /*@Transactional
    @PostMapping(value = "/{id}/received", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity receivedInvoice(@PathVariable String id, @RequestBody Map<String, Object> invoiceDetails) {

        String comments = (String) invoiceDetails.get("comments");

        Invoice invoice = invoiceRepository.findOne(id);

        if (invoice == null)
            throw new EntityNotFoundException("Invoice not found");

        String companyId = Authorizer.getLoggedInUserCompanyId();
        if(!(companyId.equals(invoice.getFromCompany().getId())) && !Authorizer.isAdmin())
            throw new AccessDeniedException("Invalid user");

        if(invoice.getStatus() == InvoiceFormStatus.RECEIVED)
            throw new IllegalStateException("Invalid status");

        User user = userRepository.findOne(Authorizer.getLoggedInUserId());

        invoiceStatusService.construct(invoice, InvoiceFormStatus.RECEIVED.getId(), user, comments);

        return new ResponseEntity<>(invoice, HttpStatus.OK);
    }*/

    @Transactional
    @PostMapping(value = "/{id}/paid", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole(T(uk.org.oners.oneps.constants.RoleType).ROLE_ADMIN)")
    public ResponseEntity paidInvoice(@PathVariable String id, @Valid @RequestBody CommentDTO commentDTO) {

        Invoice invoice = invoiceService.getInvoice(id);

        if (invoice == null)
            throw new EntityNotFoundException("Invoice not found");

        if (invoice.getStatus() == InvoiceFormStatus.PAID)
            throw new IllegalStateException("Invalid status");

        User user = userService.getUserDetails(Authorizer.getLoggedInUserId());

        invoiceStatusService.construct(invoice, InvoiceFormStatus.PAID.getId(), user, commentDTO.getComments());

        //Whenever 1ps to client invoice changed to paid status automatically it's parent invoice also changed to paid status.
        //Since Manual Adhoc Invoice doesn't have a parent invoice, this functionality will be called only Manual Permanent Placements as well as Agency 4 invoices
        if (invoice.getInvoiceType() == InvoiceType.ONEPS_TO_CLIENT || (invoice.getInvoiceGenerationType() == InvoiceGenerationType.PERM_PLACEMENT &&
                invoice.getInvoiceType() == InvoiceType.MANUAL_ONEPS_TO_CLIENT))
            invoiceStatusService.construct(invoice.getParentInvoice(), InvoiceFormStatus.PAID.getId(), user, invoice.getDescription());

        return new ResponseEntity<>(invoice, HttpStatus.OK);
    }

    @Transactional
    @GetMapping(value = "/{id}/dismiss")
    @PreAuthorize("hasAnyRole(T(uk.org.oners.oneps.constants.RoleType).ROLE_ADMIN)")
    public ResponseEntity dismissInvoice(@PathVariable String id) {

        Invoice invoice = invoiceService.getInvoice(id);

        User user = userService.getUserDetails(Authorizer.getLoggedInUserId());

        invoiceService.dismissContract(invoice, user);

        return new ResponseEntity<>(invoice, HttpStatus.OK);
    }

    @Transactional
    @PostMapping(value = "/generate/csv")
    public void generateCsv(@RequestBody InvoiceFilter invoiceFilter, HttpServletResponse response) {

        BeanValidator.setContentDisposition(response, "invoice.csv");
        response.setContentType(ContentType.CSV.getDescription());
        response.setCharacterEncoding("UTF-8");

        String header = "Invoice#,Date,Invoice from,Invoice to,Placement,Generated for,Invoice amount,VAT amount,Total amount,DueDate,status";

        try {
            PrintWriter out = response.getWriter();
            out.write(header);
            out.write("\n");
            List<InvoiceSummary> invoices = invoiceRepository.findByFilter(invoiceFilter, false);
            invoices.forEach((InvoiceSummary invoice) ->
            {
                out.write(invoice.toString());
                out.write("\n");
            });

        } catch (IOException e) {
            log.error("Error downloading file: {}", e);

        }
    }

    @Transactional
    @PostMapping(value = "/add")
    @PreAuthorize("hasAnyRole(T(uk.org.oners.oneps.constants.RoleType).ROLE_ADMIN)")
    public ResponseEntity add(@Valid @RequestBody InvoiceManualSettingDTO invoiceManualSettingDTO, BindingResult result) {

        manualInvoiceService.validateManualInvoice(invoiceManualSettingDTO, result);

        if (result.hasErrors())
            throw new ValidationFailedException(result);

        List<Invoice> invoice = manualInvoiceService.createManualInvoice(invoiceManualSettingDTO);

        return new ResponseEntity<>(invoice, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/{id}/matches")
    @PreAuthorize("hasAnyRole(T(uk.org.oners.oneps.constants.RoleType).ROLE_ADMIN)")
    public ResponseEntity findInvoice(@PathVariable String id) {
        final boolean isActiveFlag = true;
        List<Invoice> invoices = invoiceRepository.findByIdAndInvoiceGenerationTypeNotInAndIsActive(id, InvoiceGenerationType.CREDIT_NOTE.getId(), isActiveFlag);

        if (invoices.size() == 0)
            throw new IllegalArgumentException("Enter a valid Invoice id");

        return new ResponseEntity<>(invoices, HttpStatus.OK);
    }
}
