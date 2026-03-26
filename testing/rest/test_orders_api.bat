@echo off
:: ============================================================
:: GlobalBooks – REST API Test Script (curl)
:: Q7: OrdersService endpoint tests
:: Q13: OAuth2 JWT token flow via Spring Authorization Server
:: ============================================================

echo.
echo ============================================================
echo   Step 1: Get OAuth2 JWT Token from Spring Authorization Server
echo   (client_credentials grant – no user login needed)
echo ============================================================
echo.
echo Running: POST http://localhost:9000/oauth2/token
echo.
curl -s -X POST http://localhost:9000/oauth2/token ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "grant_type=client_credentials" ^
  -d "client_id=orders-client" ^
  -d "client_secret=secret" ^
  -d "scope=read write"
echo.
echo.
echo --- Copy the access_token value from the JSON above ---
echo --- Then paste it below when prompted ---
echo.
set /p TOKEN="Paste access_token here: "

echo.
echo ============================================================
echo   Step 2: POST /api/v1/orders – Create an Order (201)
echo ============================================================
curl -s -w "\nHTTP Status: %%{http_code}\n" -X POST http://localhost:8081/api/v1/orders ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -d "{\"customerId\":\"C1001\",\"items\":[{\"bookId\":\"B001\",\"quantity\":2},{\"bookId\":\"B003\",\"quantity\":1}],\"shippingAddress\":{\"street\":\"1 Book Lane\",\"city\":\"London\",\"country\":\"UK\",\"postalCode\":\"EC1A 1BB\"}}"
echo.

echo.
echo ============================================================
echo   Step 3: GET /api/v1/orders – List All Orders (200)
echo ============================================================
curl -s -w "\nHTTP Status: %%{http_code}\n" -X GET http://localhost:8081/api/v1/orders ^
  -H "Authorization: Bearer %TOKEN%"
echo.

echo.
echo ============================================================
echo   Step 4: GET /api/v1/orders/{id} – Retrieve Order (200)
echo ============================================================
echo Replace ORD-XXXXXXXX-001 with actual orderId from Step 2
set /p ORDERID="Enter orderId: "
curl -s -w "\nHTTP Status: %%{http_code}\n" -X GET http://localhost:8081/api/v1/orders/%ORDERID% ^
  -H "Authorization: Bearer %TOKEN%"
echo.

echo.
echo ============================================================
echo   Step 5: GET without token – Should return 401
echo ============================================================
curl -s -w "\nHTTP Status: %%{http_code}\n" -X GET http://localhost:8081/api/v1/orders
echo.
echo   Expected: HTTP 401 Unauthorized

echo.
echo ============================================================
echo   Step 6: POST with invalid body – Should return 400
echo ============================================================
curl -s -w "\nHTTP Status: %%{http_code}\n" -X POST http://localhost:8081/api/v1/orders ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -d "{\"customerId\":\"\",\"items\":[]}"
echo.
echo   Expected: HTTP 400 Bad Request with validation errors

echo.
echo ============================================================
echo   Step 7: Verify OIDC Discovery Endpoint
echo ============================================================
curl -s http://localhost:9000/.well-known/openid-configuration
echo.

echo.
echo ============================================================
echo   Step 8: Verify JWK Set (Public Keys)
echo ============================================================
curl -s http://localhost:9000/oauth2/jwks
echo.

echo.
echo ============================================================
echo   Tests Complete!
echo ============================================================
pause
