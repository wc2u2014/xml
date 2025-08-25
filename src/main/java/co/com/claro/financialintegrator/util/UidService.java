/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.com.claro.financialintegrator.util;

import co.com.claro.financialintegrator.domain.UidServiceResponse;

/**
 *
 * @author castr
 */
public class UidService {

    public static UidServiceResponse generateUid() {
      
        long uid = java.util.concurrent.ThreadLocalRandom.current().nextLong();
        
      
        UidServiceResponse response = new UidServiceResponse();
        response.setUid(String.valueOf(uid));
        
        return response;
    }
}
