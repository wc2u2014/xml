package co.com.claro.financialintegrator.ws.security;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

public class HeaderHandlerResolver implements HandlerResolver {
	
	final private String user;
	final private String pass;
	
	public HeaderHandlerResolver(String user, String pass) {
		this.user=user;
		this.pass=pass;
	}
	
	public List<Handler> getHandlerChain(PortInfo portInfo) {
		  List<Handler> handlerChain = new ArrayList<Handler>();

		  HeaderHandler hh = new HeaderHandler(user,pass);

		  handlerChain.add(hh);

		  return handlerChain;
		   }

}
