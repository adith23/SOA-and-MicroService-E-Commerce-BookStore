package com.globalbooks.catalog;

import com.globalbooks.catalog.exception.BookNotFoundException;
import com.globalbooks.catalog.model.Book;
import com.globalbooks.catalog.model.PriceResponse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.List;

/**
 * SEI (Service Endpoint Interface) for CatalogService.
 * Defines the SOAP contract — matches the portType in catalog.wsdl.
 */
@WebService(
    name            = "CatalogPortType",
    targetNamespace = "http://catalog.globalbooks.com/v1"
)
public interface CatalogPortType {

    @WebMethod(operationName = "getBookById")
    @RequestWrapper(localName = "getBookByIdRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "getBookByIdResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    Book getBookById(@WebParam(name = "bookId", targetNamespace = "http://catalog.globalbooks.com/types/v1") String bookId)
            throws BookNotFoundException;

    @WebMethod(operationName = "getBookPrice")
    @RequestWrapper(localName = "getBookPriceRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "getBookPriceResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    PriceResponse getBookPrice(@WebParam(name = "bookId", targetNamespace = "http://catalog.globalbooks.com/types/v1") String bookId)
            throws BookNotFoundException;

    @WebMethod(operationName = "searchBooks")
    @RequestWrapper(localName = "searchBooksRequest", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    @ResponseWrapper(localName = "searchBooksResponse", targetNamespace = "http://catalog.globalbooks.com/types/v1")
    List<Book> searchBooks(@WebParam(name = "keyword", targetNamespace = "http://catalog.globalbooks.com/types/v1") String keyword);
}
