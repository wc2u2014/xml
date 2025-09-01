<%-- 
    Document   : index
    Created on : 20-jun-2014, 12:03:00
    Author     : USUARIO
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
    <%
     String ls_accion    = (String)request.getParameter("ps_accion");
     
     %>
     
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
         <script type="text/javascript">

function ejecutar(accion){

if (accion == "C")
document.location = "faces/App_BuscarCertificado.xhtml";
else
    document.location = "faces/App_BuscarPlaca.xhtml";

}

</script>

    
<body onload="ejecutar('<%=ls_accion%>')">
        
    </body>


     
</html>
