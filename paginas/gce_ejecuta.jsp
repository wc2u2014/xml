<%-- 
    Document   : gce_ejecuta
    Created on : 25/05/2014, 07:58:23 PM
    Author     : yoveri
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
       
        
    <%-- start web service invocation --%><hr/>
    <%
    String ls_banco    = "1";//(String)request.getParameter("ps_banco");
    String ls_sucursal = "1";//(String)request.getParameter("ps_sucursal");
    String ls_canal = "1";//(String)request.getParameter("ps_canal");
    String ls_fecha = "1";//(String)request.getParameter("ps_fecha");
    String ls_comprobante = " ";//(String)request.getParameter("ps_comprobante");
    String ls_certificado = (String)request.getParameter("ps_certificado");
    
    try {
	wscargareporte.MetodosService service = new wscargareporte.MetodosService();
	wscargareporte.Metodos port = service.getMetodosPort();
	 // TODO initialize WS operation arguments here
	java.lang.String banco = ls_banco;
	java.lang.String sucursal = ls_sucursal;
	java.lang.String canal = ls_canal;
	java.lang.String fechaHora =ls_fecha;
	java.lang.String comprobantePago = ls_comprobante;
	java.lang.String certificado = ls_certificado;
	// TODO process result here
	wscargareporte.RetDatos result = port.ejecutaCertificado(banco, sucursal, canal, fechaHora, comprobantePago, certificado);
	out.println("Result = "+result);
        
        response.setContentType("application/pdf");                 
        ServletOutputStream ouputStream = response.getOutputStream();
        System.out.println(result.getGbReporte());
        ouputStream.write(result.getGbReporte());
        ouputStream.flush();
        ouputStream.close();
        response.flushBuffer();
        
        
    } catch (Exception ex) {
	// TODO handle custom exceptions here
    }
    %>
    <%-- end web service invocation --%><hr/>
</html>
