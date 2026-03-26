package com.globalbooks.catalog;

import com.globalbooks.catalog.exception.BookNotFoundException;
import com.globalbooks.catalog.exception.BookNotFoundFaultInfo;
import com.globalbooks.catalog.model.Book;
import com.globalbooks.catalog.model.PriceResponse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.HandlerChain;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalBooks CatalogService - SOAP Web Service Implementation
 *
 * Exposes three operations:
 *   - getBookById(bookId)  -> Book
 *   - getBookPrice(bookId) -> PriceResponse
 *   - searchBooks(keyword) -> List<Book>
 *
 * Contract-first: WSDL is pre-defined in WEB-INF/wsdl/catalog.wsdl
 *
 * Q5: Service annotations, sun-jaxws.xml config, web.xml config
 */
@WebService(
    serviceName     = "CatalogService",
    portName        = "CatalogPort",
    targetNamespace = "http://catalog.globalbooks.com/v1",
    wsdlLocation    = "WEB-INF/wsdl/catalog.wsdl",
    endpointInterface = "com.globalbooks.catalog.CatalogPortType"
)
@HandlerChain(file = "handlers.xml")
public class CatalogServiceImpl implements CatalogPortType {

    /**
     * In-memory catalog – simulates a database.
     * In production this would be replaced with a JPA repository or external DB.
     */
    private static final Map<String, Book> CATALOG = new HashMap<>();

    static {
        CATALOG.put("B001", new Book("B001", "Clean Code",
                "Robert C. Martin", "978-0132350884",
                new BigDecimal("31.99"), "USD", "Programming"));

        CATALOG.put("B002", new Book("B002", "Design Patterns",
                "Gang of Four", "978-0201633610",
                new BigDecimal("44.99"), "USD", "Architecture"));

        CATALOG.put("B003", new Book("B003", "The Pragmatic Programmer",
                "David Thomas, Andrew Hunt", "978-0135957059",
                new BigDecimal("39.99"), "USD", "Programming"));

        CATALOG.put("B004", new Book("B004", "Microservices Patterns",
                "Chris Richardson", "978-1617294549",
                new BigDecimal("49.99"), "USD", "Architecture"));

        CATALOG.put("B005", new Book("B005", "Clean Architecture",
                "Robert C. Martin", "978-0134494166",
                new BigDecimal("34.99"), "USD", "Architecture"));

        CATALOG.put("B006", new Book("B006", "Domain-Driven Design",
                "Eric Evans", "978-0321125217",
                new BigDecimal("55.99"), "USD", "Architecture"));
    }

    /**
     * Retrieves full book details by ID.
     *
     * @param bookId unique book identifier (e.g. "B001")
     * @return Book object with all fields populated
     * @throws BookNotFoundException if no book exists with the given ID
     */
    @Override
    @WebMethod(operationName = "getBookById",
               action = "http://catalog.globalbooks.com/v1/getBookById")
    @RequestWrapper(localName = "getBookByIdRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "getBookByIdResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    public Book getBookById(@WebParam(name = "bookId", targetNamespace = "http://catalog.globalbooks.com/types/v1") String bookId)
            throws BookNotFoundException {

        if (bookId == null || bookId.trim().isEmpty()) {
            throw new BookNotFoundException(
                "Book ID must not be empty",
                new BookNotFoundFaultInfo("", "Book ID must not be empty")
            );
        }

        Book book = CATALOG.get(bookId.trim());
        if (book == null) {
            throw new BookNotFoundException(
                "Book not found: " + bookId,
                new BookNotFoundFaultInfo(bookId, "No book exists with ID: " + bookId)
            );
        }
        return book;
    }

    /**
     * Returns price and currency for a given book.
     * Used by the BPEL PlaceOrder process in a loop for each order item.
     *
     * @param bookId unique book identifier
     * @return PriceResponse containing bookId, price, currency
     * @throws BookNotFoundException if no book exists with the given ID
     */
    @Override
    @WebMethod(operationName = "getBookPrice",
               action = "http://catalog.globalbooks.com/v1/getBookPrice")
    @RequestWrapper(localName = "getBookPriceRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "getBookPriceResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    public PriceResponse getBookPrice(@WebParam(name = "bookId", targetNamespace = "http://catalog.globalbooks.com/types/v1") String bookId)
            throws BookNotFoundException {

        Book book = getBookById(bookId);
        return new PriceResponse(book.getBookId(), book.getPrice(), book.getCurrency());
    }

    /**
     * Searches books by title keyword (case-insensitive).
     *
     * @param keyword search term to match against book titles
     * @return list of matching books (empty list if none found)
     */
    @Override
    @WebMethod(operationName = "searchBooks",
               action = "http://catalog.globalbooks.com/v1/searchBooks")
    @RequestWrapper(localName = "searchBooksRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "searchBooksResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    public List<Book> searchBooks(@WebParam(name = "keyword", targetNamespace = "http://catalog.globalbooks.com/types/v1") String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(CATALOG.values());
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        return CATALOG.values().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(lowerKeyword)
                          || b.getAuthor().toLowerCase().contains(lowerKeyword)
                          || (b.getCategory() != null &&
                              b.getCategory().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }
}
