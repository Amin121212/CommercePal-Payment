<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head>
    <meta charset="ISO-8859-1">
    <title>Order</title>
</head>

<body>
    <h2>Order Ref :  ${OrderRef}</h2>
    <h2>Customer Name :  ${CustomerName}</h2>
    <h2>Order Date :  ${OrderDate}</h2>
    <h3>Product List</h3>
    <#assign accounts = orderItems>

    <table border="1">
        <tr>
            <td>Sub Order Ref</td>
            <td>Product Name</td>
            <td>No of Products</td>
        </tr>
       

        <#list accounts as account>
            <tr>
                <td th:text="${account.ItemOrderRef}">${account.ItemOrderRef}</td>
                <td th:text="${account.ProductName}">${account.ProductName}</td>
                <td th:text="${account.NoOfProduct}">${account.NoOfProduct}</td>
            </tr>
        </#list>
    </table>
</body>

</html>